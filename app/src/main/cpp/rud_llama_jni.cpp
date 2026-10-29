#include <jni.h>
#include <android/log.h>
#include <llama.h>

#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>

#define LOG_TAG "RudLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    llama_model* g_model = nullptr;
    llama_context* g_context = nullptr;
    std::mutex g_mutex;
    std::atomic<bool> g_stop{false};
    std::once_flag g_backend_once;

    void throwJava(JNIEnv* env, const std::string& message) {
        jclass cls = env->FindClass("java/lang/IllegalStateException");
        if (cls) env->ThrowNew(cls, message.c_str());
    }

    void freeAll() {
        if (g_context) {
            llama_free(g_context);
            g_context = nullptr;
        }
        if (g_model) {
            llama_model_free(g_model);
            g_model = nullptr;
        }
    }

    void logCallback(ggml_log_level level, const char* text, void*) {
        if (level == GGML_LOG_LEVEL_ERROR) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", text);
        }
    }

    bool tokenize(const llama_vocab* vocab, const std::string& text, std::vector<llama_token>& out) {
        int needed = -llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), nullptr, 0, true, true);
        if (needed <= 0) return false;
        out.resize(needed);
        int count = llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), out.data(), (int32_t) out.size(), true, true);
        if (count < 0) return false;
        out.resize(count);
        return true;
    }

    // GetStringUTFChars devolve "modified UTF-8", que quebra emojis.
    // Aqui convertemos via bytes UTF-8 reais.
    std::string jstringToStd(JNIEnv* env, jstring s) {
        if (!s) return {};
        jclass stringClass = env->GetObjectClass(s);
        jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
        jstring charset = env->NewStringUTF("UTF-8");
        auto bytes = (jbyteArray) env->CallObjectMethod(s, getBytes, charset);
        env->DeleteLocalRef(charset);
        env->DeleteLocalRef(stringClass);
        if (env->ExceptionCheck() || !bytes) return {};
        jsize len = env->GetArrayLength(bytes);
        std::string result((size_t) len, '\0');
        if (len > 0) env->GetByteArrayRegion(bytes, 0, len, reinterpret_cast<jbyte*>(&result[0]));
        env->DeleteLocalRef(bytes);
        return result;
    }
}

extern "C" {

// Retorna null em caso de sucesso ou uma mensagem de erro.
JNIEXPORT jstring JNICALL
Java_com_rud_ia_LocalModelEngine_nativeLoadModel(
    JNIEnv* env, jobject, jstring modelPath, jint nCtx, jint nThreads) {

    std::call_once(g_backend_once, []() {
        llama_log_set(logCallback, nullptr);
        llama_backend_init();
    });

    std::string path = jstringToStd(env, modelPath);
    if (path.empty()) return env->NewStringUTF("Caminho do modelo inválido.");

    std::lock_guard<std::mutex> lock(g_mutex);
    freeAll();

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;

    g_model = llama_model_load_from_file(path.c_str(), mp);
    if (!g_model) {
        LOGE("Falha ao carregar modelo GGUF: %s", path.c_str());
        return env->NewStringUTF("Não foi possível abrir o arquivo do modelo. Ele pode estar incompleto ou corrompido.");
    }

    const int threads = std::max(1, (int) nThreads);
    int ctxSizes[] = {(int) nCtx, 2048, 1024};
    for (int size : ctxSizes) {
        if (size <= 0) continue;
        llama_context_params cp = llama_context_default_params();
        cp.n_ctx = (uint32_t) size;
        cp.n_batch = 512;
        cp.n_ubatch = 512;
        cp.n_threads = threads;
        cp.n_threads_batch = threads;
        g_context = llama_init_from_model(g_model, cp);
        if (g_context) {
            LOGI("Modelo carregado. n_ctx=%d threads=%d", size, threads);
            return nullptr;
        }
        LOGE("Falha ao criar contexto com n_ctx=%d, tentando menor", size);
    }

    freeAll();
    return env->NewStringUTF("Memória insuficiente para iniciar o modelo. Feche outros aplicativos e tente novamente.");
}

JNIEXPORT jint JNICALL
Java_com_rud_ia_LocalModelEngine_nativeContextSize(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_context ? (jint) llama_n_ctx(g_context) : 0;
}

JNIEXPORT jint JNICALL
Java_com_rud_ia_LocalModelEngine_nativeCountTokens(JNIEnv* env, jobject, jstring prompt) {
    std::string text = jstringToStd(env, prompt);
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model) return -1;
    std::vector<llama_token> tokens;
    if (!tokenize(llama_model_get_vocab(g_model), text, tokens)) return -1;
    return (jint) tokens.size();
}

