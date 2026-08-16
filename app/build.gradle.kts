plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.airi.odslm"
    compileSdk = 34
    ndkVersion = "25.2.9519653"

    defaultConfig {
        applicationId = "com.airi.odslm"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-phase1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // arm64-v8a only — target device is Exynos 990 (S20 Ultra)
        // ponytail: no x86/x86_64, no armeabi-v7a for Phase 1 PoC
        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        // NDK / CMake build configuration (inside defaultConfig for per-variant cmake args)
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                // Exynos 990 = Cortex-A77 = ARMv8.2-A (NOT armv8.7a)
                // dotprod = fused dot-product for Q4_K_M (big speedup on A77)
                // fp16    = half-precision float arithmetic
                // CPU-only: no GPU flags — Exynos 990 Vulkan driver unstable with llama.cpp
                arguments += "-DANDROID_PLATFORM=android-29"
                arguments += "-DANDROID_ARM_NEON=ON"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // JNI / NDK configuration — CMake path to LlamaCppBridge build
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform() // JUnit 5
            }
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // --- Testing ---
    // JUnit 5 for unit tests
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // MockK for mocking in Kotlin tests
    testImplementation(libs.mockk)

    // Coroutines test utilities
    testImplementation(libs.coroutines.test)

    // Room in-memory for repository tests
    testImplementation(libs.room.testing)

    // LeakCanary — memory leak detection in debug builds (no config needed)
    debugImplementation(libs.leakcanary)
}
