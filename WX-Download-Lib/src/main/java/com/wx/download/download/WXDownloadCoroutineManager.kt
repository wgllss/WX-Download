package com.wx.download.download

import com.wx.download.utils.WLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import java.io.File

class WXDownloadCoroutineManager(private val downLoadFileBean: WXDownloadFileBean, private val channel: Channel<WXState>, private val stateHolder: WXStateHolder) {

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
    private val tempFile by lazy { downLoadFileBean.tempFile }

    /**
     * 下载是否成功的标记
     */
    private var isLoadSuccess = false

    private val mis by lazy { "{下载:(${downLoadFileBean.fileSiteURL})" }

    suspend fun download(downloadNet: WXDownloadNet) = coroutineScope {
        WLog.i(this, "${mis}开始 download ${Thread.currentThread().name}")
        val start = System.currentTimeMillis()
        downloadNet.run {
            if (isConnect(mis, downLoadFileBean, channel, stateHolder)) {
                isLoadSuccess = initTemp(mis, startPos, endPos, tempFile, downLoadFileBean, channel, stateHolder)

                WLog.i(this, "${mis} 分 ${downLoadFileBean.fileAsyncNumb}个 异步任务 下载文件 ${Thread.currentThread().name}")
                if (!isLoadSuccess) {
                    val fileAsynNum = downLoadFileBean.fileAsyncNumb
                    val isRange = downLoadFileBean.isRange
                    if (isRange) {
                        val sets = mutableSetOf<Deferred<Any>>()
                        for (i in 0 until fileAsynNum) {
                            val downloadDeferred = async(Dispatchers.IO) {
                                WXRangeDownload(downLoadFileBean, channel, startPos[i], endPos[i], i, stateHolder).runDownload(this@run)
                            }
                            sets.add(downloadDeferred)
                        }
                        sets.awaitAll()
                    } else {
                        val downloadDeferred = async(Dispatchers.IO) {
                            WXRangeDownload(downLoadFileBean, channel, stateHolder = stateHolder).runDownload(this@run)
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
                        tempFile.delete() // 临时文件删除
//                        downLoadFileBean.lengthFile.delete()
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
                    tempFile.delete() // 临时文件删除
                }
            }
        }
    }
}