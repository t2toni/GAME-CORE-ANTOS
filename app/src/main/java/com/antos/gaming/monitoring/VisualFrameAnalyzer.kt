package com.antos.gaming.monitoring

import android.media.Image
import java.nio.ByteBuffer

class VisualFrameAnalyzer(
    private val onFramePresented: () -> Unit
) {

    private var previousSignature: Long = 0L
    private var initialized = false

    fun process(image: Image) {

        try {
            val signature = calculateSignature(image)

            if (!initialized) {
                previousSignature = signature
                initialized = true
                return
            }

            if (signature != previousSignature) {
                previousSignature = signature
                onFramePresented()
            }

        } finally {
            image.close()
        }
    }

    private fun calculateSignature(image: Image): Long {

        val plane = image.planes[0]

        val buffer: ByteBuffer = plane.buffer

        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val width = image.width
        val height = image.height

        /*
         * Sample multiple points across the screen.
         *
         * This avoids hashing the entire 1080p/1440p frame.
         */
        val points = arrayOf(
            0.20f to 0.20f,
            0.50f to 0.20f,
            0.80f to 0.20f,

            0.20f to 0.50f,
            0.50f to 0.50f,
            0.80f to 0.50f,

            0.20f to 0.80f,
            0.50f to 0.80f,
            0.80f to 0.80f
        )

        var hash = 17L

        for ((xRatio, yRatio) in points) {

            val x = (width * xRatio).toInt()
            val y = (height * yRatio).toInt()

            val position =
                y * rowStride + x * pixelStride

            if (position >= 0 && position < buffer.limit()) {

                val originalPosition = buffer.position()

                buffer.position(position)

                /*
                 * RGBA/YUV layout varies by device.
                 * Reading several bytes still provides a useful
                 * visual signature.
                 */
                var value = 0

                repeat(
                    minOf(
                        pixelStride,
                        buffer.remaining()
                    )
                ) {
                    value = value * 31 + buffer.get().toInt()
                }

                hash = hash * 31 + value

                buffer.position(originalPosition)
            }
        }

        return hash
    }

    fun reset() {
        previousSignature = 0L
        initialized = false
    }
}
