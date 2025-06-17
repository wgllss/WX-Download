package com.wx.download.download

import com.wx.download.utils.FileUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope

class WXDownloadFileTask(
    val which: Int, val fileSiteURL: String, val strDownloadDir: String, val fileSaveName: String, val channel: Channel<WXState>, var fileAsyncNumb: Int, val deleteOnExit: Boolean
) {

    /** 每个下载任务都有对应的状态 **/
    private val stateHolder by lazy { WXStateHolder().apply { which = this@WXDownloadFileTask.which } }

    private val mDownLoadBean by lazy {
        FileUtils.createDir(strDownloadDir)
        WXDownloadFileBean(which, fileSiteURL, strDownloadDir, fileSaveName, fileAsyncNumb, deleteOnExit)
    }

    suspend fun download(downloadNet: WXDownloadNet) {
        WXDownloadCoroutineManager(mDownLoadBean, channel, stateHolder).download(downloadNet)
    }

    fun pauseDownload() {
        mDownLoadBean.isAbortDownload = true
    }

    suspend fun waiting() = coroutineScope {
        channel.send(stateHolder.waiting)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WXDownloadFileTask
        if (which != other.which) return false
        return fileSiteURL == other.fileSiteURL && strDownloadDir == other.strDownloadDir && fileSaveName == other.fileSaveName && fileAsyncNumb == other.fileAsyncNumb
    }

    override fun hashCode() = which + fileSiteURL.hashCode() + strDownloadDir.hashCode() + fileSaveName.hashCode() + fileAsyncNumb

}