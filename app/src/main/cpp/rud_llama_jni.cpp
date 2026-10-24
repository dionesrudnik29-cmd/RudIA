#include <jni.h>
#include <android/log.h>
#include <cstdlib>
#include <string>

namespace {

static bool g_model_loaded = false;

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_rud_ia_LocalModelEngine_nativeLoadModel(JNIEnv* env, jobject /*thiz*/, jstring modelPath) {
    if (modelPath == nullptr) {
        g_model_loaded = false;
        return JNI_FALSE;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    const std::string model_path(path);
    env->ReleaseStringUTFChars(modelPath, path);

    const bool exists = std::ifstream(model_path, std::ios::binary).good();
    if (!exists) {
        g_model_loaded = false;
        return JNI_FALSE;
    }

    // This is the native bridge point for llama.cpp integration.
    // The actual Qwen3-4B GGUF model loading will be connected here once the
    // runtime is added to the project as a proper native dependency.
    g_model_loaded = true;
    __android_log_print(ANDROID_LOG_INFO, "RudLLM", "Native model bridge initialized: %s", model_path.c_str());
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_rud_ia_LocalModelEngine_nativeGenerate(JNIEnv* env, jobject /*thiz*/, jstring prompt) {
    if (!g_model_loaded) {
        const char* msg = "O modelo local ainda não foi carregado. Adicione o runtime nativo do Qwen3-4B GGUF para gerar respostas reais.";
        return env->NewStringUTF(msg);
    }

    if (prompt == nullptr) {
        const char* msg = "Prompt vazio. Forneça uma mensagem para gerar uma resposta.";
        return env->NewStringUTF(msg);
    }

    const char* raw = env->GetStringUTFChars(prompt, nullptr);
    if (raw == nullptr) {
        const char* msg = "Erro ao obter o prompt enviado para a inferência local.";
        return env->NewStringUTF(msg);
    }

    const std::string prompt_text(raw);
    env->ReleaseStringUTFChars(prompt, raw);

    const std::string response =
        "A inferência real do Qwen3-4B via llama.cpp ainda está em preparação. "
        "Este é um placeholder nativo para a ponte Android/JNI e o runtime local. "
        "Prompt recebido: " + prompt_text;

    return env->NewStringUTF(response.c_str());
}

JNIEXPORT void JNICALL
Java_com_rud_ia_LocalModelEngine_nativeReleaseModel(JNIEnv* /*env*/, jobject /*thiz*/) {
    g_model_loaded = false;
    __android_log_print(ANDROID_LOG_INFO, "RudLLM", "Native model bridge released.");
}

}  // extern "C"
