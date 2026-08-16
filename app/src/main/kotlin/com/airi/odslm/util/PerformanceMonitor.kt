package com.airi.odslm.util

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Monitors device thermal and performance metrics during inference.
 * Requirements: NF11, Risk 2 (Task 9).
 */
class PerformanceMonitor(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    
    companion object {
        private const val TAG = "PerformanceMonitor"
    }

    /**
     * Logs the current thermal state of the device.
     */
    fun logThermalStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Use getThermalHeadroom (1 second prediction)
            val headroom = powerManager?.getThermalHeadroom(1) ?: Float.NaN
            Log.i(TAG, "Thermal Headroom (API 30+): $headroom")
        } else {
            // API 29 (S20 Ultra min API): We can't get exact temperature easily without registering receiver
            // We just log that we are on API 29 for now.
            Log.i(TAG, "Thermal Headroom not available on API 29 without Battery Receiver")
        }
    }

    /**
     * Measures TTFT (Time-To-First-Token) and logs it.
     */
    fun logTTFT(startTimeMs: Long) {
        val ttft = System.currentTimeMillis() - startTimeMs
        Log.i(TAG, "TTFT (Time-To-First-Token): $ttft ms")
    }

    /**
     * Measures total generation time and logs it.
     */
    fun logTotalTime(startTimeMs: Long, tokenCount: Int) {
        val totalTime = System.currentTimeMillis() - startTimeMs
        val tps = if (totalTime > 0) tokenCount / (totalTime / 1000f) else 0f
        Log.i(TAG, "Total Inference Time: $totalTime ms ($tokenCount tokens, ${String.format("%.2f", tps)} tok/s)")
    }
}
