package com.wx.download.download

import com.wx.download.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class WXDownloadFileTask(
    val which: Int, val fileSiteURL: String, val strDownloadDir: String, val fileSaveName: String, val channel: Channel<WXState>, var fileAsyncNumb: Int = 1
) {
    var coroutineScope: CoroutineScope? = null

    //每个下载任务都有对应的状态
    private val stateHolder by lazy { StateHolder().apply { which = this@WXDownloadFileTask.which } }
    private var downloadCoroutineManager: WXDownloadCoroutineManager? = null

    private val mDownLoadBean by lazy {
        FileUtils.createDir(strDownloadDir)
        WXDownloadFileBean(which, fileSiteURL, strDownloadDir, fileSaveName, fileAsyncNumb)
    }

    suspend fun download(coroutineScope: CoroutineScope) {
        downloadCoroutineManager = WXDownloadCoroutineManager(mDownLoadBean, channel, stateHolder)
        this.coroutineScope = coroutineScope
        downloadCoroutineManager?.start(coroutineScope)
    }

    fun pauseDownload() {
        mDownLoadBean.isAbortDownload = true
    }

    fun waiting() {
        coroutineScope?.launch {
            channel.send(stateHolder.waiting)
        }
    }
}