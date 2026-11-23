package com.antos.gaming

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.widget.Toast

object FloatingAppLauncher {

    const val DISCORD = "com.discord"
    const val BRAVE = "com.brave.browser"
    const val INSTAGRAM = "com.instagram.android"

    fun launch(
        context: Context,
        packageName: String
    ) {

        val pm = context.packageManager

        val intent = pm.getLaunchIntentForPackage(packageName)

        if (intent == null) {

            Toast.makeText(
                context,
                "App is not installed",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            try {

                /*
                 * Request a floating/freeform window.
                 *
                 * These coordinates are only a REQUEST.
                 * Android/Motorola decides whether the
                 * requested bounds are actually honoured.
                 */

                val bounds = Rect(
                    150,   // left
                    150,   // top
                    850,   // right
                    900    // bottom
                )

                val options =
                    ActivityOptions.makeBasic()

                options.launchBounds = bounds

                context.startActivity(
                    intent,
                    options.toBundle()
                )

                return

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback
        context.startActivity(intent)
    }
}
