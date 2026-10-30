package com.rud.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val engine = remember(context) { LocalModelEngine(context.applicationContext) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var modelState by remember { mutableStateOf(engine.state) }
    var status by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }

    val ready = modelState == ModelRuntimeState.READY && !downloading

    fun refreshStatus() {
        if (busy || downloading) return

        busy = true
        if (engine.state != ModelRuntimeState.READY) {
            modelState = ModelRuntimeState.LOADING
        }

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engine.refreshState()
                }
                modelState = engine.state
                status = engine.statusText()
            } catch (e: Exception) {
                modelState = ModelRuntimeState.ERROR
                status = "Erro: ${e.message ?: "não foi possível verificar"}"
            } finally {
                busy = false
            }
        }
    }

    fun downloadModel() {
        if (busy || downloading) return

        downloading = true
        downloadError = null
        progress = 0

        scope.launch {
            val result = engine.downloadModel { value ->
                scope.launch(Dispatchers.Main) {
                    progress = value
                }
            }
            downloading = false
            result.onSuccess { refreshStatus() }
            result.onFailure { error ->
                downloadError = error.message ?: "Erro desconhecido"
            }
        }
    }

    fun sendMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || !ready || busy) return

        input = ""
        messages.add(ChatMessage(prompt, isUser = true))
        messages.add(ChatMessage("Pensando...", isUser = false))
        busy = true

        scope.launch {
            try {
                val answer = withContext(Dispatchers.IO) {
                    engine.generate(prompt).getOrElse { error ->
                        "Erro ao gerar resposta: ${error.message ?: "falha no modelo"}"
                    }
                }
                messages[messages.lastIndex] = ChatMessage(answer, isUser = false)
            } catch (e: Exception) {
                messages[messages.lastIndex] = ChatMessage(
                    "Erro: ${e.message ?: "não foi possível gerar a resposta"}",
                    isUser = false
                )
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val headerText = when {
        downloading -> "Baixando a IA... $progress%"
        modelState == ModelRuntimeState.READY -> "Pronta para conversar"
        modelState == ModelRuntimeState.LOADING -> "Carregando a IA..."
        modelState == ModelRuntimeState.ERROR -> "Erro ao carregar a IA"
        else -> "Baixe a IA para começar"
    }
    val headerColor = when {
        modelState == ModelRuntimeState.READY && !downloading -> StatusReady
        modelState == ModelRuntimeState.ERROR -> StatusError
        else -> StatusWaiting
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChatHeader(
            statusText = headerText,
            statusColor = headerColor,
            newChatEnabled = !busy && messages.isNotEmpty(),
            onMenuClick = { showInfo = true },
            onNewChatClick = { messages.clear() }
        )

        ModelBanner(
            modelState = modelState,
            downloading = downloading,
            progress = progress,
            downloadError = downloadError,
            onDownloadClick = { downloadModel() },
            onRetryClick = { refreshStatus() }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                WelcomeContent(
                    ready = ready,
                    onSuggestionClick = { sendMessage(it) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }

        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            ready = ready,
            busy = busy,
            onSend = { sendMessage(input) }
        )
    }

    if (showInfo) {
        ModelInfoSheet(
            status = status,
            canRefresh = !busy && !downloading,
            onRefresh = { refreshStatus() },
            onDismiss = { showInfo = false }
        )
    }
}
