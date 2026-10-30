package com.rud.ia

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ---------- Paleta roxa do Rud IA ----------
private val Background = Color(0xFF0B0714)
private val BackgroundDeep = Color(0xFF07040E)
private val Panel = Color(0xFF191027)
private val PanelHigh = Color(0xFF241838)
private val Violet = Color(0xFF9B5CFF)
private val VioletDeep = Color(0xFF6A35D9)
private val Orchid = Color(0xFFC86BFA)
private val Neon = Color(0xFFC5A0FF)
private val TextWhite = Color(0xFFF7F1FF)
private val Muted = Color(0xFFB9AACD)
private val ReadyGreen = Color(0xFF5BE49B)
private val WarnAmber = Color(0xFFFFC857)
private val ErrorRed = Color(0xFFFF6B81)

private val BrandBrush = Brush.linearGradient(listOf(Violet, VioletDeep))
private val BrandBrushBright = Brush.linearGradient(listOf(Orchid, Violet, VioletDeep))
private val ScreenBrush = Brush.verticalGradient(listOf(Color(0xFF160B2B), Background, BackgroundDeep))

private val RudColors = darkColorScheme(
    primary = Violet,
    secondary = Neon,
    background = Background,
    surface = Panel,
    surfaceVariant = PanelHigh,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = Muted,
    error = ErrorRed
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RudColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    RudRoot()
                }
            }
        }
    }
}

// ---------- Raiz: tela de abertura + app ----------

@Composable
private fun RudRoot(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var splashDone by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!splashDone) {
            delay(2000) // tempo da animação de abertura
            vm.state.first { it.historyLoaded } // e espera o histórico ser restaurado
            splashDone = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (state.historyLoaded) RudApp(vm, state)
        AnimatedVisibility(
            visible = !splashDone,
            enter = EnterTransition.None,
            exit = fadeOut(tween(500)) + scaleOut(targetScale = 1.12f, animationSpec = tween(500))
        ) {
            SplashScreen()
        }
    }
}

@Composable
private fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "splash")
    val rotation by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "rotation"
    )
    val pulse by t.animateFloat(
        0.92f, 1.06f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    val glow by t.animateFloat(
        0.25f, 0.7f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )
    val appear = remember { Animatable(0f) }
    val textAppear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        delay(350)
        textAppear.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        alpha = appear.value
                        val s = 0.6f + 0.4f * appear.value
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Violet.copy(alpha = glow), Color.Transparent),
                            center = center,
                            radius = r
                        ),
                        radius = r
                    )
                    val stroke = 5.dp.toPx()
                    val ringR = r * 0.72f
                    rotate(rotation) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color.Transparent, VioletDeep, Violet, Neon)),
                            startAngle = 0f,
                            sweepAngle = 320f,
                            useCenter = false,
                            topLeft = Offset(center.x - ringR, center.y - ringR),
                            size = Size(ringR * 2, ringR * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    val orbit = r * 0.9f
                    for (i in 0 until 3) {
                        val a = Math.toRadians((-rotation * 0.7f + i * 120f).toDouble())
                        drawCircle(
                            color = if (i == 0) Neon else Orchid.copy(alpha = 0.7f),
                            radius = (if (i == 0) 4.5f else 3f).dp.toPx(),
                            center = Offset(center.x + orbit * cos(a).toFloat(), center.y + orbit * sin(a).toFloat())
                        )
                    }
                }
                RudLogo(
                    size = 92.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                )
            }

            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textAppear.value
                    translationY = (1f - textAppear.value) * 24.dp.toPx()
                }
            ) {
                Text(
                    "RUD IA",
                    color = TextWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Inteligência artificial no seu celular",
                    color = Muted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(30.dp))
                TypingDots(color = Neon, dot = 8.dp)
            }
        }

        Text(
            "100% offline · privado",
            color = Muted.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = textAppear.value }
        )
    }
}

