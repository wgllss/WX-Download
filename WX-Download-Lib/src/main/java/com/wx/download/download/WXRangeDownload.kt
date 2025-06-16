package com.wx.download.download

import com.wx.download.utils.HttpUtils
import com.wx.download.utils.WLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class WXRangeDownload(
    val downLoadFileBean: WXDownloadFileBean, val channel: Channel<WXState>, var startPos: Long = 0, val endPos: Long = 0, val asynID: Int = 0, val stateHolder: WXStateHolder
) {
    private val timeout = 30000 // 超时时间
    private val reTryNum = 5 // 超时重试次数

    private lateinit var file: RandomAccessFile // 存放的文件
    private lateinit var tempFile: RandomAccessFile // 指针文件
    private var curNum = 0 // 当前重试次数
    private var isOK = false // 下载完成
    private val mis by lazy { "[(${downLoadFileBean.fileSiteURL})子任务:${asynID}]"; } // 提示信息
    private var isRange = true // 是否支持断点续传

    suspend fun runDownload() {
        try {
            file = RandomAccessFile(downLoadFileBean.saveFile, "rw")  // 存放的文件
            tempFile = RandomAccessFile(downLoadFileBean.tempFile[asynID], "rw")  //指针文件
        } catch (e: Exception) {
            e.printStackTrace()
        }

        WLog.i(this, "$mis 开始下载! ${Thread.currentThread().name}")
        curNum = 0
        while (curNum < reTryNum && !isOK) {
            if (curNum > 0) {
                WLog.i(this, "$mis 第${curNum}次重试下载:")
            }
            downLoad()
        }
    }

    /**
     * 首次连接,初使化长度
     */
    private suspend fun downLoad() = coroutineScope {
        WLog.i(this, "$mis 第${curNum}次 downLoad下载:-任务号:$asynID 开始位置:$startPos,结束位置：$endPos")

        var inputStream: InputStream? = null
        var con: HttpURLConnection? = null
        var myFileLength = 0L // 临时文件长度,用于减少下载进度消息数量
        try {
            curNum++
            var count = 0L
            val url = URL(downLoadFileBean.fileSiteURL)
            con = HttpUtils.getHttpURLConnection(url, timeout)
            HttpUtils.setConHeader(con!!)
            if (startPos < endPos && isRange) {
                // 设置下载数据的起止区间
                con.setRequestProperty("Range", "bytes=$startPos-$endPos")
                WLog.i(this, "'${downLoadFileBean.fileSiteURL}'-任务号:$asynID 开始位置:$startPos,结束位置：$endPos")
            }
            file.seek(startPos) // 转到文件指针位置
            val responseCode = con.responseCode
            // 判断http status是否为HTTP/1.1 206 Partial Content或者200 OK
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = con.inputStream // 打开输入流
                var len = 0
                val b = ByteArray(1024)
                tempFile.seek(0L)
                file.seek(startPos)

                while (isActive && !downLoadFileBean.isAbortDownload && !isOK && (inputStream.read(b).also { len = it }) != -1) {
                    file.write(b, 0, len) // 写入临时数据文件,外性能需要提高
                    count += len.toLong()
                    startPos += len.toLong()

                    tempFile.writeLong(startPos) // 写入断点数据文件
                    if ((count - myFileLength) > 1024) {
                        myFileLength = count
                        var tempSize = 0L
                        val file = downLoadFileBean.saveFile
                        if (file.exists()) {
                            tempSize = file.length()
                        }
                        val nPercent = (tempSize * 100 / downLoadFileBean.fileLength).toInt()
                        channel.send(stateHolder.downloading.apply { progress = nPercent })
                    }
                    if (startPos >= endPos) {
                        isOK = true
                    } // 下载完成
                }
                if (isOK) WLog.e(this, "$mis 下载完成")
                else WLog.e(this, "$mis 下载暂停")

            }
        } catch (e: Exception) {
            WLog.e(this, "$mis 异常: ${e.message}") // logger.debug
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
    }
}