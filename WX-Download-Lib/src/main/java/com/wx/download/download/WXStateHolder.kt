package com.wx.download.download

class WXStateHolder {
    var which: Int = 0
    val none by lazy { WXState.None().apply { which = this@WXStateHolder.which } }
    val waiting by lazy { WXState.Waiting().apply { which = this@WXStateHolder.which } }
    val downloading by lazy { WXState.Downloading().apply { which = this@WXStateHolder.which } }
    val pause by lazy { WXState.Pause().apply { which = this@WXStateHolder.which } }
    val failed by lazy { WXState.Failed().apply { which = this@WXStateHolder.which } }
    val succeed by lazy { WXState.Succeed().apply { which = this@WXStateHolder.which } }
}