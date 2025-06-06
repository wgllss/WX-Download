package com.wx.download.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip

object FlowUtils {

    fun flowZip(flowNum: Int, download: suspend (i: Int) -> Unit): Flow<Int> {
        var flow: Flow<Int>? = null
        var flowNew: Flow<Int>? = null
        for (i in 0 until flowNum) {
            val createFlow = flow {
                download.invoke(i)
                emit(i)
            }
            if (flow == null) {
                flow = createFlow
            } else {
                if (flowNew == null) {
                    val f = flow!!.zip(createFlow) { it, it2 ->
                        WLog.i(this@FlowUtils, "下载成功:$it")
                        WLog.i(this@FlowUtils, "下载成功:$it2")

                        return@zip 0
                    }
                    flowNew = f
                } else {
                    val f = flowNew!!.zip(createFlow) { _, it2 ->
                        WLog.i(this@FlowUtils, "下载成功:$it2")

                        return@zip 0
                    }
                    flowNew = f
                }
            }
        }
        return flowNew!!
    }

}