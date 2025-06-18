package com.wx.download.download

import com.wx.download.utils.WLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import java.io.RandomAccessFile

class WXRangeDownload(
    val downLoadFileBean: WXDownloadFileBean, val channel: Channel<WXState>, var startPos: Long = 0, val endPos: Long = 0, val asynID: Int = 0, val stateHolder: WXStateHolder
) {
    private val reTryNum = 5 // 超时重试次数

    private lateinit var file: RandomAccessFile // 存放的文件
    private lateinit var tempFile: RandomAccessFile // 指针文件
    private var curNum = 0 // 当前重试次数
    private var isOK = false // 下载完成
    private val mis by lazy { "[(${downLoadFileBean.fileSiteURL})子任务:${asynID}]"; } // 提示信息

    suspend fun runDownload(downloadNet: WXDownloadNet) {
        try {
            file = RandomAccessFile(downLoadFileBean.saveFile, "rw")  // 存放的文件
            tempFile = RandomAccessFile(downLoadFileBean.tempFile, "rw")  //指针文件
        } catch (e: Exception) {
            e.printStackTrace()
        }

        WLog.i(this, "$mis 开始下载! ${Thread.currentThread().name}")
        curNum = 0
        while (curNum < reTryNum && !isOK) {
            if (curNum > 0) {
                WLog.i(this, "$mis 第${curNum}次重试下载:")
            }
            isOK = downloadNet.downloadChunk(mis, asynID, downLoadFileBean, file, tempFile, startPos, endPos, channel, stateHolder)
            curNum++
        }
    }
}