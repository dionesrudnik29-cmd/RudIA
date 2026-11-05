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

    // Modelo antigo (Qwen3-4B, ≈2,5 GB). Pesado demais para o Snapdragon 685.
    // Não é apagado sozinho: o usuário decide no menu se quer liberar o espaço.
    private val legacyFiles = listOf(
        File(modelDir, LEGACY_MODEL_FILE_NAME),
        File(modelDir, "$LEGACY_MODEL_FILE_NAME.part")
    )

    // Leitor de imagens (SmolVLM2-500M + projetor de visão, ≈550 MB no total).
    // Fica separado da IA principal e só é carregado ao analisar uma foto.
    private val visionModelFile = File(modelDir, VISION_MODEL_FILE_NAME)
    private val visionProjFile = File(modelDir, VISION_PROJ_FILE_NAME)

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
    private external fun nativeDescribeImage(
        modelPath: String,
        mmprojPath: String,
        rgb: ByteArray,
        width: Int,
        height: Int,
        prompt: String,
        maxTokens: Int,
        nThreads: Int,
        callback: TokenCallback
    )

    /** Verifica se o arquivo existe, tem tamanho plausível e cabeçalho GGUF. */
    fun isModelInstalled(): Boolean {
        if (!modelFile.isFile || modelFile.length() < MIN_MODEL_BYTES) return false
        return hasGgufHeader(modelFile)
    }

    fun partialBytes(): Long = if (partFile.isFile) partFile.length() else 0L

    /** Quanto espaço o modelo antigo de 4B ainda ocupa (0 se não existir). */
    fun legacyModelBytes(): Long = legacyFiles.filter { it.isFile }.sumOf { it.length() }

    /** Apaga só o modelo antigo de 4B. Nunca toca no modelo atual nem nas conversas. */
    fun deleteLegacyModel(): Boolean {
        var ok = true
        legacyFiles.forEach { if (it.exists() && !it.delete()) ok = false }
        return ok
    }

    /** Bloqueante. Chamar dentro de withContext(ModelDispatcher). */
    fun load() {
        nativeLibraryError?.let {
            throw ModelException("A parte nativa do app não foi carregada ($it). Reinstale o APK.")
        }
        if (isLoaded) return
        if (!isModelInstalled()) throw ModelException("Arquivo do modelo não encontrado.")

        val threads = ModelConfig.threadsFor(Runtime.getRuntime().availableProcessors())
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
        return ModelConfig.contextSizeFor(info.availMem)
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
                    // Só caracteres completos: evita "�" no meio de emojis.
                    onText(Utf8.decodeComplete(buffer.toByteArray()))
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

    // ---------- Leitor de imagens ----------

    fun isVisionInstalled(): Boolean =
        visionModelFile.isFile && visionModelFile.length() == VISION_MODEL_BYTES && hasGgufHeader(visionModelFile) &&
            visionProjFile.isFile && visionProjFile.length() == VISION_PROJ_BYTES && hasGgufHeader(visionProjFile)

    /**
     * Bloqueante. Chamar dentro de withContext(ModelDispatcher).
     * Descreve a foto (em inglês) usando o leitor de imagens local.
     */
    fun describeImage(image: RgbImage): String {
        nativeLibraryError?.let {
            throw ModelException("A parte nativa do app não foi carregada ($it). Reinstale o APK.")
        }
        if (!isVisionInstalled()) throw ModelException("O leitor de imagens não está instalado.")
        val buffer = ByteArrayOutputStream()
        val threads = ModelConfig.threadsFor(Runtime.getRuntime().availableProcessors())
        try {
            nativeDescribeImage(
                visionModelFile.absolutePath,
                visionProjFile.absolutePath,
                image.pixels,
                image.width,
                image.height,
                PromptBuilder.VISION_PROMPT,
                PromptBuilder.VISION_MAX_TOKENS,
                threads,
                TokenCallback { bytes ->
                    buffer.write(bytes, 0, bytes.size)
                    true
                }
            )
        } catch (e: OutOfMemoryError) {
            throw ModelException("Memória insuficiente para analisar a imagem. Feche outros aplicativos.", e)
        } catch (e: IllegalStateException) {
            throw ModelException(e.message ?: "Falha ao analisar a imagem.", e)
        }
        return PromptBuilder.cleanDescription(buffer.toString("UTF-8"))
    }

    /** Baixa o leitor de imagens (dois arquivos), retomando se cair. */
    suspend fun downloadVision(onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        val total = VISION_TOTAL_BYTES
        val parts = listOf(
            Triple(VISION_MODEL_URL, visionModelFile, VISION_MODEL_BYTES),
            Triple(VISION_PROJ_URL, visionProjFile, VISION_PROJ_BYTES)
        )
        var needed = 0L
        for ((_, file, size) in parts) {
            if (file.isFile && file.length() == size) continue
            val part = partOf(file)
            needed += size - (if (part.isFile) part.length() else 0L)
        }
        ensureSpace(needed)

        var before = 0L
        for ((url, file, size) in parts) {
            if (file.isFile && file.length() == size) {
                before += size
                onProgress(before, total)
                continue
            }
            val offset = before
            val part = partOf(file)
            fetchResumable(url, part, size) { done, _ -> onProgress(offset + done, total) }
            if (part.length() != size || !hasGgufHeader(part)) {
                part.delete()
                throw ModelException("O arquivo do leitor de imagens veio corrompido. Tente baixar de novo.")
            }
            if (file.exists()) file.delete()
            if (!part.renameTo(file)) throw ModelException("Não foi possível salvar o leitor de imagens.")
            before += size
        }
        onProgress(total, total)
    }

    fun visionPartialBytes(): Long {
        var sum = 0L
        for (f in listOf(visionModelFile, visionProjFile)) {
            val part = partOf(f)
            sum += when {
                f.isFile -> f.length()
                part.isFile -> part.length()
                else -> 0L
            }
        }
        return sum
    }

    private fun partOf(file: File) = File(file.parentFile, file.name + ".part")

    /** Download com retomada (cabeçalho Range) para um arquivo .part. */
    private suspend fun fetchResumable(url: String, part: File, expected: Long, onProgress: (Long, Long) -> Unit) =
        withContext(Dispatchers.IO) {
            var existing = if (part.isFile) part.length() else 0L
            if (existing >= expected) return@withContext
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "RudIA-Android")
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            try {
                val code = connection.responseCode
                if (code == 416 && existing > 0) return@withContext
                if (code !in 200..299) throw ModelException("O servidor respondeu com erro HTTP $code.")
                val append = code == 206
                if (!append) existing = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(part, append).use { output ->
                        copyWithProgress(input, output, existing, expected, onProgress) { ensureActive() }
                    }
                }
                if (part.length() != expected) {
                    throw ModelException("Download incompleto. Toque em Baixar para continuar de onde parou.")
                }
            } finally {
                connection.disconnect()
            }
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
            throw ModelException("Esse arquivo é pequeno demais para ser um modelo de IA (.gguf).")
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
        // Qwen3-1.7B Q4_K_M (≈1,1 GB): mesma família e mesmo formato de conversa
        // do Qwen3-4B, mas cerca de 2,5x mais leve e rápido no celular.
        const val MODEL_FILE_NAME = "Qwen3-1.7B-Q4_K_M.gguf"
        const val MODEL_URL =
            "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true"
        const val MIN_MODEL_BYTES = 800_000_000L
        const val EXPECTED_MODEL_BYTES = 1_107_409_472L
        const val LEGACY_MODEL_FILE_NAME = "Qwen3-4B-Q4_K_M.gguf"

        // Leitor de imagens: SmolVLM2-500M (ggml-org), versão fixa do repositório.
        private const val VISION_REPO =
            "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/ccd7aae53bcb1997355c2f094959e72b3642ce17"
        const val VISION_MODEL_FILE_NAME = "SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        const val VISION_PROJ_FILE_NAME = "mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        const val VISION_MODEL_URL = "$VISION_REPO/$VISION_MODEL_FILE_NAME?download=true"
        const val VISION_PROJ_URL = "$VISION_REPO/$VISION_PROJ_FILE_NAME?download=true"
        const val VISION_MODEL_BYTES = 436_808_704L
        const val VISION_PROJ_BYTES = 108_785_184L
        const val VISION_TOTAL_BYTES = VISION_MODEL_BYTES + VISION_PROJ_BYTES

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
