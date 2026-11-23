package com.antos.gaming.recording

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecorder(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isRecording = false
    private var videoFile: File? = null

    fun startRecording() {
        if (isRecording) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        val density = metrics.densityDpi
        var width = metrics.widthPixels
        var height = metrics.heightPixels
        
        width = if (width % 2 != 0) width - 1 else width
        height = if (height % 2 != 0) height - 1 else height

        initRecorder(width, height)
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenRecorder",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )

        mediaRecorder?.start()
        isRecording = true
    }

    private fun initRecorder(width: Int, height: Int) {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val antosDir = File(moviesDir, "ANTOS")
        if (!antosDir.exists()) {
            antosDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        videoFile = File(antosDir, "ANTOS_$timestamp.mp4")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            // Audio recording disabled for simplicity, requires RECORD_AUDIO permission
            // setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile?.absolutePath)
            setVideoSize(width, height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            // setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(8000000)
            setVideoFrameRate(60)
            prepare()
        }
    }

    fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        virtualDisplay?.release()
        
        mediaRecorder = null
        virtualDisplay = null
        isRecording = false
    }
}
