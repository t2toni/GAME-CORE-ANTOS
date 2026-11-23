package com.antos.gaming.monitoring

data class FpsState(
    val fps: Int = 0,
    val averageFps: Int = 0,
    val minimumFps: Int = 0,
    val maximumFps: Int = 0,
    val frameCount: Long = 0,
    val droppedFrames: Long = 0,
    val isMonitoring: Boolean = false,
    val history: List<Int> = emptyList()
)
