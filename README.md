# AIRI Neuron

> **On-Device Multimodal AI Module** — komponen inferensi lokal dari ekosistem [AIRI](https://github.com/moeru-ai/airi).
> Ditenagai oleh [MiniCPM-V 4.6](https://github.com/OpenBMB/MiniCPM-V) via [llama.cpp](https://github.com/ggml-org/llama.cpp) — menerima teks dan gambar, menghasilkan respons sepenuhnya di perangkat Android tanpa internet.

[![CI](https://github.com/your-repo/AIRI/actions/workflows/ci.yml/badge.svg)](https://github.com/your-repo/AIRI/actions/workflows/ci.yml)
![Platform](https://img.shields.io/badge/platform-Android%2010%2B-green)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-blue)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Tentang

**AIRI Neuron** adalah modul inferensi on-device dari sistem AI **AIRI**. Analoginya: jika AIRI adalah otak keseluruhan, maka Neuron adalah unit dasar yang memproses dan menghasilkan respons secara mandiri di perangkat.

Proyek ini adalah **Phase 1 / PoC** — membuktikan bahwa model vision-language dapat berjalan di perangkat Android mid-range (Samsung S20 Ultra) tanpa server cloud.

### Kemampuan

| Fitur | Detail |
|-------|--------|
| 💬 **Chat teks** | Kirim pertanyaan, terima respons dari model AI lokal |
| 🖼️ **Analisis gambar** | Lampirkan foto — model mendeskripsikan atau menjawab pertanyaan tentang gambar |
| 🔒 **Offline-first** | Tidak ada data yang keluar dari perangkat — privasi penuh |
| ⚡ **On-device inference** | llama.cpp langsung di CPU/GPU via NDK, tanpa middleware |

### Konteks Riset

- **Tujuan jangka pendek:** Proof of Concept — model SLM multimodal berjalan di Android
- **Tujuan jangka panjang:** Menjadi inference engine untuk sistem AIRI yang lebih besar
- **Use case target:** Asisten aksesibilitas offline untuk pengguna tunanetra

---

## Tech Stack

| Komponen | Teknologi | Keterangan |
|----------|-----------|------------|
| **Model AI** | MiniCPM-V 4.6 (Q4\_K\_M GGUF) | Vision-Language SLM, 4-bit quantized |
| **Inference Engine** | llama.cpp | C++ library, ARM NEON + Vulkan support |
| **Bahasa** | Kotlin + C++ (NDK) | Kotlin untuk UI/logic, C++ untuk JNI bridge |
| **Arsitektur** | MVVM + StateFlow | ViewModel sebagai single source of truth |
| **Database** | Room (SQLite) | Riwayat chat lokal, tanpa enkripsi di Phase 1 |
| **UI** | XML Layouts + RecyclerView | Lebih stabil dengan NDK vs Jetpack Compose |
| **CI/CD** | GitHub Actions | Unit test + lint otomatis |

---

## Status Development

| Task | Status | Catatan |
|------|--------|---------|
| 🟢 FASE A: Perbaikan dokumentasi | ✅ Selesai | SRS, Arsitektur, Risk, Testing diperbarui |
| 🟢 Task 1: Project skeleton + CI | ✅ Selesai | AGP 8.5.2, KSP, JUnit5, `arm64-v8a` only |
| 🟢 Task 2: Data layer (Room) | ✅ Selesai | ChatEntity, ChatDao, AppDatabase, ChatRepository |
| 🟢 Task 3: ChatViewModel + StateFlow | ✅ Selesai | sendPrompt, attachImage, clearError, Factory |
| 🔵 Task 4: ChatActivity + RecyclerView | 🚧 Berikutnya | Layout XML, ChatAdapter, ChatViewHolder |
| ⚪ Task 5: llama.cpp research \[GATE\] | 🔵 Direncanakan | Pin versi, konfirmasi pendekatan JNI |
| ⚪ Task 6: JNI Bridge (LlamaCppBridge) | ⬜ Direncanakan | C++ NDK, LlamaCppBridge.kt + .cpp |
| ⚪ Task 7: InferenceManager + wiring | ⬜ Direncanakan | Ganti TODO stubs di ChatViewModel |
| ⚪ Task 8: Security layer | ⬜ Direncanakan | InputValidator, OutputFilter |
| ⚪ Task 9: Performance monitoring | ⬜ Direncanakan | Thermal + memory monitoring |

---

## Requirements

| Item | Nilai |
|------|-------|
| **Target device** | Samsung Galaxy S20 Ultra (Exynos 990, 12 GB RAM) |
| **Min Android** | API 29 (Android 10) |
| **Target Android** | API 34 (Android 14) |
| **ABI** | `arm64-v8a` only |
| **NDK** | r25c (`25.2.9519653`) |
| **JDK** | 17+ (gunakan JBR bawaan Android Studio) |
| **AGP** | 8.5.2 |
| **Kotlin** | 1.9.25 |
| **Gradle** | 8.7 |

---

## Setup

### 1. Clone & buka di Android Studio

```bash
git clone <repo-url>
# Android Studio: File → Open → e:\Project\AIRI\odslm\
```

### 2. Jalankan unit tests

```powershell
# JAVA_HOME sudah dikonfigurasi permanen di ~/.gradle/gradle.properties
.\gradlew.bat test
```

> `org.gradle.java.home` sudah di-set ke Android Studio JBR.
> Tidak perlu `$env:JAVA_HOME` manual lagi.

### 3. Run lint

```powershell
.\gradlew.bat lint
```

### 4. Push model GGUF ke device (Task 7+)

```bash
adb push minicpm-v-4.6.Q4_K_M.gguf /sdcard/
# App memindahkan file ke internal storage saat pertama kali dibuka.
```

> File `.gguf` dikecualikan dari git. Push model ke device secara langsung.

---

## Arsitektur

```
UI Layer
  ChatActivity
      │  (observe StateFlow, dispatch actions)
      ▼
ViewModel Layer
  ChatViewModel ───► ChatRepository ───► Room (chat_history.db)
      │
      ▼
Inference Layer
  InferenceManager                  [Task 7]
      │  (Dispatchers.Default, coroutine scope)
      ▼
JNI Bridge
  LlamaCppBridge (.kt + .cpp)       [Task 6]
      │  (mmap GGUF, ARM NEON)
      ▼
Model Layer
  llama.cpp ─► MiniCPM-V 4.6 (text: Q4_K_M)
             └► mmproj-model-f16.gguf (vision tower)
```

**Kontrak MVVM:**
- `ChatActivity` hanya memanggil `ChatViewModel` — tidak ada akses langsung ke data/JNI
- `ImageProcessor` dipanggil dari ViewModel, bukan Activity
- `InferenceManager` di-cancel di `onCleared()` untuk mencegah memory leak

---

## Struktur Package

```
app/src/main/kotlin/com/airi/odslm/
├── ui/          # Activities, Adapters, ViewHolders
├── viewmodel/   # ChatViewModel, ChatUiState
├── data/        # Room: ChatEntity, ChatDao, AppDatabase, ChatRepository
├── jni/         # Kotlin JNI interface ke llama.cpp [Task 6]
└── util/        # InputValidator, OutputFilter, PerformanceMonitor [Task 8-9]

app/src/main/cpp/               # C++ NDK source [Task 6]
```

---

## Dokumentasi

| Dokumen | Deskripsi | Versi |
|---------|-----------|-------|
| [01\_SRS\_Phase1.md](docs/01_SRS_Phase1.md) | Software Requirements Specification | 1.1 |
| [02\_System\_Architecture.md](docs/02_System_Architecture.md) | Arsitektur sistem (MVVM, JNI, data flow) | 1.1 |
| [03\_System\_Prompt\_AI.md](docs/03_System_Prompt_AI.md) | Panduan generate kode dengan AI | 1.1 |
| [04\_Risk\_Management.md](docs/04_Risk_Management.md) | Manajemen risiko teknis & etika | 1.1 |
| [05\_Testing\_Rules.md](docs/05_Testing_Rules.md) | Aturan pengujian, target coverage, CI | 1.1 |
| [ISSUES.md](ISSUES.md) | Catatan issues, bug fixes, dan changelog | — |

---

## Keputusan Desain Utama

| Keputusan | Pilihan | Alasan |
|-----------|---------|--------|
| DI framework | ❌ Tidak ada (ViewModelProvider.Factory) | Single Activity, YAGNI |
| Enkripsi DB | ❌ Tanpa SQLCipher di Phase 1 | Offline PoC, storage internal dilindungi OS |
| UI toolkit | ❌ XML Layouts (bukan Compose) | Stabilitas lebih baik dengan NDK |
| ABI filter | `arm64-v8a` only | Target device Exynos 990 |
| JNI approach | Ditentukan di Task 5 | `examples/llama.android` vs custom wrapper |
| Thermal API | API 30+: `getThermalHeadroom()` / API 29: `BatteryManager.EXTRA_TEMPERATURE` | S20 Ultra min API 29 |

---

## Hubungan dengan Ekosistem AIRI

```
AIRI (sistem besar)
└── AIRI Neuron   ← repo ini
      Inference engine on-device untuk Android
      Input: teks + gambar
      Output: teks respons dari SLM lokal
```

Ke depan, AIRI Neuron akan menjadi salah satu komponen yang dipanggil oleh sistem AIRI melalui interface yang akan didefinisikan di Phase 2+.

---

## Lisensi

Dirilis di bawah [MIT License](LICENSE).
Copyright (c) 2026 Aan Andiyana Sandi
