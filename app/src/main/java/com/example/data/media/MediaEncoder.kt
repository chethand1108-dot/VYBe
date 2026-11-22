package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class EncodedMedia(
    val mediaType: String,
    val dataUrl: String,
    val durationSec: Long = 0L
)

object MediaEncoder {
    suspend fun encodeFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        maxBase64Chars: Int = 180_000
    ): Result<EncodedMedia> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.applicationContext.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            if (mimeType.startsWith("video/")) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context.applicationContext, uri)
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    val durationSec = (durationMs / 1000L).coerceIn(1L, 3600L)
                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                        ?: error("Could not read video preview frame.")
                    val scaled = scaleBitmap(frame, maxDimension)
                    val dataUrl = compressToDataUrl(scaled, maxBase64Chars)
                    if (scaled !== frame) frame.recycle()
                    Result.success(EncodedMedia(mediaType = "video", dataUrl = dataUrl, durationSec = durationSec))
                } finally {
                    retriever.release()
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                    ?: error("Could not decode selected image.")
                val scaled = scaleBitmap(decoded, maxDimension)
                val dataUrl = compressToDataUrl(scaled, maxBase64Chars)
                if (scaled !== decoded) decoded.recycle()
                Result.success(EncodedMedia(mediaType = "photo", dataUrl = dataUrl, durationSec = 0L))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeDataUrl(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return try {
            val base64Part = dataUrl.substringAfter("base64,", dataUrl)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var inSampleSize = 1
        val largest = max(width, height)
        if (largest > reqSize) {
            while ((largest / inSampleSize) > reqSize * 2) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        val largest = max(w, h)
        if (largest <= maxDim) return source
        val scale = maxDim.toFloat() / largest.toFloat()
        val targetW = (w * scale).roundToInt().coerceAtLeast(1)
        val targetH = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun compressToDataUrl(bitmap: Bitmap, maxBase64Chars: Int): String {
        var quality = 76
        val stream = ByteArrayOutputStream()
        while (quality >= 28) {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$encoded"
            if (dataUrl.length <= maxBase64Chars) {
                return dataUrl
            }
            quality -= 12
        }
        stream.reset()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 24, stream)
        val fallback = "data:image/jpeg;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        require(fallback.length <= maxBase64Chars) { "Media exceeds upload limit even after compression." }
        return fallback
    }
}
