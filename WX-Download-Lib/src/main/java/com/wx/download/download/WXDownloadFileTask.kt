package com.wx.download.download

import com.wx.download.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

class WXDownloadFileTask(
    val coroutineScope: CoroutineScope, val which: Int, val fileSiteURL: String, val strDownloadDir: String, val fileSaveName: String, val channel: Channel<WXState>, var fileAsyncNumb: Int = 1
) {

    //每个下载任务都有对应的状态
    private val stateHolder by lazy { StateHolder() }
    private var downloadCoroutineManager: WXDownloadCoroutineManager? = null

    private val mDownLoadBean by lazy {
        FileUtils.createDir(strDownloadDir)
        WXDownloadFileBean(which, fileSiteURL, strDownloadDir, fileSaveName, fileAsyncNumb)
    }

    suspend fun download() {
        downloadCoroutineManager = WXDownloadCoroutineManager(mDownLoadBean, channel, stateHolder)
        downloadCoroutineManager?.start(coroutineScope)
    }

    fun pauseDownload() {
        mDownLoadBean.isAbortDownload = true
    }
}