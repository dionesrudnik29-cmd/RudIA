package com.rud.ia

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

sealed interface ModelStatus {
    data object Checking : ModelStatus
    data object Missing : ModelStatus
    data class Downloading(val done: Long, val total: Long) : ModelStatus
    data class Importing(val done: Long, val total: Long) : ModelStatus
    data object Loading : ModelStatus
    data object Ready : ModelStatus
    data class Error(val message: String, val modelPresent: Boolean) : ModelStatus
}

/** Situação do leitor de imagens (usado só quando o usuário manda uma foto). */
sealed interface VisionStatus {
    data object Checking : VisionStatus
    data object Missing : VisionStatus
    data class Downloading(val done: Long, val total: Long) : VisionStatus
    data object Ready : VisionStatus
    data class Error(val message: String) : VisionStatus
}

/** Etapa da resposta em andamento, para mostrar no balão. */
enum class GenerationPhase { READING_IMAGE, THINKING }

data class UiState(
    val historyLoaded: Boolean = false,
    val model: ModelStatus = ModelStatus.Checking,
    val conversations: List<Conversation> = emptyList(),
    val currentId: String? = null,
    val generating: Boolean = false,
    val partialDownloadBytes: Long = 0L,
    val legacyModelBytes: Long = 0L,
    val vision: VisionStatus = VisionStatus.Checking,
    val visionPartialBytes: Long = 0L,
    /** Foto escolhida e ainda não enviada. */
    val attachment: String? = null,
    val preparingAttachment: Boolean = false,
    val phase: GenerationPhase? = null,
    /** Mostrar o raciocínio da IA antes da resposta. */
    val showThinking: Boolean = true,
    /** Aviso curto para mostrar na tela (consumido pela interface). */
    val notice: String? = null
) {
    val current: Conversation? get() = conversations.firstOrNull { it.id == currentId }
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = LocalModelEngine.get(app)
    private val store = ChatStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var transferJob: Job? = null
    private var visionJob: Job? = null
    private var generationJob: Job? = null
    private val imagesDir = ImageTools.imagesDir(app)
    private val prefs = app.getSharedPreferences("rudia_prefs", android.content.Context.MODE_PRIVATE)

    fun setShowThinking(on: Boolean) {
        prefs.edit().putBoolean("show_thinking", on).apply()
        _state.update { it.copy(showThinking = on) }
    }

    @Volatile
    private var stopRequested = false

    init {
        _state.update { it.copy(showThinking = prefs.getBoolean("show_thinking", true)) }
        viewModelScope.launch {
            // 1) Restaura o histórico salvo no aparelho.
            val saved = store.load().map { c ->
                c.copy(messages = c.messages.map { m ->
                    if (m.status == MessageStatus.PENDING) {
                        m.copy(
                            status = MessageStatus.STOPPED,
                            text = m.text.ifBlank { "Resposta interrompida porque o app foi fechado." }
                        )
                    } else m
                })
            }
            val list = saved.ifEmpty { listOf(createConversation()) }.sortedByDescending { it.updatedAt }
            _state.update {
                it.copy(historyLoaded = true, conversations = list, currentId = list.first().id)
            }
            // 2) Verifica e carrega o modelo automaticamente.
            checkModel()
            // 3) Leitor de imagens e limpeza de fotos que não pertencem a nenhuma conversa.
            checkVision()
            cleanOrphanImages(list)
        }
    }

    // ---------- Modelo ----------

    fun checkModel() {
        refreshLegacy()
        if (transferJob?.isActive == true) return
        if (engine.isLoaded) {
            setModel(ModelStatus.Ready)
            return
        }
        transferJob = viewModelScope.launch {
            setModel(ModelStatus.Checking)
            val installed = withContext(Dispatchers.IO) { engine.isModelInstalled() }
            _state.update { it.copy(partialDownloadBytes = engine.partialBytes()) }
            if (installed) loadModel() else setModel(ModelStatus.Missing)
        }
    }

    private suspend fun loadModel() {
        setModel(ModelStatus.Loading)
        try {
            withContext(ModelDispatcher) { engine.load() }
            setModel(ModelStatus.Ready)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            setModel(ModelStatus.Error(e.message ?: "Falha ao carregar o modelo.", modelPresent = true))
        }
    }

    fun downloadModel() {
        if (transferJob?.isActive == true) return
        transferJob = viewModelScope.launch {
            setModel(ModelStatus.Downloading(engine.partialBytes(), -1L))
            try {
                engine.download { done, total -> setModel(ModelStatus.Downloading(done, total)) }
                loadModel()
            } catch (e: CancellationException) {
                _state.update { it.copy(partialDownloadBytes = engine.partialBytes()) }
                setModel(ModelStatus.Missing)
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(partialDownloadBytes = engine.partialBytes()) }
                setModel(ModelStatus.Error(friendly(e), modelPresent = false))
            }
        }
    }

    fun importModel(uri: Uri) {
        if (transferJob?.isActive == true) return
        transferJob = viewModelScope.launch {
            setModel(ModelStatus.Importing(0L, -1L))
            try {
                engine.importFrom(uri) { done, total -> setModel(ModelStatus.Importing(done, total)) }
                loadModel()
            } catch (e: CancellationException) {
                setModel(ModelStatus.Missing)
                throw e
            } catch (e: Throwable) {
                setModel(ModelStatus.Error(friendly(e), modelPresent = false))
            }
        }
    }

    private fun refreshLegacy() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { engine.legacyModelBytes() }
            _state.update { it.copy(legacyModelBytes = bytes) }
        }
    }

    /** Apaga o modelo antigo de 4B para liberar espaço. Conversas não são tocadas. */
    fun deleteLegacyModel() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { engine.deleteLegacyModel() }
            refreshLegacy()
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
    }

    private fun setModel(status: ModelStatus) {
        _state.update { it.copy(model = status) }
    }

    private fun friendly(e: Throwable): String = when (e) {
        is UnknownHostException -> "Sem conexão com a internet. Conecte-se e tente de novo."
        is SocketTimeoutException -> "A conexão ficou lenta demais. Toque em Baixar para continuar de onde parou."
        is OutOfMemoryError -> "Memória insuficiente."
        else -> e.message ?: "Erro desconhecido."
    }

    // ---------- Leitor de imagens ----------

    fun checkVision() {
        if (visionJob?.isActive == true) return
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { engine.isVisionInstalled() }
            val partial = withContext(Dispatchers.IO) { engine.visionPartialBytes() }
            _state.update {
                it.copy(
                    vision = if (installed) VisionStatus.Ready else VisionStatus.Missing,
                    visionPartialBytes = partial
                )
            }
        }
    }

    fun downloadVision() {
        if (visionJob?.isActive == true) return
        visionJob = viewModelScope.launch {
            setVision(VisionStatus.Downloading(engine.visionPartialBytes(), LocalModelEngine.VISION_TOTAL_BYTES))
            try {
                engine.downloadVision { done, total -> setVision(VisionStatus.Downloading(done, total)) }
                setVision(VisionStatus.Ready)
                notify("Pronto! Agora a Rud IA consegue ver suas fotos 📷")
            } catch (e: CancellationException) {
                _state.update { it.copy(visionPartialBytes = engine.visionPartialBytes()) }
                setVision(VisionStatus.Missing)
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(visionPartialBytes = engine.visionPartialBytes()) }
                setVision(VisionStatus.Error(friendly(e)))
            }
        }
    }

    fun cancelVisionDownload() {
        visionJob?.cancel()
    }

    private fun setVision(status: VisionStatus) {
        _state.update { it.copy(vision = status) }
    }

    /** Prepara a foto escolhida (reduz e salva) para enviar na próxima mensagem. */
    fun attachImage(uri: Uri) {
        if (_state.value.preparingAttachment) return
        _state.update { it.copy(preparingAttachment = true) }
        viewModelScope.launch {
            try {
                val file = File(imagesDir, "pending_${newId()}.jpg")
                withContext(Dispatchers.IO) { ImageTools.prepare(getApplication<Application>(), uri, file) }
                val old = _state.value.attachment
                _state.update { it.copy(attachment = file.absolutePath) }
                if (old != null) withContext(Dispatchers.IO) { File(old).delete() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(if (e is OutOfMemoryError) "Essa foto é grande demais." else (e.message ?: "Não foi possível abrir essa foto."))
            } finally {
                _state.update { it.copy(preparingAttachment = false) }
            }
        }
    }

    fun removeAttachment() {
        val path = _state.value.attachment ?: return
        _state.update { it.copy(attachment = null) }
        viewModelScope.launch(Dispatchers.IO) { File(path).delete() }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun notify(message: String) {
        _state.update { it.copy(notice = message) }
    }

    /** Apaga fotos que não estão em nenhuma conversa (sobras de exclusões antigas). */
    private fun cleanOrphanImages(conversations: List<Conversation>) {
        val startedAt = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val used = conversations.flatMap { c -> c.messages.mapNotNull { it.imagePath } }.toSet()
            imagesDir.listFiles()?.forEach { f ->
                // Nunca apaga uma foto escolhida agora há pouco (ainda não enviada).
                if (f.absolutePath !in used && f.lastModified() < startedAt) f.delete()
            }
        }
    }

    // ---------- Conversa ----------

    fun send(text: String): Boolean {
        val prompt = text.trim()
        val s = _state.value
        val pending = s.attachment
        if ((prompt.isEmpty() && pending == null) || s.generating || s.preparingAttachment) return false
        if (s.model !is ModelStatus.Ready) return false
        if (pending != null && s.vision !is VisionStatus.Ready) {
            notify("Baixe o leitor de imagens para a Rud IA ver fotos.")
            return false
        }
        val convId = s.currentId ?: return false

        val history = s.current?.messages.orEmpty()
        val userId = newId()
        // A foto deixa de ser "pendente" e passa a pertencer à mensagem.
        val imagePath = pending?.let { path ->
            val target = File(imagesDir, "$userId.jpg")
            if (File(path).renameTo(target)) target.absolutePath else path
        }
        val userMsg = ChatMessage(userId, Role.USER, prompt, imagePath = imagePath)
        val botMsg = ChatMessage(newId(), Role.ASSISTANT, "", MessageStatus.PENDING)

        updateConversation(convId) { c ->
            c.copy(
                title = if (c.messages.isEmpty()) PromptBuilder.makeTitle(prompt.ifEmpty { "📷 Foto" }) else c.title,
                updatedAt = System.currentTimeMillis(),
                messages = c.messages + userMsg + botMsg
            )
        }
        stopRequested = false
        _state.update {
            it.copy(
                generating = true,
                attachment = null,
                phase = if (imagePath != null) GenerationPhase.READING_IMAGE else GenerationPhase.THINKING
            )
        }
        persist()

        val think = _state.value.showThinking
        generationJob = viewModelScope.launch {
            var finalText = ""
            var status = MessageStatus.DONE
            try {
                finalText = withContext(ModelDispatcher) {
                    var description: String? = null
                    if (imagePath != null) {
                        val image = ImageTools.loadRgb(File(imagePath))
                        val seen = engine.describeImage(image)
                        description = seen
                        if (stopRequested) return@withContext ""
                        if (seen.isBlank()) {
                            throw ModelException("O leitor de imagens não conseguiu entender essa foto. Tente outra.")
                        }
                        updateMessage(convId, userMsg.id) { it.copy(imageDescription = seen) }
                        persist()
                        _state.update { it.copy(phase = GenerationPhase.THINKING) }
                    }
                    val ctx = engine.contextSize
                    val maxNew = PromptBuilder.maxNewTokens(ctx, think)
                    val fullPrompt = PromptBuilder.build(
                        history = history,
                        userText = prompt,
                        contextSize = ctx,
                        maxNewTokens = maxNew,
                        countTokens = { engine.countTokens(it) },
                        imageDescription = description,
                        thinking = think
                    )
                    val raw = engine.generate(fullPrompt, maxNew) { partial ->
                        if (think) {
                            val (t, a) = PromptBuilder.splitThinking(partial)
                            updateMessage(convId, botMsg.id) { it.copy(thinking = t.ifBlank { null }, text = a) }
                        } else {
                            val shown = PromptBuilder.clean(partial)
                            updateMessage(convId, botMsg.id) { it.copy(text = shown) }
                        }
                    }
                    if (think) {
                        val (t, a) = PromptBuilder.splitThinking(raw)
                        updateMessage(convId, botMsg.id) { it.copy(thinking = t.ifBlank { null }) }
                        a.trimEnd()
                    } else PromptBuilder.clean(raw).trimEnd()
                }
                if (stopRequested) status = MessageStatus.STOPPED
                if (finalText.isBlank()) {
                    if (stopRequested) {
                        finalText = "Geração interrompida."
                    } else {
                        status = MessageStatus.ERROR
                        finalText = "O modelo não retornou texto. Tente reformular a pergunta."
                    }
                }
            } catch (e: CancellationException) {
                engine.stop()
                status = MessageStatus.STOPPED
                throw e
            } catch (e: Throwable) {
                status = MessageStatus.ERROR
                finalText = "Erro: " + friendly(e)
            } finally {
                val text = finalText.ifBlank {
                    findMessage(convId, botMsg.id)?.text?.ifBlank { null } ?: "Geração interrompida."
                }
                updateMessage(convId, botMsg.id) { it.copy(text = text, status = status) }
                updateConversation(convId) { it.copy(updatedAt = System.currentTimeMillis()) }
                _state.update { it.copy(generating = false, phase = null) }
                withContext(NonCancellable) { store.save { _state.value.conversations } }
            }
        }
        return true
    }

    fun stopGeneration() {
        stopRequested = true
        engine.stop()
    }

    fun newConversation() {
        val s = _state.value
        if (s.generating) return
        if (s.current?.messages?.isEmpty() == true) return // já está numa conversa vazia
        val conv = createConversation()
        _state.update { it.copy(conversations = listOf(conv) + it.conversations, currentId = conv.id) }
        persist()
    }

    fun selectConversation(id: String) {
        if (_state.value.generating) return
        _state.update { it.copy(currentId = id) }
    }

    private var lastDeleted: Pair<Conversation, Int>? = null

    /**
     * Apaga a conversa na hora. Antes, a exclusão era ignorada em silêncio
     * quando a conversa estava gerando resposta; agora a geração é parada e
     * a conversa é apagada mesmo assim. Retorna true se algo foi apagado.
     */
    fun deleteConversation(id: String): Boolean {
        val s = _state.value
        if (s.generating && s.currentId == id) stopGeneration()
        val result = ConversationOps.delete(s.conversations, s.currentId, id) { createConversation() }
            ?: return false
        lastDeleted = result.removed to result.removedIndex
        _state.update { it.copy(conversations = result.conversations, currentId = result.currentId) }
        persist()
        return true
    }

    /** Desfaz a última exclusão (botão "Desfazer" do aviso). */
    fun undoDelete() {
        val (conv, index) = lastDeleted ?: return
        lastDeleted = null
        _state.update { s ->
            val list = ConversationOps.restore(s.conversations, conv, index)
            val keepCurrent = s.generating && list.any { it.id == s.currentId }
            s.copy(conversations = list, currentId = if (keepCurrent) s.currentId else conv.id)
        }
        persist()
    }

    // ---------- Auxiliares ----------

    private fun createConversation(now: Long = System.currentTimeMillis()) =
        Conversation(id = newId(), title = "Nova conversa", updatedAt = now, messages = emptyList())

    private fun newId() = UUID.randomUUID().toString()

    private fun updateConversation(id: String, change: (Conversation) -> Conversation) {
        _state.update { s ->
            s.copy(conversations = s.conversations.map { if (it.id == id) change(it) else it })
        }
    }

    private fun updateMessage(convId: String, msgId: String, change: (ChatMessage) -> ChatMessage) {
        updateConversation(convId) { c ->
            c.copy(messages = c.messages.map { if (it.id == msgId) change(it) else it })
        }
    }

    private fun findMessage(convId: String, msgId: String): ChatMessage? =
        _state.value.conversations.firstOrNull { it.id == convId }?.messages?.firstOrNull { it.id == msgId }

    private fun persist() {
        viewModelScope.launch { store.save { _state.value.conversations } }
    }

    override fun onCleared() {
        // Se a tela for destruída no meio de uma resposta, para a geração nativa.
        engine.stop()
        super.onCleared()
    }
}
