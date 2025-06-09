package com.wx.download.download

import com.wx.download.utils.WLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class WXDownloadManager private constructor() {

    private val _downloadStateFlow = MutableSharedFlow<WXState>()

    /**
     * 同时下载的任务数量
     */
    private var maxTaskNumber = 5

    private var isFirst = true

    /**
     * 任务map正在下载的
     */
    private val runningMapTask by lazy { ConcurrentHashMap<String, WXDownloadFileTask>() }
    private val runningMapKey by lazy { ConcurrentHashMap<String, String>() }
    private val deque by lazy { ConcurrentLinkedQueue<WXDownloadFileTask>() }

    private val channel by lazy { Channel<WXState>() }

    companion object {
        val instance by lazy { WXDownloadManager() }
    }

    fun download(coroutineScope: CoroutineScope, which: Int, fileSiteURL: String, strDownloadDir: String, fileSaveName: String, fileAsyncNumb: Int = 1) {
        coroutineScope.launch(Dispatchers.IO) {
            WLog.i(this@WXDownloadManager, "download ${Thread.currentThread().name}")

            val downloadTask = WXDownloadFileTask(this, which, fileSiteURL, strDownloadDir, fileSaveName, channel, fileAsyncNumb)
            if (runningMapTask.size < maxTaskNumber) {
                val key = StringBuilder().append(which).append(fileSiteURL).append(strDownloadDir).append(fileSaveName).append(fileAsyncNumb).toString()
                if (!runningMapTask.containsKey(key)) {
                    runningMapTask[key] = downloadTask
                    downloadTask.download()
                }
                val whichKey = "$which"
                runningMapKey.takeUnless { it.containsKey(whichKey) }?.put(whichKey, key)
            } else {
                deque.takeUnless { it.contains(downloadTask) }?.add(downloadTask)
            }
            if (isFirst) {
                isFirst = false
                launch {
                    channel.consumeEach {
                        if (it is WXState.Succeed || it is WXState.Failed) {
                            val whichKey = "${it.which}"
                            runningMapKey.takeIf { it.containsKey(whichKey) }?.let {
                                runningMapTask.remove(it[whichKey])
                                it.remove(whichKey)
                            }

                            deque.takeIf { it.size > 0 }?.first()?.run {
                                val key = StringBuilder().append(which).append(fileSiteURL).append(strDownloadDir).append(fileSaveName).append(fileAsyncNumb).toString()
                                if (!runningMapTask.containsKey(key)) {
                                    runningMapTask[key] = this@run
                                    download()
                                }
                            }
                        }
                        _downloadStateFlow.emit(it)
                    }
                }
            }
        }
    }

    fun downloadStatusFlow() = _downloadStateFlow

    fun downloadPause(which: Int) {
        val whichKey = "$which"
        runningMapKey.takeIf { it.containsKey(whichKey) }?.get(whichKey)?.let { key ->
            runningMapTask.takeIf { it.containsKey(key) }?.get(key)?.pauseDownload()
        }
    }
}