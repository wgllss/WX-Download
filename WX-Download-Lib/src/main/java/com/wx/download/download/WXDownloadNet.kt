package com.wx.download.download

import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.RandomAccessFile

interface WXDownloadNet {

    suspend fun isConnect(mis: String, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean

    suspend fun initTemp(mis: String, startPos: LongArray, endPos: LongArray, tempFile: Array<File>, downLoadFileBean: WXDownloadFileBean, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean

    fun initTempPosition(exists: Boolean, startPos: LongArray, endPos: LongArray, tempFile: Array<File>, tempFileFos: Array<RandomAccessFile>, fileAsyncNumb: Int, fileLength: Long)

    suspend fun downloadChunk(mis: String, asynID: Int, downLoadFileBean: WXDownloadFileBean, file: RandomAccessFile, tempFile: RandomAccessFile, startPosi: Long, endPos: Long, channel: Channel<WXState>, stateHolder: WXStateHolder): Boolean
}