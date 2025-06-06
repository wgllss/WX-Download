package com.wx.download.download

import android.os.Build
import com.wx.download.utils.HttpUtils
import com.wx.download.utils.WLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class WXDownloadCoroutineManager(private val downLoadFileBean: WXDownloadFileBean, val channel: Channel<WXState>, val stateHolder: StateHolder) {

    /**
     * 开始位置
     */
    private val startPos by lazy { LongArray(downLoadFileBean.fileAsyncNumb) }

    /**
     * 结束位置
     */
    private val endPos by lazy { LongArray(downLoadFileBean.fileAsyncNumb) }

    /**
     * 文件长度
     */
    private var fileLength: Long = -1

    /**
     * 文件下载的临时信息
     */
    private val tempFile by lazy { downLoadFileBean.tempFile }

    /**
     * 下载是否成功的标记
     */
    private var isLoadSuccess = false

    private val mis by lazy { "{下载:(${downLoadFileBean.fileSiteURL}"; }

    fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            if (startConnect()) {
                initTemp()
                // 2:分多个异步下载文件
                if (!isLoadSuccess) {
                    val fileAsynNum: Int = downLoadFileBean.fileAsyncNumb
                    WLog.i(this, this@WXDownloadCoroutineManager.mis + "开始")
                    val isRange: Boolean = downLoadFileBean.isRange
                    if (isRange) {
                        val sets = mutableSetOf<Deferred<Any>>()
                        for (i in 0 until fileAsynNum) {
                            val downloadDeferred = async(Dispatchers.IO) {
                                WXRealDownload(downLoadFileBean, channel, startPos[i], endPos[i], i, stateHolder).runDownload()
                            }
                            sets.add(downloadDeferred)
                        }
                        sets.forEach {
                            it.await()
                        }
                    } else {
                        val downloadDeferred = async {
                            WXRealDownload(downLoadFileBean, channel, stateHolder = stateHolder).runDownload()
                        }
                        downloadDeferred.await()
                    }
//                    WLog.e(this@WXDownloadCoroutineManager,"dffdffffffffffffffffd")

                    val file = downLoadFileBean.saveFile
                    // 删除临时文件
                    val downloadFileSize = file.length()
                    var msg = "失败"
                    if (downloadFileSize == this@WXDownloadCoroutineManager.fileLength) {
                        tempFile.delete() // 临时文件删除
                        msg = "成功"
                        downLoadFileBean.isDownSuccess = true // 下载成功
                        channel.send(stateHolder.succeed)
                        // 下载成功,处理解析文件
                    } else {
                        channel.send(stateHolder.failed)
                    }

                    val end = System.currentTimeMillis()
                    WLog.i(this, msg + "下载'${downLoadFileBean.fileSaveName}'花时：${(end - start).toDouble() / 1000}秒")
                } else {
                    downLoadFileBean.isDownSuccess = true // 下载成功
                    channel.send(stateHolder.succeed)
                    val end = System.currentTimeMillis()
                    WLog.i(this, "成功下载'${downLoadFileBean.fileSaveName}'花时：${(end - start).toDouble() / 1000}秒")
                }
            }
        }
    }

    private suspend fun startConnect(): Boolean {
        delay(300)
        var curNum = 0 // 重试计数器
        val tryNum = 1 // 重试的次数
        val waitTime = 500 // 等待500ms
        var connect = false
        // 将有3次的重试机会.大概花时3s
        while (curNum < tryNum && !connect) {
            if (curNum > 0) {
                WLog.e(this@WXDownloadCoroutineManager, this.mis + "第${curNum}次重试连接:")
                delay((waitTime * curNum).toLong())
            }
            connect = connect()
            curNum++
        }
        return connect
    }

    private suspend fun connect(): Boolean {
        var httpConnection: HttpURLConnection? = null
        try {
            val url = URL(downLoadFileBean.fileSiteURL)
            httpConnection = HttpUtils.getHttpURLConnection(url, 10000)?.apply {
                HttpUtils.setConHeader(this)
                connect()
            }
            httpConnection?.let {
                val responseCode = httpConnection.responseCode
                if (responseCode <= 400) {
                    fileLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        httpConnection.contentLengthLong // 设置下载长度
                    } else {
                        httpConnection.contentLength.toLong() // 设置下载长度
                    }
                    WLog.i(this, "$mis-请求返回fileLength:$fileLength")
                    if (fileLength == -1L) {
                        val inputstream = httpConnection.inputStream
                        val swapStream = ByteArrayOutputStream()
                        val buff = ByteArray(512)
                        var rc = 0
                        while ((inputstream.read(buff, 0, 512).also { rc = it }) > 0) {
                            swapStream.write(buff, 0, rc)
                        }
                        val b = swapStream.toByteArray()
                        fileLength = b.size.toLong()
                        WLog.i(this, "$mis-请求返回fileLength2:$fileLength")
                        inputstream.close()
                        swapStream.close()
                    }
                    downLoadFileBean.fileLength = fileLength
                    return true // 失败成功
                }
                WLog.i(this, "$mis-请求返回responseCode=$responseCode,连接失败")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            channel.send(stateHolder.failed)
            WLog.e(this, "$mis-请求连接${downLoadFileBean.fileSiteURL}异常:${e.message}")
        } finally {
            httpConnection?.disconnect() // 关闭连接
        }
        return false // 失败返回
    }

    private suspend fun initTemp() {
        var tempFileFos: RandomAccessFile? = null
        try {
            val file: File = downLoadFileBean.saveFile
            val fileAsyncNumb: Int = downLoadFileBean.fileAsyncNumb
            if (file.exists()) {
                val localFileSize = file.length() // 本地的文件大小
                if (localFileSize < this.fileLength || tempFile.exists()) { // 小于的开始断点下载
                    WLog.i(this, "重新断点续传..")
                    /** 线程数下载的大小  */
                    tempFileFos = RandomAccessFile(tempFile, "rw")
                    // 从临时文件读取断点位置
                    val num = tempFileFos.readInt()
                    WLog.i(this, "启动的任务数$num")
                    for (i in 0..<fileAsyncNumb) {
                        startPos[i] = tempFileFos.readLong() // 开始的位置
                        endPos[i] = tempFileFos.readLong() // 结束的位置
                    }
                } else {
                    isLoadSuccess = true
                }
            } else {
                // 目标文件不存在，则创建新文件
                file.createNewFile()
                tempFile.createNewFile()
                tempFileFos = RandomAccessFile(tempFile, "rw")
                val fileThreadSize = this.fileLength / fileAsyncNumb // 每个线程需要下载的大小
                tempFileFos.writeInt(fileAsyncNumb) // 首个写入线程数量
                for (i in 0..<fileAsyncNumb) {
                    startPos[i] = fileThreadSize * i
                    if (i == fileAsyncNumb - 1) {
                        endPos[i] = this.fileLength
                    } else {
                        endPos[i] = fileThreadSize * (i + 1) - 1
                    }
                    // end position
                    tempFileFos.writeLong(startPos[i])
                    // current position
                    tempFileFos.writeLong(endPos[i])
                }
            }
        } catch (e: Exception) {
            channel.send(stateHolder.failed)
            WLog.e(this, "${mis}异常：${e.message}")
        } finally {
            try {
                tempFileFos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}