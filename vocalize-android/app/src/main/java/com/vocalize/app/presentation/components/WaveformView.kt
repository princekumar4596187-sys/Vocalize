package com.vocalize.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vocalize.app.presentation.theme.VocalizeRed

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = VocalizeRed,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    minBarHeight: Float = 0.05f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_anim")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2π
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "idle_phase"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidthPx = barWidth.toPx()
        val spacingPx = barSpacing.toPx()
        val totalBarWidth = barWidthPx + spacingPx
        val maxBars = (canvasWidth / totalBarWidth).toInt().coerceAtLeast(1)
        val centerY = canvasHeight / 2f

        if (isRecording && amplitudes.isNotEmpty()) {
            val display = amplitudes.takeLast(maxBars)
            val padded = if (display.size < maxBars) {
                List(maxBars - display.size) { minBarHeight } + display
            } else display

            padded.forEachIndexed { index, amplitude ->
                val x = index * totalBarWidth + barWidthPx / 2f
                val heightFraction = (amplitude.coerceIn(minBarHeight, 1f))
                val halfHeight = (heightFraction * canvasHeight / 2f).coerceAtLeast(4f)

                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barColor.copy(alpha = 0.4f),
                            barColor,
                            barColor.copy(alpha = 0.4f)
                        ),
                        startY = centerY - halfHeight,
                        endY = centerY + halfHeight
                    ),
                    start = Offset(x, centerY - halfHeight),
                    end = Offset(x, centerY + halfHeight),
                    strokeWidth = barWidthPx,
                    cap = StrokeCap.Round
                )
            }
        } else {
            // Idle animation — sine wave
            repeat(maxBars) { index ->
                val x = index * totalBarWidth + barWidthPx / 2f
                val wave = kotlin.math.sin(idlePhase + index * 0.4f).toFloat()
                val amp = if (isRecording) 0f else 0.12f + 0.08f * ((wave + 1f) / 2f)
                val halfHeight = (amp * canvasHeight / 2f).coerceAtLeast(4f)

                drawLine(
                    color = barColor.copy(alpha = 0.3f + 0.2f * ((wave + 1f) / 2f)),
                    start = Offset(x, centerY - halfHeight),
                    end = Offset(x, centerY + halfHeight),
                    strokeWidth = barWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
