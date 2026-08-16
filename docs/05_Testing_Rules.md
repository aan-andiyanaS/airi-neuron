# Testing Rules & Quality Assurance
## Phase 1: On-Device Multimodal SLM

**Version:** 1.1  
**Date:** 2026-08-16  
**Standard:** IEEE 830 (Software Testing Documentation)  

---

## 1. Unit Testing Rules

### 1.1 Testing Coverage

| Component | Framework | Minimum Coverage |
|---|---|---|
| ViewModel (Kotlin) | JUnit 5 + MockK | 80%+ |
| Repository (Kotlin) | JUnit 5 + Room in-memory | 80%+ |
| InferenceManager (Kotlin) | JUnit 5 + Coroutines test | 70%+ |
| JNI Bridge (C++) | GoogleTest (on-device only) | 70%+ |
| ImageProcessor (Kotlin) | JUnit 5 | 80%+ |
| InputValidator (Kotlin) | JUnit 5 | 80%+ |
| OutputFilter (Kotlin) | JUnit 5 | 80%+ |

### 1.2 Test Writing Rules

```kotlin
// CORRECT: Test with edge cases, descriptive name
@Test
fun `infer with empty prompt returns error`() = runTest {
    val result = inferenceManager.infer("", null)
    assertTrue(result.isFailure)
}

@Test
fun `infer with large image returns success`() = runTest {
    val largeImage = ByteArray(10 * 1024 * 1024) // 10MB
    val result = inferenceManager.infer("test", largeImage)
    assertTrue(result.isSuccess)
}

// INCORRECT: Test without edge cases, non-descriptive name
@Test
fun testInfer() {
    val result = inferenceManager.infer("test", null)
    assertTrue(result.isSuccess) // does not test error case
}
```

### 1.3 Mandatory Edge Cases to Test

| Function | Edge Cases |
|---|---|
| `infer(prompt, image)` | Empty prompt, very long prompt (>2000 char), null image, very large image (>10MB) |
| `loadModel(path)` | Path does not exist, path corrupt (non-GGUF file), path is valid GGUF but wrong model type |
| `saveChat(message)` | Empty message, very long message, database full |
| `resizeImage(bitmap)` | Very small bitmap (<100x100), very large bitmap (>4000x4000), null bitmap |
| `validateInput(text, file)` | Text >2000 char (truncate/error, no crash), image >10MB (error, not processed), non-image file format (error, not processed), invalid file path (error, no crash) |

---

## 2. Integration Testing Rules

### 2.1 Mandatory Test Scenarios

| ID | Scenario | Pass Criteria |
|---|---|---|
| I1 | Load model at app start | Model successfully loaded in <5 seconds, no OOM kill |
| I2 | Text-only inference | Prompt "Hello" produces response in <2 seconds (time-to-first-token) |
| I3 | Multimodal inference | Prompt "What is this?" + cat image produces response in <5 seconds |
| I4 | Chat history saved | After app restart, chat history persists |
| I5 | Inference cancelled when activity destroyed | Running inference properly cancelled, no leaks |
| I6 | Multiple concurrent inferences | Two prompts sent simultaneously, both responses appear correctly (not corrupted) |

### 2.2 Device Testing

| Device | OS | RAM | Status |
|---|---|---|---|
| Samsung S20 Ultra (Exynos 990) | Android 10-12 | 12GB | **Mandatory** (target device) |
| Other device (optional) | Android 10+ | 8GB+ | Recommended for cross-device validation |

### 2.3 Performance Benchmark

| Metric | Target | Measurement Method |
|---|---|---|
| Time-to-first-token (text) | <2 seconds | Log time from send prompt to first token appears |
| Time-to-first-token (multimodal) | <5 seconds | Log time from send prompt+image to first token appears |
| Decode throughput | TBD after baseline measurement on S20 Ultra | Calculate total tokens / total decode time |
| Memory usage during inference | <2GB total | `Debug.getMemoryInfo()` before and after inference |
| Battery drain (10 inferences) | <5% | Measure battery level before and after 10 consecutive inferences |

---

## 3. Security Testing Rules

### 3.1 Input Validation Test

