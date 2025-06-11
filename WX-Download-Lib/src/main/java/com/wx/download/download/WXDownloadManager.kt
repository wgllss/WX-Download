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
    var maxTaskNumber = 3

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

    fun downloadInit(coroutineScope: CoroutineScope, maxTaskNumber: Int) {
        this.maxTaskNumber = maxTaskNumber
        coroutineScope.launch(Dispatchers.IO) {
            WLog.i(this@WXDownloadManager, "downloadInit ${Thread.currentThread().name}")
            channel.consumeEach {
                when (it) {
                    is WXState.Succeed, is WXState.Failed, is WXState.Pause -> {
                        val whichKey = "${it.which}"
                        runningMapKey.takeIf { it.containsKey(whichKey) }?.let {
                            runningMapTask.remove(it[whichKey])
                            it.remove(whichKey)
                        }
                        WLog.e(this@WXDownloadManager, "等待：${deque.size}")

                        deque.takeIf { it.size > 0 }?.poll()?.run {
                            val key = StringBuilder().append(this.which).append(this.fileSiteURL).append(this.strDownloadDir).append(this.fileSaveName).append(this.fileAsyncNumb).toString()
                            if (!runningMapTask.containsKey(key)) {
                                runningMapTask[key] = this@run
                                coroutineScope.launch(Dispatchers.IO) {
                                    download(this)
                                }
                            }
                        }
                    }
                    else -> {

                    }
                }
                _downloadStateFlow.emit(it)
            }
        }
    }

    fun download(coroutineScope: CoroutineScope, which: Int, fileSiteURL: String, strDownloadDir: String, fileSaveName: String, fileAsyncNumb: Int = 1) {
        coroutineScope.launch(Dispatchers.IO) {
            WLog.i(this@WXDownloadManager, "download ${Thread.currentThread().name}")

            val downloadTask = WXDownloadFileTask(which, fileSiteURL, strDownloadDir, fileSaveName, channel, fileAsyncNumb)
            val key = StringBuilder().append(which).append(fileSiteURL).append(strDownloadDir).append(fileSaveName).append(fileAsyncNumb).toString()
            val whichKey = "$which"
            if (runningMapTask.size < maxTaskNumber) {
                if (!runningMapTask.containsKey(key)) {
                    runningMapTask[key] = downloadTask
                    downloadTask.download(this)
                }
                runningMapKey.takeUnless { it.containsKey(whichKey) }?.put(whichKey, key)
            } else {
                runningMapKey.takeUnless { it.containsKey(whichKey) }?.let {
                    it.put(whichKey, key)
                    deque.takeUnless { it.contains(downloadTask) }?.add(downloadTask)
                }
                downloadTask.waiting()
                WLog.e(this@WXDownloadManager, "正在等待：${deque.size}")
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