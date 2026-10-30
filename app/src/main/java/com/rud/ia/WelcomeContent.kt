package com.rud.ia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Suggestion(val emoji: String, val text: String)

private val suggestions = listOf(
    Suggestion("✍️", "Escreva uma mensagem de aniversário"),
    Suggestion("💡", "Me dê ideias para ganhar dinheiro"),
    Suggestion("📚", "Explique um assunto de forma simples"),
    Suggestion("🍳", "Sugira uma receita rápida")
)

@Composable
fun WelcomeContent(
    ready: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val floating = rememberInfiniteTransition(label = "logoFloat")
    val floatY by floating.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "logoFloatY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SparkleLogo(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer { translationY = floatY.dp.toPx() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Olá! Como posso ajudar?",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Assim que a IA estiver pronta, é só conversar.",
            color = Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        suggestions.forEachIndexed { index, suggestion ->
            val delay = 200 + index * 110
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(450, delayMillis = delay)) +
                    slideInVertically(tween(450, delayMillis = delay)) { it / 3 }
            ) {
                SuggestionChip(
                    suggestion = suggestion,
                    enabled = ready,
                    onClick = { onSuggestionClick(suggestion.text) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: Suggestion,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, CardBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suggestion.emoji,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = suggestion.text,
            color = TextWhite,
            fontSize = 15.sp
        )
    }
}
