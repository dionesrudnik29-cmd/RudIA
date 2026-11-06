package com.rud.ia

import android.os.Bundle
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
private val CodeBg = Color(0xFF2A1C40)

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
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.attachImage(uri)
    }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) vm.attachImage(Uri.parse(uri))
    }
    var showVisionDialog by rememberSaveable { mutableStateOf(false) }
    var viewerPath by rememberSaveable { mutableStateOf<String?>(null) }

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

    fun openCamera() {
        try {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app de câmera encontrado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show()
        }
    }

    fun attach(source: AttachSource) {
        if (state.vision !is VisionStatus.Ready) {
            showVisionDialog = true
            return
        }
        when (source) {
            AttachSource.CAMERA -> openCamera()
            AttachSource.GALLERY -> galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    LaunchedEffect(state.notice) {
        val message = state.notice ?: return@LaunchedEffect
        vm.consumeNotice()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    // Fecha a janela do leitor de imagens sozinha quando o download termina.
    LaunchedEffect(state.vision) {
        if (state.vision is VisionStatus.Ready) showVisionDialog = false
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    if (showVisionDialog) {
        VisionDialog(
            status = state.vision,
            partialBytes = state.visionPartialBytes,
            onDownload = { vm.downloadVision() },
            onCancel = { vm.cancelVisionDownload() },
            onDismiss = { showVisionDialog = false }
        )
    }

    viewerPath?.let { path -> ImageViewer(path) { viewerPath = null } }

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
                        onDelete = { id -> deleteWithUndo(id) },
                        onDeleteLegacy = { vm.deleteLegacyModel() },
                        onVision = { showVisionDialog = true },
                        onToggleThinking = { vm.setShowThinking(it) }
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
                onRetry = { vm.checkModel() },
                onAttach = { attach(it) },
                onRemoveAttachment = { vm.removeAttachment() },
                onOpenImage = { viewerPath = it }
            )
        }

        // Fica por cima do menu lateral, para o aviso aparecer mesmo com ele aberto.
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
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

private enum class AttachSource { CAMERA, GALLERY }

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
    onRetry: () -> Unit,
    onAttach: (AttachSource) -> Unit,
    onRemoveAttachment: () -> Unit,
    onOpenImage: (String) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val messages = state.current?.messages.orEmpty()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ready = state.model is ModelStatus.Ready

    // Está vendo o fim da conversa? Só então a tela acompanha o texto novo,
    // para não "puxar" quem está lendo uma mensagem antiga.
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            last == null || last.index >= info.totalItemsCount - 1
        }
    }

    LaunchedEffect(state.currentId, messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size)
    }
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(lastLength) {
        if (messages.isNotEmpty() && atBottom) listState.scrollToItem(messages.size)
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
                    onPhoto = { onAttach(AttachSource.GALLERY) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message, state.phase, onOpenImage)
                    }
                    item(key = "fim") { Spacer(Modifier.height(1.dp)) }
                }

                // Versão sem escopo: aqui estamos dentro de um Box que fica num Column.
                androidx.compose.animation.AnimatedVisibility(
                    visible = !atBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PanelHigh)
                            .border(1.dp, Violet.copy(alpha = 0.5f), CircleShape)
                            .clickable { scope.launch { listState.animateScrollToItem(messages.size) } }
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Ir para o fim", tint = Neon)
                    }
                }
            }
        }

        InputBar(
            value = input,
            onValueChange = { input = it },
            ready = ready,
            generating = state.generating,
            attachment = state.attachment,
            preparingAttachment = state.preparingAttachment,
            onSubmit = { submit() },
            onStop = onStop,
            onAttach = onAttach,
            onRemoveAttachment = onRemoveAttachment
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
        ModelStatus.Ready -> ReadyGreen to "Pronta · funciona sem internet"
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
private fun EmptyState(
    ready: Boolean,
    onSuggestion: (String) -> Unit,
    onPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "empty")
    val glow by t.animateFloat(
        0.2f, 0.55f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )
    val float by t.animateFloat(
        -4f, 4f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float"
    )
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = float.dp.toPx() }
        ) {
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
            "Olá! Como posso ajudar? 👋",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (ready) "Converse, tire dúvidas ou mande uma foto. Tudo roda no seu celular."
            else "Assim que a IA estiver pronta, é só conversar.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))

        val suggestions = listOf(
            "✍️" to "Escreva uma mensagem de aniversário",
            "💡" to "Me dê ideias para ganhar dinheiro",
            "📚" to "Explique um assunto de forma simples",
            "🍳" to "Sugira uma receita rápida"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 380.dp)) {
            SuggestionChip("📷", "Mande uma foto para eu analisar", highlighted = true, onClick = onPhoto)
            suggestions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (emoji, text) ->
                        SuggestionChip(emoji, text, modifier = Modifier.weight(1f)) { onSuggestion(text) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (highlighted) Brush.linearGradient(listOf(VioletDeep.copy(alpha = 0.55f), Panel))
                else Brush.linearGradient(listOf(Panel, Panel))
            )
            .border(1.dp, Violet.copy(alpha = if (highlighted) 0.5f else 0.2f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (highlighted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(text, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(text, color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val QuickEmojis = listOf(
    "😀", "😂", "😍", "🥰", "😎", "🤔", "😅", "😭",
    "👍", "👏", "🙏", "💪", "🔥", "✨", "🎉", "❤️",
    "💜", "🚀", "💡", "📚", "🍕", "☕", "⚽", "🎵"
)

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    ready: Boolean,
    generating: Boolean,
    attachment: String?,
    preparingAttachment: Boolean,
    onSubmit: () -> Unit,
    onStop: () -> Unit,
    onAttach: (AttachSource) -> Unit,
    onRemoveAttachment: () -> Unit
) {
    var trayOpen by rememberSaveable { mutableStateOf(false) }
    val canSend = ready && !preparingAttachment && (value.isNotBlank() || attachment != null)
    val plusRotation by animateFloatAsState(if (trayOpen) 45f else 0f, label = "plus")

    Column(modifier = Modifier.padding(top = 6.dp).fillMaxWidth()) {
        AnimatedVisibility(
            visible = trayOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Panel)
                    .border(1.dp, Violet.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttachTile("📷", "Câmera", Modifier.weight(1f)) {
                        trayOpen = false
                        onAttach(AttachSource.CAMERA)
                    }
                    AttachTile("🖼️", "Galeria", Modifier.weight(1f)) {
                        trayOpen = false
                        onAttach(AttachSource.GALLERY)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("EMOJIS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                QuickEmojis.chunked(8).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { emoji ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onValueChange(value + emoji) }
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }

        if (attachment != null || preparingAttachment) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(PanelHigh)
                ) {
                    if (attachment != null) {
                        LocalImage(attachment, Modifier.fillMaxSize(), maxSide = 200)
                    } else {
                        CircularProgressIndicator(color = Neon, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (attachment != null) "Foto pronta para enviar" else "Preparando a foto…",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Pergunte algo sobre ela ou só envie", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                if (attachment != null) {
                    IconButton(onClick = onRemoveAttachment) {
                        Icon(Icons.Filled.Close, contentDescription = "Remover foto", tint = Muted)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Panel)
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(Violet.copy(alpha = 0.5f), Orchid.copy(alpha = 0.25f))),
                    RoundedCornerShape(28.dp)
                )
                .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (trayOpen) PanelHigh else Background.copy(alpha = 0.6f))
                    .clickable(enabled = !generating) { trayOpen = !trayOpen }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Foto e emojis",
                    tint = if (generating) Muted.copy(alpha = 0.4f) else Neon,
                    modifier = Modifier.graphicsLayer { rotationZ = plusRotation }
                )
            }

            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        when {
                            !ready -> "Aguardando a IA…"
                            attachment != null -> "Pergunte sobre a foto…"
                            else -> "Pergunte qualquer coisa…"
                        },
                        color = Muted
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend && !generating) onSubmit() }),
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
                val sendScale by animateFloatAsState(if (canSend) 1f else 0.9f, label = "send")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = sendScale
                            scaleY = sendScale
                        }
                        .clip(CircleShape)
                        .background(if (canSend) BrandBrushBright else Brush.linearGradient(listOf(PanelHigh, PanelHigh)))
                        .clickable(enabled = canSend) {
                            trayOpen = false
                            onSubmit()
                        }
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
}

@Composable
private fun AttachTile(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(PanelHigh, VioletDeep.copy(alpha = 0.35f))))
            .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                Text("Carregando o Qwen3-1.7B na memória…", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        "O Rud IA usa o Qwen3-1.7B (≈1,1 GB), leve o bastante para rodar bem no celular. " +
            "Ele fica salvo só neste app. Baixe uma única vez pelo Wi-Fi ou, se já tiver o arquivo .gguf, importe-o.",
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
private fun MessageBubble(message: ChatMessage, phase: GenerationPhase?, onOpenImage: (String) -> Unit) {
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
            val image = message.imagePath
            if (image != null) {
                LocalImage(
                    image,
                    Modifier
                        .widthIn(max = 240.dp)
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Violet.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onOpenImage(image) },
                    contentScale = ContentScale.Fit
                )
                if (message.text.isNotBlank()) Spacer(Modifier.height(6.dp))
            }

            val thought = message.thinking
            if (!isUser && !thought.isNullOrBlank()) {
                ThinkingCard(thought, live = message.status == MessageStatus.PENDING && message.text.isBlank())
                Spacer(Modifier.height(6.dp))
            }

            val shape = if (isUser) {
                RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
            } else {
                RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
            }
            if (!isUser || message.text.isNotBlank()) {
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
                    if (message.status == MessageStatus.PENDING && message.text.isBlank() && !message.thinking.isNullOrBlank()) {
                        Text("Formulando a resposta…", color = Muted, fontSize = 13.sp)
                    } else if (message.status == MessageStatus.PENDING && message.text.isBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TypingDots(color = Neon, dot = 7.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (phase == GenerationPhase.READING_IMAGE) "Olhando a foto… 👀" else "Pensando…",
                                    color = Muted,
                                    fontSize = 13.sp
                                )
                                if (phase == GenerationPhase.READING_IMAGE) {
                                    Text("Pode levar até 1 minuto", color = Muted.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        SelectionContainer {
                            Text(
                                text = if (isUser || isError) AnnotatedString(message.text) else richText(message.text),
                                color = if (isError) ErrorRed else TextWhite,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
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

/**
 * Mostra negrito (**texto**), itálico (*texto*), `código`, títulos (#) e
 * listas (- item) que a IA escreve, em vez dos asteriscos crus.
 */
private fun richText(raw: String): AnnotatedString = buildAnnotatedString {
    val lines = raw.split('\n')
    var inCode = false
    lines.forEachIndexed { index, original ->
        val line = original.trimEnd()
        if (line.trimStart().startsWith("```")) {
            inCode = !inCode
            return@forEachIndexed
        }
        if (inCode) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBg, fontSize = 13.sp)) {
                append(line)
            }
        } else {
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("#") -> {
                    val title = trimmed.trimStart('#').trim()
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Neon)) {
                        appendInline(title)
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    withStyle(SpanStyle(color = Neon, fontWeight = FontWeight.Bold)) { append("•  ") }
                    appendInline(trimmed.substring(2))
                }
                else -> appendInline(line)
            }
        }
        if (index < lines.lastIndex) append('\n')
    }
}

private fun AnnotatedString.Builder.appendInline(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    i += 2 // marcador ainda sem par (resposta sendo escrita): esconde
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBg, fontSize = 13.sp)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append('`')
                    i++
                }
            }
            text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i + 1 && text[end - 1] != ' ') {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append('*')
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

/** Foto salva no app, carregada fora da thread principal. */
@Composable
private fun LocalImage(
    path: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 720,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageTools.loadPreview(File(path), maxSide)?.asImageBitmap() }.getOrNull()
        }
    }
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = "Foto enviada", contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.size(160.dp).background(PanelHigh), contentAlignment = Alignment.Center) {
            Text("🖼️", fontSize = 28.sp)
        }
    }
}

