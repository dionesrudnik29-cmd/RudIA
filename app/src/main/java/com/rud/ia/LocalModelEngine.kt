package com.rud.ia

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ModelException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Todas as chamadas ao modelo (carregar / gerar) passam por esta única thread,
 * fora da thread principal. Assim a interface nunca trava.
 */
val ModelDispatcher: CoroutineDispatcher =
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rud-llm").apply { isDaemon = true }
    }.asCoroutineDispatcher()

class LocalModelEngine private constructor(private val appContext: Context) {

    // Mesmo caminho usado pelas versões anteriores do app: um modelo já baixado
    // por uma instalação com o mesmo applicationId é reaproveitado.
    private val modelDir = File(appContext.filesDir, "models")
    private val modelFile = File(modelDir, MODEL_FILE_NAME)
    private val partFile = File(modelDir, "$MODEL_FILE_NAME.part")

    @Volatile
    var isLoaded: Boolean = false
        private set

    @Volatile
    var contextSize: Int = 0
        private set

    private external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): String?
    private external fun nativeContextSize(): Int
    private external fun nativeCountTokens(prompt: String): Int
    private external fun nativeGenerate(prompt: String, maxTokens: Int, callback: TokenCallback): Int
    private external fun nativeStop()
    private external fun nativeReleaseModel()

    /** Verifica se o arquivo existe, tem tamanho plausível e cabeçalho GGUF. */
    fun isModelInstalled(): Boolean {
        if (!modelFile.isFile || modelFile.length() < MIN_MODEL_BYTES) return false
        return hasGgufHeader(modelFile)
    }

    fun partialBytes(): Long = if (partFile.isFile) partFile.length() else 0L

    /** Bloqueante. Chamar dentro de withContext(ModelDispatcher). */
    fun load() {
        nativeLibraryError?.let {
            throw ModelException("A parte nativa do app não foi carregada ($it). Reinstale o APK.")
        }
        if (isLoaded) return
        if (!isModelInstalled()) throw ModelException("Arquivo do modelo não encontrado.")

        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val error = try {
            nativeLoadModel(modelFile.absolutePath, chooseContextSize(), threads)
        } catch (e: OutOfMemoryError) {
            "Memória insuficiente para carregar o modelo. Feche outros aplicativos e tente novamente."
        } catch (e: Throwable) {
            "Falha nativa ao carregar o modelo: ${e.message ?: e.javaClass.simpleName}"
        }
        if (error != null) throw ModelException(error)

        contextSize = nativeContextSize()
        isLoaded = contextSize > 0
        if (!isLoaded) throw ModelException("O modelo abriu, mas o contexto não foi criado.")
    }

    private fun chooseContextSize(): Int {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val availGb = info.availMem / (1024.0 * 1024.0 * 1024.0)
        return when {
            availGb >= 3.5 -> 4096
            availGb >= 2.0 -> 2048
            else -> 1024
        }
    }

    /** Bloqueante. Conta tokens do texto (usado para caber no contexto). */
    fun countTokens(text: String): Int = nativeCountTokens(text)

    /**
     * Bloqueante. Gera a resposta do modelo real, chamando [onText] com o texto
     * acumulado à medida que ele é produzido.
     */
    fun generate(prompt: String, maxTokens: Int, onText: (String) -> Unit): String {
        if (!isLoaded) throw ModelException("O modelo não está carregado.")
        val buffer = ByteArrayOutputStream()
        var lastEmit = 0L
        try {
            nativeGenerate(prompt, maxTokens, TokenCallback { bytes ->
                buffer.write(bytes, 0, bytes.size)
                val now = System.currentTimeMillis()
                if (now - lastEmit >= 80L) {
                    lastEmit = now
                    onText(buffer.toString("UTF-8"))
                }
                true
            })
        } catch (e: OutOfMemoryError) {
            throw ModelException("Memória insuficiente durante a geração. Feche outros aplicativos.", e)
        } catch (e: IllegalStateException) {
            throw ModelException(e.message ?: "Falha na geração.", e)
        }
        val text = buffer.toString("UTF-8")
        onText(text)
        return text
    }

    fun stop() {
        if (nativeLibraryError == null) nativeStop()
    }

    fun release() {
        if (nativeLibraryError == null) nativeReleaseModel()
        isLoaded = false
        contextSize = 0
    }

    /** Baixa o modelo oficial, retomando um download interrompido. */
    suspend fun download(onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        var existing = partialBytes()
        ensureSpace(EXPECTED_MODEL_BYTES - existing)

        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RudIA-Android")
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }

        try {
            val code = connection.responseCode
            if (code == 416 && existing > 0) {
                // Servidor diz que já temos tudo.
                finalizeModel()
                return@withContext
            }
            if (code !in 200..299) throw ModelException("O servidor respondeu com erro HTTP $code.")

            val append = code == 206
            if (!append) existing = 0L
            val length = connection.contentLengthLong
            val total = if (length > 0) length + existing else -1L

            connection.inputStream.use { input ->
                FileOutputStream(partFile, append).use { output ->
                    copyWithProgress(input, output, existing, total, onProgress) { ensureActive() }
                }
            }

            if (total > 0 && partFile.length() != total) {
                throw ModelException("Download incompleto. Toque em Baixar para continuar de onde parou.")
            }
            finalizeModel()
        } finally {
            connection.disconnect()
        }
    }

    /** Copia um arquivo .gguf escolhido pelo usuário para o armazenamento do app. */
    suspend fun importFrom(uri: Uri, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L

        if (size in 1 until MIN_MODEL_BYTES) {
            throw ModelException("Esse arquivo é pequeno demais para ser o Qwen3-4B Q4_K_M (≈2,5 GB).")
        }
        modelDir.mkdirs()
        partFile.delete()
        ensureSpace(if (size > 0) size else EXPECTED_MODEL_BYTES)

        val input = resolver.openInputStream(uri)
            ?: throw ModelException("Não foi possível abrir o arquivo escolhido.")
        input.use { stream ->
            FileOutputStream(partFile, false).use { output ->
                copyWithProgress(stream, output, 0L, size, onProgress) { ensureActive() }
            }
        }
        finalizeModel()
    }

    private inline fun copyWithProgress(
        input: InputStream,
        output: FileOutputStream,
        start: Long,
        total: Long,
        onProgress: (Long, Long) -> Unit,
        checkActive: () -> Unit
    ) {
        val buffer = ByteArray(512 * 1024)
        var done = start
        var lastReport = 0L
        onProgress(done, total)
        while (true) {
            checkActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            done += read
            val now = System.currentTimeMillis()
            if (now - lastReport >= 300L) {
                lastReport = now
                onProgress(done, total)
            }
        }
        output.fd.sync()
        onProgress(done, total)
    }

    private fun finalizeModel() {
        if (partFile.length() < MIN_MODEL_BYTES || !hasGgufHeader(partFile)) {
            partFile.delete()
            throw ModelException("O arquivo não é um modelo GGUF válido. Ele foi descartado.")
        }
        if (modelFile.exists()) modelFile.delete()
        if (!partFile.renameTo(modelFile)) {
            throw ModelException("Não foi possível salvar o modelo no armazenamento do app.")
        }
    }

    private fun ensureSpace(bytesNeeded: Long) {
        val free = modelDir.usableSpace
        val margin = 200L * 1024 * 1024
        if (bytesNeeded > 0 && free < bytesNeeded + margin) {
            val needGb = (bytesNeeded + margin) / 1_000_000_000.0
            val freeGb = free / 1_000_000_000.0
            throw ModelException(
                "Espaço insuficiente: são necessários %.1f GB e há %.1f GB livres.".format(needGb, freeGb)
            )
        }
    }

    private fun hasGgufHeader(file: File): Boolean = try {
        file.inputStream().use { stream ->
            val header = ByteArray(4)
            stream.read(header) == 4 && String(header, Charsets.US_ASCII) == "GGUF"
        }
    } catch (_: Exception) {
        false
    }

    companion object {
        const val MODEL_FILE_NAME = "Qwen3-4B-Q4_K_M.gguf"
        const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf?download=true"
        const val MIN_MODEL_BYTES = 2_000_000_000L
        const val EXPECTED_MODEL_BYTES = 2_500_000_000L

        val nativeLibraryError: String? = try {
            System.loadLibrary("rud_llama_jni")
            null
        } catch (e: Throwable) {
            e.message ?: e.javaClass.simpleName
        }

        @Volatile
        private var instance: LocalModelEngine? = null

        fun get(context: Context): LocalModelEngine =
            instance ?: synchronized(this) {
                instance ?: LocalModelEngine(context.applicationContext).also { instance = it }
            }
    }
}
