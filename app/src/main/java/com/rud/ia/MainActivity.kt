package com.rud.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF0B0714)
private val Panel = Color(0xFF191027)
private val PanelHigh = Color(0xFF241838)
private val Violet = Color(0xFF9B5CFF)
private val Neon = Color(0xFFC5A0FF)
private val TextWhite = Color(0xFFF7F1FF)
private val Muted = Color(0xFFB9AACD)
private val ReadyGreen = Color(0xFF5BE49B)
private val WarnAmber = Color(0xFFFFC857)
private val ErrorRed = Color(0xFFFF6B81)

private val RudColors = darkColorScheme(
    primary = Violet,
    secondary = Neon,
    background = Background,
    surface = Panel,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = ErrorRed
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RudColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    RudApp()
                }
            }
        }
    }
}

@Composable
private fun RudApp(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Panel) {
                HistoryDrawer(
                    state = state,
                    onNew = {
                        vm.newConversation()
                        scope.launch { drawerState.close() }
                    },
                    onSelect = { id ->
                        vm.selectConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onDelete = { id -> vm.deleteConversation(id) }
                )
            }
        }
    ) {
        ChatScreen(
            state = state,
            onMenu = { scope.launch { drawerState.open() } },
            onNew = { vm.newConversation() },
            onSend = { vm.send(it) },
            onStop = { vm.stopGeneration() },
            onDownload = { vm.downloadModel() },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onCancelTransfer = { vm.cancelTransfer() },
            onRetry = { vm.checkModel() }
        )
    }
}

