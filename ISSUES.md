# ISSUES — Catatan Permasalahan & Perubahan
## AIRI Phase 1 — On-Device Multimodal SLM

> Dokumen ini mencatat semua issues yang ditemukan, perbaikan yang dilakukan, dan perubahan yang dibuat selama development Phase 1.

---

## ✅ CHECKPOINT 1 — PASSED (2026-08-16)

```
BUILD SUCCESSFUL in 1m 43s
64 actionable tasks: 37 executed, 2 from cache, 25 up-to-date
testDebugUnitTest   ✅ PASSED
testReleaseUnitTest ✅ PASSED
```

**Command setelah fix permanen (cukup ini saja):**
```powershell
# Dari e:\Project\AIRI\odslm\
.\gradlew.bat test
```

**Scope test yang lulus:**
- `ChatViewModelTest` — 8 unit tests (pure JVM, MockK)
- `ChatRepositoryTest` dirun terpisah: `.\gradlew.bat connectedAndroidTest` (butuh device)

---

## 🔴 CLOSED — Build Issues

### ISSUE-001: `gradlew` tidak dikenali di PowerShell Windows

| Field | Detail |
|-------|--------|
| **Status** | ✅ FIXED |
| **Tanggal** | 2026-08-16 |
| **Severity** | Blocker |
| **Task** | Task 1 (Project Skeleton) |

**Gejala:**
```
./gradlew : The term './gradlew' is not recognized as the name of a cmdlet...
```

**Root Cause:**
1. File `gradlew`, `gradlew.bat`, dan `gradle-wrapper.jar` tidak dibuat — hanya `gradle-wrapper.properties` yang ada.
2. Di PowerShell Windows, path separator untuk script lokal harus `.\ ` bukan `./`.

**Fix:**
- Jalankan `gradle wrapper --gradle-version 8.7` menggunakan Gradle 8.7 dari cache `~/.gradle/wrapper/dists/`.
- Pastikan `JAVA_HOME` mengarah ke JBR Android Studio (Java 21), bukan default JDK sistem.

**Cara menjalankan di PowerShell (Windows) yang benar:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```

---

### ISSUE-002: Gradle wrapper generation gagal — Java 8 incompatible dengan AGP 8.5.2

| Field | Detail |
|-------|--------|
| **Status** | ✅ FIXED |
| **Tanggal** | 2026-08-16 |
| **Severity** | Blocker |
| **Task** | Task 1 (Project Skeleton) |

**Gejala:**
```
Could not resolve com.android.tools.build:gradle:8.5.2.
Incompatible because this component declares a component, compatible with Java 11
and the consumer needed a component, compatible with Java 8
```

**Root Cause:**
- Sistem memiliki default Java 8 di PATH.
- AGP 8.5.2 memerlukan **Java 11+** (minimum), direkomendasikan Java 17+.
- Android Studio bundled JBR (Java 21) tersedia di `C:\Program Files\Android\Android Studio\jbr\` tetapi tidak di-set sebagai JAVA_HOME.

**Fix:**
```powershell
# Set JAVA_HOME ke JBR Android Studio sebelum menjalankan gradlew
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

**Permanent fix (opsional):** Set `JAVA_HOME` permanen di Windows System Environment Variables, atau tambahkan ke profil PowerShell:
```powershell
# Di $PROFILE (biasanya C:\Users\<user>\Documents\PowerShell\Microsoft.PowerShell_profile.ps1)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

---

### ISSUE-003: `Theme.MaterialComponents.DayNight.NoActionBar` tidak ditemukan

| Field | Detail |
|-------|--------|
| **Status** | ✅ FIXED |
| **Tanggal** | 2026-08-16 |
| **Severity** | Blocker |
| **Task** | Task 1 (Project Skeleton) |

**Gejala:**
```
error: resource style/Theme.MaterialComponents.DayNight.NoActionBar not found
error: style attribute 'attr/colorPrimaryVariant' not found
```

**Root Cause:**
- `themes.xml` awal menggunakan `Theme.MaterialComponents` dari library Material Components.
- `com.google.android.material:material` belum ditambahkan ke `app/build.gradle.kts`.
- `androidx.appcompat:appcompat` juga tidak ada di dependencies.

**Fix:**
1. Tambah `material` dan `appcompat` ke `libs.versions.toml` dan `app/build.gradle.kts`.
2. Update `themes.xml` ke `Theme.Material3.DayNight.NoActionBar` (Material 3, bundled di `material:1.12.0+`).
3. Hapus `colorPrimaryVariant` (deprecated di Material 3 — ganti ke `colorPrimaryContainer`).

**File yang diubah:**
- [`gradle/libs.versions.toml`](gradle/libs.versions.toml) — tambah `appcompatVersion`, `materialVersion`, `androidx-appcompat`, `material`
- [`app/build.gradle.kts`](app/build.gradle.kts) — tambah `implementation(libs.androidx.appcompat)`, `implementation(libs.material)`
- [`app/src/main/res/values/themes.xml`](app/src/main/res/values/themes.xml) — migrate ke Material3

---

## 🟡 OPEN — Known Limitations Phase 1

### ISSUE-004: `ChatViewModelTest` menggunakan `viewModels()` yang butuh Android framework

| Field | Detail |
|-------|--------|
| **Status** | 🟡 NOTED (by design) |
| **Tanggal** | 2026-08-16 |
| **Severity** | Low |
| **Task** | Task 3 (ChatViewModel) |

**Deskripsi:**
`ChatViewModelTest` menginstansiasi `ChatViewModel` secara langsung dengan repository mock — ini adalah unit test murni (JVM) yang tidak bergantung pada Android framework. Test ini **benar** dan tidak memerlukan fix.

`ChatRepositoryTest` menggunakan `Room.inMemoryDatabaseBuilder` yang memerlukan `AndroidJUnit4` runner — ini adalah **instrumentedTest** dan harus dijalankan di device/emulator, bukan dengan `./gradlew test`.

**Cara run:**
```powershell
# Unit tests (JVM only) — cepat, tidak perlu device
.\gradlew.bat test

