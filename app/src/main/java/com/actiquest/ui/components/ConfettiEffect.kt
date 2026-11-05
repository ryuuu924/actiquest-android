package com.actiquest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.actiquest.ui.theme.*
import kotlin.random.Random

private data class Particle(
    val initialX: Float,
    val initialY: Float,
    val targetX: Float,
    val targetY: Float,
    val size: Float,
    val color: Color,
    val rotation: Float
)

@Composable
fun ConfettiBurst(
    trigger: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    val particles = remember {
        val colors = listOf(MintEmerald, CobaltBlue, ElectricBlue, StatusPending, VioletAccent, RoyalIndigo, Color.White)
        List(45) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val distance = Random.nextDouble(120.0, 420.0)
            val targetX = (Math.cos(angle) * distance).toFloat()
            val targetY = (Math.sin(angle) * distance).toFloat() + 100f
            Particle(
                initialX = 0f,
                initialY = 0f,
                targetX = targetX,
                targetY = targetY,
                size = Random.nextFloat() * 12f + 6f,
                color = colors.random(),
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 3

            particles.forEach { p ->
                val currentX = centerX + p.targetX * progress.value
                val currentY = centerY + p.targetY * progress.value + (progress.value * progress.value * 250f)
                val alpha = (1f - progress.value).coerceIn(0f, 1f)

                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(currentX, currentY),
                    size = Size(p.size, p.size * 0.7f)
                )
            }
        }
    }
}
