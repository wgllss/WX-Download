package com.wx.download.utils

object WLog {
    var isDeBug = true

    fun e(any: Any, message: String) {
        if (isDeBug) android.util.Log.e(any.javaClass.simpleName, message)
    }

    fun i(any: Any, message: String) {
        if (isDeBug) android.util.Log.i(any.javaClass.simpleName, message)
    }
}