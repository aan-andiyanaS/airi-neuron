/**
 * LlamaCppBridge.cpp — JNI bridge between Kotlin and llama.cpp C API.
 *
 * Contract:
 *  - loadModel()  : load GGUF + mmproj, return opaque long handle (pointer to NativeContext)
 *  - infer()      : run inference (text-only or multimodal), return response string
 *  - unloadModel(): free all native memory
 *
 * Thread safety:
 *  - Each NativeContext owns a mutex; concurrent infer() calls on the same handle are serialized.
 *  - Different handles are fully independent.
 *
 * Memory model:
 *  - Model loaded via mmap (llama default) — OS handles paging when RAM is low.
 *  - KV-cache allocated per context (contextSize tokens × layers × head_dim).
 *  - mmproj loaded via mtmd — vision tower, kept in RAM during session.
 *
 * Exynos 990 notes:
 *  - n_threads = 4  (use 2×A77@2.73GHz + 2×A77@2.50GHz, skip A55 little cores)
 *  - n_gpu_layers = 0 (CPU-only; Exynos 990 Vulkan driver is unstable with llama.cpp)
 *  - GGML_USE_NEON = ON (enabled via cmake -march=armv8-a+dotprod+fp16)
 */

#include <jni.h>
#include <android/log.h>

#include <string>
#include <vector>
#include <mutex>
#include <cstring>
#include <cstdint>

// llama.cpp stable C API
#include "llama.h"

// mtmd — multimodal vision API (compiled from tools/mtmd/mtmd.cpp)
#include "mtmd.h"

// ─── Logging ─────────────────────────────────────────────────────────────────

#define LOG_TAG "LlamaCppBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ─── NativeContext ────────────────────────────────────────────────────────────

/** Opaque handle returned to Kotlin as a jlong (pointer to NativeContext). */
struct NativeContext {
    llama_model   * model = nullptr;
    llama_context * ctx   = nullptr;
    mtmd_context  * mctx  = nullptr;   // null → text-only mode
    std::mutex      mutex;              // serialise concurrent infer() calls
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Tokenize a UTF-8 string. Returns empty vector on failure. */
static std::vector<llama_token> tokenize(
        llama_model * model, const std::string & text, bool add_special) {
    int n = -llama_tokenize(llama_model_get_vocab(model), text.c_str(), (int)text.size(),
                            nullptr, 0, add_special, true);
    std::vector<llama_token> tokens(n);
    if (llama_tokenize(llama_model_get_vocab(model), text.c_str(), (int)text.size(),
                       tokens.data(), n, add_special, true) < 0) {
        tokens.clear();
    }
    return tokens;
}

/** Decode a batch of tokens; returns false on error. */
static bool decodeBatch(llama_context * ctx, std::vector<llama_token> & tokens, int n_past) {
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    // NOTE: batch.pos starts at n_past implicitly via the KV-cache offset
    (void)n_past;
    return llama_decode(ctx, batch) == 0;
}

/**
 * Simple token sampling loop.
 * Uses llama_sampler_chain (modern API, available b9049+).
 * Parameters are deliberately conservative for reliability on S20 Ultra.
 */
static std::string sampleResponse(llama_context * ctx, llama_model * model, int maxTokens) {
    llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(100));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.8f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string result;
    // n_cur unused
    
    for (int i = 0; i < maxTokens; ++i) {
        llama_token token = llama_sampler_sample(smpl, ctx, -1);
        if (llama_vocab_is_eog(llama_model_get_vocab(model), token)) break;

        char buf[256] = {};
        int  len = llama_token_to_piece(llama_model_get_vocab(model), token, buf, sizeof(buf), 0, true);
        if (len > 0) result.append(buf, len);

        llama_batch next = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, next) != 0) {
            LOGW("sampleResponse: llama_decode failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    return result;
}

// ─── JNI lifecycle ───────────────────────────────────────────────────────────

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * /* vm */, void * /* reserved */) {
    // Initialise GGML backend once per process lifetime
    llama_backend_init();
    // Redirect llama.cpp logs to Android logcat
    llama_log_set([](ggml_log_level level, const char * text, void *) {
        if (level == GGML_LOG_LEVEL_ERROR) {
            LOGE("%s", text);
        } else if (level == GGML_LOG_LEVEL_WARN) {
            LOGW("%s", text);
        } else {
            LOGI("%s", text);
        }
    }, nullptr);
    LOGI("llama backend initialised");
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM * /* vm */, void * /* reserved */) {
    llama_backend_free();
}

