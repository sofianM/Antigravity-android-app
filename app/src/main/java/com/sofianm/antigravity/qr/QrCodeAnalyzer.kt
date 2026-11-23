package com.sofianm.antigravity.qr

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.sofianm.antigravity.data.UrlValidator

/**
 * CameraX ImageAnalysis.Analyzer backed by ZXing for fast, 100% offline QR code decoding.
 * Supports both light-on-dark and dark-on-light QR codes (helpful when scanning dark-theme screens).
 */
class QrCodeAnalyzer(
    private val onUrlScanned: (String) -> Unit,
    private val onInvalidQrScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    @Volatile
    private var lastProcessedMs: Long = 0L

    @Volatile
    private var paused: Boolean = false

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (paused) return
            val now = System.currentTimeMillis()
            if (now - lastProcessedMs < 200L) return
            lastProcessedMs = now

            if (image.format != ImageFormat.YUV_420_888 &&
                image.format != ImageFormat.YUV_422_888 &&
                image.format != ImageFormat.YUV_444_888
            ) {
                return
            }

            val yPlane = image.planes.firstOrNull() ?: return
            val width = image.width
            val height = image.height
            val rowStride = yPlane.rowStride
            val buffer = yPlane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            // Copy contiguous Y luminance plane respecting rowStride
            val yBytes = if (rowStride == width) {
                data
            } else {
                ByteArray(width * height).also { out ->
                    for (row in 0 until height) {
                        val srcOffset = row * rowStride
                        val copyLen = minOf(width, data.size - srcOffset)
                        if (copyLen > 0) {
                            System.arraycopy(data, srcOffset, out, row * width, copyLen)
                        }
                    }
                }
            }

            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val decodedText = decodeFromSource(source) ?: decodeFromSource(source.invert())
            if (!decodedText.isNullOrBlank()) {
                val extractedUrl = UrlValidator.extractUrlFromQrPayload(decodedText)
                if (extractedUrl != null) {
                    paused = true
                    onUrlScanned(extractedUrl)
                } else {
                    onInvalidQrScanned(decodedText)
                }
            }
        } finally {
            image.close()
        }
    }

    private fun decodeFromSource(source: com.google.zxing.LuminanceSource): String? {
        return runCatching {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(bitmap).text
        }.getOrNull().also {
            reader.reset()
        }
    }
}
