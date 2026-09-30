package org.wonderloop.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import org.wonderloop.app.theme.*
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    CoralPrimary,
    SunSecondary,
    MintAccent,
    Color(0xFF9B59B6),
    Color(0xFF3498DB),
    Color(0xFFFF7675),
    Color(0xFFFDCB6E)
)

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val swaySpeed: Float,
    val swayWidth: Float,
    val size: Float,
    val color: Color,
    val isCircle: Boolean,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 45,
    durationMillis: Int = 3200
) {
    val particles = remember {
        val random = Random(42)
        List(particleCount) {
            ConfettiParticle(
                initialX = random.nextFloat(),
                initialY = -random.nextFloat() * 0.4f,
                speedY = 0.6f + random.nextFloat() * 0.7f,
                swaySpeed = 2f + random.nextFloat() * 3f,
                swayWidth = 15f + random.nextFloat() * 25f,
                size = 8f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                isCircle = random.nextBoolean(),
                rotationSpeed = 120f + random.nextFloat() * 240f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val progress = (time * p.speedY) % 1.2f
            val y = (p.initialY + progress) * height
            val sway = sin((time * p.swaySpeed * 2 * Math.PI).toFloat()) * p.swayWidth
            val x = (p.initialX * width) + sway
            val rotation = (time * p.rotationSpeed) % 360f

            // Only draw while on screen
            if (y in -20f..height + 20f) {
                // Fade out towards the bottom
                val alpha = ((height - y) / (height * 0.35f)).coerceIn(0f, 1f)
                val drawColor = p.color.copy(alpha = alpha)

                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = drawColor,
                            radius = p.size / 2,
                            center = Offset(x, y)
                        )
                    } else {
                        drawRect(
                            color = drawColor,
                            topLeft = Offset(x - p.size / 2, y - p.size),
                            size = Size(p.size, p.size * 1.6f)
                        )
                    }
                }
            }
        }
    }
}