// ─── loadModel ───────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jlong JNICALL
Java_com_airi_odslm_jni_LlamaCppBridge_loadModel(
        JNIEnv * env, jobject /* thiz */,
        jstring jModelPath, jstring jMmProjPath, jint contextSize) {

    const char * modelPath  = env->GetStringUTFChars(jModelPath,  nullptr);
    const char * mmProjPath = env->GetStringUTFChars(jMmProjPath, nullptr);

    auto * nc = new NativeContext();

    // ── Load GGUF model (mmap, CPU-only) ──────────────────────────────────
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;    // CPU-only; Exynos 990 Vulkan unstable
    mparams.use_mmap     = true; // OS-managed paging — avoids OOM on load

    nc->model = llama_model_load_from_file(modelPath, mparams);
    if (!nc->model) {
        LOGE("Failed to load model: %s", modelPath);
        env->ReleaseStringUTFChars(jModelPath,  modelPath);
        env->ReleaseStringUTFChars(jMmProjPath, mmProjPath);
        delete nc;
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                      "Failed to load GGUF model — check path and file integrity");
        return 0L;
    }

    // ── Create inference context ──────────────────────────────────────────
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx     = (uint32_t)contextSize;
    cparams.n_batch   = 512;
    // Exynos 990: 4 Cortex-A77 big cores; skip A55 little cores for latency
    cparams.n_threads          = 4;
    cparams.n_threads_batch    = 4;

    nc->ctx = llama_init_from_model(nc->model, cparams);
    if (!nc->ctx) {
        LOGE("Failed to create llama context (ctx_size=%d)", contextSize);
        llama_model_free(nc->model);
        env->ReleaseStringUTFChars(jModelPath,  modelPath);
        env->ReleaseStringUTFChars(jMmProjPath, mmProjPath);
        delete nc;
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                      "Failed to create inference context — try reducing contextSize");
        return 0L;
    }

    // ── Load multimodal projector (mmproj) — non-fatal if missing ─────────
    if (mmProjPath && strlen(mmProjPath) > 0) {
        mtmd_context_params mmp = mtmd_context_params_default();
        mmp.use_gpu   = false; // CPU-only
        mmp.n_threads = 4;

        nc->mctx = mtmd_init_from_file(mmProjPath, nc->model, mmp);
        if (nc->mctx) {
            LOGI("mmproj loaded: %s", mmProjPath);
        } else {
            LOGW("Failed to load mmproj — falling back to text-only mode: %s", mmProjPath);
        }
    }

    env->ReleaseStringUTFChars(jModelPath,  modelPath);
    env->ReleaseStringUTFChars(jMmProjPath, mmProjPath);

    LOGI("Model loaded. Handle: %p  ctx_size: %d  multimodal: %s",
         nc, contextSize, nc->mctx ? "yes" : "no");
    return (jlong)(uintptr_t)nc;
}

