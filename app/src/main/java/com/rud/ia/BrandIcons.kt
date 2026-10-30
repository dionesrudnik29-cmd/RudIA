package com.rud.ia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun SparkleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Violet.copy(alpha = 0.7f), Color.Transparent),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(VioletBright, VioletDeep),
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

@Composable
fun SpinningOrbit(rotation: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f - 4.dp.toPx()
        drawCircle(
            color = Violet.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        )
        rotate(degrees = rotation, pivot = center) {
            drawCircle(
                color = VioletBright,
                radius = 4.dp.toPx(),
                center = Offset(center.x, center.y - radius)
            )
            drawCircle(
                color = Lavender,
                radius = 2.5.dp.toPx(),
                center = Offset(center.x - radius, center.y)
            )
        }
    }
}

@Composable
fun MenuIcon(tint: Color) {
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
fun PlusIcon(tint: Color) {
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
fun SendIcon(tint: Color) {
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

private fun sparklePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    moveTo(cx, cy - r)
    quadraticBezierTo(cx, cy, cx + r, cy)
    quadraticBezierTo(cx, cy, cx, cy + r)
    quadraticBezierTo(cx, cy, cx - r, cy)
    quadraticBezierTo(cx, cy, cx, cy - r)
    close()
}
