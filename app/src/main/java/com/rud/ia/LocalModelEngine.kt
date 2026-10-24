package com.rud.ia

import android.content.Context
import java.io.File

enum class ModelRuntimeState {
    NOT_DOWNLOADED,
    LOADING,
    READY,
    ERROR
}

class LocalModelEngine(private val context: Context) {
    private val modelDir: File = File(context.filesDir, "models")
    private val modelFile: File = File(modelDir, "Qwen3-4B-Q4_K_M.gguf")
    val officialModelUrl: String = "https://huggingface.co/Qwen/Qwen3-4B-GGUF"

    var state: ModelRuntimeState = ModelRuntimeState.NOT_DOWNLOADED
        private set

    init {
        modelDir.mkdirs()
        refreshState()
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeReleaseModel()

    fun refreshState() {
        if (!modelFile.exists()) {
            state = ModelRuntimeState.NOT_DOWNLOADED
            return
        }

        try {
            state = if (nativeLoadModel(modelFile.absolutePath)) {
                ModelRuntimeState.READY
            } else {
                ModelRuntimeState.ERROR
            }
        } catch (_: UnsatisfiedLinkError) {
            state = ModelRuntimeState.ERROR
        }
    }

    fun statusText(): String {
        refreshState()
        return when (state) {
            ModelRuntimeState.READY -> "Modelo local pronto: Qwen3-4B GGUF Q4_K_M detectado e bridge nativa ativa."
            ModelRuntimeState.LOADING -> "Carregando modelo local Qwen3-4B..."
            ModelRuntimeState.ERROR -> "Erro ao preparar o modelo local. Verifique o arquivo GGUF, a memória do aparelho e a compatibilidade da ABI."
            ModelRuntimeState.NOT_DOWNLOADED -> "Modelo local ausente. Baixe o arquivo GGUF Qwen3-4B Q4_K_M do repositório oficial para rodar offline."
        }
    }

    fun downloadInstructionText(): String {
        return "Para baixar o modelo, use a página oficial do Qwen3-4B GGUF: $officialModelUrl. " +
            "Selecione a quantização Q4_K_M e salve o arquivo em app-specific storage antes de carregar o runtime."
    }

    fun downloadModel(): Result<String> {
        if (!modelFile.exists()) {
            return Result.success(
                "Download do modelo ainda não foi executado automaticamente. " +
                    "Use o botão de download para iniciar a etapa oficial do modelo GGUF Qwen3-4B Q4_K_M. " +
                    "Repositório oficial: $officialModelUrl"
            )
        }

        return Result.success("Modelo localizado em ${modelFile.absolutePath}")
    }

    fun generate(prompt: String): Result<String> {
        val text = prompt.trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("Digite uma mensagem antes de enviar."))
        }

        refreshState()

        if (state == ModelRuntimeState.NOT_DOWNLOADED) {
            return Result.failure(
                IllegalStateException(
                    "Modelo local ausente. Baixe o pacote Qwen3-4B-GGUF Q4_K_M antes de gerar respostas."
                )
            )
        }

        if (state == ModelRuntimeState.ERROR) {
            return Result.failure(
                IllegalStateException(
                    "O runtime nativo falhou ao carregar o modelo. Verifique o arquivo do modelo e a memória disponível."
                )
            )
        }

        return try {
            Result.success(nativeGenerate(text))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun releaseModel() {
        try {
            nativeReleaseModel()
        } catch (_: UnsatisfiedLinkError) {
            // bridge not yet linked; safe no-op during native runtime bootstrap
        }

        state = ModelRuntimeState.NOT_DOWNLOADED
    }

    companion object {
        init {
            try {
                System.loadLibrary("rud_llama_jni")
            } catch (_: UnsatisfiedLinkError) {
                // Library is expected to become available as soon as the native target is built.
            }
        }
    }
}
