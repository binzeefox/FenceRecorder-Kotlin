package com.cloud_hermits.fencerecorder.utils.recorder

import android.os.Handler
import android.os.HandlerThread
import android.os.Message

/**
 * 积分器操控类
 *
 * @author tong.xw
 * 2021/02/02 10:13
 */
class RecordController(private val maxTime: Long) {

    /**
     * 是否在记录
     */
    val recording: Boolean
        get() = isRunning || !isPausing

    /**
     * 剩余时长回调
     */
    var controllerCallback: RecordControllerCallback? = null

    // 是否在运行
    // true: 运行中、暂停中; false: 未运行、停止
    private var isRunning: Boolean = false

    // 是否暂停
    private var isPausing: Boolean = false

    // 计时器
    private val timer = object : Any() {
        private var remainTime = maxTime

        init {
            while (remainTime > 0) {
                if (isPausing) continue
                Thread.sleep(1000)
                remainTime -= 1000
                controllerCallback?.onTimeChange(remainTime)
            }
        }
    }

    fun start() {
        isRunning = true
        isPausing = false
    }

    fun stop() {
        isRunning = false
        isPausing = true
    }

    fun pause() {
        isPausing = true
    }

    fun resume() {
        isPausing = false
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////////////////////////

    enum class WHAT(val value: Int) {
        START_TIMER(0),
        PAUSE_TIMER(1)
    }
}

/**
 * 时间回调
 */
fun interface RecordControllerCallback {

    /**
     * 剩余时间变化回调
     */
    fun onTimeChange(remainTime: Long)
}