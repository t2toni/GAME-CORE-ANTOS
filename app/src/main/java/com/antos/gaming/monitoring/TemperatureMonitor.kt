package com.antos.gaming.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemperatureMonitor(private val context: Context) {
    // We use battery temperature as a proxy since true CPU temp requires root or OEM APIs
    private val _temperature = MutableStateFlow(0.0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                _temperature.value = temp / 10.0f
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