| Test Case | Expected Input | Expected Result |
|---|---|---|
| Upload file >10MB | 15MB file | Error "File too large", not processed |
| Upload non-image file | PDF file | Error "Format not supported", not processed |
| Text input >2000 characters | 3000 character text | Truncate or error, no crash |
| Invalid file path | Path "/system/etc/passwd" | Error "File not found", no crash |

### 3.2 Output Filter Test

| Test Case | Model Output | Result after Filter |
|---|---|---|
| Output contains dangerous keywords | "You can kill yourself by..." | Fallback to "Sorry, I cannot answer that" |
| Output contains navigation directions | "Walk straight 100 meters" | Display with disclaimer "results may be inaccurate" |
| Normal output | "This is a cat" | Display without filter |

### 3.3 Privacy Test

| Test Case | Validation |
|---|---|
| No network access | Monitor network traffic during inference — no outgoing requests |
| Data stored in internal storage | Check database path — must be in `/data/data/<package>/databases/` |
| No sensitive data logged | Check logcat — no log of sensitive prompts/user messages |

---

## 4. Performance Testing Rules

### 4.1 Benchmark Scenarios

| Scenario | Steps | Metrics Measured |
|---|---|---|
| Cold start | Install app → open → load model | Model load time, initial memory usage |
| Warm start | Open app (model already loaded) → inference | Time-to-first-token, decode throughput |
| Sustained load | 20 consecutive inferences | Memory usage trend, thermal throttle, battery drain |
| Multitasking | Inference while running other apps | UI frame rate, inference latency |

### 4.2 Thermal Testing

| Test Case | Steps | Pass Criteria |
|---|---|---|
| Thermal throttle detection | 10 consecutive inferences → monitor temperature | App detects throttle and automatically reduces `n_batch` |
| Recovery from throttle | After throttle, wait 5 minutes → infer again | App returns to normal performance after temperature drops |

### 4.3 Memory Leak Detection

| Tool | Usage | Pass Criteria |
|---|---|---|
| Android Profiler | Monitor memory usage during repeated inference | Memory usage stable, no continuous increase |
| LeakCanary | Install in debug build, run inference | No leaks detected in ViewModel, JNI, or Repository |
| Native memory profiler (NDK) | Monitor native memory during model load/unload | Native memory released during model unload |

---

## 5. Human-in-the-loop Validation

### 5.1 Manual Testing Checklist

Before release to production/testing users:
- [ ] Text-only inference runs smoothly on S20 Ultra (time-to-first-token <2 seconds).
- [ ] Multimodal inference runs smoothly on S20 Ultra (total <5 seconds).
- [ ] Chat history saved and persists after app restart.
- [ ] No crashes when inference cancelled (screen rotation, close app).
- [ ] Output filter works — dangerous output fallbacks to safe response.
- [ ] No network access (monitor with network inspector).
- [ ] Memory usage stable during 20 consecutive inferences (no leaks).
- [ ] Thermal throttle detected and handled correctly.

### 5.2 Model Output Validation

| Category | Manual Validation |
|---|---|
| Navigation output | **MANDATORY** manual validation for every output containing directions/location — do not blindly trust before using in real application |
| Factual output | Sample check 10% of output — ensure no obvious factual hallucinations |
| Sensitive output | Validate output filter — ensure dangerous keywords filtered correctly |

---

## 6. Continuous Integration

### 6.1 CI Pipeline

```yaml
# GitHub Actions workflow for AIRI Phase 1
# NOTE: This CI runs Kotlin unit tests and lint only.
# JNI/C++ (GoogleTest) tests require physical device — run manually on S20 Ultra.
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Android NDK
        uses: android-actions/setup-android@v3
        with:
          ndk-version: 25.2.9519653  # NDK r25c — pinned for reproducibility

      - name: Run Kotlin unit tests
        run: ./gradlew test

      - name: Run lint
        run: ./gradlew lint

      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

### 6.2 Quality Gates

| Gate | Criteria |
|---|---|
| Unit test coverage | Minimum 75% for all components |
| Lint | 0 errors, 0 critical warnings |
| Build size | APK <150MB (excluding model) |
| Performance | Time-to-first-token <2 seconds on target device (validated on-device, not in CI) |

---

## 7. References
- IEEE 830: Standard for software testing documentation
- Android Testing: https://developer.android.com/training/testing
- JUnit 5: https://junit.org/junit5/
- MockK: https://mockk.io/
- GoogleTest: https://google.github.io/googletest/
