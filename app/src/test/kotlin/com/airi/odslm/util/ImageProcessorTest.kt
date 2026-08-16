package com.airi.odslm.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests for [ImageProcessor] that can run on JVM without Android framework.
 *
 * Tests requiring real Bitmap (resize, encode) are integration tests — they need
 * a real Android device or Robolectric. Those are covered in §2.1 integration
 * scenarios I3 (multimodal inference). Not added here to avoid Robolectric
 * dependency for Phase 1 PoC. (ponytail: YAGNI)
 */
class ImageProcessorTest {

    private val mockContext: Context = mockk()
    private val mockResolver: ContentResolver = mockk()
    private val mockUri: Uri = mockk()

    init {
        every { mockContext.contentResolver } returns mockResolver
    }

    @Test
    fun `resizeAndEncode returns null when URI stream cannot be opened`() {
        every { mockResolver.openInputStream(mockUri) } returns null

        val result = ImageProcessor.resizeAndEncode(mockContext, mockUri)

        assertNull(result, "Should return null when ContentResolver returns null stream")
    }

    @Test
    fun `resizeAndEncode returns null when stream contains non-image bytes`() {
        // BitmapFactory.decodeStream returns null for non-image data
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream(ByteArray(16))

        val result = ImageProcessor.resizeAndEncode(mockContext, mockUri)

        // BitmapFactory returns null → ImageProcessor propagates null
        assertNull(result, "Should return null when stream is not a valid image")
    }

    @Test
    fun `resizeAndEncode returns null on ContentResolver exception`() {
        every { mockResolver.openInputStream(mockUri) } throws SecurityException("Permission denied")

        val result = ImageProcessor.resizeAndEncode(mockContext, mockUri)

        assertNull(result, "Should return null (not throw) on SecurityException")
    }
}
