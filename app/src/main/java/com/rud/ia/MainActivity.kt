
package com.rud.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Void = Color(0xFF080611)
private val Panel = Color(0xFF151025)
private val Violet = Color(0xFF7C3AED)
private val Neon = Color(0xFFB56BFF)
private val Lilac = Color(0xFFD9C2FF)
private val Muted = Color(0xFFA99BBF)
private val BorderViolet = Color(0xFF56318F)

private val RudColors = darkColorScheme(
    primary = Neon,
    onPrimary = Color.White,
    secondary = Violet,
    background = Void,
    surface = Panel,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF211735),
    onSurfaceVariant = Lilac
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 6, 17)
        window.navigationBarColor = android.graphics.Color.rgb(8, 6, 17)

        setContent {
            MaterialTheme(colorScheme = RudColors) {
                ChatScreen()
            }
        }
    }
}

data class Message(val text: String, val isUser: Boolean)

@Composable
private fun NeonPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "neon")
    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = modifier
            .shadow(
                elevation = (5 + glow * 7).dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Violet.copy(alpha = 0.25f),
                spotColor = Neon.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF201431),
                        Color(0xFF100C20),
                        Color(0xFF171026)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Neon.copy(alpha = glow),
                        BorderViolet.copy(alpha = 0.65f),
                        Color(0xFF8B5CF6).copy(alpha = glow)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun NeonButton(
    label: String,
    symbol: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 54.dp)
            .shadow(
                elevation = 5.dp,
                shape = shape,
                ambientColor = Violet.copy(alpha = 0.35f)
            )
            .border(1.dp, BorderViolet, shape),
        shape = shape,
        color = Color(0xFF191127)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                fontSize = 21.sp,
                color = Neon
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (enabled) Lilac else Muted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
                "Olá! Sou o Rud IA. Baixe o modelo para começar a usar a inteligência artificial local.",
                false
            )
        )
    }

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF120A22),
                        Void,
                        Color(0xFF0D0819)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(
                            10.dp,
                            CircleShape,
                            ambientColor = Violet,
                            spotColor = Neon
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Violet, Color(0xFF32145C))
                            )
                        )
                        .border(1.dp, Neon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✧", fontSize = 31.sp, color = Color.White)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Rud ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "IA",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neon
                        )
                    }
                    Text(
                        "SUA INTELIGÊNCIA LOCAL",
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = Muted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF201431))
                        .border(1.dp, BorderViolet, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("LOCAL", fontSize = 10.sp, color = Lilac)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cartão do modelo
            NeonPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF30204B))
                            .border(
                                1.dp,
                                BorderViolet,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◈", fontSize = 27.sp, color = Neon)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MODELO LOCAL",
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            color = Muted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            modelStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lilac,
                            maxLines = 2
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (modelStatus.contains(
                                        "pronto",
                                        ignoreCase = true
                                    ) ||
                                    modelStatus.contains(
                                        "carregado",
                                        ignoreCase = true
                                    )
                                ) Color(0xFF4DE6B0)
                                else Neon
                            )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    modelInstruction,
                    fontSize = 11.sp,
                    color = Muted
                )
            }

            Spacer(Modifier.height(10.dp))

            // Controles do modelo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonButton(
                    label = if (isDownloading) "Baixando..." else "Baixar modelo",
                    symbol = "↓",
                    modifier = Modifier.weight(1f),
                    enabled = !isDownloading && !isProcessing
                ) {
                    showDownloadDialog = true
                    isDownloading = true
                    downloadProgress = 0
                    downloadMessage = "Conectando ao servidor..."

                    scope.launch {
                        try {
                            val result = modelEngine.downloadModel { progress ->
                                scope.launch(Dispatchers.Main) {
                                    downloadProgress = progress
                                    downloadMessage =
                                        "Baixando modelo... $progress%"
                                }
                            }

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
                        } catch (e: Exception) {
                            downloadMessage =
                                "Erro no download: ${e.message ?: "falha inesperada"}"
                        } finally {
                            isDownloading = false
                        }
                    }
                }

                NeonButton(
                    label = "Atualizar status",
                    symbol = "↻",
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && !isDownloading
                ) {
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
                            modelStatus =
                                "Erro ao verificar: ${e.message ?: "desconhecido"}"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Área de conversa
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CONVERSA",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = Muted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BorderViolet, Color.Transparent)
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(250)) +
                            slideInVertically(
                                initialOffsetY = { it / 5 },
                                animationSpec = tween(250)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            },
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!msg.isUser) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF281642))
                                        .border(1.dp, BorderViolet, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✧", fontSize = 20.sp, color = Neon)
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            Column(
                                modifier = Modifier.widthIn(max = 290.dp),
                                horizontalAlignment = if (msg.isUser) {
                                    Alignment.End
                                } else {
                                    Alignment.Start
                                }
                            ) {
                                Text(
                                    if (msg.isUser) "VOCÊ" else "RUD IA",
                                    fontSize = 9.sp,
                                    letterSpacing = 1.5.sp,
                                    color = if (msg.isUser) Neon else Muted
                                )

                                Spacer(Modifier.height(5.dp))

                                Box(
                                    modifier = Modifier
                                        .shadow(
                                            4.dp,
                                            RoundedCornerShape(18.dp),
                                            ambientColor = Violet.copy(alpha = 0.2f)
                                        )
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 18.dp,
                                                topEnd = 18.dp,
                                                bottomStart = if (msg.isUser) 18.dp else 5.dp,
                                                bottomEnd = if (msg.isUser) 5.dp else 18.dp
                                            )
                                        )
                                