// ---------- App principal ----------

@Composable
private fun RudApp(vm: ChatViewModel, state: UiState) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }

    fun deleteWithUndo(id: String) {
        if (!vm.deleteConversation(id)) return
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Conversa apagada",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
        }
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = BackgroundDeep.copy(alpha = 0.7f),
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Panel,
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
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
                        onDelete = { id -> deleteWithUndo(id) }
                    )
                }
            }
        ) {
            ChatScreen(
                state = state,
                onMenu = { scope.launch { drawerState.open() } },
                onNew = { vm.newConversation() },
                onDeleteCurrent = { state.currentId?.let { deleteWithUndo(it) } },
                onSend = { vm.send(it) },
                onStop = { vm.stopGeneration() },
                onDownload = { vm.downloadModel() },
                onImport = { importLauncher.launch(arrayOf("*/*")) },
                onCancelTransfer = { vm.cancelTransfer() },
                onRetry = { vm.checkModel() }
            )
        }

        // Fica por cima do menu lateral, para o aviso aparecer mesmo com ele aberto.
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 84.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = PanelHigh,
                contentColor = TextWhite,
                actionColor = Neon
            )
        }
    }
}

@Composable
private fun ChatScreen(
    state: UiState,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onDeleteCurrent: () -> Unit,
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
            .background(ScreenBrush)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        TopBar(
            state = state,
            canDelete = messages.isNotEmpty(),
            onMenu = onMenu,
            onNew = onNew,
            onDelete = onDeleteCurrent
        )

        AnimatedVisibility(
            visible = !ready,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                ModelCard(state, onDownload, onImport, onCancelTransfer, onRetry)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                EmptyState(
                    ready = ready,
                    onSuggestion = { input = it },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp)
                ) {
                    items(messages, key = { it.id }) { message -> MessageBubble(message) }
                }
            }
        }

        InputBar(
            value = input,
            onValueChange = { input = it },
            ready = ready,
            generating = state.generating,
            onSubmit = { submit() },
            onStop = onStop
        )
    }
}

@Composable
private fun TopBar(
    state: UiState,
    canDelete: Boolean,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Panel.copy(alpha = 0.85f))
            .border(1.dp, Violet.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Conversas", tint = Neon)
        }
        RudLogo(size = 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.current?.title?.takeIf { canDelete } ?: "Rud IA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusLine(state.model)
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Apagar esta conversa", tint = Muted)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (state.generating) Brush.linearGradient(listOf(PanelHigh, PanelHigh)) else BrandBrush)
                .clickable(enabled = !state.generating, onClick = onNew)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nova conversa", tint = Color.White)
        }
    }
}

