package com.antos.gaming.recording

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class MediaRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val target = intent.getStringExtra("TARGET_SERVICE") ?: "RECORDING"
            
            val serviceIntent = Intent(this, com.antos.gaming.monitoring.ScreenCaptureService::class.java).apply {
                if (target == "FPS_MONITOR") {
                    action = com.antos.gaming.monitoring.ScreenCaptureService.ACTION_START
                } else {
                    action = com.antos.gaming.monitoring.ScreenCaptureService.ACTION_START_RECORD
                }
                putExtra(com.antos.gaming.monitoring.ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(com.antos.gaming.monitoring.ScreenCaptureService.EXTRA_DATA, data)
            }
            startForegroundService(serviceIntent)
        }
        finish()
    }
}
