package org.wonderloop.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.wonderloop.app.data.WonderLoopApi

/**
 * On Amazon Fire TV, scanning drawings is powered exclusively by the Mobile QR Bridge (QrScanScreen).
 * CameraScanScreen delegates directly to QrScanScreen to ensure 100% Leanback D-Pad compatibility
 * and eliminate any unused CAMERA permission dependencies.
 */
@Composable
actual fun CameraScanScreen(
    initialArtStyle: String,
    onPhotoCaptured: (imageBase64: String, artStyle: String) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { WonderLoopApi() }
    QrScanScreen(
        initialArtStyle = initialArtStyle,
        api = api,
        onPhotoCaptured = onPhotoCaptured,
        onBack = onBack
    )
}