@Composable
private fun StatusLine(model: ModelStatus) {
    val (color, text) = when (model) {
        ModelStatus.Checking -> WarnAmber to "Verificando o modelo…"
        ModelStatus.Missing -> ErrorRed to "Modelo não instalado"
        is ModelStatus.Downloading -> WarnAmber to "Baixando… ${percent(model.done, model.total)}"
        is ModelStatus.Importing -> WarnAmber to "Importando… ${percent(model.done, model.total)}"
        ModelStatus.Loading -> WarnAmber to "Carregando a IA…"
        ModelStatus.Ready -> ReadyGreen to "Online · Qwen3-4B local"
        is ModelStatus.Error -> ErrorRed to "Erro no modelo"
    }
    val t = rememberInfiniteTransition(label = "status")
    val blink by t.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "blink"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .graphicsLayer { alpha = if (model is ModelStatus.Ready) 1f else blink }
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun EmptyState(ready: Boolean, onSuggestion: (String) -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "empty")
    val glow by t.animateFloat(
        0.2f, 0.55f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Violet.copy(alpha = glow), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2
                    ),
                    radius = size.minDimension / 2
                )
            }
            RudLogo(size = 68.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Olá! Como posso ajudar?",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (ready) "Tudo roda no seu celular, sem internet." else "Assim que a IA estiver pronta, é só conversar.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        val suggestions = listOf(
            "✍️  Escreva uma mensagem de aniversário",
            "💡  Me dê ideias para ganhar dinheiro",
            "📚  Explique um assunto de forma simples",
            "🍳  Sugira uma receita rápida"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 360.dp)) {
            suggestions.forEach { s ->
                Text(
                    s,
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Panel.copy(alpha = 0.8f))
                        .border(1.dp, Violet.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { onSuggestion(s.substringAfter("  ")) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    ready: Boolean,
    generating: Boolean,
    onSubmit: () -> Unit,
    onStop: () -> Unit
) {
    val canSend = ready && value.isNotBlank()
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Panel)
            .border(1.dp, Brush.horizontalGradient(listOf(Violet.copy(alpha = 0.5f), Orchid.copy(alpha = 0.25f))), RoundedCornerShape(28.dp))
            .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 6.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(if (ready) "Pergunte qualquer coisa…" else "Aguardando a IA…", color = Muted)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            maxLines = 5,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = Neon
            )
        )

        if (generating) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PanelHigh)
                    .border(1.5.dp, Neon, CircleShape)
                    .clickable(onClick = onStop)
            ) {
                Box(Modifier.size(15.dp).background(Neon, RoundedCornerShape(4.dp)))
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) BrandBrushBright else Brush.linearGradient(listOf(PanelHigh, PanelHigh)))
                    .clickable(enabled = canSend, onClick = onSubmit)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (canSend) Color.White else Muted.copy(alpha = 0.5f)
                )
            }
        }
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
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PanelHigh, Panel)))
            .border(1.dp, Violet.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        when (val model = state.model) {
            ModelStatus.Checking -> {
                Text("Procurando a IA no aparelho…", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                GlowProgress()
            }

            ModelStatus.Loading -> {
                Text("Carregando o Qwen3-4B na memória…", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Leva alguns segundos e acontece sozinho.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                GlowProgress()
            }

            is ModelStatus.Downloading -> TransferProgress("Baixando a IA", model.done, model.total, onCancelTransfer)
            is ModelStatus.Importing -> TransferProgress("Copiando a IA", model.done, model.total, onCancelTransfer)

            ModelStatus.Missing -> InstallOptions(null, state.partialDownloadBytes, onDownload, onImport)

            is ModelStatus.Error -> {
                if (model.modelPresent) {
                    Text("Não foi possível carregar a IA", color = ErrorRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(model.message, color = TextWhite, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    PillButton("Tentar novamente", onRetry, filled = true)
                } else {
                    InstallOptions(model.message, state.partialDownloadBytes, onDownload, onImport)
                }
            }

            ModelStatus.Ready -> Unit
        }
    }
}

@Composable
private fun GlowProgress() {
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = Neon,
        trackColor = Background
    )
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, filled: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (filled) Modifier.background(BrandBrush)
                else Modifier.border(1.dp, Neon.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Text(text, color = if (filled) Color.White else Neon, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun InstallOptions(error: String?, partialBytes: Long, onDownload: () -> Unit, onImport: () -> Unit) {
    Text("A IA ainda não está instalada", color = Neon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            if (partialBytes > 0) "Continuar (${gb(partialBytes)})" else "Baixar modelo",
            onDownload,
            filled = true,
            modifier = Modifier.weight(1f)
        )
        PillButton("Importar .gguf", onImport, filled = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TransferProgress(title: String, done: Long, total: Long, onCancel: () -> Unit) {
    Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    if (total > 0) {
        LinearProgressIndicator(
            progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Neon,
            trackColor = Background
        )
    } else {
        GlowProgress()
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (total > 0) "${gb(done)} de ${gb(total)} · ${percent(done, total)}" else gb(done),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Cancelar",
            color = Neon,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onCancel).padding(8.dp)
        )
    }
    Text("Mantenha o app aberto. Se cair, o download continua de onde parou.", color = Muted, fontSize = 11.sp)
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isError = message.status == MessageStatus.ERROR

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            RudLogo(size = 30.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            val shape = if (isUser) {
                RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
            } else {
                RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .then(
                        if (isUser) Modifier.background(BrandBrush)
                        else Modifier
                            .background(Panel)
                            .border(1.dp, if (isError) ErrorRed else Violet.copy(alpha = 0.18f), shape)
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                if (message.status == MessageStatus.PENDING && message.text.isBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypingDots(color = Neon, dot = 7.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Pensando…", color = Muted, fontSize = 13.sp)
                    }
                } else {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = if (isError) ErrorRed else TextWhite,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            val footer = when (message.status) {
                MessageStatus.PENDING -> if (message.text.isNotBlank()) "escrevendo…" else null
                MessageStatus.STOPPED -> "interrompida"
                else -> null
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                if (footer != null) {
                    Text(footer, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                }
                if (!isUser && message.text.isNotBlank() && message.status != MessageStatus.PENDING) {
                    Text(
                        "Copiar",
                        color = Neon.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Resposta copiada", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
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
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RudLogo(size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Rud IA", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Suas conversas ficam neste aparelho", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (state.generating) Brush.linearGradient(listOf(PanelHigh, PanelHigh)) else BrandBrush)
                .clickable(enabled = !state.generating, onClick = onNew)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Nova conversa", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(18.dp))
        Text("RECENTES", color = Muted.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.conversations, key = { it.id }) { conv ->
                val selected = conv.id == state.currentId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) PanelHigh else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) Violet.copy(alpha = 0.45f) else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !state.generating) { onSelect(conv.id) }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Text(
                            conv.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val preview = conv.messages.lastOrNull()?.text?.replace('\n', ' ')?.takeIf { it.isNotBlank() }
                        Text(
                            preview ?: "Sem mensagens ainda",
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            dateFormat.format(Date(conv.updatedAt)),
                            color = Muted.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = { onDelete(conv.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar conversa", tint = Muted)
                    }
                }
            }
        }
    }
}

// ---------- Elementos visuais ----------

/** Logo do Rud IA: círculo roxo com uma estrela de quatro pontas. */
@Composable
private fun RudLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawCircle(BrandBrushBright)
        drawSparkle(center, this.size.minDimension * 0.30f, Color.White)
        drawSparkle(
            Offset(center.x + this.size.minDimension * 0.22f, center.y - this.size.minDimension * 0.22f),
            this.size.minDimension * 0.08f,
            Color.White.copy(alpha = 0.85f)
        )
    }
}

private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val k = r * 0.16f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        cubicTo(c.x + k, c.y - k, c.x + k, c.y - k, c.x + r, c.y)
        cubicTo(c.x + k, c.y + k, c.x + k, c.y + k, c.x, c.y + r)
        cubicTo(c.x - k, c.y + k, c.x - k, c.y + k, c.x - r, c.y)
        cubicTo(c.x - k, c.y - k, c.x - k, c.y - k, c.x, c.y - r)
        close()
    }
    drawPath(path, color)
}

/** Três pontinhos pulando (indicador de carregamento). */
@Composable
private fun TypingDots(color: Color, dot: Dp) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(dot * 0.7f), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0f at 0
                        1f at 300
                        0f at 600
                        0f at 1200
                    },
                    initialStartOffset = StartOffset(i * 160)
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(dot)
                    .graphicsLayer {
                        translationY = -y * dot.toPx() * 0.8f
                        alpha = 0.35f + 0.65f * y
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

private fun gb(bytes: Long): String = String.format(Locale("pt", "BR"), "%.2f GB", bytes / 1_000_000_000.0)

private fun percent(done: Long, total: Long): String =
    if (total > 0) "${((done * 100) / total).coerceIn(0, 100)}%" else ""
