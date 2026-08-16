package com.airi.odslm.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Validates user inputs (text and image) before sending them to the model.
 * Requirements: NF8 (Task 8).
 */
object InputValidator {
    private const val MAX_TEXT_LENGTH = 1000
    private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

    fun validateText(text: String): Boolean {
        return text.isNotBlank() && text.length <= MAX_TEXT_LENGTH
    }

    fun validateImage(context: Context, uri: Uri): Boolean {
        var size: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size <= MAX_IMAGE_SIZE_BYTES
    }
}
