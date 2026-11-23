package com.antos.gaming.monitoring

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class FpsMonitor : Choreographer.FrameCallback {

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _state = MutableStateFlow(FpsState(isMonitoring = true))
    val state: StateFlow<FpsState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var startTimeNanos = 0L
    private var frameCount = 0
    private var totalFrames = 0L
    private var minFps = Int.MAX_VALUE
    private var maxFps = 0
    private var isRunning = false

    fun start() {
        mainHandler.post {
            if (isRunning) return@post
            isRunning = true
            startTimeNanos = 0L
            frameCount = 0
            totalFrames = 0L
            minFps = Int.MAX_VALUE
            maxFps = 0
            _state.value = _state.value.copy(isMonitoring = true)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stop() {
        mainHandler.post {
            if (!isRunning) return@post
            isRunning = false
            Choreographer.getInstance().removeFrameCallback(this)
            frameCount = 0
            startTimeNanos = 0L
            _fps.value = 0
            _state.value = _state.value.copy(isMonitoring = false)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        if (startTimeNanos == 0L) {
            startTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
            return
        }

        frameCount++
        totalFrames++

        val elapsedNanos = frameTimeNanos - startTimeNanos
        if (elapsedNanos >= 1_000_000_000L) {
            val calculatedFps = (frameCount * 1_000_000_000.0 / elapsedNanos).roundToInt()
            _fps.value = calculatedFps

            minFps = minOf(minFps, calculatedFps)
            maxFps = maxOf(maxFps, calculatedFps)

            val currentHistory = _state.value.history.toMutableList()
            currentHistory.add(calculatedFps)
            if (currentHistory.size > 20) {
                currentHistory.removeAt(0)
            }

            _state.value = _state.value.copy(
                fps = calculatedFps,
                averageFps = calculatedFps,
                minimumFps = if (minFps == Int.MAX_VALUE) calculatedFps else minFps,
                maximumFps = maxFps,
                frameCount = totalFrames,
                history = currentHistory,
                isMonitoring = true
            )

            frameCount = 0
            startTimeNanos = frameTimeNanos
        }

        Choreographer.getInstance().postFrameCallback(this)
    }
}
