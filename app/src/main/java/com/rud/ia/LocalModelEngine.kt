
package com.rud.ia

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class ModelRuntimeState {
    NOT_DOWNLOADED,
    LOADING,
    READY,
    ERROR
}

class LocalModelEngine(private val context: Context) {
    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, "Qwen3-4B-Q4_K_M.gguf")

    private val modelUrl =
        "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf?download=true"

    val officialModelUrl = modelUrl

    var state: ModelRuntimeState = ModelRuntimeState.NOT_DOWNLOADED
        private set

    init {
        modelDir.mkdirs()
        if (modelFile.exists() && modelFile.length() > 0) {
            state = ModelRuntimeState.NOT_DOWNLOADED
        }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeReleaseModel()

    suspend fun downloadModel(
        onProgress: (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            modelDir.mkdirs()
            val tempFile = File(modelDir, modelFile.name + ".part")

            connection = (URL(modelUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
                connect()
            }

            if (connection.responseCode !in 200..299) {
                throw Exception("Servidor retornou HTTP ${connection.responseCode}")
            }

            val total = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)

                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloaded += count

                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }

            if (tempFile.length() == 0L) {
                throw Exception("O arquivo baixado está vazio.")
            }

            if (!tempFile.renameTo(modelFile)) {
                tempFile.copyTo(modelFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(100)
            Result.success("Download concluído. Arquivo salvo no aplicativo.")
        } catch (e: Exception) {
            Result.failure(Exception("Falha no download: ${e.message}", e))
        } finally {
            connection?.disconnect()
        }
    }

    fun refreshState() {
        if (!modelFile.exists() || modelFile.length() == 0L) {
            state = ModelRuntimeState.NOT_DOWNLOADED
            return
        }

        state = try {
            if (nativeLoadModel(modelFile.absolutePath)) {
                ModelRuntimeState.READY
            } else {
                ModelRuntimeState.ERROR
            }
        } catch (_: Throwable) {
            ModelRuntimeState.ERROR
        }
    }

    fun statusText(): String {
        return when (state) {
            ModelRuntimeState.READY -> "Modelo local pronto."
            ModelRuntimeState.LOADING -> "Carregando modelo local..."
            ModelRuntimeState.ERROR -> "Erro ao carregar o modelo. Verifique o arquivo e a memória."
            ModelRuntimeState.NOT_DOWNLOADED -> {
                if (modelFile.exists()) "Modelo baixado. Toque em Atualizar status para carregar."
                else "Modelo local ainda não baixado."
            }
        }
    }

    fun downloadInstructionText(): String =
        "Baixe o Qwen3-4B GGUF Q4_K_M usando o botão Baixar modelo."

    fun generate(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Digite uma mensagem."))
        }

        refreshState()

        if (state != ModelRuntimeState.READY) {
            return Result.failure(
                IllegalStateException("O modelo não está pronto. Baixe e carregue o modelo primeiro.")
            )
        }

        return try {
            Result.success(nativeGenerate(prompt.trim()))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun releaseModel() {
        try {
            nativeReleaseModel()
        } catch (_: Throwable) {
        }
        state = ModelRuntimeState.NOT_DOWNLOADED
    }

    companion object {
        init {
            try {
                System.loadLibrary("rud_llama_jni")
            } catch (_: UnsatisfiedLinkError) {
            }
        }
    }
}
