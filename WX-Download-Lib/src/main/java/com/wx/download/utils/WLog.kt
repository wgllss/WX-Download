package com.wx.download.utils

import com.wx.download.BuildConfig


object WLog {

    fun e(any: Any, message: String) {
        if (BuildConfig.DEBUG) android.util.Log.e(any.javaClass.simpleName, message)
    }

    fun i(any: Any, message: String) {
        if (BuildConfig.DEBUG) android.util.Log.i(any.javaClass.simpleName, message)
    }
}