package com.nofish.asm_test

import android.os.SystemClock
import android.util.Log
import android.view.View


object ViewClickMonitor {

    private const val MIN_DURATION = 500L

    private var lastClickTime = 0L

    private var clickIndex = 0

    @JvmStatic
    fun onClick(view: View): Boolean {
        clickIndex++
        val currentTime = SystemClock.elapsedRealtime()
        val isEnabled = currentTime - lastClickTime > MIN_DURATION
        if (isEnabled) {
            lastClickTime = currentTime
        }
        log("onClick $clickIndex , isEnabled : $isEnabled")
        return isEnabled
    }

    private fun log(log: String) {
        Log.e("ViewClickCheck", log)
    }

}