JNIEXPORT void JNICALL
Java_com_rud_ia_LocalModelEngine_nativeStop(JNIEnv*, jobject) {
    g_stop.store(true);
}

// Gera a resposta enviando cada pedaço de texto (bytes UTF-8) para o callback.
// Retorna o número de tokens gerados. Lança IllegalStateException em erro.
JNIEXPORT jint JNICALL
Java_com_rud_ia_LocalModelEngine_nativeGenerate(
    JNIEnv* env, jobject, jstring prompt, jint maxTokens, jobject callback) {

    std::string input = jstringToStd(env, prompt);
    if (input.empty()) {
        throwJava(env, "Mensagem vazia.");
        return 0;
    }

    jclass cbClass = env->FindClass("com/rud/ia/TokenCallback");
    if (!cbClass) return 0; // ClassNotFoundException já pendente
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "([B)Z");
    if (!onToken) return 0;

    std::lock_guard<std::mutex> lock(g_mutex);
    g_stop.store(false);

    if (!g_model || !g_context) {
        throwJava(env, "O modelo não está carregado.");
        return 0;
    }

    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens;
    if (!tokenize(vocab, input, tokens)) {
        throwJava(env, "Não foi possível preparar a mensagem.");
        return 0;
    }

    const int nCtx = (int) llama_n_ctx(g_context);
    if ((int) tokens.size() >= nCtx - 16) {
        throwJava(env, "A conversa ficou longa demais para a memória do modelo. Inicie uma nova conversa.");
        return 0;
    }
    const int budget = std::min((int) maxTokens, nCtx - (int) tokens.size() - 1);

    llama_memory_clear(llama_get_memory(g_context), true);

    // Processa o prompt em blocos de n_batch (evita erro com prompts longos).
    const int nBatch = 512;
    for (size_t i = 0; i < tokens.size(); i += nBatch) {
        if (g_stop.load()) return 0;
        int n = (int) std::min((size_t) nBatch, tokens.size() - i);
        llama_batch batch = llama_batch_get_one(tokens.data() + i, n);
        if (llama_decode(g_context, batch) != 0) {
            throwJava(env, "Erro ao processar a mensagem (memória insuficiente?).");
            return 0;
        }
    }

    // Parâmetros recomendados pela Qwen para o modo sem raciocínio.
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.05f, 0.0f, 0.5f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(20));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.8f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::vector<char> piece(256);
    int generated = 0;
    std::string error;

    for (int i = 0; i < budget; ++i) {
        if (g_stop.load()) break;

        llama_token token = llama_sampler_sample(sampler, g_context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        // special=false: não mostra tokens de controle como <|im_end|>.
        int len = llama_token_to_piece(vocab, token, piece.data(), (int32_t) piece.size(), 0, false);
        if (len < 0) {
            piece.resize((size_t) -len);
            len = llama_token_to_piece(vocab, token, piece.data(), (int32_t) piece.size(), 0, false);
        }

        if (len > 0) {
            jbyteArray bytes = env->NewByteArray(len);
            env->SetByteArrayRegion(bytes, 0, len, reinterpret_cast<const jbyte*>(piece.data()));
            jboolean keepGoing = env->CallBooleanMethod(callback, onToken, bytes);
            env->DeleteLocalRef(bytes);
            if (env->ExceptionCheck() || !keepGoing) break;
        }

        generated++;
        llama_batch batch = llama_batch_get_one(&token, 1);
        if (llama_decode(g_context, batch) != 0) {
            error = "Erro durante a geração (memória insuficiente?).";
            break;
        }
    }

    llama_sampler_free(sampler);

    if (!error.empty() && !env->ExceptionCheck()) {
        throwJava(env, error);
    }
    return generated;
}

JNIEXPORT void JNICALL
Java_com_rud_ia_LocalModelEngine_nativeReleaseModel(JNIEnv*, jobject) {
    g_stop.store(true);
    std::lock_guard<std::mutex> lock(g_mutex);
    freeAll();
}

}
