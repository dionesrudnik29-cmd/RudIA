
package com.rud.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Background = Color(0xFF0B0714)
private val Panel = Color(0xFF191027)
private val Violet = Color(0xFF9B5CFF)
private val Neon = Color(0xFFC5A0FF)
private val TextWhite = Color(0xFFF7F1FF)
private val Muted = Color(0xFFB9AACD)

private val RudColors = darkColorScheme(
    primary = Violet,
    secondary = Neon,
    background = Background,
    surface = Panel,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = RudColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
private fun ChatScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val engine = remember(context) {
        LocalModelEngine(context.applicationContext)
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Olá! Sou o Rud IA. Carregue o modelo local para começar.",
                false
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var status by remember {
        mutableStateOf("Verificando modelo...")
    }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    fun refreshStatus() {
        if (busy || downloading) return

        busy = true
        status = "Verificando modelo..."

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engine.refreshState()
                }
                status = engine.statusText()
            } catch (e: Exception) {
                status = "Erro: ${e.message ?: "não foi possível verificar"}"
            } finally {
                busy = false
            }
        }
    }

    fun downloadModel() {
        if (busy || downloading) return

        downloading = true
        progress = 0
        showDownloadDialog = true
        status = "Baixando modelo..."

        scope.launch {
            try {
                val result = engine.downloadModel { value ->
                    scope.launch(Dispatchers.Main) {
                        progress = value
                    }
                }

                result.fold(
                    onSuccess = { message ->
                        status = message
                    },
                    onFailure = { error ->
                        status = "Falha no download: ${
                            error.message ?: "erro desconhecido"
                        }"
                    }
                )
            } catch (e: Exception) {
                status = "Falha no download: ${
                    e.message ?: "erro desconhecido"
                }"
            } finally {
                downloading = false
                showDownloadDialog = false
            }
        }
    }

    fun sendMessage() {
        val prompt = input.trim()

        if (prompt.isEmpty() || busy || downloading) return

        input = ""
        messages.add(ChatMessage(prompt, true))
        messages.add(ChatMessage("Pensando...", false))
        busy = true

        scope.launch {
            try {
                val answer = withContext(Dispatchers.IO) {
                    if (engine.state != ModelRuntimeState.READY) {
                        engine.refreshState()
                    }

                    if (engine.state != ModelRuntimeState.READY) {
                        "O modelo não está pronto. Use Atualizar status para tentar carregá-lo."
                    } else {
                        engine.generate(prompt).getOrElse { error ->
                            "Erro ao gerar resposta: ${
                                error.message ?: "falha no modelo"
                            }"
                        }
                    }
                }

                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.lastIndex)
                }
                messages.add(ChatMessage(answer, false))
                status = engine.statusText()
            } catch (e: Exception) {
                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.lastIndex)
                }
                messages.add(
                    ChatMessage(
                        "Erro: ${e.message ?: "não foi possível gerar a resposta"}",
                        false
                    )
                )
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "RUD IA",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Neon
        )

        Text(
            text = "INTELIGÊNCIA ARTIFICIAL LOCAL",
            fontSize = 11.sp,
            color = Muted
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "MODELO LOCAL",
                fontWeight = FontWeight.Bold,
                color = Neon
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = status,
                color = TextWhite,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { downloadModel() },
                    enabled = !busy && !downloading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Violet
                    )
                ) {
                    Text("Baixar modelo")
                }

                Button(
                    onClick = { refreshStatus() },
                    enabled = !busy && !downloading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38244F)
                    )
                ) {
                    Text("Atualizar status")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                enabled = !busy && !downloading,
                placeholder = {
                    Text("Digite sua mensagem...", color = Muted)
                },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { sendMessage() }
                ),
                maxLines = 4
            )

            Button(
                onClick = { sendMessage() },
                enabled = input.isNotBlank() && !busy && !downloading,
                modifier = Modifier.size(54.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Violet
                )
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("➤", fontSize = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "O processamento local pode demorar dependendo do aparelho.",
            color = Muted,
            fontSize = 10.sp
        )
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                // O download continua até terminar.
            },
            title = { Text("Baixando modelo") },
            text = {
                Column {
                    Text("$progress% concluído")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Neon
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "O arquivo pode ter vários gigabytes. Mantenha a conexão ativa.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        Text(
            text = if (message.isUser) "VOCÊ" else "RUD IA",
            color = Neon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(
                    color = if (message.isUser) {
                        Color(0xFF38244F)
                    } else {
                        Panel
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = TextWhite,
                fontSize = 14.sp
            )
        }
    }
}
