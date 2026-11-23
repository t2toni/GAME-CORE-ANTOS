package com.antos.gaming.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VioletColorScheme = darkColorScheme(
    primary = VioletMain,
    secondary = VioletLight,
    tertiary = HudAccent,
    background = NearBlack,
    surface = DarkPanel,
    error = ErrorRed,
    onPrimary = NearBlack,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = TextPrimary
)

@Composable
fun AntosTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = VioletColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
