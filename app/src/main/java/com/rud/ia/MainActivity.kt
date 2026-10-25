
package com.rud.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}

data class Message(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val modelEngine = remember(context) { LocalModelEngine(context) }
    val scope = rememberCoroutineScope()

    var textState by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var modelStatus by remember { mutableStateOf(modelEngine.statusText()) }
    var modelInstruction by remember {
        mutableStateOf(modelEngine.downloadInstructionText())
    }

    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadMessage by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            Message(
                "Olá! Sou o Rud IA. Baixe o modelo para começar a usar a IA local.",
                false
            )
        )
    }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rud IA") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        modelStatus,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        modelInstruction,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !isDownloading && !isProcessing,
                    onClick = {
                        showDownloadDialog = true
                        isDownloading = true
                        downloadProgress = 0
                        downloadMessage = "Conectando ao servidor..."

                        scope.launch {
                            val result = modelEngine.downloadModel { progress ->
                                scope.launch(Dispatchers.Main) {
                                    downloadProgress = progress
                                    downloadMessage =
                                        "Baixando modelo... $progress%"
                                }
                            }

                            isDownloading = false

                            if (result.isSuccess) {
                                downloadProgress = 100
                                downloadMessage = result.getOrThrow()
                                modelStatus = modelEngine.statusText()
                                modelInstruction =
                                    modelEngine.downloadInstructionText()
                            } else {
                                downloadMessage =
                                    result.exceptionOrNull()?.message
                                        ?: "O download falhou."
                            }
                        }
                    }
                ) {
                    Text(if (isDownloading) "Baixando..." else "Baixar modelo")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && !isDownloading,
                    onClick = {
                        isProcessing = true

                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    modelEngine.refreshState()
                                    Pair(
                                        modelEngine.statusText(),
                                        modelEngine.downloadInstructionText()
                                    )
                                }

                                modelStatus = result.first
                                modelInstruction = result.second
                            } catch (e: Exception) {
                                modelStatus = "Erro ao verificar o modelo: ${
                                    e.message ?: "erro desconhecido"
                                }"
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    Text("Atualizar status")
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Surface(
                            color = if (msg.isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(msg.text, Modifier.padding(12.dp))
                        }
                    }
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Converse com o Rud IA...") },
                    enabled = !isProcessing && !isDownloading,
                    maxLines = 4
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    enabled = textState.isNotBlank() &&
                        !isProcessing && !isDownloading,
                    onClick = {
                        val question = textState.trim()
                        if (question.isEmpty()) return@Button

                        messages.add(Message(question, true))
                        textState = ""
                        isProcessing = true

                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    modelEngine.generate(question)
                                }

                                if (response.isSuccess) {
                                    messages.add(
                                        Message(response.getOrThrow(), false)
                                    )
                                } else {
                                    messages.add(
                                        Message(
                                            response.exceptionOrNull()?.message
                                                ?: "Não foi possível processar a mensagem.",
                                            false
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                messages.add(
                                    Message(
                                        "Erro: ${e.message ?: "falha inesperada"}",
                                        false
                                    )
                                )
                            } finally {
                                isProcessing = false
                                modelStatus = modelEngine.statusText()
                            }
                        }
                    }
                ) {
                    Text("Enviar")
                }
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) showDownloadDialog = false
            },
            title = {
                Text(
                    if (isDownloading) "Baixando modelo"
                    else "Download do modelo"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(downloadMessage)

                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("$downloadProgress%")
                    Text(
                        "Mantenha o aplicativo aberto durante o download.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(
                        onClick = { showDownloadDialog = false }
                    ) {
                        Text("Fechar")
                    }
                }
            }
        )
    }
}
