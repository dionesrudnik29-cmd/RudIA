package com.rud.ia

import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.launch

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

data class Message(
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val modelEngine = remember(context) { LocalModelEngine(context) }

    var textState by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var modelStatus by remember { mutableStateOf(modelEngine.statusText()) }
    var modelInstruction by remember { mutableStateOf(modelEngine.downloadInstructionText()) }

    val messages = remember {
        mutableStateListOf(
            Message(
                "Olá! Sou o Rud IA. Estou pronto para receber o mecanismo de IA local.",
                false
            )
        )
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(modelEngine) {
        modelStatus = modelEngine.statusText()
        modelInstruction = modelEngine.downloadInstructionText()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rud IA") }
            )
        }
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = modelStatus,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = modelInstruction,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(modelEngine.officialModelUrl)
                        )

                        val resolved = browserIntent.resolveActivity(context.packageManager)
                        if (resolved != null) {
                            context.startActivity(browserIntent)
                        }

                        modelStatus = modelEngine.statusText()
                        modelInstruction = modelEngine.downloadInstructionText()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Baixar modelo")
                }

                OutlinedButton(
                    onClick = {
                        modelStatus = modelEngine.statusText()
                        modelInstruction = modelEngine.downloadInstructionText()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atualizar status")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(12.dp)
                            )
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
                    placeholder = {
                        Text("Converse com o Rud IA...")
                    },
                    enabled = !isProcessing,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    enabled = textState.isNotBlank() && !isProcessing,
                    onClick = {
                        val question = textState.trim()

                        if (question.isEmpty()) return@Button

                        messages.add(Message(question, true))
                        textState = ""
                        isProcessing = true
                        modelStatus = modelEngine.statusText()

                        val response = modelEngine.generate(question)

                        if (response.isSuccess) {
                            messages.add(Message(response.getOrThrow(), false))
                        } else {
                            messages.add(
                                Message(
                                    response.exceptionOrNull()?.message
                                        ?: "Não foi possível processar a mensagem no momento.",
                                    false
                                )
                            )
                        }

                        isProcessing = false
                        modelStatus = modelEngine.statusText()

                        scope.launch {
                            listState.animateScrollToItem(messages.lastIndex)
                        }
                    }
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}