// ─── infer ───────────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jstring JNICALL
Java_com_airi_odslm_jni_LlamaCppBridge_infer(
        JNIEnv * env, jobject /* thiz */,
        jlong handle, jstring jPrompt,
        jbyteArray jImageRgba, jint imgWidth, jint imgHeight) {

    auto * nc = (NativeContext *)(uintptr_t)handle;
    if (!nc || !nc->model || !nc->ctx) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "infer() called with invalid handle — call loadModel() first");
        return nullptr;
    }

    std::lock_guard<std::mutex> lock(nc->mutex);

    const char * prompt = env->GetStringUTFChars(jPrompt, nullptr);

    // Format prompt for MiniCPM-V 4.6 Instruct (simple template)
    // Note: --reasoning off equivalent — Instruct checkpoint has no <think> block
    std::string formatted = "<用户>";
    formatted += prompt;
    formatted += "<AI>";
    env->ReleaseStringUTFChars(jPrompt, prompt);

    // Clear KV-cache before each turn (stateless per-turn for Phase 1)
    llama_memory_clear(llama_get_memory(nc->ctx), true);

    std::string response;

    // ── Multimodal path (text + image) ────────────────────────────────────
    if (jImageRgba != nullptr && nc->mctx) {
        jsize rgbaLen = env->GetArrayLength(jImageRgba);
        jbyte * rgbaData = env->GetByteArrayElements(jImageRgba, nullptr);

        // mtmd expects RGB (3 bytes/pixel); our Kotlin side sends RGBA (4 bytes)
        // Convert RGBA → RGB by stripping alpha channel
        int nPixels = imgWidth * imgHeight;
        std::vector<uint8_t> rgbData(nPixels * 3);
        auto * src = (const uint8_t *)rgbaData;
        uint8_t * dst = rgbData.data();
        for (int p = 0; p < nPixels; ++p) {
            dst[p * 3 + 0] = src[p * 4 + 0]; // R
            dst[p * 3 + 1] = src[p * 4 + 1]; // G
            dst[p * 3 + 2] = src[p * 4 + 2]; // B
            // alpha src[p*4+3] discarded
        }
        env->ReleaseByteArrayElements(jImageRgba, rgbaData, JNI_ABORT);

        // Create mtmd bitmap (RGB, nx × ny)
        mtmd_bitmap * bmp = mtmd_bitmap_init(
                (uint32_t)imgWidth, (uint32_t)imgHeight, rgbData.data());

        if (bmp) {
            // Prepare input: one bitmap + text prompt
            mtmd_input_text text_in;
            text_in.text        = formatted.c_str();
            text_in.add_special = true; // add BOS token

            mtmd_input_chunks * chunks = mtmd_input_chunks_init();
            const mtmd_bitmap * bitmaps[] = { bmp };

            int32_t ret = mtmd_tokenize(nc->mctx, chunks, &text_in, bitmaps, 1);
            if (ret == 0) {
                // Decode each chunk (text tokens interleaved with image embeddings)
                size_t n_chunks = mtmd_input_chunks_size(chunks);
                int n_past = 0;
                bool ok = true;

                for (size_t c = 0; c < n_chunks && ok; ++c) {
                    const mtmd_input_chunk * chunk = mtmd_input_chunks_get(chunks, c);
                    mtmd_input_chunk_type type = mtmd_input_chunk_get_type(chunk);

                    if (type == MTMD_INPUT_CHUNK_TYPE_TEXT) {
                        size_t n_tokens = 0;
                        const llama_token * tokens = mtmd_input_chunk_get_tokens_text(chunk, &n_tokens);
                        std::vector<llama_token> tv(tokens, tokens + n_tokens);
                        ok = decodeBatch(nc->ctx, tv, n_past);
                        n_past += (int)n_tokens;
                    } else if (type == MTMD_INPUT_CHUNK_TYPE_IMAGE) {
                        // Image chunk: encode vision embeddings and decode
                        ok = (mtmd_encode_chunk(nc->mctx, chunk) == 0);
                        if (ok) {
                            int n_tokens = (int)mtmd_input_chunk_get_n_tokens(chunk);
                            float * embd = mtmd_get_output_embd(nc->mctx);
                            llama_batch batch = llama_batch_get_one(nullptr, n_tokens);
                            batch.embd = embd;
                            ok = (llama_decode(nc->ctx, batch) == 0);
                        }
                        n_past += (int)mtmd_input_chunk_get_n_pos(chunk);
                    }
                }

                if (ok) {
                    response = sampleResponse(nc->ctx, nc->model, 512);
                } else {
                    LOGE("Multimodal decode chunk failed");
                    response = "[ERROR: multimodal decode failed]";
                }
            } else {
                LOGE("mtmd_tokenize failed: %d", ret);
                response = "[ERROR: image tokenisation failed]";
            }

            mtmd_input_chunks_free(chunks);
            mtmd_bitmap_free(bmp);
        } else {
            LOGE("mtmd_bitmap_init failed (w=%d h=%d)", imgWidth, imgHeight);
            response = "[ERROR: bitmap init failed]";
        }

    // ── Text-only path ────────────────────────────────────────────────────
    } else {
        if (jImageRgba != nullptr && !nc->mctx) {
            LOGW("Image provided but mmproj not loaded — using text-only path");
        }

        auto tokens = tokenize(nc->model, formatted, true);
        if (tokens.empty()) {
            LOGE("Tokenisation returned empty result");
            return env->NewStringUTF("[ERROR: tokenisation failed]");
        }

        if (!decodeBatch(nc->ctx, tokens, 0)) {
            LOGE("llama_decode (prefill) failed");
            return env->NewStringUTF("[ERROR: prefill decode failed]");
        }

        response = sampleResponse(nc->ctx, nc->model, 512);
    }

    LOGI("Inference complete. Response length: %zu", response.size());
    return env->NewStringUTF(response.c_str());
}

// ─── unloadModel ─────────────────────────────────────────────────────────────

extern "C" JNIEXPORT void JNICALL
Java_com_airi_odslm_jni_LlamaCppBridge_unloadModel(
        JNIEnv * /* env */, jobject /* thiz */, jlong handle) {

    auto * nc = (NativeContext *)(uintptr_t)handle;
    if (!nc) return;

    {   // Acquire lock to wait for any in-flight infer() to complete
        std::lock_guard<std::mutex> lock(nc->mutex);

        if (nc->mctx)  { mtmd_free(nc->mctx);         nc->mctx  = nullptr; }
        if (nc->ctx)   { llama_free(nc->ctx);          nc->ctx   = nullptr; }
        if (nc->model) { llama_model_free(nc->model);  nc->model = nullptr; }
    }

    delete nc;
    LOGI("Model unloaded. Handle %p released.", nc);
}
