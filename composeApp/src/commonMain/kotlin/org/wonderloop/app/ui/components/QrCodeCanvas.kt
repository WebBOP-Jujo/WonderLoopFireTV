package org.wonderloop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Multiplatform contract for generating a 2D boolean QR code matrix.
 */
expect fun generateQrMatrix(content: String): Array<BooleanArray>

/**
 * High-performance, crisp vector QR Code renderer for 1080p and 4K TV displays.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF2B253A),
    lightColor: Color = Color.White
) {
    val matrix = remember(content) {
        if (content.isBlank()) emptyArray() else generateQrMatrix(content)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(lightColor)
            .padding(14.dp)
            .aspectRatio(1f)
    ) {
        if (matrix.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val numRows = matrix.size
                val numCols = matrix[0].size
                val cellWidth = size.width / numCols.toFloat()
                val cellHeight = size.height / numRows.toFloat()

                for (r in 0 until numRows) {
                    for (c in 0 until numCols) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = darkColor,
                                topLeft = Offset(c * cellWidth, r * cellHeight),
                                size = Size(cellWidth + 0.5f, cellHeight + 0.5f) // +0.5f prevents subpixel hairline gaps
                            )
                        }
                    }
                }
            }
        }
    }
}