# Instrumented tests (Room) — butuh device/emulator
.\gradlew.bat connectedAndroidTest
```

---

### ISSUE-005: InferenceManager belum diimplementasi — placeholder response

| Field | Detail |
|-------|--------|
| **Status** | 🟡 TODO (Task 7) |
| **Tanggal** | 2026-08-16 |
| **Severity** | Medium |
| **Task** | Task 7 (InferenceManager + wiring) |

**Deskripsi:**
`ChatViewModel.sendPrompt()` saat ini menyimpan placeholder response `"[Inference not yet wired — Task 7]"` ke database. Ini disengaja sebagai stub agar pipeline state + persistence dapat divalidasi sebelum JNI diintegrasikan.

**TODO di code:**
```kotlin
// TODO (Task 7): call InferenceManager.infer(text, imageBytes) here.
```

**Fix:** Diimplementasi di Task 7 setelah llama.cpp research gate (Task 5) selesai.

---

### ISSUE-006: `JAVA_HOME` harus di-set manual setiap sesi PowerShell

| Field | Detail |
|-------|--------|
| **Status** | ✅ FIXED |
| **Tanggal** | 2026-08-16 |
| **Severity** | Low |
| **Task** | Task 1 (Project Skeleton) |

**Gejala:**
Setiap kali membuka terminal baru, harus menjalankan:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```
Sebelum bisa menjalankan `gradlew.bat`.

**Root Cause:**
Sistem memiliki Java 8 di PATH default. Gradle membutuhkan Java 11+ untuk AGP 8.5.2. Tidak ada konfigurasi permanen yang mengarahkan Gradle ke JBR Android Studio.

**Fix — User-level Gradle Properties:**
Tulis `org.gradle.java.home` ke file `~/.gradle/gradle.properties` (user-level, bukan project-level).
File ini dibaca Gradle otomatis untuk semua project, tidak perlu masuk git, tidak mempengaruhi CI.

```powershell
# One-time setup — sudah dilakukan
[System.IO.File]::WriteAllText(
  "$env:USERPROFILE\.gradle\gradle.properties",
  "org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr`n"
)
```

**File yang dibuat:** `C:\Users\Aan Andiyana Sandi\.gradle\gradle.properties`
```properties
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
```

**Setelah fix — cukup jalankan:**
```powershell
# Dari e:\Project\AIRI\odslm\
.\gradlew.bat test
```

---

### ISSUE-007: NDK CMakeLists.txt belum dikonfigurasi

| Field | Detail |
|-------|--------|
| **Status** | 🟡 TODO (Task 5-6) |
| **Tanggal** | 2026-08-16 |
| **Severity** | Medium |
| **Task** | Task 5 (llama.cpp research), Task 6 (JNI Bridge) |

**Deskripsi:**
`app/build.gradle.kts` memiliki baris CMake yang di-comment:
```kotlin
// externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
```

Ini disengaja karena integrasi llama.cpp memerlukan research gate (Task 5) untuk menentukan:
- Versi llama.cpp yang compatible dengan MiniCPM-V 4.6
- Apakah menggunakan official `examples/llama.android` atau custom wrapper

**Fix:** Diimplementasi di Task 6 setelah Task 5 research gate selesai.

---

### ISSUE-008: Project dipindah ke subfolder `odslm`

| Field | Detail |
|-------|--------|
| **Status** | ✅ DONE |
| **Tanggal** | 2026-08-16 |
| **Severity** | Structural change |
| **Task** | Setup / Organisasi project |

**Latar Belakang:**
Project Android awalnya dibuat langsung di `e:\Project\AIRI\`. User meminta project dimasukkan ke subfolder `odslm` agar:
- Root `AIRI/` bisa menampung multiple sub-project di masa depan
- Nama folder mencerminkan package name `com.airi.odslm`
- Struktur lebih rapi untuk pengembangan lanjutan

**Perubahan Struktur:**

_Sebelum:_
```
e:\Project\AIRI\
├── app\
├── docs\
├── gradle\
├── gradlew.bat
└── ...
```

_Sesudah:_
```
e:\Project\AIRI\
└── odslm\            ← Android project root (baru)
    ├── app\
    ├── docs\
    ├── gradle\
    ├── gradlew.bat
    └── ...