/** Foto em tela cheia. Toque para fechar. */
@Composable
private fun ImageViewer(path: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDeep.copy(alpha = 0.96f))
                .clickable(onClick = onClose)
        ) {
            LocalImage(path, Modifier.fillMaxWidth(), maxSide = 1600, contentScale = ContentScale.Fit)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PanelHigh)
                    .clickable(onClick = onClose)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = TextWhite)
            }
        }
    }
}

/** Janela para instalar o leitor de imagens. */
@Composable
private fun VisionDialog(
    status: VisionStatus,
    partialBytes: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelHigh,
        title = { Text("📷 Leitor de imagens", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                when (status) {
                    is VisionStatus.Downloading -> {
                        Text("Baixando… pode fechar esta janela, o download continua.", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (status.total > 0) (status.done.toFloat() / status.total).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Neon,
                            trackColor = Background
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${gb(status.done)} de ${gb(status.total)} · ${percent(status.done, status.total)}",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    VisionStatus.Ready -> Text("Pronto! Já pode mandar fotos ✅", color = TextWhite)
                    VisionStatus.Checking -> Text("Verificando…", color = Muted)
                    else -> {
                        Text(
                            "Para ver fotos, a Rud IA precisa de um leitor de imagens extra " +
                                "(${gb(LocalModelEngine.VISION_TOTAL_BYTES)}). Ele é baixado uma única vez e depois " +
                                "funciona sem internet. Cada foto leva de 20 a 60 segundos para ser analisada.",
                            color = Muted,
                            fontSize = 13.sp
                        )
                        if (status is VisionStatus.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text(status.message, color = ErrorRed, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (status) {
                is VisionStatus.Downloading -> TextButton(onClick = onDismiss) { Text("Fechar", color = Neon) }
                VisionStatus.Missing, is VisionStatus.Error -> TextButton(onClick = onDownload) {
                    Text(
                        if (partialBytes > 0) "Continuar download" else "Baixar agora",
                        color = Neon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                else -> TextButton(onClick = onDismiss) { Text("OK", color = Neon) }
            }
        },
        dismissButton = {
            when (status) {
                is VisionStatus.Downloading -> TextButton(onClick = onCancel) { Text("Cancelar", color = ErrorRed) }
                VisionStatus.Missing, is VisionStatus.Error -> TextButton(onClick = onDismiss) { Text("Agora não", color = Muted) }
                else -> Unit
            }
        }
    )
}

/** Caixa recolhível com o que a IA pensou antes de responder. */
@Composable
private fun ThinkingCard(text: String, live: Boolean) {
    var open by remember { mutableStateOf(false) }
    val expanded = open || live
    Column(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VioletDeep.copy(alpha = 0.18f))
            .border(1.dp, Violet.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(enabled = !live) { open = !open }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💭", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                if (live) "Pensando…" else "Raciocínio da IA",
                color = Neon,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (live) {
                Spacer(Modifier.width(8.dp))
                TypingDots(color = Neon, dot = 5.dp)
            } else {
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "▲ ocultar" else "▼ ver detalhes", color = Muted, fontSize = 11.sp)
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(
                // Durante a geração mostra só o fim, para não empurrar a tela.
                if (live && text.length > 600) "…" + text.takeLast(600) else text,
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun HistoryDrawer(
    state: UiState,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteLegacy: () -> Unit,
    onVision: () -> Unit,
    onToggleThinking: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
    var confirmLegacy by remember { mutableStateOf(false) }

    if (confirmLegacy) {
        AlertDialog(
            onDismissRequest = { confirmLegacy = false },
            containerColor = PanelHigh,
            title = { Text("Apagar a IA antiga?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "A versão antiga (Qwen3-4B) era pesada demais para este celular e não é mais usada. " +
                        "Apagar libera ${gb(state.legacyModelBytes)}. Suas conversas não serão apagadas.",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmLegacy = false; onDeleteLegacy() }) {
                    Text("Apagar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLegacy = false }) { Text("Cancelar", color = Neon) }
            }
        )
    }

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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
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

        Spacer(Modifier.height(10.dp))
        val (visionTitle, visionSub) = when (val v = state.vision) {
            VisionStatus.Ready -> "Leitor de imagens" to "Instalado ✅ · a IA vê suas fotos"
            is VisionStatus.Downloading -> "Baixando leitor de imagens" to "${percent(v.done, v.total)} · toque para ver"
            is VisionStatus.Error -> "Leitor de imagens" to "Falhou · toque para tentar de novo"
            VisionStatus.Checking -> "Leitor de imagens" to "Verificando…"
            VisionStatus.Missing -> "Ensinar a IA a ver fotos" to "Baixar leitor de imagens (${gb(LocalModelEngine.VISION_TOTAL_BYTES)})"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(PanelHigh, VioletDeep.copy(alpha = 0.3f))))
                .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable(onClick = onVision)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("📷", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(visionTitle, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(visionSub, color = Muted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable { onToggleThinking(!state.showThinking) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("💭", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Mostrar raciocínio", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Respostas melhores, porém mais lentas", color = Muted, fontSize = 11.sp)
            }
            androidx.compose.material3.Switch(
                checked = state.showThinking,
                onCheckedChange = onToggleThinking,
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Violet)
            )
        }

        if (state.legacyModelBytes > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { confirmLegacy = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Neon)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Liberar espaço", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("IA antiga ocupa ${gb(state.legacyModelBytes)}", color = Muted, fontSize = 11.sp)
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
