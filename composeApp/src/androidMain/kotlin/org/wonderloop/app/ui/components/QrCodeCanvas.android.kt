package org.wonderloop.app.ui.components

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

actual fun generateQrMatrix(content: String): Array<BooleanArray> {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val result = Array(height) { BooleanArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                result[y][x] = bitMatrix.get(x, y)
            }
        }
        result
    } catch (e: Exception) {
        println("⚠️ [QrCodeCanvas] Failed to generate QR matrix: ${e.message}")
        emptyArray()
    }
}
