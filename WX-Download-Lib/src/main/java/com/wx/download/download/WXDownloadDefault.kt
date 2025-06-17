package com.wx.download.download

object WXDownloadDefault {
    /** 同时最大并行任务 **/
    const val DEFAULT_MAX_TASK_NUMBER = 3

    /** 断点续传分片最小大小，小于它没有必要分片 **/
    const val DEFAULT_MIN_DOWNLOAD_RANGE_SIZE = 1024 * 1024 * 1L

    /** 断点续传分片最小大小，最小下载了100k才更新进度 **/
    const val DEFAULT_MIN_DOWNLOAD_PROGRESS_RANGE_SIZE = 1024 * 100 * 1L


    const val TIME_OUT = 30000
}