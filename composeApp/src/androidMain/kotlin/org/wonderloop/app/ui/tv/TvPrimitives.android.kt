package org.wonderloop.app.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect

@Composable
actual fun TvClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
    shape: Shape,
    containerColor: Color,
    focusedContainerColor: Color,
    contentColor: Color,
    borderColor: Color,
    focusedBorderColor: Color,
    focusedScale: Float,
    glowColor: Color,
    contentDescription: String?,
    playClickSound: Boolean,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = {
            if (playClickSound) PlatformAudioService.playSfx(SoundEffect.POP)
            onClick()
        },
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = contentColor,
            pressedContainerColor = focusedContainerColor,
            pressedContentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        border = ClickableSurfaceDefaults.border(
            border = if (borderColor == Color.Transparent) Border.None else Border(BorderStroke(1.5.dp, borderColor), shape = shape),
            focusedBorder = Border(BorderStroke(3.5.dp, focusedBorderColor), shape = shape)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = if (glowColor == Color.Transparent) Glow.None else Glow(elevationColor = glowColor, elevation = 12.dp)
        )
    ) {
        content(focused)
    }
}

@Composable
actual fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
