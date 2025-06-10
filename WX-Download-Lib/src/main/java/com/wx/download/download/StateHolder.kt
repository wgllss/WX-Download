package com.wx.download.download

class StateHolder {
    var which: Int = 0
    val none by lazy { WXState.None().apply { which = this@StateHolder.which } }
    val waiting by lazy { WXState.Waiting().apply { which = this@StateHolder.which } }
    val downloading by lazy { WXState.Downloading().apply { which = this@StateHolder.which } }
    val pause by lazy { WXState.Pause().apply { which = this@StateHolder.which } }
    val failed by lazy { WXState.Failed().apply { which = this@StateHolder.which } }
    val succeed by lazy { WXState.Succeed().apply { which = this@StateHolder.which } }
}