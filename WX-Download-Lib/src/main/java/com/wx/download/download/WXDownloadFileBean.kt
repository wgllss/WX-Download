package com.wx.download.download

import java.io.File
import kotlin.concurrent.Volatile

class WXDownloadFileBean(
    val which: Int, //多个文件一起下载，用来区分哪一个文件,可统一回调一起处理
    val fileSiteURL: String,  //文件的网络地址
    val fileSavePath: String, //文件保存的路径
    val fileSaveName: String, //保存的文件名
    var fileAsyncNumb: Int = 1 //文件异步任务数，可能不是线程，一个线程下多个协程进行异步
) {

    var fileLength: Long = -1
    var isRange = true //是否支持断点续传
    var isDownSuccess = false

    /**
     * 下载暂停标志
     */
    @Volatile
    var isAbortDownload = false

    val fileTempName: String
        get() = ".${fileSaveName}tmp${which}_${fileLength}"

    val saveFile: File
        get() = File(StringBuilder(fileSavePath).append(File.separator).append(fileSaveName).toString())

    val tempFile: File
        get() = File(StringBuilder(fileSavePath).append(File.separator).append(fileTempName).toString())

}