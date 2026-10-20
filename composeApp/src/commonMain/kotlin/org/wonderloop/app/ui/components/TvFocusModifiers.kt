package org.wonderloop.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

val TvFocusGold = Color(0xFFFFD166)
val TvFocusCoral = Color(0xFFD9384A)

/**
 * 10-Foot UI D-Pad focus & click modifier for Amazon Fire TV remote controls.
 * Highlights the focused element with a bright 3.5dp golden/coral border,
 * smooth scale zoom (1.04x), optional warm focus background, and responds to DPAD_CENTER / ENTER / Click.
 */
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    focusedScale: Float = 1.04f,
    focusBorderColor: Color = TvFocusGold,
    focusBorderWidth: Dp = 3.5.dp,
    focusedContainerColor: Color? = null,
    focusedElevation: Dp = 8.dp,
    focusRequester: FocusRequester? = null,
    playClickSound: Boolean = true,
    onFocusChangedCallback: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) focusedScale else 1f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "tv_focus_scale"
    )

    val baseModifier = if (focusRequester != null) {
        this.focusRequester(focusRequester)
    } else {
        this
    }

    baseModifier
        .scale(scale)
        .then(
            if (isFocused && enabled) {
                var m: Modifier = Modifier
                if (focusedContainerColor != null) {
                    m = m.background(focusedContainerColor, shape)
                }
                if (focusedElevation > 0.dp) {
                    m = m.shadow(elevation = focusedElevation, shape = shape, clip = false)
                }
                m.border(width = focusBorderWidth, color = focusBorderColor, shape = shape)
            } else {
                Modifier
            }
        )
        .onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (isFocused != nowFocused) {
                isFocused = nowFocused
                onFocusChangedCallback?.invoke(nowFocused)
            }
        }
        .onKeyEvent { event ->
            if (!enabled) return@onKeyEvent false
            val isSelectKey = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter ||
                event.key == Key.Spacebar
            if (isSelectKey) {
                if (event.type == KeyEventType.KeyUp) {
                    if (playClickSound) {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                    }
                    onClick()
                }
                true
            } else {
                false
            }
        }
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) {
            if (playClickSound) {
                PlatformAudioService.playSfx(SoundEffect.POP)
            }
            onClick()
        }
}

/**
 * Perfectly symmetrical, pixel-crisp geometric D-Pad arrows (◄ ►)
 * rendered using Canvas so both sides are identical in all Android system fonts.
 */
@Composable
fun DPadArrows(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2B2340),
    width: Dp = 16.dp,
    height: Dp = 10.dp
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val triWidth = (w - 4.dp.toPx()) / 2f

        // Left arrow pointing Left ◄
        val leftPath = Path().apply {
            moveTo(0f, h / 2f)
            lineTo(triWidth, 0f)
            lineTo(triWidth, h)
            close()
        }
        drawPath(leftPath, color = color)

        // Right arrow pointing Right ►
        val rightX = w - triWidth
        val rightPath = Path().apply {
            moveTo(w, h / 2f)
            lineTo(rightX, 0f)
            lineTo(rightX, h)
            close()
        }
        drawPath(rightPath, color = color)
    }
}

/**
 * Minimalist, sleek outlined lock icon matching Fire TV HTML design (#lk symbol).
 * Replaces the heavy yellow emoji with a clean SVG-style stroke.
 */
@Composable
fun MinimalistLockIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF2B2340),
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.2.dp.toPx()

        // Lock shackle (curved arch)
        val shackleLeft = w * 0.30f
        val shackleRight = w * 0.70f
        val shackleTop = h * 0.12f
        val shackleBottom = h * 0.48f

        val shacklePath = Path().apply {
            moveTo(shackleLeft, shackleBottom)
            lineTo(shackleLeft, shackleTop + (shackleRight - shackleLeft) / 2f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = shackleLeft,
                    top = shackleTop,
                    right = shackleRight,
                    bottom = shackleTop + (shackleRight - shackleLeft)
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(shackleRight, shackleBottom)
        }
        drawPath(
            shacklePath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lock body (rounded rectangle)
        val bodyLeft = w * 0.18f
        val bodyTop = h * 0.44f
        val bodyWidth = w * 0.64f
        val bodyHeight = h * 0.46f
        val cornerRadius = 3.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Keyhole dot
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.5f, bodyTop + bodyHeight * 0.44f)
        )
    }
}

