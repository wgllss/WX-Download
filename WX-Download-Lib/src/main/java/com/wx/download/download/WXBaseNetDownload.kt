package com.wx.download.download

import com.wx.download.utils.WLog
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.RandomAccessFile

abstract class WXBaseNetDownload : WXDownloadNet {

    override suspend fun initTemp(mis: String, startPos: LongArray, endPos: LongArray, tempFile: File, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean {
        val fileAsyncNumb = downLoadFileBean.fileAsyncNumb
        val fileLength = downLoadFileBean.fileLength
        val tempFileFos = RandomAccessFile(tempFile, "rw")
        var isLoadSuccess = false
        try {
            val file = downLoadFileBean.saveFile
            if (file.exists()) {
                val localFileSize = file.length() // 本地的文件大小
                var isExists = tempFile.exists()
                WLog.e(this, "localFileSize:$localFileSize fileLength:$fileLength")
                if (localFileSize != fileLength && isExists) { // 小于的开始断点下载
                    val nPercent = (localFileSize * 100f / downLoadFileBean.fileLength)
                    channel.send(stateHolder.downloading.apply { progress = nPercent })
                    WLog.i(this, "$mis-重新断点续传..")
                    initTempPosition(true, startPos, endPos, tempFile, tempFileFos, fileAsyncNumb, fileLength)
                } else {
                    if (downLoadFileBean.deleteOnExit) {
                        file.delete()
                        // 目标文件不存在，则创建新文件
                        file.createNewFile()
                        initTempPosition(false, startPos, endPos, tempFile, tempFileFos, fileAsyncNumb, fileLength)
                    } else {
                        isLoadSuccess = true
                    }
                }
            } else {
                // 目标文件不存在，则创建新文件
                file.createNewFile()
                initTempPosition(false, startPos, endPos, tempFile, tempFileFos, fileAsyncNumb, fileLength)
            }
        } catch (e: Exception) {
            channel.send(stateHolder.failed)
            WLog.e(this, "${mis}异常：${e.message}")
        } finally {
            try {
                tempFileFos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return isLoadSuccess
    }

    override fun initTempPosition(exists: Boolean, startPos: LongArray, endPos: LongArray, tempFile: File, tempFileFos: RandomAccessFile, fileAsyncNumb: Int, fileLength: Long) {
        val fileThreadSize = fileLength / fileAsyncNumb // 每个线程需要下载的大小
        tempFileFos.seek(13)
        for (i in 0..<fileAsyncNumb) {
            if (!exists) tempFile.createNewFile()
//            tempFileFos = RandomAccessFile(tempFile, "rw")
            startPos[i] = if (!exists) fileThreadSize * i else tempFileFos.readLong() // 开始的位置
            WLog.e(this, "startPos[${i}]:${startPos[i]} ")
            if (!exists) {
                tempFileFos.writeLong(startPos[i])
            }

            if (i == fileAsyncNumb - 1) {
                endPos[i] = fileLength
            } else {
                endPos[i] = fileThreadSize * (i + 1) - 1
            }
        }
    }
}