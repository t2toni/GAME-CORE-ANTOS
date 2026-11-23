package com.antos.gaming.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(private val context: Context) {
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }
            }
        }
    }

    fun start() {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
