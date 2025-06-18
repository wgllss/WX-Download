package com.wx.download.download.net

import android.os.Build
import com.wx.download.download.WXBaseNetDownload
import com.wx.download.download.WXDownloadDefault
import com.wx.download.download.WXDownloadDefault.DEFAULT_MIN_DOWNLOAD_PROGRESS_RANGE_SIZE
import com.wx.download.download.WXDownloadFileBean
import com.wx.download.download.WXSafeRandomAccessFile
import com.wx.download.download.WXState
import com.wx.download.download.WXStateHolder
import com.wx.download.utils.HttpUtils
import com.wx.download.utils.WLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class WXHttpURLConnectionImpl(private val minDownloadRangeSize: Long) : WXBaseNetDownload() {

    override suspend fun isConnect(mis: String, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        var curNum = 0 // 重试计数器
        val tryNum = 3 // 重试的次数
        val waitTime = 500 // 等待500ms
        var connect = false
        // 将有3次的重试机会.大概花时3s
        while (curNum < tryNum && !connect) {
            if (curNum > 0) {
                WLog.e(this, "${mis} 第${curNum}次重试连接:")
                delay((waitTime * curNum).toLong())
            }
            connect = connectAndGetFileLength(mis, downLoadFileBean, channel, stateHolder)
            if (connect) {
                return true
            } else {
                curNum++
            }
        }
        return connect
    }

    private suspend fun connectAndGetFileLength(mis: String, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        var tempFile: RandomAccessFile
        if (downLoadFileBean.tempFile.exists()) {
            tempFile = RandomAccessFile(downLoadFileBean.tempFile, "rw")
            tempFile.seek(0L)
            downLoadFileBean.fileLength = tempFile.readLong() // 8个位置
            downLoadFileBean.isRange = tempFile.readBoolean() // 1个位置
            downLoadFileBean.fileAsyncNumb = tempFile.readInt() //4个位置
            WLog.e(this, "缓存文件总大小：${downLoadFileBean.fileLength} 是否断点续传${downLoadFileBean.isRange} 分${downLoadFileBean.fileAsyncNumb}片")

            tempFile.close()
            if (downLoadFileBean.fileLength > 0) return true
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
                    downLoadFileBean.tempFile.takeUnless { it.exists() }?.createNewFile()
                    tempFile = RandomAccessFile(downLoadFileBean.tempFile.absoluteFile, "rw")
                    tempFile.writeLong(fileLength) //存取文件长度   占8个位置
                    tempFile.writeBoolean(downLoadFileBean.isRange)//存取文件服务器是否支持断点续传 占1个位置
                    if (fileLength < minDownloadRangeSize) {
                        //如果文件大小小于1M 默认就只分一块下载
                        downLoadFileBean.fileAsyncNumb = 1
                    }
                    tempFile.writeInt(downLoadFileBean.fileAsyncNumb) //占4个位置
                    tempFile.close()
                    return true // 失败成功
                }
                WLog.i(this, "$mis-请求返回responseCode=$responseCode,连接失败")
            }
        } catch (e: Exception) {
            channel.send(stateHolder.failed)
            WLog.e(this, "$mis-请求连接${downLoadFileBean.fileSiteURL}异常:${e.message}")
        } finally {
            httpConnection?.disconnect() // 关闭连接
        }
        return false // 失败返回
    }

    override suspend fun downloadChunk(mis: String, asynID: Int, downLoadFileBean: WXDownloadFileBean, file: WXSafeRandomAccessFile, tempFile: WXSafeRandomAccessFile, startPosi: Long, endPos: Long, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        val isComplete = coroutineScope {
            var isOK = false
            var inputStream: InputStream? = null
            var con: HttpURLConnection? = null
            var myFileLength = 0L // 临时文件长度,用于减少下载进度消息数量
            try {
                var startPos = startPosi
                var count = 0L
                val url = URL(downLoadFileBean.fileSiteURL)
                con = HttpUtils.getHttpURLConnection(url, WXDownloadDefault.TIME_OUT)
                HttpUtils.setConHeader(con!!)
                if (startPos < endPos && downLoadFileBean.isRange) {
                    // 设置下载数据的起止区间
                    con.setRequestProperty("Range", "bytes=$startPos-$endPos")
                    WLog.i(this, "'${downLoadFileBean.fileSiteURL}'-任务号:$asynID 开始位置:$startPos,结束位置：$endPos")
                }
                val responseCode = con.responseCode
                // 判断http status是否为HTTP/1.1 206 Partial Content或者200 OK
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    inputStream = con.inputStream // 打开输入流
                    var len = 0
                    val b = ByteArray(1024)
                    file.seek(startPos)

                    while (isActive && !downLoadFileBean.isAbortDownload && !isOK && (inputStream.read(b).also { len = it }) != -1) {
                        file.write(b, 0, len) // 写入临时数据文件,外性能需要提高
                        count += len.toLong()
                        startPos += len.toLong()
//                        tempFile.seek(13L + 8 * asynID)
                        tempFile.writeLong((13L + 8 * asynID), startPos) // 写入断点数据文件位置
                        if ((count - myFileLength) > DEFAULT_MIN_DOWNLOAD_PROGRESS_RANGE_SIZE) {
                            myFileLength = count
                            var tempSize = 0L
                            val file = downLoadFileBean.saveFile
                            if (file.exists()) {
                                tempSize = file.length()
                            }
                            val nPercent = (tempSize * 100f / downLoadFileBean.fileLength)
                            channel.send(stateHolder.downloading.apply { progress = nPercent })
                        }
                        if (startPos >= endPos) {
                            isOK = true
                        } // 下载完成
                    }
                    if (isOK) WLog.e(this, "$mis 下载完成")
                    else WLog.e(this, "$mis 下载暂停 已经下载到位置：$startPos")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                WLog.e(this@WXHttpURLConnectionImpl, "$mis 异常: ${e.message}")
            } finally {
                try {
                    // 关闭连接
                    con?.disconnect()
                    inputStream?.close()
                    // 关闭文件
                    file.close()
                    // 文件指针文件
                    tempFile.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return@coroutineScope isOK
        }
        return isComplete
    }
}