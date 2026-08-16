# System Prompt & Context for AI Code Generator
## AIRI Neuron — Phase 1: On-Device Multimodal SLM

**Version:** 1.1  
**Date:** 2026-08-16  
**Purpose:** Guidelines for AI code generator to produce consistent, safe, and standards-compliant code  

---

## 1. Coding Style Instructions

### 1.1 Programming Languages
- **Kotlin** for all Android code (UI, ViewModel, Repository, Data layer).
- **C++** for JNI layer and llama.cpp binding.
- **No Java** — all new code must be Kotlin, except JNI binding which is C++.

### 1.2 Code Structure
- Use coroutines (`Dispatchers.Default`) for inference, do not block main thread.
- Error handling must use `runCatching`, no uncaught exceptions allowed.
- Comments only for "why" complex logic exists, not for explaining syntax.
- Variable and function names must be descriptive (e.g., `inferenceManager`, not `im`).
- Avoid magic numbers — use `companion object` for constants.

### 1.3 Desired Code Examples

```kotlin
// CORRECT: Error handling with runCatching, descriptive names
suspend fun infer(prompt: String, imageBytes: ByteArray?): Result<String> {
    return runCatching {
        val tokens = llamaBridge.tokenize(prompt)
        val result = llamaBridge.decode(tokens, imageBytes)
        result.text
    }
}

// INCORRECT: Exception not handled, non-descriptive names
fun infer(p: String): String {
    return llamaBridge.decode(llamaBridge.tokenize(p), null) // can throw exception
}
```

---

## 2. Coding Rules

### 2.1 Public Functions
- Every public function must have at least 1 unit test (JUnit 5 for Kotlin, GoogleTest for C++).
- Functions performing I/O (file, database, network) must be `suspend` or return `Result<T>`.
- Functions that can fail (inference, file load) must return `Result<T>`, not direct value.

### 2.2 UI
- UI must be responsive — show loading indicator during inference.
- No blocking operations on main thread (inference, large file load, large database queries).
- Use StateFlow/LiveData for reactive UI updates, not manual text setting in callbacks.

### 2.3 Memory Management
- GGUF model must not be loaded fully into heap — must use `mmap`.
- Image bitmaps must be resized before processing (max 1024x1024 for multimodal).
- Image cache must be cleared when storage is low (use `onTrimMemory()` callback).

### 2.4 Lifecycle
- Coroutine scope must be properly cancelled when activity/fragment is destroyed.
- JNI resources (local refs) must be released with `deleteLocalRef` when no longer used.
- Model must be unloaded when app is closed (no native memory leaks).

---

## 3. Security Guardrails

### 3.1 Input Validation
- Validate image file input (max 10MB size, jpg/png format) before processing.
- Validate text input (max 2000 characters) before sending to model.
- Never trust user input — always sanitize before using in database queries or file paths.

### 3.2 Output Filter
- Add simple output filter (regex for dangerous keywords) before displaying model response.
- Fallback to safe response ("Sorry, I cannot answer that") if output is filtered.
- Log filtered output for debugging (but do not log sensitive user data).

### 3.3 Data Privacy
- No network access without explicit user permission (phase 1 offline-first).
- Chat history stored locally, not sent to any server.
- GGUF model must not modify system files — read-only from existing files.

---

## 4. Testing Rules

> See [05_Testing_Rules.md](05_Testing_Rules.md) for complete testing rules, coverage targets, edge cases, integration test scenarios, and CI pipeline configuration.

---

## 5. Human-in-the-loop Oversight

> See [04_Risk_Management.md](04_Risk_Management.md) §4 for the complete code review checklist and manual validation requirements.

---

## 6. Example Prompts for AI Code Generator

### 6.1 UI Prompt
```
Create ChatActivity.kt with RecyclerView for chat history, EditText for text input, 
and attach image button. Use StateFlow from ChatViewModel for UI updates. 
Show ProgressBar during inference. Do not block main thread.
```

### 6.2 ViewModel Prompt
```
Create ChatViewModel.kt with StateFlow for chat messages and loading state. 
Have function sendPrompt(prompt: String, imageBytes: ByteArray?) that calls 
InferenceManager.infer() in Dispatchers.Default, then updates StateFlow with result. 
Handle errors with runCatching, fallback to error message if failed.
ImageProcessor must be called from ViewModel (not from Activity) to maintain MVVM separation.
```

### 6.3 JNI Prompt
```
Create LlamaCppBridge.cpp with JNI functions: loadModel(path: String), 
infer(prompt: String, imageBytes: ByteArray?), unloadModel(). 
Use llama_tokenize() and llama_decode() from llama.cpp. 
Load model via mmap, do not read fully into heap. Handle errors with return Result.
```

---

## 7. References
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- Android Architecture Components: https://developer.android.com/topic/architecture
- llama.cpp documentation: https://github.com/ggml-org/llama.cpp
- JNI Specification: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/jniTOC.html
