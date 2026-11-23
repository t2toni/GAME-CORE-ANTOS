package com.antos.gaming.ui.sidebar

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.widget.Toast
import android.app.ActivityOptions

fun openAppInPopup(
    context: Context,
    packageName: String
) {
    try {
        val pm = context.packageManager

        val launchIntent =
            pm.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            Toast.makeText(
                context,
                "App is not installed",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        )

        val options = ActivityOptions.makeBasic()

        // Force freeform window mode via reflection (WINDOWING_MODE_FREEFORM = 5)
        try {
            val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            method.invoke(options, 5)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Use dynamic size based on screen metrics for better popup placement
        val dm = context.resources.displayMetrics
        val width = (dm.widthPixels * 0.75).toInt()
        val height = (dm.heightPixels * 0.65).toInt()
        val left = (dm.widthPixels - width) / 2
        val top = (dm.heightPixels - height) / 2
        val bounds = Rect(left, top, left + width, top + height)

        options.launchBounds = bounds

        context.startActivity(
            launchIntent,
            options.toBundle()
        )

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Popup mode isn't supported on this device",
            Toast.LENGTH_SHORT
        ).show()

        // Fallback: open normally
        val intent =
            context.packageManager
                .getLaunchIntentForPackage(packageName)

        intent?.let {
            context.startActivity(it)
        }
    }
}