```

**Cara kerja setelah relokasi:**
```powershell
# Pindah ke root project yang baru
cd e:\Project\AIRI\odslm

# Jalankan test (JAVA_HOME sudah permanen via ~/.gradle/gradle.properties)
.\gradlew.bat test
```

**Android Studio:** Re-open project dari `e:\Project\AIRI\odslm\` (File → Open).

**Hasil verifikasi:** BUILD SUCCESSFUL in 6s setelah relokasi.

---

## 📋 Changelog — Perubahan yang Dilakukan

### FASE A: Perbaikan Dokumentasi (2026-08-16)

| File | Perubahan |
|------|-----------|
| `docs/01_SRS_Phase1.md` | NF2 latency → TBD (diukur di S20 Ultra), F3.3 multimodal → Medium priority, tambah NF11 battery drain ≤5% |
| `docs/02_System_Architecture.md` | Fix MVVM flow — `ImageProcessor` dipanggil ViewModel bukan Activity. Tambah lifecycle note `InferenceManager`. ADR §5.5 no SQLCipher. |
| `docs/03_System_Prompt_AI.md` | Hapus duplikat §4 & §5, ganti dengan cross-reference ke doc 04 dan 05. Tambah note MVVM boundary di §6.2. |
| `docs/04_Risk_Management.md` | T4: tambah API 29 fallback thermal. E4: hapus SQLCipher Phase 1 (YAGNI). Tambah checklist MVVM + lifecycle. |
| `docs/05_Testing_Rules.md` | Tambah InputValidator/OutputFilter ke coverage table. Tambah edge case `validateInput()`. Fix CI YAML NDK setup + comment GoogleTest on-device only. |

### FASE B Task 1: Project Skeleton (2026-08-16)

| File | Keterangan |
|------|------------|
| `settings.gradle.kts` | [NEW] Root Gradle settings |
| `build.gradle.kts` | [NEW] Root build file (plugin declarations) |
| `gradle/libs.versions.toml` | [NEW] Version catalog (AGP, Kotlin, Room, Coroutines, JUnit5, MockK, Material) |
| `gradle/wrapper/gradle-wrapper.properties` | [NEW] Pinned Gradle 8.7 |
| `gradlew` / `gradlew.bat` | [NEW] Generated via `gradle wrapper --gradle-version 8.7` |
| `gradle/wrapper/gradle-wrapper.jar` | [NEW] Binary wrapper |
| `gradle.properties` | [NEW] JVM args, configuration cache, Kotlin style |
| `app/build.gradle.kts` | [NEW] App module: minSdk 29, arm64-v8a, JUnit5, viewBinding, Room KSP |
| `app/proguard-rules.pro` | [NEW] Keep JNI classes + Room |
| `app/src/main/AndroidManifest.xml` | [NEW] No INTERNET permission (offline-first) |
| `app/src/main/res/values/strings.xml` | [NEW] App strings (errors, labels, safe response) |
| `app/src/main/res/values/themes.xml` | [NEW] Material3 DayNight theme |
| `.github/workflows/ci.yml` | [NEW] GitHub Actions: Kotlin tests + lint + NDK setup |
| `.gitignore` | [NEW] Exclude *.gguf, *.jks, build/ |
| `README.md` | [NEW] Full project documentation |

### FASE B Task 2: Data Layer (2026-08-16)

| File | Keterangan |
|------|------------|
| `data/ChatEntity.kt` | [NEW] Room entity `chat_messages`. `MessageRole` constants object. |
| `data/ChatDao.kt` | [NEW] DAO: `insertMessage` (suspend), `getAllMessages` (Flow), `clearAll` (suspend) |
| `data/AppDatabase.kt` | [NEW] Singleton Room database, version 1, schema export enabled |
| `data/ChatRepository.kt` | [NEW] `runCatching` wrapper, `allMessages: Flow`, `saveMessage`, `clearHistory` |
| `androidTest/.../ChatRepositoryTest.kt` | [NEW] 7 integration tests dengan Room in-memory DB |

### FASE B Task 3: ChatViewModel (2026-08-16)

| File | Keterangan |
|------|------------|
| `viewmodel/ChatUiState.kt` | [NEW] `ChatMessage` UI model, `ChatUiState` immutable data class |
| `viewmodel/ChatViewModel.kt` | [MODIFIED] Full implementation: StateFlow, `sendPrompt`, `attachImage`, `clearError`, `Factory` |
| `test/.../ChatViewModelTest.kt` | [NEW] 8 pure JVM unit tests dengan MockK |

### Bug Fixes & Setup (2026-08-16)

| Issue | Fix | File yang Terpengaruh |
|-------|-----|-----------------------|
| ISSUE-002: Java 8 incompatible dengan AGP 8.5.2 | Set `JAVA_HOME` ke Android Studio JBR (Java 21) saat generate wrapper | — |
| ISSUE-003: `Theme.MaterialComponents` tidak ditemukan | Tambah `material:1.12.0`, `appcompat:1.7.0`, migrate ke `Theme.Material3` | `libs.versions.toml`, `app/build.gradle.kts`, `themes.xml`, `colors.xml` |
| ISSUE-003: Launcher icon tidak ada | Generate PNG placeholder (48–192px) via Python Pillow + adaptive icon XML | `mipmap-*/ic_launcher.png`, `drawable/ic_launcher_foreground.xml`, `mipmap-anydpi-v26/*.xml` |
| ISSUE-006: JAVA_HOME manual tiap sesi | Tulis `org.gradle.java.home` ke `~/.gradle/gradle.properties` (user-level, permanen) | `C:/Users/Aan Andiyana Sandi/.gradle/gradle.properties` |
| ISSUE-008: Project relokasi ke `odslm` | Pindahkan semua file dari `e:\Project\AIRI\` ke `e:\Project\AIRI\odslm\` | Seluruh project root |

---

## 🔵 Next Tasks

| Task | Prioritas | Estimasi |
|------|-----------|----------|
| Task 4: ChatActivity + RecyclerView UI | High | 1 hari |
| Task 5: llama.cpp research gate | High | 0.5 hari |
| CHECKPOINT 2: GGUF load di S20 Ultra | Mandatory | Manual test |

---

## ✅ CHECKPOINT 2 — MAJOR REFACTOR & JNI INTEGRATION (2026-08-17)

**Deskripsi Perubahan:**
Sejak commit terakhir, proyek telah melalui banyak pembaruan arsitektur besar, termasuk migrasi UI, integrasi JNI C++, dan pembersihan kode. Seluruh aplikasi sekarang sudah dapat dikompilasi dengan lancar (`BUILD SUCCESSFUL`).

### 🚀 Fitur & Integrasi Baru
1. **Llama.cpp & NDK JNI Bridge (`Task 5-7`)**
   - Menambahkan `llama.cpp` sebagai git submodule.
   - Mengimplementasikan `LlamaCppBridge.cpp` (C++) dan `LlamaCppBridge.kt` (Kotlin) untuk mengeksekusi model SLM secara langsung di perangkat.
   - Membuat `InferenceManager.kt` untuk menangani *thread* latar belakang dan alokasi memori model.

2. **Security & Validation Layer (`Task 8`)**
   - Menambahkan `InputValidator` (panjang maksimal & ukuran file gambar).
   - Menambahkan `OutputFilter` untuk menyaring teks keluaran dari model.

3. **Migrasi Penuh ke Jetpack Compose (`Task 10`)**
   - Menghapus total semua *XML Layouts* (`activity_chat.xml`, `item_chat_user.xml`) dan `ChatAdapter.kt`.
   - Membuat komponen UI modern (Material 3) dengan Compose: `ChatScreen`, `ChatBubble`, `BottomNavBar`, `LibraryScreen`, `SettingsScreen`, dsb.
   - Tema *Dark Mode* (Default Stitch: pure black `#000000`) dan *Light Mode* kini sepenuhnya didukung dan dikelola melalui `ThemePreferences`.

### 🧹 Perbaikan Dokumentasi & Clean Code
- **Generalisasi Use-Case:** Menghapus seluruh referensi penggunaan spesifik "Tunanetra" dari `README.md`, `01_SRS_Phase1.md`, dan `04_Risk_Management.md`. Proyek kini diposisikan sebagai "Asisten AI luring terpadu" secara umum.
- **Clean Code Audit:** Memperbaiki *error handling* di `InferenceManager` agar mereturn `Result<String>`, menghapus penggunaan `GlobalScope` yang rentan bocor memori, serta mengekstrak *magic strings* di `ChatViewModel`.
- **Compiler Warnings:** Memperbaiki _unused parameter_ dan API Compose yang _deprecated_ (contoh: `Icons.Default.Send` menjadi `Icons.AutoMirrored.Filled.Send`).

---

*Terakhir diperbarui: 2026-08-17 oleh Antigravity AI*
