package com.wx.download.download

import android.os.Build
import com.wx.download.utils.HttpUtils
import com.wx.download.utils.WLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class WXDownloadCoroutineManager(private val downLoadFileBean: WXDownloadFileBean, val channel: Channel<WXState>, val stateHolder: WXStateHolder) {

    /**
     * 开始位置
     */
    private val startPos by lazy { LongArray(downLoadFileBean.fileAsyncNumb) }

    /**
     * 结束位置
     */
    private val endPos by lazy { LongArray(downLoadFileBean.fileAsyncNumb) }

    /**
     * 文件下载的临时信息
     */
    private val tempFile: Array<File> by lazy { downLoadFileBean.tempFile }

    /**
     * 下载是否成功的标记
     */
    private var isLoadSuccess = false

    private val mis by lazy { "{下载:(${downLoadFileBean.fileSiteURL})"; }

    //存取文件长度
    private lateinit var lengthFile: RandomAccessFile


    suspend fun start() = coroutineScope {
        WLog.i(this, "${mis}开始 start ${Thread.currentThread().name}")
        val start = System.currentTimeMillis()
        if (isConnect()) {
            initTemp()
            WLog.i(this, "${mis} 分 ${downLoadFileBean.fileAsyncNumb} 异步任务 下载文件 ${Thread.currentThread().name}")
            // 2:分多个异步下载文件
            if (!isLoadSuccess) {
                val fileAsynNum = downLoadFileBean.fileAsyncNumb
                WLog.i(this, "${mis} 开始")
                val isRange = downLoadFileBean.isRange
                if (isRange) {
                    val sets = mutableSetOf<Deferred<Any>>()
                    for (i in 0 until fileAsynNum) {
                        val downloadDeferred = async(Dispatchers.IO) {
                            WXRangeDownload(downLoadFileBean, channel, startPos[i], endPos[i], i, stateHolder).runDownload()
                        }
                        sets.add(downloadDeferred)
                    }
                    sets.forEach { it.await() }
                } else {
                    val downloadDeferred = async(Dispatchers.IO) {
                        WXRangeDownload(downLoadFileBean, channel, stateHolder = stateHolder).runDownload()
                    }
                    downloadDeferred.await()
                }


                val file = downLoadFileBean.saveFile
                // 删除临时文件
                val downloadFileSize = file.length()
                var msg = "失败"
                if (downloadFileSize == downLoadFileBean.fileLength) {
                    msg = "成功"
                    downLoadFileBean.isDownSuccess = true // 下载成功
                    channel.send(stateHolder.succeed)
                    tempFile.forEach {
                        it.delete()
                    }// 临时文件删除
                    // 下载成功,处理解析文件
                } else {
                    if (downLoadFileBean.isAbortDownload) channel.send(stateHolder.pause)
                    else channel.send(stateHolder.failed)
                }
                val end = System.currentTimeMillis()
                WLog.i(this, "$msg 下载'${downLoadFileBean.fileSaveName}' 大小:${downloadFileSize / 1024 / 1024}M, 花时：${(end - start).toDouble() / 1000}秒")
            } else {
                WLog.i(this, "已经存在")
                downLoadFileBean.isDownSuccess = true // 下载成功
                channel.send(stateHolder.succeed)
                val end = System.currentTimeMillis()
                WLog.i(this, "成功下载'${downLoadFileBean.fileSaveName}'花时：${(end - start).toDouble() / 1000}秒")
                tempFile.forEach {
                    it.delete()
                }// 临时文件删除
            }
        }
    }

    private suspend fun isConnect(): Boolean {
        var curNum = 0 // 重试计数器
        val tryNum = 3 // 重试的次数
        val waitTime = 500 // 等待500ms
        var connect = false
        // 将有3次的重试机会.大概花时3s
        while (curNum < tryNum && !connect) {
            if (curNum > 0) {
                WLog.e(this@WXDownloadCoroutineManager, "${mis} 第${curNum}次重试连接:")
                delay((waitTime * curNum).toLong())
            }
            connect = connect()
            if (connect) {
                return true
            } else {
                curNum++
            }
        }
        return connect
    }

    private suspend fun connect(): Boolean {
        if (downLoadFileBean.lengthFile.exists()) {
            lengthFile = RandomAccessFile(downLoadFileBean.lengthFile, "rw")
            downLoadFileBean.fileLength = lengthFile.readLong()
            downLoadFileBean.isRange = lengthFile.readBoolean()
            downLoadFileBean.fileAsyncNumb = lengthFile.readInt()
            lengthFile.close()
            return true
        }

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
                    val acceptRanges = it.getHeaderField("Accept-Ranges")
                    downLoadFileBean.isRange = ("bytes" == acceptRanges)
                    WLog.i(this, "$mis-支持断点续传:${downLoadFileBean.isRange}")
                    var fileLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        httpConnection.contentLengthLong // 设置下载长度
                    } else {
                        httpConnection.contentLength.toLong() // 设置下载长度
                    }
                    WLog.i(this, "$mis-请求返回fileLength:$fileLength")
                    if (fileLength == -1L) {
                        val inputstream = httpConnection.inputStream
                        val buff = ByteArray(512)
                        var count = 0L
                        var rc = 0
                        while ((inputstream.read(buff, 0, 512).also { rc = it }) > 0) {
                            count += rc
                        }
                        fileLength = count
                        WLog.i(this, "$mis-请求返回fileLength2:$fileLength")
                        inputstream.close()
                    }

                    downLoadFileBean.fileLength = fileLength

                    lengthFile = RandomAccessFile(downLoadFileBean.lengthFile, "rw")
                    lengthFile.writeLong(fileLength) //存取文件长度
                    lengthFile.writeBoolean(downLoadFileBean.isRange)//存取文件服务器是否支持断点续传
                    if (fileLength < 1L * 1024 * 1024) {
                        //如果文件大小小于1M 默认就只分一块下载
                        downLoadFileBean.fileAsyncNumb = 1
                    }
                    lengthFile.writeInt(downLoadFileBean.fileAsyncNumb)
                    lengthFile.close()
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

    private suspend fun initTemp() = coroutineScope {
        val tempFileFos = Array(downLoadFileBean.fileAsyncNumb) { RandomAccessFile(tempFile[0], "rw") }
        try {
            val file = downLoadFileBean.saveFile
            val fileAsyncNumb = downLoadFileBean.fileAsyncNumb
            val fileLength = downLoadFileBean.fileLength
            if (file.exists()) {
                val localFileSize = file.length() // 本地的文件大小
                var isExists = false
                tempFile.forEach {
                    if (it.exists()) {
                        isExists = true
                        return@forEach
                    }
                }
                WLog.e(this@WXDownloadCoroutineManager, "localFileSize:$localFileSize fileLength:$fileLength")
                if (localFileSize != fileLength && isExists) { // 小于的开始断点下载
                    val nPercent = (localFileSize * 100 / downLoadFileBean.fileLength).toInt()
                    channel.send(stateHolder.downloading.apply { progress = nPercent })
                    WLog.i(this, "$mis-重新断点续传..")
                    /** 线程数下载的大小  */
                    val fileThreadSize = fileLength / fileAsyncNumb // 每个线程需要下载的大小
                    for (i in 0..<fileAsyncNumb) {
                        tempFileFos[i] = RandomAccessFile(tempFile[i], "rw")
                        startPos[i] = tempFileFos[i].readLong() // 开始的位置
                        WLog.e(this, "startPos[${i}]:${startPos[i]} ")
                        if (i == fileAsyncNumb - 1) {
                            endPos[i] = fileLength
                        } else {
                            endPos[i] = fileThreadSize * (i + 1) - 1
                        }
                    }
                } else {
                    if (downLoadFileBean.deleteOnExit) {
                        file.delete()

                        // 目标文件不存在，则创建新文件
                        file.createNewFile()
                        val fileThreadSize = fileLength / fileAsyncNumb // 每个线程需要下载的大小
                        for (i in 0..<fileAsyncNumb) {
                            tempFile[i].createNewFile()
                            tempFileFos[i] = RandomAccessFile(tempFile[i], "rw")

                            startPos[i] = fileThreadSize * i
                            if (i == fileAsyncNumb - 1) {
                                endPos[i] = fileLength
                            } else {
                                endPos[i] = fileThreadSize * (i + 1) - 1
                            }
                            tempFileFos[i].writeLong(startPos[i])
                        }
                    } else {
                        isLoadSuccess = true
                        WLog.e(this, "isLoadSuccess $isLoadSuccess")
                    }
                }
            } else {
                // 目标文件不存在，则创建新文件
                file.createNewFile()
                val fileThreadSize = fileLength / fileAsyncNumb // 每个线程需要下载的大小
                for (i in 0..<fileAsyncNumb) {
                    tempFile[i].createNewFile()
                    tempFileFos[i] = RandomAccessFile(tempFile[i], "rw")

                    startPos[i] = fileThreadSize * i
                    if (i == fileAsyncNumb - 1) {
                        endPos[i] = fileLength
                    } else {
                        endPos[i] = fileThreadSize * (i + 1) - 1
                    }
                    tempFileFos[i].writeLong(startPos[i])
                }
            }
        } catch (e: Exception) {
            channel.send(stateHolder.failed)
            WLog.e(this, "${mis}异常：${e.message}")
        } finally {
            try {
                tempFileFos.forEach { it.close() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}