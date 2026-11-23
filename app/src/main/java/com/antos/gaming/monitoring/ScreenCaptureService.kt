package com.antos.gaming.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.antos.gaming.recording.ScreenRecorder

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "START_CAPTURE" // For FPS
        const val ACTION_STOP = "STOP_CAPTURE"
        const val ACTION_START_RECORD = "START_RECORD"
        const val ACTION_STOP_RECORD = "STOP_RECORD"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        const val CHANNEL_ID = "fps_monitor"
        const val NOTIFICATION_ID = 1001
        var isRecording = false
        var isFpsActive = false
    }

    private var projection: MediaProjection? = null
    
    // FPS Monitor resources
    private var imageReader: ImageReader? = null
    private var visualFrameAnalyzer: VisualFrameAnalyzer? = null
    
    // Recording resources
    private var screenRecorder: ScreenRecorder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        when (intent?.action) {
            ACTION_START -> {
                isFpsActive = true
                if (data != null) {
                    handleNewProjection(resultCode, data)
                } else if (projection != null) {
                    startFps()
                }
            }
            ACTION_STOP -> {
                isFpsActive = false
                stopFps()
                checkStopService()
            }
            ACTION_START_RECORD -> {
                isRecording = true
                if (data != null) {
                    handleNewProjection(resultCode, data)
                } else if (projection != null) {
                    startRecording()
                }
            }
            ACTION_STOP_RECORD -> {
                isRecording = false
                stopRecording()
                checkStopService()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleNewProjection(resultCode: Int, data: Intent) {
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Stop any old projection/virtual displays
        stopFps()
        stopRecording()
        projection?.stop()

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)

        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopFps()
                stopRecording()
                projection = null
                checkStopService()
            }
        }, null)

        if (isFpsActive) startFps()
        if (isRecording) startRecording()
    }
    
    private fun startFps() {
        if (imageReader != null) return // already running
        
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 3)

        val fpsMonitor = MonitoringManager.getInstance(this).gameFpsMonitor
        fpsMonitor.start()

        visualFrameAnalyzer = VisualFrameAnalyzer(
            onFramePresented = {
                fpsMonitor.onFrame(System.nanoTime())
            }
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                visualFrameAnalyzer?.process(image)
            }
        }, null)

        projection?.createVirtualDisplay(
            "AntosGamingFPS",
            width, height, metrics.densityDpi, 0,
            imageReader?.surface, null, null
        )
    }

    private fun stopFps() {
        MonitoringManager.getInstance(this).gameFpsMonitor.stop()
        visualFrameAnalyzer?.reset()
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
    }

    private fun startRecording() {
        if (screenRecorder != null) return // already running
        projection?.let {
            screenRecorder = ScreenRecorder(this, it)
            screenRecorder?.startRecording()
        }
    }

    private fun stopRecording() {
        screenRecorder?.stopRecording()
        screenRecorder = null
    }
    
    private fun checkStopService() {
        if (!isFpsActive && !isRecording) {
            projection?.stop()
            projection = null
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Antos Gaming Core")
                .setContentText("Screen capture active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Antos Gaming Core")
                .setContentText("Screen capture active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isFpsActive = false
        isRecording = false
        stopFps()
        stopRecording()
        projection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
