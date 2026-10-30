package com.rud.ia

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-200f) }
    val titleReveal = remember { Animatable(0f) }

    val orbit = rememberInfiniteTransition(label = "splashOrbit")
    val orbitAngle by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "orbitAngle"
    )

    LaunchedEffect(Unit) {
        launch {
            logoRotation.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                1f,
                tween(900, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
            )
        }
        delay(600)
        titleReveal.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(1000)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val orbitRadius = size.minDimension / 2f * 0.85f
                repeat(3) { index ->
                    val angle = Math.toRadians((orbitAngle + index * 120f).toDouble())
                    val dot = Offset(
                        center.x + orbitRadius * cos(angle).toFloat(),
                        center.y + orbitRadius * sin(angle).toFloat()
                    )
                    drawCircle(
                        color = Lavender.copy(alpha = 0.4f + 0.2f * index),
                        radius = 5.dp.toPx(),
                        center = dot
                    )
                }
            }

            SparkleLogo(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation.value
                    }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Rud IA",
            color = TextWhite,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                alpha = titleReveal.value
                translationY = (1f - titleReveal.value) * 24.dp.toPx()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sua IA no seu celular",
            color = Muted,
            fontSize = 15.sp,
            modifier = Modifier.graphicsLayer {
                alpha = titleReveal.value
                translationY = (1f - titleReveal.value) * 24.dp.toPx()
            }
        )
    }
}
