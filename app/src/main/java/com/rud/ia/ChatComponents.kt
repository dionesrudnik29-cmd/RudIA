package com.rud.ia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal val PortugueseLocale = Locale("pt", "BR")

@Composable
fun ChatHeader(
    statusText: String,
    statusColor: Color,
    newChatEnabled: Boolean,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .violetGlow(shape, 10.dp)
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, Violet.copy(alpha = 0.55f), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            MenuIcon(VioletBright)
        }

        SparkleLogo(modifier = Modifier.size(40.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = "Rud IA",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        FilledIconButton(
            onClick = onNewChatClick,
            enabled = newChatEnabled,
            modifier = Modifier.violetGlow(CircleShape, 10.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Violet,
                contentColor = Color.White
            )
        ) {
            PlusIcon(Color.White)
        }
    }
}

@Composable
fun ModelBanner(
    modelState: ModelRuntimeState,
    downloadProgress: DownloadProgress?,
    downloadError: String?,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    AnimatedVisibility(
        visible = downloadProgress != null || modelState != ModelRuntimeState.READY,
        enter = fadeIn(tween(400)) + expandVertically(tween(400)),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .violetGlow(shape, 8.dp)
                .clip(shape)
                .background(CardSurface)
                .border(1.dp, Violet.copy(alpha = 0.45f), shape)
                .padding(16.dp)
        ) {
            when {
                downloadProgress != null -> DownloadDetails(downloadProgress)

                modelState == ModelRuntimeState.LOADING -> {
                    Text(
                        text = "Carregando a IA na memória...",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Leva alguns segundos e acontece sozinho.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Violet,
                        trackColor = CardBorder
                    )
                }

                else -> {
                    val isError = modelState == ModelRuntimeState.ERROR
                    Text(
                        text = if (isError) {
                            "Não foi possível carregar a IA"
                        } else {
                            "A IA ainda não está no celular"
                        },
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isError -> "Feche outros apps e tente de novo."
                            downloadError != null -> downloadError
                            else -> "Baixe uma vez para usar sem internet."
                        },
                        color = if (downloadError != null && !isError) StatusError else Muted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = if (isError) onRetryClick else onDownloadClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        Text(if (isError) "Tentar de novo" else "Baixar IA")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadDetails(progress: DownloadProgress) {
    val hasTotal = progress.totalBytes > 0L
    val percent = if (hasTotal) {
        (progress.downloadedBytes * 100L / progress.totalBytes).toInt().coerceIn(0, 100)
    } else {
        0
    }

    Text(
        text = "Baixando a IA...",
        color = TextWhite,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (hasTotal) {
            "${formatBytes(progress.downloadedBytes)} de ${formatBytes(progress.totalBytes)}"
        } else {
            "${formatBytes(progress.downloadedBytes)} baixados"
        },
        color = TextWhite,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "${formatSpeed(progress.bytesPerSecond)} · $percent%",
        color = VioletBright,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(12.dp))
    if (hasTotal) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = Violet,
            trackColor = CardBorder
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Violet,
            trackColor = CardBorder
        )
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    ready: Boolean,
    busy: Boolean,
    onSend: () -> Unit
) {
    val canType = ready && !busy
    val canSend = canType && value.isNotBlank()
    val placeholder = when {
        !ready -> "Aguardando a IA..."
        busy -> "Aguardando resposta..."
        else -> "Digite sua mensagem..."
    }
    val shape = RoundedCornerShape(32.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .violetGlow(shape, 10.dp)
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, Violet.copy(alpha = 0.55f), shape)
            .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Muted,
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = canType,
                textStyle = TextStyle(color = TextWhite, fontSize = 15.sp),
                cursorBrush = SolidColor(Violet),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (canSend) onSend()
                }),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(48.dp)
                .violetGlow(CircleShape, 12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Violet,
                contentColor = Color.White
            )
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                SendIcon(Color.White)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
    ) {
        val shape = RoundedCornerShape(18.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .violetGlow(shape, if (message.isUser) 8.dp else 2.dp)
                    .clip(shape)
                    .background(if (message.isUser) UserBubble else CardSurface)
                    .border(1.dp, Violet.copy(alpha = 0.4f), shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(PortugueseLocale, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(PortugueseLocale, "%.0f MB", bytes / 1_000_000.0)
    else -> String.format(PortugueseLocale, "%.0f KB", bytes / 1_000.0)
}

private fun formatSpeed(bytesPerSecond: Long): String = when {
    bytesPerSecond >= 1_000_000L -> String.format(PortugueseLocale, "%.1f MB/s", bytesPerSecond / 1_000_000.0)
    else -> String.format(PortugueseLocale, "%.0f KB/s", bytesPerSecond / 1_000.0)
}
