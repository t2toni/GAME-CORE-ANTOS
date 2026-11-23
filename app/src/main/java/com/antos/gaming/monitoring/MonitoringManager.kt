package com.antos.gaming.monitoring

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

class MonitoringManager private constructor(context: Context) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val batteryMonitor = BatteryMonitor(context)
    val temperatureMonitor = TemperatureMonitor(context)
    val memoryMonitor = MemoryMonitor(context)
    val networkMonitor = NetworkMonitor(context)
    val normalFpsMonitor = FpsMonitor()
    val gameFpsMonitor = GameFpsMonitor()

    val batteryLevel: StateFlow<Int> = batteryMonitor.batteryLevel
    val temperature: StateFlow<Float> = temperatureMonitor.temperature
    val availableMemoryGb: StateFlow<Float> = memoryMonitor.availableMemoryGb
    val normalFpsState: StateFlow<FpsState> = normalFpsMonitor.state
    val accurateFpsState: StateFlow<FpsState> = gameFpsMonitor.state
    val fpsState: StateFlow<FpsState> = normalFpsMonitor.state
    val latency: StateFlow<Long> = networkMonitor.latency
    val networkType: StateFlow<String> = networkMonitor.networkType

    private var activeCount = 0

    @Synchronized
    fun startMonitoring() {
        if (activeCount == 0) {
            batteryMonitor.start()
            temperatureMonitor.start()
            memoryMonitor.start(coroutineScope)
            networkMonitor.start(coroutineScope)
            normalFpsMonitor.start()
            // Note: GameFpsMonitor (accurate visual FPS) is started via ScreenCaptureService
        }
        activeCount++
    }

    @Synchronized
    fun stopMonitoring() {
        if (activeCount > 0) {
            activeCount--
            if (activeCount == 0) {
                batteryMonitor.stop()
                temperatureMonitor.stop()
                memoryMonitor.stop()
                networkMonitor.stop()
                normalFpsMonitor.stop()
                gameFpsMonitor.stop()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MonitoringManager? = null

        fun getInstance(context: Context): MonitoringManager {
            return instance ?: synchronized(this) {
                instance ?: MonitoringManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
