cmake_minimum_required(VERSION 3.22.1)
project(llama-jni CXX C)

# ---------------------------------------------------------------------------
# ARM flags — Exynos 990 = Cortex-A77 = ARMv8.2-A (NOT armv8.7a/SME)
# dotprod : fused dot-product for Q4_K_M quantization (major speedup on A77)
# fp16    : half-precision float arithmetic
# ---------------------------------------------------------------------------
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -march=armv8-a+dotprod+fp16 -O3")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+dotprod+fp16 -O3")

# ---------------------------------------------------------------------------
# llama.cpp build options (Exynos 990: CPU-only, no Vulkan, no OpenMP)
# ---------------------------------------------------------------------------
set(GGML_VULKAN     OFF CACHE BOOL "Disable Vulkan — driver unstable on Exynos 990" FORCE)
set(GGML_OPENMP     OFF CACHE BOOL "Disable OpenMP — not supported in Android NDK"  FORCE)
set(GGML_LLAMAFILE  OFF CACHE BOOL "Disable llamafile — not supported on Android"   FORCE)
set(GGML_METAL      OFF CACHE BOOL "Disable Metal — iOS only"                       FORCE)

set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

# ---------------------------------------------------------------------------
# JNI shared library
# mtmd.cpp is compiled directly into our .so to avoid CMake target ambiguity
# ---------------------------------------------------------------------------
file(GLOB_RECURSE MTMD_SRC "llama.cpp/tools/mtmd/*.cpp")
list(FILTER MTMD_SRC EXCLUDE REGEX "mtmd-cli\\.cpp$")
list(FILTER MTMD_SRC EXCLUDE REGEX "mtmd-debug\\.cpp$")

add_library(llama-jni SHARED
    LlamaCppBridge.cpp
    ${MTMD_SRC}
)

target_include_directories(llama-jni PRIVATE
    llama.cpp/include           # llama.h — stable C API
    llama.cpp/src               # llama-impl.h, sampling internals
    llama.cpp/ggml/include      # ggml.h
    llama.cpp/tools/mtmd        # mtmd.h — multimodal API
    llama.cpp/vendor            # miniaudio dependency
)

target_link_libraries(llama-jni PRIVATE
    llama       # core inference engine
    android     # Android NDK
    log         # __android_log_print
)

target_compile_options(llama-jni PRIVATE
    -fexceptions    # required for std::mutex, std::string
    -frtti          # JNI reflection support
    -fvisibility=hidden
)

target_compile_features(llama-jni PRIVATE cxx_std_17)
