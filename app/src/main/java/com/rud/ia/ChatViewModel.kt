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

data class UiState(
    val historyLoaded: Boolean = false,
    val model: ModelStatus = ModelStatus.Checking,
    val conversations: List<Conversation> = emptyList(),
    val currentId: String? = null,
    val generating: Boolean = false,
    val partialDownloadBytes: Long = 0L
) {
    val current: Conversation? get() = conversations.firstOrNull { it.id == currentId }
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = LocalModelEngine.get(app)
    private val store = ChatStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var transferJob: Job? = null
    private var generationJob: Job? = null

    @Volatile
    private var stopRequested = false

    init {
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
        }
    }

    // ---------- Modelo ----------

    fun checkModel() {
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

    // ---------- Conversa ----------

    fun send(text: String): Boolean {
        val prompt = text.trim()
        val s = _state.value
        if (prompt.isEmpty() || s.generating || s.model !is ModelStatus.Ready) return false
        val convId = s.currentId ?: return false

        val history = s.current?.messages.orEmpty()
        val userMsg = ChatMessage(newId(), Role.USER, prompt)
        val botMsg = ChatMessage(newId(), Role.ASSISTANT, "", MessageStatus.PENDING)

        updateConversation(convId) { c ->
            c.copy(
                title = if (c.messages.isEmpty()) PromptBuilder.makeTitle(prompt) else c.title,
                updatedAt = System.currentTimeMillis(),
                messages = c.messages + userMsg + botMsg
            )
        }
        stopRequested = false
        _state.update { it.copy(generating = true) }
        persist()

        generationJob = viewModelScope.launch {
            var finalText = ""
            var status = MessageStatus.DONE
            try {
                finalText = withContext(ModelDispatcher) {
                    val ctx = engine.contextSize
                    val maxNew = PromptBuilder.maxNewTokens(ctx)
                    val fullPrompt = PromptBuilder.build(history, prompt, ctx, maxNew) { engine.countTokens(it) }
                    val raw = engine.generate(fullPrompt, maxNew) { partial ->
                        val shown = PromptBuilder.clean(partial)
                        updateMessage(convId, botMsg.id) { it.copy(text = shown) }
                    }
                    PromptBuilder.clean(raw).trimEnd()
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
                _state.update { it.copy(generating = false) }
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

    fun deleteConversation(id: String) {
        val s = _state.value
        if (s.generating && s.currentId == id) return
        var remaining = s.conversations.filterNot { it.id == id }
        if (remaining.isEmpty()) remaining = listOf(createConversation())
        val current = if (s.currentId == id) remaining.first().id else s.currentId
        _state.update { it.copy(conversations = remaining, currentId = current) }
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
