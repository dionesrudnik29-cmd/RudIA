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

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long
)

class LocalModelEngine(private val context: Context) {

    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(
        modelDir,
        "Qwen3-0.6B-Q4_K_M.gguf"
    )

    private val modelUrl =
        "https://huggingface.co/unsloth/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf?download=true"

    val officialModelUrl = modelUrl

    @Volatile
    var state: ModelRuntimeState = ModelRuntimeState.NOT_DOWNLOADED
        private set

    init {
        modelDir.mkdirs()

        File(modelDir, "Qwen3-4B-Q4_K_M.gguf").delete()

        if (modelFile.exists() && modelFile.length() > 0L) {
            state = ModelRuntimeState.NOT_DOWNLOADED
        }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeReleaseModel()

    suspend fun downloadModel(
        onProgress: (DownloadProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            modelDir.mkdirs()

            val tempFile = File(
                modelDir,
                modelFile.name + ".part"
            )

            connection =
                (URL(modelUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    connect()
                }

            if (connection.responseCode !in 200..299) {
                throw Exception(
                    "Servidor retornou HTTP ${connection.responseCode}"
                )
            }

            val total = connection.contentLengthLong
            var downloaded = 0L
            var windowStart = System.currentTimeMillis()
            var windowBytes = 0L

            connection.inputStream.use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)

                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloaded += count

                        val now = System.currentTimeMillis()
                        val elapsed = now - windowStart
                        if (elapsed >= 500L) {
                            val speed = (downloaded - windowBytes) * 1000L / elapsed
                            onProgress(DownloadProgress(downloaded, total, speed))
                            windowStart = now
                            windowBytes = downloaded
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

            Result.success(
                "Download concluído. Arquivo salvo no aplicativo."
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Falha no download: ${e.message}", e)
            )
        } finally {
            connection?.disconnect()
        }
    }

    @Synchronized
    fun refreshState() {
        if (state == ModelRuntimeState.READY) {
            return
        }

        if (!modelFile.exists() || modelFile.length() == 0L) {
            state = ModelRuntimeState.NOT_DOWNLOADED
            return
        }

        state = ModelRuntimeState.LOADING

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
            ModelRuntimeState.READY ->
                "Modelo local pronto."

            ModelRuntimeState.LOADING ->
                "Carregando modelo local..."

            ModelRuntimeState.ERROR ->
                "Erro ao carregar o modelo. Verifique o arquivo e a memória."

            ModelRuntimeState.NOT_DOWNLOADED -> {
                if (modelFile.exists() && modelFile.length() > 0L) {
                    "Modelo baixado. Toque em Atualizar status para carregar."
                } else {
                    "Modelo local ainda não baixado."
                }
            }
        }
    }

    @Synchronized
    fun generate(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Digite uma mensagem.")
            )
        }

        // Só tenta carregar o modelo se ele ainda não estiver pronto.
        if (state != ModelRuntimeState.READY) {
            refreshState()
        }

        if (state != ModelRuntimeState.READY) {
            return Result.failure(
                IllegalStateException(
                    "O modelo não está pronto. Baixe e carregue o modelo primeiro."
                )
            )
        }

        return try {
            val response = nativeGenerate(prompt.trim())

            if (response.isBlank()) {
                Result.failure(
                    Exception("O modelo retornou uma resposta vazia.")
                )
            } else {
                Result.success(response)
            }
        } catch (e: Throwable) {
            Result.failure(
                Exception(
                    "Falha ao gerar resposta: ${e.message ?: "erro nativo"}",
                    e
                )
            )
        }
    }

    @Synchronized
    fun releaseModel() {
        try {
            nativeReleaseModel()
        } catch (_: Throwable) {
            // Evita que uma falha ao liberar o modelo feche a interface.
        }

        state = ModelRuntimeState.NOT_DOWNLOADED
    }

    companion object {
        init {
            try {
                System.loadLibrary("rud_llama_jni")
            } catch (_: UnsatisfiedLinkError) {
                // O erro será apresentado quando o modelo for utilizado.
            }
        }
    }
}
