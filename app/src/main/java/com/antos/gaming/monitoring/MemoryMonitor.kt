package com.antos.gaming.monitoring

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryMonitor(private val context: Context) {
    private val _availableMemoryGb = MutableStateFlow(0.0f)
    val availableMemoryGb: StateFlow<Float> = _availableMemoryGb.asStateFlow()

    private val _totalMemoryGb = MutableStateFlow(0.0f)
    val totalMemoryGb: StateFlow<Float> = _totalMemoryGb.asStateFlow()

    private var job: Job? = null
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun start(coroutineScope: CoroutineScope) {
        job = coroutineScope.launch {
            val memoryInfo = ActivityManager.MemoryInfo()
            while (isActive) {
                activityManager.getMemoryInfo(memoryInfo)
                _availableMemoryGb.value = memoryInfo.availMem / (1024f * 1024f * 1024f)
                if (_totalMemoryGb.value == 0.0f) {
                    _totalMemoryGb.value = memoryInfo.totalMem / (1024f * 1024f * 1024f)
                }
                delay(2000) // Update every 2 seconds
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
