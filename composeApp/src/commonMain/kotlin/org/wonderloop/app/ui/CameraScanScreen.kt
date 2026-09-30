package org.wonderloop.app.ui

import androidx.compose.runtime.Composable

@Composable
expect fun CameraScanScreen(
    initialArtStyle: String = "watercolor",
    onPhotoCaptured: (imageBase64: String, artStyle: String) -> Unit,
    onBack: () -> Unit
)
