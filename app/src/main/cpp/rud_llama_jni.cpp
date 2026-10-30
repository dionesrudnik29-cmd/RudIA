
#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <string>
#include <vector>
#include <mutex>

#define LOG_TAG "RudLLM"

namespace {
    llama_model* g_model = nullptr;
    llama_context* g_context = nullptr;
    std::mutex g_mutex;

    size_t completeUtf8Length(const std::string& s) {
        size_t n = s.size();
        for (size_t back = 1; back <= 4 && back <= n; ++back) {
            unsigned char c = static_cast<unsigned char>(s[n - back]);
            if ((c & 0xC0) == 0x80) continue;
            size_t need = (c >= 0xF0) ? 4 : (c >= 0xE0) ? 3 : (c >= 0xC0) ? 2 : 1;
            return back >= need ? n : n - back;
        }
        return n;
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_rud_ia_LocalModelEngine_nativeLoadModel(
    JNIEnv* env, jobject, jstring modelPath) {

    if (!modelPath) return JNI_FALSE;

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    llama_model_params mp = llama_model_default_params();
    g_model = llama_model_load_from_file(path, mp);

    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                            "Falha ao carregar modelo GGUF");
        return JNI_FALSE;
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 256;
    cp.n_threads = 4;

    g_context = llama_init_from_model(g_model, cp);

    if (!g_context) {
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_rud_ia_LocalModelEngine_nativeGenerate(
    JNIEnv* env, jobject, jstring prompt, jobject listener) {

    if (!prompt) return env->NewStringUTF("Digite uma mensagem.");

    const char* raw = env->GetStringUTFChars(prompt, nullptr);
    if (!raw) return env->NewStringUTF("Erro ao ler mensagem.");

    std::string input(raw);
    env->ReleaseStringUTFChars(prompt, raw);

    jclass listenerClass = env->GetObjectClass(listener);
    jmethodID onPartial = env->GetMethodID(
        listenerClass, "onPartialAnswer", "(Ljava/lang/String;)V");
    env->DeleteLocalRef(listenerClass);
    if (!onPartial) return env->NewStringUTF("Erro interno ao preparar a resposta.");

    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_model || !g_context)
        return env->NewStringUTF("Carregue o modelo local primeiro.");

    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::string formatted =
        "<|im_start|>user\n" + input +
        "<|im_end|>\n<|im_start|>assistant\n";

    int needed = -llama_tokenize(
        vocab, formatted.c_str(), formatted.size(),
        nullptr, 0, true, true);

    if (needed <= 0)
        return env->NewStringUTF("Não foi possível tokenizar a mensagem.");

    std::vector<llama_token> tokens(needed);
    int count = llama_tokenize(
        vocab, formatted.c_str(), formatted.size(),
        tokens.data(), tokens.size(), true, true);

    if (count < 0)
        return env->NewStringUTF("Erro ao preparar os tokens.");

    tokens.resize(count);
    llama_memory_clear(llama_get_memory(g_context), true);

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_context, batch) != 0)
        return env->NewStringUTF("Erro ao processar a mensagem.");

    llama_sampler* sampler = llama_sampler_chain_init(
        llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string output;
    size_t sent = 0;
    const int max_tokens = 1024;
    int generated = 0;

    for (int i = 0; i < max_tokens; ++i) {
        llama_token token = llama_sampler_sample(
            sampler, g_context, -1);

        if (llama_vocab_is_eog(vocab, token)) break;

        char piece[1024];
        int len = llama_token_to_piece(
            vocab, token, piece, sizeof(piece), 0, true);

        if (len > 0) output.append(piece, len);
        generated++;

        size_t safe = completeUtf8Length(output);
        if (safe > sent) {
            sent = safe;
            jstring partial = env->NewStringUTF(output.substr(0, safe).c_str());
            env->CallVoidMethod(listener, onPartial, partial);
            env->DeleteLocalRef(partial);
        }

        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(g_context, batch) != 0) break;
    }

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG,
                        "Resposta gerada com %d tokens", generated);

    llama_sampler_free(sampler);
    return env->NewStringUTF(output.substr(0, completeUtf8Length(output)).c_str());
}

JNIEXPORT void JNICALL
Java_com_rud_ia_LocalModelEngine_nativeReleaseModel(
    JNIEnv*, jobject) {

    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_context) llama_free(g_context);
    if (g_model) llama_model_free(g_model);

    g_context = nullptr;
    g_model = nullptr;
}

}