@Composable
private fun ChatScreen(
    state: UiState,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onSend: (String) -> Boolean,
    onStop: () -> Unit,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancelTransfer: () -> Unit,
    onRetry: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val messages = state.current?.messages.orEmpty()
    val listState = rememberLazyListState()
    val ready = state.model is ModelStatus.Ready

    // Rola até o fim quando chega mensagem nova ou o texto cresce.
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(state.currentId, messages.size, lastLength) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    fun submit() {
        if (onSend(input)) input = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.Menu, contentDescription = "Conversas", tint = Neon)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("RUD IA", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Neon)
                StatusLine(state.model)
            }
            IconButton(onClick = onNew, enabled = !state.generating) {
                Icon(Icons.Filled.Add, contentDescription = "Nova conversa", tint = Neon)
            }
        }

        if (!ready) {
            Spacer(Modifier.height(8.dp))
            ModelCard(state, onDownload, onImport, onCancelTransfer, onRetry)
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Como posso ajudar?", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Tudo roda no seu celular, sem internet.", color = Muted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message -> MessageBubble(message) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(if (ready) "Digite sua mensagem..." else "Aguardando o modelo...", color = Muted)
                },
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet,
                    unfocusedBorderColor = PanelHigh,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = Neon
                )
            )

            if (state.generating) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelHigh)
                ) {
                    Box(Modifier.size(16.dp).background(Neon, RoundedCornerShape(3.dp)))
                }
            } else {
                Button(
                    onClick = { submit() },
                    enabled = ready && input.isNotBlank(),
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
private fun StatusLine(model: ModelStatus) {
    val (color, text) = when (model) {
        ModelStatus.Checking -> WarnAmber to "Verificando o modelo…"
        ModelStatus.Missing -> ErrorRed to "Modelo não instalado"
        is ModelStatus.Downloading -> WarnAmber to "Baixando modelo… ${percent(model.done, model.total)}"
        is ModelStatus.Importing -> WarnAmber to "Importando modelo… ${percent(model.done, model.total)}"
        ModelStatus.Loading -> WarnAmber to "Carregando o modelo…"
        ModelStatus.Ready -> ReadyGreen to "Pronto · Qwen3-4B local"
        is ModelStatus.Error -> ErrorRed to "Erro no modelo"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(text, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ModelCard(
    state: UiState,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancelTransfer: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        when (val model = state.model) {
            ModelStatus.Checking -> {
                Text("Procurando o modelo no aparelho…", color = TextWhite, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Neon)
            }

            ModelStatus.Loading -> {
                Text("Carregando o Qwen3-4B na memória…", color = TextWhite, fontSize = 14.sp)
                Text("Isso leva alguns segundos e acontece sozinho.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Neon)
            }

            is ModelStatus.Downloading -> TransferProgress("Baixando o modelo", model.done, model.total, onCancelTransfer)
            is ModelStatus.Importing -> TransferProgress("Copiando o modelo", model.done, model.total, onCancelTransfer)

            ModelStatus.Missing -> InstallOptions(null, state.partialDownloadBytes, onDownload, onImport)

            is ModelStatus.Error -> {
                if (model.modelPresent) {
                    Text("Não foi possível carregar o modelo", color = ErrorRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(model.message, color = TextWhite, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                        Text("Tentar novamente")
                    }
                } else {
                    InstallOptions(model.message, state.partialDownloadBytes, onDownload, onImport)
                }
            }

            ModelStatus.Ready -> Unit
        }
    }
}

@Composable
private fun InstallOptions(error: String?, partialBytes: Long, onDownload: () -> Unit, onImport: () -> Unit) {
    Text("Modelo de IA não instalado", color = Neon, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    if (error != null) {
        Text(error, color = ErrorRed, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
    }
    Text(
        "O Rud IA usa o Qwen3-4B (Q4_K_M, ≈2,5 GB), que fica salvo só neste app. " +
            "Baixe uma única vez pelo Wi-Fi ou, se já tiver o arquivo .gguf no celular, importe-o.",
        color = Muted,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onDownload,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Text(if (partialBytes > 0) "Continuar (${gb(partialBytes)})" else "Baixar modelo")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
            Text("Importar .gguf", color = Neon)
        }
    }
}

@Composable
private fun TransferProgress(title: String, done: Long, total: Long, onCancel: () -> Unit) {
    Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    if (total > 0) {
        LinearProgressIndicator(
            progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Neon
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Neon)
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (total > 0) "${gb(done)} de ${gb(total)} · ${percent(done, total)}" else gb(done),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCancel) { Text("Cancelar", color = Neon) }
    }
    Text("Mantenha o app aberto. Se cair, o download continua de onde parou.", color = Muted, fontSize = 11.sp)
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "VOCÊ" else "RUD IA",
            color = Neon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        val borderColor = if (message.status == MessageStatus.ERROR) ErrorRed else Color.Transparent
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(if (isUser) Violet.copy(alpha = 0.35f) else Panel, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (message.status == MessageStatus.PENDING && message.text.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Neon, strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Pensando…", color = Muted, fontSize = 14.sp)
                }
            } else {
                SelectionContainer {
                    Text(
                        text = message.text,
                        color = if (message.status == MessageStatus.ERROR) ErrorRed else TextWhite,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        when (message.status) {
            MessageStatus.PENDING -> if (message.text.isNotBlank()) {
                Text("escrevendo…", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            MessageStatus.STOPPED ->
                Text("interrompida", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            else -> Unit
        }
    }
}

@Composable
private fun HistoryDrawer(
    state: UiState,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Conversas", color = Neon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNew,
            enabled = !state.generating,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Nova conversa")
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.conversations, key = { it.id }) { conv ->
                val selected = conv.id == state.currentId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) PanelHigh else Color.Transparent, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !state.generating) { onSelect(conv.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            conv.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${conv.messages.size} mensagens · ${dateFormat.format(Date(conv.updatedAt))}",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { pendingDelete = conv }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar conversa", tint = Muted)
                    }
                }
            }
        }
    }

    pendingDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Apagar conversa?") },
            text = { Text("\"${conv.title}\" será apagada deste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(conv.id)
                    pendingDelete = null
                }) { Text("Apagar", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun gb(bytes: Long): String = String.format(Locale("pt", "BR"), "%.2f GB", bytes / 1_000_000_000.0)

private fun percent(done: Long, total: Long): String =
    if (total > 0) "${((done * 100) / total).coerceIn(0, 100)}%" else ""
