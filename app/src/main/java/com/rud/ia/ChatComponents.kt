package com.rud.ia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatHeader(
    statusText: String,
    statusColor: Color,
    newChatEnabled: Boolean,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            MenuIcon(Lavender)
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
    downloading: Boolean,
    progress: Int,
    downloadError: String?,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    AnimatedVisibility(
        visible = downloading || modelState != ModelRuntimeState.READY,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (title, subtitle) = when {
            downloading -> "Baixando a IA..." to "$progress% concluído. Mantenha a conexão ativa."
            modelState == ModelRuntimeState.LOADING ->
                "Carregando a IA na memória..." to "Leva alguns segundos e acontece sozinho."
            modelState == ModelRuntimeState.ERROR ->
                "Não foi possível carregar a IA" to "Feche outros apps e tente de novo."
            else ->
                "A IA ainda não está no celular" to
                    (downloadError ?: "Baixe uma vez para usar sem internet.")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Muted,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                downloading -> LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Violet,
                    trackColor = CardBorder
                )

                modelState == ModelRuntimeState.LOADING -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Violet,
                    trackColor = CardBorder
                )

                else -> Button(
                    onClick = if (modelState == ModelRuntimeState.ERROR) {
                        onRetryClick
                    } else {
                        onDownloadClick
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) {
                    Text(
                        if (modelState == ModelRuntimeState.ERROR) {
                            "Tentar de novo"
                        } else {
                            "Baixar IA"
                        }
                    )
                }
            }
        }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(32.dp))
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
            modifier = Modifier.size(48.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (message.isUser) UserBubble else CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInfoSheet(
    status: String,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        contentColor = TextWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sobre a Rud IA",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = "A IA roda direto no seu celular. Depois de baixada, você pode conversar sem internet.",
                fontSize = 14.sp,
                color = Muted
            )
            if (status.isNotBlank()) {
                Text(
                    text = status,
                    fontSize = 14.sp,
                    color = TextWhite
                )
            }
            Button(
                onClick = onRefresh,
                enabled = canRefresh,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) {
                Text("Atualizar status")
            }
        }
    }
}

@Composable
fun SparkleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Violet.copy(alpha = 0.55f), Color.Transparent),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Lavender, VioletDeep),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            radius = r * 0.62f,
            center = c
        )
        drawPath(sparklePath(c.x, c.y, r * 0.36f), Color.White)
        drawPath(sparklePath(c.x + r * 0.32f, c.y - r * 0.32f, r * 0.12f), Color.White)
    }
}

private fun sparklePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    moveTo(cx, cy - r)
    quadraticBezierTo(cx, cy, cx + r, cy)
    quadraticBezierTo(cx, cy, cx, cy + r)
    quadraticBezierTo(cx, cy, cx - r, cy)
    quadraticBezierTo(cx, cy, cx, cy - r)
    close()
}

@Composable
private fun MenuIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        listOf(0.25f, 0.5f, 0.75f).forEach { f ->
            drawLine(
                color = tint,
                start = Offset(0f, size.height * f),
                end = Offset(size.width, size.height * f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlusIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SendIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val tip = Offset(w * 0.9f, h * 0.5f)
        drawLine(tint, Offset(w * 0.1f, h * 0.5f), tip, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.55f, h * 0.15f), tip, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.55f, h * 0.85f), tip, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
