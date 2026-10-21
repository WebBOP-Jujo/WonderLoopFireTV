package org.wonderloop.app.ui.tv

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** WonderLoop focus language: warm gold ring + soft glow, shared by every focusable element. */
val TvFocusRing = Color(0xFFFFD166)
val TvInk = Color(0xFF2B253A)

/**
 * Focusable, clickable surface for the 10-foot UI.
 *
 * On Android/Fire OS it is backed by Compose for TV (`androidx.tv.material3.Surface`), which handles
 * D-pad focus, the OK/Enter key, long-press, focus scale, focus border and glow natively, so screens no
 * longer need hand-written key handling for clicks. The [content] lambda receives whether the surface is focused.
 */
@Composable
expect fun TvClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = Color.White,
    focusedContainerColor: Color = containerColor,
    contentColor: Color = TvInk,
    borderColor: Color = Color.Transparent,
    focusedBorderColor: Color = TvFocusRing,
    focusedScale: Float = 1.05f,
    glowColor: Color = Color.Transparent,
    contentDescription: String? = null,
    playClickSound: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
)

/** Keeps the TV screen on while composed (reading, narration, waiting for a story): no screensaver mid-story. */
@Composable
expect fun KeepScreenOn()
