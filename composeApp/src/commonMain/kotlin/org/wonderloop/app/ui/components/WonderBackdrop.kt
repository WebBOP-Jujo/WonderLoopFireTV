package org.wonderloop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

enum class BackdropMood {
    /** Children's screens: colour glows plus crayon sparkles and squiggles in the margins. */
    Playful,
    /** Grown-up screens: the same palette, quieter, no doodles. */
    Calm
}

/** Accent colours given to child profiles in order (avatar ring, card header, chips). */
val ProfileAccents = listOf(
    Color(0xFFE0485A), // coral
    Color(0xFF14A37F), // mint
    Color(0xFF7C5CE0)  // violet
)

fun profileAccent(index: Int): Color = ProfileAccents[((index % ProfileAccents.size) + ProfileAccents.size) % ProfileAccents.size]

private val BackdropCreamTop = Color(0xFFFFFBF5)
private val BackdropCreamBottom = Color(0xFFFFF3E6)
private val BackdropCalmBottom = Color(0xFFF6F3EE)

private data class Glow(val x: Float, val y: Float, val radius: Float, val color: Color)
private data class Sparkle(val x: Float, val y: Float, val size: Float, val color: Color)

private val PlayfulGlows = listOf(
    Glow(0.04f, 0.06f, 0.42f, Color(0xFFFF8A80).copy(alpha = 0.17f)),
    Glow(0.88f, 0.10f, 0.38f, Color(0xFFFFD166).copy(alpha = 0.24f)),
    Glow(0.94f, 0.96f, 0.44f, Color(0xFF6FD3B5).copy(alpha = 0.18f)),
    Glow(0.10f, 0.94f, 0.38f, Color(0xFFB9A6F5).copy(alpha = 0.16f))
)

private val CalmGlows = listOf(
    Glow(0.92f, 0.08f, 0.40f, Color(0xFF6FD3B5).copy(alpha = 0.12f)),
    Glow(0.06f, 0.92f, 0.40f, Color(0xFFB9A6F5).copy(alpha = 0.10f)),
    Glow(0.10f, 0.08f, 0.30f, Color(0xFFFFD166).copy(alpha = 0.12f))
)

// Kept to the screen margins so they never sit behind text
private val Sparkles = listOf(
    Sparkle(0.020f, 0.40f, 11f, Color(0xFFF2B630)),
    Sparkle(0.975f, 0.52f, 13f, Color(0xFFE0485A)),
    Sparkle(0.42f, 0.025f, 8f, Color(0xFF14A37F)),
    Sparkle(0.70f, 0.975f, 10f, Color(0xFF7C5CE0)),
    Sparkle(0.985f, 0.20f, 7f, Color(0xFF14A37F)),
    Sparkle(0.015f, 0.72f, 8f, Color(0xFFE0485A)),
    Sparkle(0.30f, 0.985f, 7f, Color(0xFFF2B630)),
    Sparkle(0.62f, 0.02f, 6f, Color(0xFFE0485A))
)

private val Dots = listOf(
    Sparkle(0.035f, 0.55f, 3f, Color(0xFF7C5CE0)),
    Sparkle(0.965f, 0.36f, 3.5f, Color(0xFFF2B630)),
    Sparkle(0.52f, 0.985f, 3f, Color(0xFF14A37F)),
    Sparkle(0.25f, 0.015f, 3f, Color(0xFFE0485A)),
    Sparkle(0.80f, 0.985f, 2.5f, Color(0xFFE0485A))
)

/**
 * The shared WonderLoop canvas: warm cream with soft colour glows in the corners and, on children's
 * screens, a few crayon sparkles and squiggles peeking from the margins (drawn once and cached).
 */
@Composable
fun WonderBackdrop(
    modifier: Modifier = Modifier,
    mood: BackdropMood = BackdropMood.Playful,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BackdropCreamTop, if (mood == BackdropMood.Calm) BackdropCalmBottom else BackdropCreamBottom)
                )
            )
            .drawWithCache {
                val w = size.width
                val h = size.height
                val unit = density
                val glows = if (mood == BackdropMood.Calm) CalmGlows else PlayfulGlows
                val glowBrushes = glows.map { g ->
                    Brush.radialGradient(
                        colors = listOf(g.color, Color.Transparent),
                        center = Offset(g.x * w, g.y * h),
                        radius = g.radius * maxOf(w, h)
                    )
                }

                val sparklePaths = if (mood == BackdropMood.Playful) {
                    Sparkles.map { s ->
                        val cx = s.x * w
                        val cy = s.y * h
                        val r = s.size * unit
                        Path().apply {
                            moveTo(cx, cy - r)
                            quadraticBezierTo(cx, cy, cx + r, cy)
                            quadraticBezierTo(cx, cy, cx, cy + r)
                            quadraticBezierTo(cx, cy, cx - r, cy)
                            quadraticBezierTo(cx, cy, cx, cy - r)
                            close()
                        } to s.color
                    }
                } else {
                    emptyList()
                }

                fun squiggle(startX: Float, startY: Float, length: Float, amplitude: Float): Path = Path().apply {
                    val steps = 40
                    moveTo(startX, startY)
                    for (i in 1..steps) {
                        val t = i / steps.toFloat()
                        lineTo(startX + t * length, startY + sin(t * 4 * PI).toFloat() * amplitude)
                    }
                }
                val squiggles = if (mood == BackdropMood.Playful) {
                    listOf(
                        // Both in the top overscan band, above any screen's header row
                        squiggle(0.02f * w, 0.02f * h, 0.11f * w, 5 * unit) to Color(0xFFE0485A),
                        squiggle(0.86f * w, 0.018f * h, 0.11f * w, 5 * unit) to Color(0xFF14A37F)
                    )
                } else {
                    emptyList()
                }
                val crayon = Stroke(width = 3.5f * unit, cap = StrokeCap.Round)

                onDrawBehind {
                    glowBrushes.forEach { drawRect(brush = it) }
                    if (mood == BackdropMood.Playful) {
                        squiggles.forEach { (path, color) -> drawPath(path, color.copy(alpha = 0.22f), style = crayon) }
                        sparklePaths.forEach { (path, color) -> drawPath(path, color.copy(alpha = 0.45f)) }
                        Dots.forEach { d ->
                            drawCircle(d.color.copy(alpha = 0.35f), radius = d.size * unit, center = Offset(d.x * w, d.y * h))
                        }
                    }
                }
            },
        content = content
    )
}
