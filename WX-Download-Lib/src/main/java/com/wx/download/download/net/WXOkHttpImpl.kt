package com.wx.download.download.net

import com.wx.download.download.WXBaseNetDownload
import com.wx.download.download.WXDownloadDefault.DEFAULT_MIN_DOWNLOAD_PROGRESS_RANGE_SIZE
import com.wx.download.download.WXDownloadFileBean
import com.wx.download.download.WXSafeRandomAccessFile
import com.wx.download.download.WXState
import com.wx.download.download.WXStateHolder
import com.wx.download.utils.WLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile

class WXOkHttpImpl(private val minDownloadRangeSize: Long) : WXBaseNetDownload() {
    private val client = OkHttpClient()

    override suspend fun isConnect(mis: String, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        var tempFile: RandomAccessFile
        if (downLoadFileBean.tempFile.exists()) {
            tempFile = RandomAccessFile(downLoadFileBean.tempFile, "rw")
            tempFile.seek(0L)
            downLoadFileBean.fileLength = tempFile.readLong()
            downLoadFileBean.isRange = tempFile.readBoolean()
            downLoadFileBean.fileAsyncNumb = tempFile.readInt()

            WLog.e(this, "缓存文件总大小：${downLoadFileBean.fileLength} 是否断点续传${downLoadFileBean.isRange} 分${downLoadFileBean.fileAsyncNumb}片")

            tempFile.close()
            if (downLoadFileBean.fileLength > 0)
                return true
        }
        val request = Request.Builder().url(downLoadFileBean.fileSiteURL).build()
        val response = client.newCall(request).execute()
        response.use {
            try {
                if (it.isSuccessful) {
                    downLoadFileBean.isRange = (it.code == 206 || it.header("Content-Range")?.isNotEmpty() == true || it.header("Accept-Ranges") == "bytes")
                    var fileLength = it.body?.contentLength() ?: -1L
                    if (fileLength == -1L) {
                        val inputStream = it.body?.byteStream()!!
                        val buff = ByteArray(512)
                        var count = 0L
                        var rc = 0
                        while ((inputStream.read(buff, 0, 512).also { rc = it }) > 0) {
                            count += rc
                        }
                        fileLength = count
                        WLog.i(this, "$mis-请求返回fileLength2:$fileLength")
                        inputStream?.close()
                    }
                    downLoadFileBean.fileLength = fileLength

                    tempFile = RandomAccessFile(downLoadFileBean.tempFile, "rw")
                    tempFile.writeLong(fileLength) //存取文件长度
                    tempFile.writeBoolean(downLoadFileBean.isRange)//存取文件服务器是否支持断点续传
                    if (fileLength < minDownloadRangeSize) {
                        //如果文件大小小于1M 默认就只分一块下载
                        downLoadFileBean.fileAsyncNumb = 1
                    }
                    tempFile.writeInt(downLoadFileBean.fileAsyncNumb)
                    tempFile.close()
                    return true // 失败成功
                }
            } catch (e: Exception) {
                channel.send(stateHolder.failed)
                WLog.e(this, "$mis-请求连接${downLoadFileBean.fileSiteURL}异常:${e.message}")
            } finally {

            }
            return false // 失败返回
        }
    }


    override suspend fun downloadChunk(mis: String, asynID: Int, downLoadFileBean: WXDownloadFileBean, file: WXSafeRandomAccessFile, tempFile: WXSafeRandomAccessFile, startPosi: Long, endPos: Long, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        val isComplete = coroutineScope {
            var isOK = false
            val request = Request.Builder().url(downLoadFileBean.fileSiteURL)
            var startPos = startPosi
            if (startPos < endPos && downLoadFileBean.isRange) {
                request.header("Range", "bytes=$startPos-$endPos")
                WLog.i(this, "'${downLoadFileBean.fileSiteURL}'-任务号:$asynID 开始位置:$startPos,结束位置：$endPos")
            }
            client.newCall(request.build()).execute().use { response ->
                try {
                    file.seek(startPos) // 转到文件指针位置
                    response.body?.byteStream()?.use { input ->
                        file.seek(startPos)

                        var count = 0L
                        var len = 0
                        val b = ByteArray(1024)
                        var myFileLength = 0L // 临时文件长度,用于减少下载进度消息数量
                        while (isActive && !downLoadFileBean.isAbortDownload && !isOK && (input.read(b).also { len = it } != -1)) {
                            file.write(b, 0, len) // 写入临时数据文件,外性能需要提高
                            count += len.toLong()
                            startPos += len.toLong()
//                            tempFile.seek(13L + 8 * asynID)
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
                    WLog.e(this@WXOkHttpImpl, "$mis 异常: ${e.message}")
                } finally {
                    file.close()
                    tempFile.close()
                }
                return@coroutineScope isOK
            }
        }
        return isComplete
    }
}