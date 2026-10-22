package org.wonderloop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MarkCoral = Color(0xFFFF6B6B)
private val MarkViolet = Color(0xFFA77BCA)
private val MarkTeal = Color(0xFF1FBE9C)

/** Lobes made a bit rounder than a pure lemniscate so it reads as a friendly ∞ (same as the app icon). */
private const val LOBE_ROUNDNESS = 1.45f
private const val Y_EXTENT = 0.3536f * LOBE_ROUNDNESS

/**
 * The WonderLoop infinity mark: one lemniscate ribbon (coral → violet → teal) whose strands cross
 * over-under. The same geometry generates the launcher icon and the Fire TV banner.
 * [gapColor] is painted around the upper strand at the crossing, so pass the background behind it.
 * Its natural proportion is about 1.7:1 (width:height).
 */
@Composable
fun WonderLoopMark(modifier: Modifier, gapColor: Color = Color(0xFFFFFBF5)) {
    Canvas(modifier = modifier) {
        // Ribbon is 15 % of the mark's total width; fit that total width in both dimensions
        val markWidth = minOf(size.width, size.height / (0.85f * Y_EXTENT + 0.15f))
        val stroke = markWidth * 0.15f
        val halfWidth = markWidth * 0.425f
        val center = Offset(size.width / 2f, size.height / 2f)

        fun point(t: Float): Offset {
            val d = 1f + sin(t) * sin(t)
            return Offset(center.x + cos(t) / d * halfWidth, center.y + sin(t) * cos(t) / d * LOBE_ROUNDNESS * halfWidth)
        }
        fun curve(from: Float, to: Float, steps: Int, close: Boolean = false) = Path().apply {
            for (i in 0..steps) {
                val p = point(from + (to - from) * i / steps)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            if (close) close()
        }

        val brush = Brush.horizontalGradient(
            listOf(MarkCoral, MarkViolet, MarkTeal),
            startX = center.x - halfWidth - stroke / 2f,
            endX = center.x + halfWidth + stroke / 2f
        )
        val crossing = (PI / 2).toFloat()
        drawPath(curve(0f, (2 * PI).toFloat(), 180, close = true), brush, style = Stroke(width = stroke, join = StrokeJoin.Round))
        // Over-under: clear a gap around the upper strand, then redraw it a little longer so its ends leave no seam
        drawPath(curve(crossing - 0.36f, crossing + 0.36f, 30), gapColor, style = Stroke(width = stroke * 1.55f, cap = StrokeCap.Butt))
        drawPath(curve(crossing - 0.5f, crossing + 0.5f, 40), brush, style = Stroke(width = stroke, cap = StrokeCap.Butt))
    }
}
