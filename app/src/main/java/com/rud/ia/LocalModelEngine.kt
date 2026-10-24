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

    var state: ModelRuntimeState = ModelRuntimeState.NOT_DOWNLOADED
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        state = if (modelFile.exists()) {
            ModelRuntimeState.READY
        } else {
            ModelRuntimeState.NOT_DOWNLOADED
        }
    }

    fun statusText(): String {
        refreshState()
        return when (state) {
            ModelRuntimeState.READY -> "Modelo local pronto: Qwen3-4B GGUF Q4_K_M detectado."
            ModelRuntimeState.LOADING -> "Carregando modelo local Qwen3-4B..."
            ModelRuntimeState.ERROR -> "Erro ao preparar o modelo local."
            ModelRuntimeState.NOT_DOWNLOADED -> "Modelo local ausente. O download do Qwen3-4B GGUF Q4_K_M e a integração com llama.cpp ainda precisam ser concluídos."
        }
    }

    fun generate(prompt: String): Result<String> {
        refreshState()

        return when (state) {
            ModelRuntimeState.READY -> {
                Result.success(
                    "A integração real do Qwen3-4B via llama.cpp ainda não foi concluída. " +
                        "O runtime nativo será conectado nesta etapa de desenvolvimento.\n\n" +
                        "Prompt recebido: $prompt"
                )
            }
            ModelRuntimeState.NOT_DOWNLOADED -> {
                Result.failure(
                    IllegalStateException(
                        "Modelo local ausente. Baixe o arquivo Qwen3-4B GGUF Q4_K_M e configure o runtime nativo para iniciar a geração real."
                    )
                )
            }
            ModelRuntimeState.LOADING -> {
                Result.failure(
                    IllegalStateException("O modelo está sendo carregado. Aguarde a finalização do processo.")
                )
            }
            ModelRuntimeState.ERROR -> {
                Result.failure(
                    IllegalStateException("O motor local falhou ao inicializar. Verifique a memória e o arquivo do modelo.")
                )
            }
        }
    }
}
