package com.rud.ia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val engine = remember(context) { LocalModelEngine(context.applicationContext) }
    val store = remember(context) { ChatHistoryStore(context.applicationContext) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val conversations = remember { mutableStateListOf<Conversation>() }
    var currentId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var input by remember { mutableStateOf("") }
    var modelState by remember { mutableStateOf(engine.state) }
    var status by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }

    val downloading = downloadProgress != null
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

        downloadError = null
        downloadProgress = DownloadProgress(0L, 0L, 0L)

        scope.launch {
            val result = engine.downloadModel { progress ->
                scope.launch(Dispatchers.Main) {
                    downloadProgress = progress
                }
            }
            downloadProgress = null
            result.onSuccess { refreshStatus() }
            result.onFailure { error ->
                downloadError = error.message ?: "Erro desconhecido"
            }
        }
    }

    fun persistConversation() {
        val snapshot = messages.toList()
        val firstPrompt = snapshot.firstOrNull { it.isUser } ?: return
        val updated = Conversation(
            id = currentId,
            title = firstPrompt.text.take(60),
            updatedAt = System.currentTimeMillis(),
            messages = snapshot
        )
        val index = conversations.indexOfFirst { it.id == currentId }
        if (index >= 0) {
            conversations[index] = updated
        } else {
            conversations.add(updated)
        }

        val allConversations = conversations.toList()
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { store.save(allConversations) }.isSuccess
            }
            historyError = if (saved) null else "Não foi possível salvar o histórico neste celular."
        }
    }

    fun startNewChat() {
        if (busy) return
        currentId = UUID.randomUUID().toString()
        messages.clear()
        input = ""
    }

    fun openConversation(conversation: Conversation) {
        if (busy) return
        currentId = conversation.id
        messages.clear()
        messages.addAll(conversation.messages)
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
                    engine.generate(prompt) { partial ->
                        scope.launch(Dispatchers.Main) {
                            messages[messages.lastIndex] = ChatMessage(partial, isUser = false)
                        }
                    }.getOrElse { error ->
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
                persistConversation()
            }
        }
    }

    LaunchedEffect(Unit) {
        entered = true
        conversations.addAll(withContext(Dispatchers.IO) { store.load() })
        refreshStatus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val headerText = when {
        downloading -> "Baixando a IA..."
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CardSurface,
                drawerContentColor = TextWhite
            ) {
                ChatHistoryDrawer(
                    conversations = conversations.toList(),
                    activeId = currentId,
                    status = status,
                    historyError = historyError,
                    canRefresh = !busy && !downloading,
                    onNewChat = {
                        startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onOpen = { conversation ->
                        openConversation(conversation)
                        scope.launch { drawerState.close() }
                    },
                    onRefresh = { refreshStatus() }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBrush)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it }
            ) {
                ChatHeader(
                    statusText = headerText,
                    statusColor = headerColor,
                    newChatEnabled = !busy && messages.isNotEmpty(),
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNewChatClick = { startNewChat() }
                )
            }

            ModelBanner(
                modelState = modelState,
                downloadProgress = downloadProgress,
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

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(600, delayMillis = 200)) +
                    slideInVertically(tween(600, delayMillis = 200)) { it }
            ) {
                ChatInputBar(
                    value = input,
                    onValueChange = { input = it },
                    ready = ready,
                    busy = busy,
                    onSend = { sendMessage(input) }
                )
            }
        }
    }
}
