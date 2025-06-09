package com.wx.download

import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wx.download.download.WXDownloadManager
import com.wx.download.download.WXDownloadFileTask
import com.wx.download.download.WXState
import com.wx.download.utils.WLog
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOADING
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOAD_COMPLETE
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOAD_PAUSE
import com.wx.progress.dynamic.ProgressModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class WXDownLoadViewModel : ViewModel() {

    private val downloadManager by lazy { WXDownloadManager.instance }


    private val _datas = MutableLiveData<ProgressModel>()
    val datas: LiveData<ProgressModel> = _datas

    private val _progress = MutableLiveData<Float>()
    val progress: LiveData<Float> = _progress

    private val environmentGetExternalStorageDirectory: String = Environment.getExternalStorageDirectory().path
    private var sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    private fun getSDPath(): String = if (sdCardExist) environmentGetExternalStorageDirectory else ""

//    private lateinit var downloadFile: WXDownloadFileTask

//    //模拟下载暂停
//    private var isPause = false

    fun download22() {
        val which = 0
        val url = "https://imtt.dd.qq.com/16891/apk/96881CC7639E84F35E86421691CBBA5D.apk?fsname=com.sina.weibo_11.1.3_4842.apk"
//            val url =  "http://192.168.3.108:8080/assets/apk/google_18489_201606032222.apk"
        val strDownloadDir = getSDPath()
        val fileSaveName = "3333.apk"
        val fileAsyncNumb = 2
//        downloadFile = viewModelScope.download(0, url, strDownloadDir, fileSaveName, 3)
//        downloadFile.download()
//        val file = File(strDownloadDir, fileSaveName)

        downloadManager.download(viewModelScope, which, url, strDownloadDir, fileSaveName, fileAsyncNumb)
        downloadManager.downloadStatusFlow().onEach {
            when (it) {
                is WXState.None -> {}
                is WXState.Downloading -> {
                    _progress.value = it.progress.toFloat()
                }

                is WXState.Pause -> {}
                is WXState.Failed -> {}
                is WXState.Succeed -> {
                    _progress.value = 100f
                }

                is WXState.Waiting -> {}
            }
        }.launchIn(viewModelScope)


//        viewModelScope.launch {
//            val url = "https://imtt.dd.qq.com/16891/apk/96881CC7639E84F35E86421691CBBA5D.apk?fsname=com.sina.weibo_11.1.3_4842.apk"
////            val url =  "http://192.168.3.108:8080/assets/apk/google_18489_201606032222.apk"
//            val strDownloadDir = getSDPath()
//            val fileSaveName = "3333.apk"
//        }
    }

    @Volatile
    private var progressDD = 0f

    fun add() {
        viewModelScope.launch {
            val model = ProgressModel("下载按钮", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装"
            }
            _datas.value = model
        }
    }

    fun onClick(mode: Int) = when (mode) {
        WX_PROGRESS_BUTTON_DOWNLOADING -> {
            //正在下载
            download22()
        }

        WX_PROGRESS_BUTTON_DOWNLOAD_PAUSE -> {
            //暂停
            pause()
        }

        WX_PROGRESS_BUTTON_DOWNLOAD_COMPLETE -> {
            //下载完成
            finish()
        }

        else -> {
        }
    }


//    private fun download() {
//        viewModelScope.launch {
//            isPause = false
//            while (progressDD < 100 && !isPause) {
//                delay(200)
//                if (progressDD < 100f) progressDD += 2
//                _progress.value = progressDD
//            }
//        }
//    }

    private fun pause() {
//        isPause = true
//        if (this::downloadFile.isInitialized) {
//            downloadFile.pauseDownload()
//        }
        downloadManager.downloadPause(0)
        val model = datas.value!!
        model.statusText = "继续下载"
        _datas.value = model
    }

    private fun finish() {

    }

}