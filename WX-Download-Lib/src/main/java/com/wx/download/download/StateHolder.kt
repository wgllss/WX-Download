package com.wx.download.download

class StateHolder {
    val none by lazy { WXState.None() }
    val waiting by lazy { WXState.Waiting() }
    val downloading by lazy { WXState.Downloading() }
    val stopped by lazy { WXState.Pause() }
    val failed by lazy { WXState.Failed() }
    val succeed by lazy { WXState.Succeed() }
}