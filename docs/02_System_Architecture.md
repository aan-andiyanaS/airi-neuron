# System Architecture
## AIRI Neuron — Phase 1: On-Device Multimodal SLM

**Version:** 1.2  
**Date:** 2026-08-16  
**Standard:** ISO/IEC/IEEE 42010  

---

## 1. Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Kotlin)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ChatActivity │  │ChatAdapter  │  │InputEditText│  │AttachButton │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Controller Layer (Kotlin)                      │
│  ┌──────────────────────┐  ┌──────────────────────┐                 │
│  │   ChatViewModel      │  │  InferenceManager    │                 │
│  │   (StateFlow/State)  │  │  (Coroutines)        │                 │
│  │   + ImageProcessor   │  │  (ViewModel-scoped)  │                 │
│  └──────────────────────┘  └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────────┘
         │                              │
         │                              ▼
         │              ┌──────────────────────────────────────────┐
         │              │           JNI Layer (C++ NDK)            │
         │              │  ┌──────────────────────────────────┐  │
         │              │  │     LlamaCppBridge.cpp             │  │
         │              │  │  - loadModel()                     │  │
         │              │  │  - infer(prompt, image)            │  │
         │              │  │  - unloadModel()                   │  │
         │              │  └──────────────────────────────────┘  │
         │              └──────────────────────────────────────────┘
         │                              │
         │                              ▼
         │              ┌──────────────────────────────────────────┐
         │              │           llama.cpp (C++)                │
         │              │  - llama_load_model_from_file()          │
         │              │  - llama_decode()                        │
         │              │  - llama_tokenize()                      │
         │              └──────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Data Layer (Kotlin)                           │
│  ┌──────────────────────┐  ┌──────────────────────┐                 │
│  │   ChatRepository     │  │   ChatEntity         │                 │
│  │   (Room DAO)         │  │   (@Entity)          │                 │
│  └──────────────────────┘  └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Storage Layer                                 │
│  ┌──────────────────────┐  ┌──────────────────────┐                 │
│  │   Room (SQLite)      │  │   File System        │                 │
│  │   (chat_history.db)  │  │   (model.gguf)       │                 │
│  └──────────────────────┘  └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. Architecture Components

### 2.1 UI Layer

| Component | File | Description |
|---|---|---|
| ChatActivity | `ChatActivity.kt` | Main activity, initializes ViewModel and RecyclerView. Delegates all logic to ViewModel — no direct calls to ImageProcessor. |
| ChatAdapter | `ChatAdapter.kt` | Adapter for displaying chat bubbles (user vs model). `ChatViewHolder` is a **sealed class inside ChatAdapter** (not a separate file) — eliminates unchecked cast and keeps ViewHolder logic co-located with adapter. |
| InputEditText | (in layout) | EditText for user text input |
| AttachButton | (in layout) | Button to open image file picker — on result, calls `ChatViewModel.attachImage(uri)` |
| SendButton | (in layout) | Button to call `ChatViewModel.sendPrompt(text)` |
| LoadingIndicator | (in layout) | LinearProgressIndicator during inference |

### 2.2 Controller Layer

| Component | File | Description |
|---|---|---|
| ChatViewModel | `ChatViewModel.kt` | ViewModel with StateFlow for UI state. Owns `InferenceManager` (ViewModel-scoped, not singleton). Calls `ImageProcessor` internally — Activity never calls ImageProcessor directly. |
| InferenceManager | `InferenceManager.kt` | Manager for running inference in background coroutine. Lifecycle: created in ChatViewModel, cancelled in `ViewModel.onCleared()`. |
| ImageProcessor | `ImageProcessor.kt` | Image preprocessing (resize to max 1024x1024, encode) before inference. Called by ChatViewModel, not by Activity. |

> **Lifecycle note:** `InferenceManager` is **ViewModel-scoped** — it is instantiated inside `ChatViewModel` and its coroutine scope is cancelled in `ChatViewModel.onCleared()`. It is **not** a singleton or Application-scoped object. This ensures proper cleanup when the Activity is destroyed.

### 2.3 JNI Layer

| Component | File | Description |
|---|---|---|
| LlamaCppBridge | `LlamaCppBridge.cpp` | JNI binding for model load, inference, unload |
| ModelLoader | `ModelLoader.cpp` | Helper for mmap model loading |
| llama-jni.h | `llama-jni.h` | JNI header (generated via `javah` or manual) |

### 2.4 Data Layer

| Component | File | Description |
|---|---|---|
| ChatRepository | `ChatRepository.kt` | Repository pattern for chat data access |
| ChatDao | `ChatDao.kt` | Data Access Object for Room |
| ChatEntity | `ChatEntity.kt` | Room entity for chat_history table |
| AppDatabase | `AppDatabase.kt` | Database class for Room |

### 2.5 Storage Layer

| Component | Path | Description |
|---|---|---|
| Model GGUF | `context.filesDir/models/minicpm-v-4.6.Q4_K_M.gguf` | Quantized LLM backbone |
| MMProj | `context.filesDir/models/mmproj-model-f16.gguf` | Vision tower for multimodal |
| Database | `context.getDatabasePath("chat_history.db")` | SQLite database for chat history |

