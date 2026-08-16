package com.airi.odslm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Preprocesses images before sending to the model.
 *
 * Responsibilities:
 * - Resize bitmap to max [MAX_DIMENSION] on the longest edge (preserving aspect ratio).
 * - Extract raw RGBA byte array for JNI transfer (mtmd format).
 *
 * Called from [ChatViewModel], not from Activity (MVVM boundary).
 *
 * Returns null on any error — caller treats null as "no image".
 */
object ImageProcessor {

    /** Max dimension (width or height) in pixels before inference. Per SRS §3.2 / Arch §2.2. */
    private const val MAX_DIMENSION = 1024

    /** JPEG quality for encoding. 85 balances quality vs memory. */
    private const val JPEG_QUALITY = 85

    /**
     * Opens [uri], resizes to fit within [MAX_DIMENSION]×[MAX_DIMENSION], and extracts RGBA bytes.
     *
     * @return Pair of RGBA byte array and dimensions (width, height), or null on error.
     */
    fun processImage(context: Context, uri: Uri): Pair<ByteArray, Pair<Int, Int>>? = runCatching {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        
        // Ensure we get ARGB_8888 for consistent 4-byte RGBA extraction
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val original = BitmapFactory.decodeStream(inputStream, null, options) ?: return null
        inputStream.close()

        val resized = resize(original)
        
        // Extract raw RGBA bytes
        val size = resized.rowBytes * resized.height
        val byteBuffer = java.nio.ByteBuffer.allocate(size)
        resized.copyPixelsToBuffer(byteBuffer)
        
        val width = resized.width
        val height = resized.height
        
        if (resized !== original) resized.recycle()
        original.recycle()
        
        Pair(byteBuffer.array(), Pair(width, height))
    }.getOrNull()

    /** Scales [bitmap] down so neither dimension exceeds [MAX_DIMENSION]. No-op if already fits. */
    private fun resize(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_DIMENSION) return bitmap

        val scale = MAX_DIMENSION.toFloat() / maxSide
        val targetWidth = (bitmap.width * scale).toInt()
        val targetHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
