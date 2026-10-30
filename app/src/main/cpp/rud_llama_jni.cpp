
#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>

#define LOG_TAG "RudLLM"

namespace {
    llama_model* g_model = nullptr;
    llama_context* g_context = nullptr;
    std::mutex g_mutex;
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
    cp.n_ctx = 1024;
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
    JNIEnv* env, jobject, jstring prompt) {

    if (!prompt) return env->NewStringUTF("Digite uma mensagem.");

    const char* raw = env->GetStringUTFChars(prompt, nullptr);
    if (!raw) return env->NewStringUTF("Erro ao ler mensagem.");

    std::string input(raw);
    env->ReleaseStringUTFChars(prompt, raw);

    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_model || !g_context)
        return env->NewStringUTF("Carregue o modelo local primeiro.");

    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::string formatted =
        "<|im_start|>user\n" + input +
        "<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n";

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
    const int max_tokens = 200;

    for (int i = 0; i < max_tokens; ++i) {
        llama_token token = llama_sampler_sample(
            sampler, g_context, -1);

        if (llama_vocab_is_eog(vocab, token)) break;

        char piece[1024];
        int len = llama_token_to_piece(
            vocab, token, piece, sizeof(piece), 0, true);

        if (len > 0) output.append(piece, len);

        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(g_context, batch) != 0) break;
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
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
