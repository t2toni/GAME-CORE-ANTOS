package com.antos.gaming.data

data class GameProfile(
    val packageName: String,
    val performanceMode: Boolean = true,
    val brightness: Float = -1f, // -1 means system default
    val isLandscapeLocked: Boolean = false,
    val blockNotifications: Boolean = true,
    val showFps: Boolean = true,
    val touchProtection: Boolean = false
)
