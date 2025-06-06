package com.wx.download.utils
//
//import com.wx.download.download.WXDownloadFileTask
//import com.wx.download.download.WXState
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.channels.Channel
//
///**
// * 扩展下载方法(自定义下载参数)
// */
//fun CoroutineScope.download(which: Int, fileSiteURL: String, strDownloadDir: String, fileSaveName: String, channel: Channel<WXState>, fileAsyncNumb: Int = 1) = WXDownloadFileTask(this, which, fileSiteURL, strDownloadDir, fileSaveName, channel, fileAsyncNumb)
