package com.airi.odslm.jni

/**
 * Kotlin JNI interface to LlamaCppBridge.cpp.
 *
 * Contract:
 *  1. Call [loadModel] once — store the returned handle.
 *  2. Call [infer] as many times as needed on the same handle.
 *  3. Call [unloadModel] in ChatViewModel.onCleared() — MUST be called to free native memory.
 *
 * Threading:
 *  - [infer] is blocking (runs on the calling thread).
 *  - Call from Dispatchers.Default or a dedicated thread — never from Main.
 *  - Concurrent [infer] calls on the same handle are serialised by a native mutex.
 *
 * Error handling:
 *  - [loadModel] throws RuntimeException if the model cannot be loaded.
 *  - [infer] throws RuntimeException if called with an invalid handle.
 *  - Wrap all calls in runCatching{} in ChatViewModel.
 *
 * @see com.airi.odslm.viewmodel.ChatViewModel
 * @see com.airi.odslm.util.InferenceManager (Task 7)
 */
object LlamaCppBridge {

    /**
     * Load GGUF model and multimodal projector into native memory.
     *
     * @param modelPath   Absolute path to the LLM backbone (e.g. MiniCPM-V-4.6-Q4_K_M.gguf).
     * @param mmProjPath  Absolute path to the vision projector (mmproj-MiniCPM-V-4.6-F16.gguf).
     *                    Pass empty string "" to disable multimodal support.
     * @param contextSize KV-cache size in tokens. Use 4096 for Phase 1 (safe on 12GB RAM).
     * @return Opaque native handle (non-zero on success). Pass to [infer] and [unloadModel].
     * @throws RuntimeException if the model cannot be loaded.
     */
    external fun loadModel(modelPath: String, mmProjPath: String, contextSize: Int): Long

    /**
     * Run inference. Blocking — call from a background coroutine (Dispatchers.Default).
     *
     * @param contextHandle Handle returned by [loadModel].
     * @param prompt        Raw user text (no chat template — bridge applies MiniCPM-V template).
     * @param imageRgba     Raw RGBA pixel data (width × height × 4 bytes), or null for text-only.
     * @param imageWidth    Width of the image in pixels (ignored if imageRgba is null).
     * @param imageHeight   Height of the image in pixels (ignored if imageRgba is null).
     * @return Model response as a UTF-8 string.
     * @throws IllegalStateException if contextHandle is invalid (0 or freed).
     */
    external fun infer(
        contextHandle: Long,
        prompt: String,
        imageRgba: ByteArray?,
        imageWidth: Int,
        imageHeight: Int
    ): String

    /**
     * Release all native memory associated with the handle.
     * Must be called exactly once per successful [loadModel] call.
     * Safe to call with 0 (no-op).
     *
     * @param contextHandle Handle returned by [loadModel].
     */
    external fun unloadModel(contextHandle: Long)

    init {
        // libllama-jni.so is built by CMakeLists.txt from LlamaCppBridge.cpp + llama.cpp submodule
        System.loadLibrary("llama-jni")
    }
}
