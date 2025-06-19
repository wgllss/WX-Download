package com.wx.download

import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wx.download.download.WXDownloadManager
import com.wx.download.download.WXState
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOADING
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOAD_COMPLETE
import com.wx.progress.WX_PROGRESS_BUTTON_DOWNLOAD_PAUSE
import com.wx.progress.dynamic.ProgressModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WXDownLoadViewModel : ViewModel() {

    private val downloadManager by lazy { WXDownloadManager.instance }


    private val _datas = MutableLiveData<MutableList<ProgressModel>>()
    val datas: LiveData<MutableList<ProgressModel>> = _datas

    private val downloadDatas = mutableListOf(
        "https://gitee.com/wgllss888/WXDynamicPlugin/raw/master/WX-Resource/skins/blue.apk",//该地址 contentLengthLong 方案是获取不到的
        "https://imtt.dd.qq.com/16891/apk/96881CC7639E84F35E86421691CBBA5D.apk?fsname=com.sina.weibo_11.1.3_4842.apk",
        "https://imtt.dd.qq.com/sjy.00022/sjy.00004/16891/apk/8C6FDC631C3D853BF29321F86EE739FF.apk?fsname=com.taobao.idlefish_7.21.31.apk",
        "https://imtt.dd.qq.com/sjy.00022/sjy.00004/16891/apk/822A20774AEE3225DD7BCAA20DB56C7D.apk?fsname=com.taobao.taobao_10.49.10.apk",
        "https://imtt.dd.qq.com/sjy.00022/sjy.00004/16891/apk/7B73608E1C88ED42581EC313748001E5.apk?fsname=com.tmall.wireless_15.52.0.apk",
        "https://imtt.dd.qq.com/sjy.00022/sjy.00004/16891/apk/4DB00D67C6672380F3CCE96A58FBE0DD.apk?fsname=com.alibaba.wireless_11.61.0.0.apk",
    )

    private val environmentGetExternalStorageDirectory: String = Environment.getExternalStorageDirectory().path
    private var sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    private fun getSDPath(): String = if (sdCardExist) environmentGetExternalStorageDirectory else ""
    private val strDownloadDir = "${getSDPath()}/WX_Download/"


    fun download(which: Int) {
        val url = downloadDatas[which]
        var fileSaveName = url.substring(url.lastIndexOf("?") + 1, url.length)
        if (which == 0) fileSaveName = "blue.apk"
        val fileAsyncNumb = 2
        downloadManager.download(viewModelScope, which, url, strDownloadDir, fileSaveName, fileAsyncNumb, true)
    }

    fun add() {
        viewModelScope.launch {
            val list = mutableListOf(ProgressModel("下载按钮1", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装1"
            }, ProgressModel("下载按钮2", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装2"
            }, ProgressModel("下载按钮3", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装3"
            }, ProgressModel("下载按钮4", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装4"
            }, ProgressModel("下载按钮5", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装5"
            }, ProgressModel("下载按钮6", strokeColor = Color.Red, mBackgroundSecondColor = Color.Red).apply {
                maxProgress = 100f
                statusFinishText = "点击安装6"
            })
            _datas.value = list
        }


        /** 初始化 最大并行同时下载文件个数 **/
        downloadManager.downloadInit(viewModelScope, 3, true)

        /** 监听文件下载状态 及下载进度 **/
        downloadManager.downloadStatusFlow().onEach {
            when (it) {
                is WXState.None -> {

                }

                is WXState.Downloading -> {
                    _datas.value!![it.which].progress.value = it.progress
                }

                is WXState.Pause -> {}
                is WXState.Failed -> {}
                is WXState.Succeed -> {
                    _datas.value!![it.which].progress.value = 100f
                }

                is WXState.Waiting -> {}
            }
        }.launchIn(viewModelScope)

        /** 初始化上次没有下载完的文件下载进度 **/
        downloadDatas.forEachIndexed { index, url ->
            var fileSaveName = url.substring(url.lastIndexOf("?") + 1, url.length)
            if (index == 0) fileSaveName = "blue.apk"
            // 注意：  要与点击下载传入保存的文件名fileSaveName ，保存的文件路径strDownloadDir ，which 全部要一致
            downloadManager.initTempFilePercent(viewModelScope, index, strDownloadDir, fileSaveName)
        }

    }

    fun onClick(mode: Int, which: Int) = when (mode) {
        WX_PROGRESS_BUTTON_DOWNLOADING -> {
            //正在下载
            download(which)
        }

        WX_PROGRESS_BUTTON_DOWNLOAD_PAUSE -> {
            //暂停
            pause(which)
        }

        WX_PROGRESS_BUTTON_DOWNLOAD_COMPLETE -> {
            //下载完成
            finish()
        }

        else -> {
        }
    }

    private fun pause(which: Int) {
        downloadManager.downloadPause(which)
        val list = datas.value!!
        val model = list[which]
        model.statusText = "继续下载${which}"
        _datas.value = list
    }

    private fun finish() {

    }
}