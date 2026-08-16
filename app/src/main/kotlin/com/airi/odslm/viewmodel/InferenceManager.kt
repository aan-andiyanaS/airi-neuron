package com.airi.odslm.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.airi.odslm.jni.LlamaCppBridge
import com.airi.odslm.util.ImageProcessor
import com.airi.odslm.util.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * Manages the lifecycle of LlamaCppBridge and handles inference execution on a background thread.
 * Requirements: Inference Layer (Task 7, Task 9).
 */
class InferenceManager(private val context: Context) {
    private val performanceMonitor = PerformanceMonitor(context)
    private var modelHandle: Long = 0L
    private val mutex = Mutex()

    companion object {
        private const val TAG = "InferenceManager"
    }

    /**
     * Initializes the LlamaCppBridge. 
     * In Phase 1, we expect the GGUF to be placed in standard app storage or sdcard.
     */
    suspend fun loadModel(modelPath: String, mmProjPath: String, contextSize: Int): Boolean = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (!File(modelPath).exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return@withContext false
            }
            
            try {
                modelHandle = LlamaCppBridge.loadModel(modelPath, mmProjPath, contextSize)
                Log.i(TAG, "Model loaded successfully with handle: $modelHandle")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model", e)
                false
            }
        }
    }

    /**
     * Runs text or multimodal inference.
     * Extracts RGBA byte array using ImageProcessor if an image Uri is provided.
     */
    suspend fun infer(prompt: String, imageUri: Uri?): Result<String> = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (modelHandle == 0L) {
                return@withContext Result.failure(IllegalStateException("Error: Model is not loaded. Please ensure the model file is on the device."))
            }

            var imageBytes: ByteArray? = null
            var imgWidth = 0
            var imgHeight = 0

            // Process image if attached
            if (imageUri != null) {
                Log.i(TAG, "Processing image: $imageUri")
                val processed = ImageProcessor.processImage(context, imageUri)
                if (processed != null) {
                    imageBytes = processed.first
                    imgWidth = processed.second.first
                    imgHeight = processed.second.second
                    Log.i(TAG, "Image processed to ${imgWidth}x${imgHeight} RGBA")
                } else {
                    Log.e(TAG, "Failed to process image")
                    return@withContext Result.failure(IllegalArgumentException("Error: Failed to process the attached image."))
                }
            }

            // Run inference
            Log.i(TAG, "Starting inference...")
            performanceMonitor.logThermalStatus()
            val startTime = System.currentTimeMillis()

            return@withContext runCatching {
                val response = LlamaCppBridge.infer(modelHandle, prompt, imageBytes, imgWidth, imgHeight)
                
                // Log TTFT (a bit simplified here since infer() is synchronous returning the whole string,
                // but we log total time which is close to TTFT + gen time)
                performanceMonitor.logTotalTime(startTime, response.split(" ").size) // Rough token estimate
                
                response
            }.onFailure { e ->
                Log.e(TAG, "Inference failed", e)
            }
        }
    }

    /**
     * Frees native memory. Must be called when ViewModel is cleared.
     */
    fun unloadModel() {
        // Launch on NonCancellable to ensure memory is freed even if ViewModel scope is dying
        CoroutineScope(Dispatchers.Default + NonCancellable).launch {
            mutex.withLock {
                if (modelHandle != 0L) {
                    LlamaCppBridge.unloadModel(modelHandle)
                    modelHandle = 0L
                    Log.i(TAG, "Model unloaded")
                }
            }
        }
    }
}
