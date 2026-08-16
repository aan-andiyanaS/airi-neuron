# Software Requirements Specification (SRS)
## AIRI Neuron — Phase 1: On-Device Multimodal SLM (MiniCPM-V 4.6)

**Version:** 1.2  
**Date:** 2026-08-16  
**Status:** Updated — Task 4 done, code/doc inconsistencies resolved  

---

## 1. Introduction

### 1.1 Purpose
This document defines the functional and non-functional requirements for an Android application that runs a multimodal language model (MiniCPM-V 4.6) locally on mobile devices, without relying on network connectivity.

### 1.2 Scope
- Multimodal inference (text + image) using llama.cpp via JNI.
- Simple chat UI with local conversation history.
- Model memory management (mmap, quantization) to prevent OOM kills.
- **Excluded:** 3D characters, ARCore, memory tiering, YOLO integration (later phases).

### 1.3 Definitions and Acronyms
| Term | Definition |
|---|---|
| SLM | Small Language Model |
| VLM | Vision-Language Model |
| GGUF | Quantized model file format for llama.cpp |
| JNI | Java Native Interface |
| OOM | Out Of Memory |
| NDK | Native Development Kit |
| TTFT | Time-To-First-Token |

---

## 2. Overall Description

### 2.1 Product Perspective
This application is a proof-of-concept for validating on-device multimodal inference performance on Exynos 990 hardware (S20 Ultra). Validation results will inform architectural decisions for phases 2-5 (3D character integration, ARCore, memory tiering).

### 2.2 User Classes
| Class | Description |
|---|---|
| Developer | Performs testing and performance validation |
| End User (thesis) | Visually impaired users who will use the navigation application (later phase) |

### 2.3 Operating Environment
- **OS:** Android 10+ (API 29+)
- **Architecture:** arm64-v8a (Exynos 990)
- **RAM minimum:** 8GB (recommended 12GB)
- **Storage:** 512MB free for GGUF model + cache
- **Connectivity:** Not required (offline-first)

---

## 3. Functional Requirements

### 3.1 Input and Output

| ID | Description | Input | Output |
|---|---|---|---|
| F1 | User text input | Text from keyboard | Text displayed in chat history |
| F2 | User image input | File picker (`ACTION_OPEN_DOCUMENT`), jpg/png format, max 10MB | Thumbnail displayed in chat history |
| F3 | Multimodal model inference | Text prompt + image (optional) | Text response from model |
| F4 | Display response | - | Response displayed in chat history, with loading indicator during inference |
| F5 | Conversation history | - | History saved locally (Room), persists after app restart |

### 3.2 Inference Requirements

| ID | Description | Acceptance Criteria |
|---|---|---|
| F3.1 | Load model | GGUF model + mmproj successfully loaded via mmap, no OOM |
| F3.2 | Text-only inference | Text prompt (≤50 tokens) produces response in <2 seconds (time-to-first-token) |
| F3.3 | Multimodal inference | Text prompt + image produces response in <5 seconds total |
| F3.4 | Context window | Supports up to 1024 token context (model + KV-cache) |

---

## 4. Non-Functional Requirements

### 4.1 Performance

| ID | Description | Metric |
|---|---|---|
| NF1 | Time-to-first-token latency | <2 seconds for short text prompt (≤50 tokens) |
| NF2 | Decode throughput | **TBD** — Measured directly on S20 Ultra (Exynos 990) during Task 5 benchmark. Target will be set after baseline measurement. |
| NF3 | Memory stability | No OOM kills in 15-20 consecutive chat turns |
| NF4 | App size | Final APK <150MB (excluding GGUF model) |

### 4.2 Reliability

| ID | Description | Criteria |
|---|---|---|
| NF5 | Offline-first | All core functions work without internet connection |
| NF6 | Error handling | No crashes from uncaught exceptions — all errors handled with `runCatching` |
| NF7 | Lifecycle management | Inference properly cancelled when activity is destroyed (no leaks) |

### 4.3 Security

| ID | Description | Criteria |
|---|---|---|
| NF8 | Input validation | Image files validated (size ≤10MB, jpg/png format) before processing |
| NF9 | Output filter | Model output filtered with regex for dangerous keywords (fallback to safe response) |
| NF10 | Data privacy | No data sent over network (phase 1 offline-first) |

### 4.4 Power

| ID | Description | Metric |
|---|---|---|
| NF11 | Battery drain | ≤5% battery consumption per 10 consecutive inferences (measured on S20 Ultra) |

---

## 5. Interface Requirements

### 5.1 User Interfaces

| Component | Description |
|---|---|
| ChatActivity | Main activity with RecyclerView for chat history |
| ChatAdapter | Adapter for displaying chat bubbles (user vs model). `ChatViewHolder` is a sealed class **inside ChatAdapter** — no separate file. |
| InputEditText | EditText for user text input |
| AttachButton | Button to open image file picker |
| SendButton | Button to send prompt to model |
| LoadingIndicator | LinearProgressIndicator during inference |

### 5.2 External Interfaces

| Component | Description |
|---|---|
| llama.cpp | C++ library for model inference, accessed via JNI |
| Room | SQLite library for chat history storage |
| Storage Access Framework | Android API for selecting image files from storage |

---

## 6. Assumptions and Dependencies

### 6.1 Assumptions
- GGUF model + mmproj already available on device storage (not downloaded via app).
- User has device with minimum 8GB RAM (recommended 12GB).
- llama.cpp already built with CPU backend (ARM NEON) for arm64-v8a.

### 6.2 Dependencies
- Android NDK (r25c or newer)
- llama.cpp (version with MiniCPM-V 4.6 multimodal support — pinned after Task 5 research)
- Room (version 2.5+)
- Coroutines (version 1.7+)

---

## 7. Implementation Notes

### 7.1 Requirements Priority
| Priority | Requirement IDs |
|---|---|
| High (required for MVP) | F1, F2, F3, F3.1, F3.2, NF1, NF2, NF3, NF5, NF6 |
| Medium (important for UX) | F3.3, F4, F5, NF4, NF7, NF8, NF11 |
| Low (optional for phase 1) | F3.4, NF9, NF10 |

> **Note on F3.3:** Multimodal inference (text + image) is elevated to **Medium** priority because MiniCPM-V 4.6 is fundamentally a vision-language model. Excluding it from Phase 1 validation would undermine the PoC's primary purpose.

### 7.2 Measurable "Smooth" Criteria
Do not let "smooth" be a subjective criterion. Set concrete numbers before moving to next phase:
- Time-to-first-token latency under ~2 seconds for short text prompts.
- Decode throughput: **TBD after baseline measurement on S20 Ultra** (NF2).
- No OOM kills in 15-20 consecutive chat turns.
- For image input: total time from image upload to first response under ~5 seconds.
- Battery drain: ≤5% per 10 consecutive inferences (NF11).

---

## 8. References
- ISO/IEC 42001: AI management system governance
- IEEE 830 / ISO/IEC/IEEE 42010: Software requirements and architecture documentation
