package com.wx.download.download

sealed class WXState {
    var which: Int = 0
        internal set

    class None : WXState()
    class Waiting : WXState()

    class Downloading : WXState() {
        var progress: Float = 0f
            internal set
    }

    class Pause : WXState()
    class Failed : WXState()
    class Succeed : WXState()
}