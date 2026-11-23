package com.antos.gaming.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class GameFpsMonitor {

    private val _state = MutableStateFlow(FpsState())
    val state: StateFlow<FpsState> = _state.asStateFlow()

    private var windowStartNs = 0L
    private var windowFrames = 0

    private var totalFrames = 0L

    private var minFps = Int.MAX_VALUE
    private var maxFps = 0

    fun start() {
        windowStartNs = 0L
        windowFrames = 0
        totalFrames = 0

        minFps = Int.MAX_VALUE
        maxFps = 0

        _state.value = FpsState(
            isMonitoring = true
        )
    }

    fun onFrame(timestampNs: Long) {

        if (windowStartNs == 0L) {
            windowStartNs = timestampNs
            return
        }

        windowFrames++
        totalFrames++

        val elapsedNs =
            timestampNs - windowStartNs

        if (elapsedNs >= 1_000_000_000L) {

            val fps =
                (windowFrames.toDouble() *
                        1_000_000_000.0 /
                        elapsedNs)
                    .roundToInt()

            minFps = minOf(minFps, fps)
            maxFps = maxOf(maxFps, fps)

            val currentHistory = _state.value.history.toMutableList()
            currentHistory.add(fps)
            if (currentHistory.size > 20) {
                currentHistory.removeAt(0)
            }

            _state.value = _state.value.copy(
                fps = fps,
                averageFps = fps,
                minimumFps =
                    if (minFps == Int.MAX_VALUE)
                        fps
                    else
                        minFps,
                maximumFps = maxFps,
                frameCount = totalFrames,
                history = currentHistory
            )

            windowFrames = 0
            windowStartNs = timestampNs
        }
    }

    fun stop() {

        _state.value = _state.value.copy(
            isMonitoring = false
        )
    }
}
