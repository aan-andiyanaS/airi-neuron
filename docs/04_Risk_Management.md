# Ethical and Technical Risk Management
## AIRI Neuron — Phase 1: On-Device Multimodal SLM

**Version:** 1.2  
**Date:** 2026-08-16  
**Standard:** ISO/IEC 42001 (AI Management System)  

---

## 1. Technical Risks

### 1.1 Memory and Stability

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| T1 | OOM kill during model load | App crashes at initialization, user cannot use | High | Load model via `mmap`, limit context window to max 1024 tokens, monitor memory usage with `Debug.getMemoryInfo()` | Developer |
| T2 | Memory leak in JNI | App crashes after several inference sessions, native memory not released | Medium | Ensure `deleteLocalRef` is called for unused JNI objects, unload model in `onCleared()`, test with leak detector | Developer |
| T3 | Race condition during concurrent inference | Crash or corrupted inference results when two prompts sent simultaneously | Medium | Use `Mutex` for model access, queue prompts in `InferenceManager`, limit to 1 active inference at a time | Developer |

### 1.2 Performance and Thermal

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| T4 | Thermal throttle | Latency increases drastically after several minutes of sustained inference | High | Monitor temperature with platform-appropriate API: **API 30+** use `PowerManager.getThermalHeadroom()`; **API 29 fallback** use `BatteryManager.EXTRA_TEMPERATURE` as proxy — throttle when temperature >42°C. Dynamically reduce `n_batch` when throttle detected, limit sustained inference duration. | Developer |
| T5 | Latency too high | Poor user experience, model response too slow to use | Medium | Optimize backend (ARM NEON, Vulkan offload), Q4_K_M quantization, limit context window, measure directly on target device (S20 Ultra) | Developer |
| T6 | Unstable decode throughput | Model response stutters, inconsistent UX | Medium | Monitor tokens/second, fallback to smaller model if throughput below threshold, log performance for debugging | Developer |

### 1.3 Lifecycle and Integration

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| T7 | Inference not cancelled when activity destroyed | Memory leak, inference continues running in background | Medium | `InferenceManager` is ViewModel-scoped — its coroutine scope is cancelled in `ChatViewModel.onCleared()`. Validate with lifecycle test (I5). | Developer |
| T8 | JNI binding incompatible with llama.cpp version | Crash during model load or inference, native error | Low | Pin stable llama.cpp version after Task 5 research. Test on multiple devices. Document compatible version. | Developer |
| T9 | GGUF model corrupt or incompatible | App cannot load model, inference error | Low | Validate model via `llama_model_load_from_file()` return value on load. Return `Result.failure` with descriptive error. Show error dialog to user. Keep local copy of model. | Developer |

---

## 2. Ethical Risks

### 2.1 Model Output

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| E1 | Model produces harmful output (misinformation, hate speech) | User receives unsafe response, potential danger for navigation app for visually impaired | Medium | Add simple output filter (regex for dangerous keywords), fallback to safe response, log filtered output for debugging | Developer |
| E2 | Model hallucinates facts (e.g., wrong navigation directions) | Visually impaired user could get lost or hurt if they trust model output | High | **Do not use model output for critical navigation in phase 1** — manually validate every navigation output, add disclaimer "results may be inaccurate" | Developer |
| E3 | Bias in model output (e.g., gender, race stereotypes) | Unintentional discrimination, user feels uncomfortable | Medium | Choose model with audited bias (MiniCPM-V 4.6 relatively neutral), monitor output for bias, document model limitations | Developer |

### 2.2 Privacy and Data Security

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| E4 | Chat history data leaks to other apps | User privacy compromised, sensitive data accessible to others | Low | **Phase 1:** Store data in app internal storage (`/data/data/<pkg>/`) — protected by Android OS. Do not log sensitive data. Full SQLCipher encryption deferred to Phase 2 when real user PII is involved. (YAGNI) | Developer |
| E5 | GGUF model modifies system files | System file damage, unintentional malware | Low | Load model via mmap read-only, do not grant write permission to model, validate file path before load | Developer |
| E6 | Network access without user permission | User data sent to server without knowledge, privacy violation | Low | **Phase 1 offline-first** — no network access at all, validate with network monitor, document in SRS | Developer |

### 2.3 Dependency and Sustainability

| ID | Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|---|
| E7 | Open-source model deleted/license changed | Cannot update model, project hindered | Low | Keep local copy of GGUF model in repository, document model version and license, have fallback alternative model | Developer |
| E8 | llama.cpp no longer supports MiniCPM-V | Cannot update llama.cpp, stuck on old version | Low | Pin stable llama.cpp version, document compatible version, have fallback text-only model (Qwen2.5-1.5B) if multimodal fails | Developer |
| E9 | Target device (S20 Ultra) no longer supported in new Android versions | App does not run on new Android versions | Low | Test on multiple Android versions (10, 11, 12+), document minimum version (API 29+), have fallback device for testing | Developer |

---

## 3. Risk Matrix

### 3.1 Mitigation Priority

| Priority | Risk IDs | Reason |
|---|---|---|
| **High** | T1, T4, E2 | Direct impact on app stability and user safety (visually impaired navigation) |
| **Medium** | T2, T3, T5, T7, E1, E3, E4 | Impact on UX and privacy, but not immediately dangerous |
| **Low** | T6, T8, T9, E5, E6, E7, E8, E9 | Low impact or small probability, but still need mitigation |

### 3.2 Monitoring Triggers

| Risk ID | Trigger | Action |
|---|---|---|
| T1 | Memory usage >80% during model load | Reduce context window, log warning, fallback to smaller model |
| T4 | API 30+: `getThermalHeadroom()` <30%; API 29: temperature >42°C | Reduce `n_batch`, limit inference duration, log warning |
| E1 | Output filtered for dangerous keywords | Fallback to safe response, log filtered output, manual review |
| E2 | Output contains navigation directions | Add disclaimer "results may be inaccurate", manual validation before use |

---

## 4. Human-in-the-loop Oversight

### 4.1 Code Review Checklist
- [ ] Every public function has unit tests covering edge cases (empty input, large input, error handling).
- [ ] No `print()` or `println()` in production code — use `Log.d`/`Log.e` with consistent tag `AIRI_*`.
- [ ] No hard-coded file paths — use `context.filesDir` or `Environment.getExternalStorageDirectory()`.
- [ ] JNI binding does not leak — ensure `deleteLocalRef` is called for unused JNI objects.
- [ ] Complete error handling — no uncaught exceptions.
- [ ] UI responsive — loading indicator present during inference.
- [ ] Output filter for dangerous keywords — fallback to safe response if detected.
- [ ] `InferenceManager` cancelled in `ChatViewModel.onCleared()` — not in `Activity.onDestroy()`.
- [ ] `ImageProcessor` called from ViewModel, not from Activity.

### 4.2 Manual Validation
- Humans must review inference logic (tokenization, decoding) — do not blindly trust AI output.
- Validate that mmap is actually used for model loading (not reading fully into heap).
- Validate that coroutine scope is properly cancelled during lifecycle events.
- **Specifically for navigation output:** manually validate every output containing directions/location before using in real application.

---

## 5. References
- ISO/IEC 42001: Standard for AI management systems (AI governance, risk management)
- IEEE 830 / ISO/IEC/IEEE 42010: Software requirements and architecture documentation
- llama.cpp documentation: https://github.com/ggml-org/llama.cpp
- Android Security Best Practices: https://developer.android.com/topic/security/best-practices
