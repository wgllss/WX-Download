package com.wx.download.download

import com.wx.download.utils.FileUtils
import com.wx.download.utils.WLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class WXDownloadManager private constructor() {

    /** 状态接受流，热流 **/
    private val _downloadStateFlow = MutableSharedFlow<WXState>()

    /**
     * 同时下载的任务数量
     */
    private var maxTaskNumber = 3

    /**
     * 任务map正在下载的
     */
    private val runningMapTask by lazy { ConcurrentHashMap<String, WXDownloadFileTask>() }

    /** 下载的所有存储 task的key **/
    private val runningMapKey by lazy { ConcurrentHashMap<Int, String>() }

    /** 等待队列**/
    private val waitingDeque by lazy { ConcurrentLinkedQueue<WXDownloadFileTask>() }

    /** 协程之间通信 **/
    private val channel by lazy { Channel<WXState>() }

    companion object {
        val instance by lazy { WXDownloadManager() }
    }

    //初始化下载
    fun downloadInit(coroutineScope: CoroutineScope, maxTaskNumber: Int) {
        this.maxTaskNumber = maxTaskNumber
        coroutineScope.launch {
            WLog.i(this@WXDownloadManager, "downloadInit ${Thread.currentThread().name}")
            channel.consumeEach { s ->
                when (s) {
                    is WXState.Succeed, is WXState.Failed, is WXState.Pause -> {
                        runningMapKey.takeIf { it.containsKey(s.which) }?.let {
                            runningMapTask.remove(it[s.which])
                            it.remove(s.which)
                        }

                        WLog.e(this@WXDownloadManager, "等待：${waitingDeque.size}")

                        waitingDeque.takeIf { it.size > 0 }?.poll()?.run {
                            val key = StringBuilder().append(this.which).append(this.fileSiteURL).append(this.strDownloadDir).append(this.fileSaveName).append(this.fileAsyncNumb).toString()
                            if (!runningMapTask.containsKey(key)) {
                                runningMapTask[key] = this@run
                                coroutineScope.launch(Dispatchers.IO) {
                                    download()
                                }
                            }
                        }
                    }

                    else -> {

                    }
                }
                _downloadStateFlow.emit(s)
            }
        }
    }

    fun download(coroutineScope: CoroutineScope, which: Int, fileSiteURL: String, strDownloadDir: String, fileSaveName: String, fileAsyncNumb: Int = 1) {
        coroutineScope.launch(Dispatchers.IO) {
            WLog.i(this@WXDownloadManager, "download ${Thread.currentThread().name}")
            val downloadTask = WXDownloadFileTask(which, fileSiteURL, strDownloadDir, fileSaveName, channel, fileAsyncNumb)
            val key = StringBuilder().append(which).append(fileSiteURL).append(strDownloadDir).append(fileSaveName).append(fileAsyncNumb).toString()
            if (runningMapTask.size < maxTaskNumber) {
                runningMapKey.takeUnless { it.containsKey(which) }?.put(which, key)
                if (!runningMapTask.containsKey(key)) {
                    runningMapTask[key] = downloadTask
                    downloadTask.download()
                }
            } else {
                runningMapKey.takeUnless { it.containsKey(which) }?.let {
                    it[which] = key
                    waitingDeque.takeUnless { it.contains(downloadTask) }?.add(downloadTask)
                }
                downloadTask.waiting()
                WLog.e(this@WXDownloadManager, "正在等待：${waitingDeque.size}")
            }
        }
    }

    //初始化已经下载的进度
    fun initTempFilePercent(coroutineScope: CoroutineScope, whichFile: Int, strDownloadDir: String, fileSaveName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val saveFile = File(StringBuilder(strDownloadDir).append(File.separator).append(fileSaveName).toString())
            val fileLengthFile = File(StringBuilder(strDownloadDir).append(File.separator).append(fileSaveName).append("_fileLength").toString())
            if (saveFile.exists() && fileLengthFile.exists()) {
                val localFileSize = saveFile.length() // 本地的文件大小
                val lengthFile = RandomAccessFile(fileLengthFile, "rw")
                val fileLength = lengthFile.readLong()
                val nPercent = (localFileSize * 100 / fileLength).toInt()
                val stateHolder = WXStateHolder().apply { which = whichFile }
                channel.send(stateHolder.downloading.apply { progress = nPercent })
                lengthFile.close()
            }
        }
    }

    fun downloadStatusFlow() = _downloadStateFlow

    fun downloadPause(which: Int) {
        runningMapKey.takeIf { it.containsKey(which) }?.get(which)?.let { key ->
            runningMapTask.takeIf { it.containsKey(key) }?.get(key)?.pauseDownload()
        }
    }
}