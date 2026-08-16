package com.airi.odslm.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.airi.odslm.jni.LlamaCppBridge
import com.airi.odslm.util.ImageProcessor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class InferenceManagerTest {

    private lateinit var context: Context
    private lateinit var inferenceManager: InferenceManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        
        // Mock static Android Log
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Mock Objects
        mockkObject(LlamaCppBridge)
        mockkObject(ImageProcessor)

        inferenceManager = InferenceManager(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkObject(LlamaCppBridge)
        unmockkObject(ImageProcessor)
    }

    @Test
    fun `loadModel returns false if file does not exist`() = runTest {
        // We cannot easily mock File constructor, so we just use a bogus path that doesn't exist
        val result = inferenceManager.loadModel("/invalid/path/bogus.gguf", "", 1024)
        assertFalse(result)
        verify(exactly = 0) { LlamaCppBridge.loadModel(any(), any(), any()) }
    }

    @Test
    fun `infer returns error when model is not loaded`() = runTest {
        val result = inferenceManager.infer("Hello", null)
        assertTrue(result.startsWith("Error: Model is not loaded"))
    }

    @Test
    fun `infer falls back to error if image processing fails`() = runTest {
        // Force loadModel to bypass file check for testing. Wait, modelHandle is private.
        // We need to use reflection or just test the image processing path while handle is 0?
        // Wait, if handle is 0, it returns early.
        // Let's use reflection to set modelHandle
        val field = InferenceManager::class.java.getDeclaredField("modelHandle")
        field.isAccessible = true
        field.set(inferenceManager, 12345L)

        val uri = mockk<Uri>()
        every { ImageProcessor.processImage(any(), any()) } returns null

        val result = inferenceManager.infer("Hello", uri)
        
        assertTrue(result.startsWith("Error: Failed to process the attached image"))
    }

    @Test
    fun `infer returns correct response for text only`() = runTest {
        val field = InferenceManager::class.java.getDeclaredField("modelHandle")
        field.isAccessible = true
        field.set(inferenceManager, 12345L)

        every { LlamaCppBridge.infer(any(), any(), any(), any(), any()) } returns "Test response"

        val result = inferenceManager.infer("Hello", null)
        
        assertEquals("Test response", result)
        verify { LlamaCppBridge.infer(12345L, "Hello", null, 0, 0) }
    }

    @Test
    fun `infer returns correct response for multimodal`() = runTest {
        val field = InferenceManager::class.java.getDeclaredField("modelHandle")
        field.isAccessible = true
        field.set(inferenceManager, 12345L)

        val uri = mockk<Uri>()
        val dummyBytes = ByteArray(10)
        every { ImageProcessor.processImage(any(), any()) } returns Pair(dummyBytes, Pair(100, 200))
        every { LlamaCppBridge.infer(any(), any(), any(), any(), any()) } returns "Image response"

        val result = inferenceManager.infer("Describe image", uri)
        
        assertEquals("Image response", result)
        verify { LlamaCppBridge.infer(12345L, "Describe image", dummyBytes, 100, 200) }
    }
}
