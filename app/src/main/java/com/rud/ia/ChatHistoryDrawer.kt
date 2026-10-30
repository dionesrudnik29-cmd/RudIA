package com.rud.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

private const val DAY_MILLIS = 86_400_000.0

@Composable
fun ChatHistoryDrawer(
    conversations: List<Conversation>,
    activeId: String,
    status: String,
    historyError: String?,
    canRefresh: Boolean,
    onNewChat: () -> Unit,
    onOpen: (Conversation) -> Unit,
    onRefresh: () -> Unit
) {
    val now = System.currentTimeMillis()
    val groups = conversations
        .sortedByDescending { it.updatedAt }
        .groupBy { dayLabel(it.updatedAt, now) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = "Conversas",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Text("Nova conversa")
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (groups.isEmpty()) {
            Text(
                text = "Suas conversas aparecem aqui e ficam salvas neste celular.",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            groups.forEach { (label, group) ->
                item(key = "header-$label") {
                    Text(
                        text = label,
                        color = Lavender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
                items(group, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        active = conversation.id == activeId,
                        onClick = { onOpen(conversation) }
                    )
                }
            }
        }

        ModelStatusFooter(
            status = status,
            historyError = historyError,
            canRefresh = canRefresh,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(if (active) UserBubble else Color.Transparent)
            .border(1.dp, if (active) Violet.copy(alpha = 0.6f) else CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = conversation.title,
            color = TextWhite,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timeLabel(conversation.updatedAt),
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ModelStatusFooter(
    status: String,
    historyError: String?,
    canRefresh: Boolean,
    onRefresh: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, CardBorder, shape)
            .padding(16.dp)
    ) {
        Text(
            text = "Modelo da IA",
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = status.ifBlank { "Verificando..." },
            color = Muted,
            fontSize = 13.sp
        )
        if (historyError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = historyError,
                color = StatusError,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
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

private val ptBr = PortugueseLocale

private fun dayLabel(timestamp: Long, now: Long): String {
    val daysAgo = ((startOfDay(now) - startOfDay(timestamp)) / DAY_MILLIS).roundToInt()
    return when (daysAgo) {
        0 -> "Hoje"
        1 -> "Ontem"
        else -> SimpleDateFormat("dd/MM/yyyy", ptBr).format(Date(timestamp))
    }
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("HH:mm", ptBr).format(Date(timestamp))

private fun startOfDay(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