---

## 3. Technology Constraints

### 3.1 Inference Backend
- **Library:** llama.cpp (version with MiniCPM-V 4.6 multimodal support — pinned after Task 5 research)
- **Backend:** CPU (ARM NEON), Vulkan optional for GPU offload
- **Quantization:** Q4_K_M for LLM backbone, f16 for vision tower (mmproj)
- **Context window:** Maximum 1024 tokens (model + KV-cache)

### 3.2 Native Bridge
- **Binding:** JNI binding — prefer `examples/llama.android` official module if it covers the required API surface. Custom wrapper only if official module is insufficient (decision deferred to Task 5 research gate).
- **Thread safety:** Model access protected by Mutex, inference run in separate thread
- **Memory management:** Model loaded via mmap, not read fully into heap

### 3.3 Storage
- **Chat history:** Room (SQLite) with ChatEntity. No encryption in Phase 1 (YAGNI; deferred to Phase 2).
- **GGUF model:** File system (not stored in database)
- **Image cache:** App cache directory (auto-cleared when storage low)

---

## 4. Component Interactions

### 4.1 Text-Only Inference Flow

```
1. User types text → ChatActivity
2. ChatActivity calls ChatViewModel.sendPrompt(text)
3. ChatViewModel calls InferenceManager.infer(prompt)
4. InferenceManager runs in Dispatchers.Default
5. InferenceManager calls LlamaCppBridge.infer(prompt, null)
6. LlamaCppBridge calls llama_tokenize() + llama_decode()
7. Decoded tokens returned to InferenceManager
8. InferenceManager sends result to ChatViewModel
9. ChatViewModel updates StateFlow, UI displays response
10. ChatRepository saves history to Room
```

### 4.2 Multimodal Inference Flow (Text + Image)

```
1. User taps AttachButton → ChatActivity opens file picker
2. File picker returns URI → ChatActivity calls ChatViewModel.attachImage(uri)
3. ChatViewModel stores URI in pendingImageUri state (ImageProcessor NOT called yet — encoding deferred to send time to avoid wasting memory if user cancels)
   [ImageProcessor is called by ViewModel at sendPrompt(), NOT by Activity — MVVM boundary preserved]
4. User types text + taps Send → ChatActivity calls ChatViewModel.sendPrompt(text)
5. ChatViewModel calls ImageProcessor.resizeAndEncode(pendingImageUri) → imageBytes
6. ChatViewModel calls InferenceManager.infer(prompt, imageBytes)
7. InferenceManager calls LlamaCppBridge.infer(prompt, imageBytes)
8. LlamaCppBridge calls vision encoder (mmproj) + llama_decode()
9. Decoded tokens returned to InferenceManager
10. InferenceManager sends result to ChatViewModel
11. ChatViewModel updates StateFlow, UI displays response
12. ChatRepository saves history to Room
```

### 4.3 Lifecycle Management Flow

```
1. Activity.onCreate() → ChatViewModel created by ViewModelProvider
2. ChatViewModel.init{} → InferenceManager instantiated (ViewModel-scoped)
3. Activity.onStart() → ChatViewModel.loadHistory()
4. Activity.onDestroy() → ViewModel.onCleared() → InferenceManager.cleanup()
5. InferenceManager.cleanup() → LlamaCppBridge.unloadModel()
```

> **Why NOT cancel on onPause():** Inference should continue when user temporarily backgrounds the app (e.g., checks another app). Cancellation only on `onCleared()` ensures the model is stopped only when the ViewModel is actually destroyed.

---

## 5. Architectural Decisions

### 5.1 Why MVVM?
- Clear separation between UI and business logic.
- ViewModel survives configuration changes (screen rotation).
- Easy to test with unit tests for ViewModel without Android framework.
- **Specifically:** Activity is never allowed to call `ImageProcessor` or `InferenceManager` directly. All logic routes through ViewModel.

### 5.2 Why Coroutines?
- Model inference runs in background without blocking main thread.
- Easy to cancel when activity is destroyed (Structured Concurrency).
- Natural integration with Flow/StateFlow for reactive UI updates.

### 5.3 Why mmap for Model Loading?
- Avoids memory duplication (heap + native memory).
- Enables large models to load without OOM kills.
- OS handles automatic paging when system memory is low.

### 5.4 Why Room for History?
- Safe and easy-to-use SQLite abstraction.
- Compile-time query validation.
- Natural integration with Kotlin coroutines (suspend DAO methods).

### 5.5 Why No SQLCipher in Phase 1?
- Phase 1 is an offline PoC with no real user PII beyond local test data.
- Android internal storage (`/data/data/<pkg>/`) is already protected by the OS.
- SQLCipher adds a native dependency (~4MB) and complexity without a current threat model to justify it.
- **Deferred to Phase 2** when real users and sensitive data are involved. (YAGNI)

---

## 6. References
- ISO/IEC/IEEE 42010: Standard for software architecture documentation
- llama.cpp documentation: https://github.com/ggml-org/llama.cpp
- Android Architecture Components: https://developer.android.com/topic/architecture
