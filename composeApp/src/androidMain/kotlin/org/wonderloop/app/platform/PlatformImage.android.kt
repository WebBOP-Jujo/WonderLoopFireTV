package org.wonderloop.app.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wonderloop.app.theme.SunSecondary
import org.wonderloop.app.theme.TextMuted

private fun calculateInSampleSize(outWidth: Int, outHeight: Int, maxDim: Int = 1024): Int {
    var inSampleSize = 1
    if (outWidth > maxDim || outHeight > maxDim) {
        while (outWidth / (inSampleSize * 2) >= maxDim || outHeight / (inSampleSize * 2) >= maxDim) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun decodeOptimizedFile(path: String, maxDim: Int = 1024): Bitmap? {
    val boundsOpts = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, boundsOpts)
    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

    val isOpaqueJpeg = boundsOpts.outMimeType.equals("image/jpeg", ignoreCase = true) ||
        path.endsWith(".jpg", ignoreCase = true) ||
        path.endsWith(".jpeg", ignoreCase = true)

    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
        inPreferredConfig = if (isOpaqueJpeg) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(path, decodeOpts)
}

private fun decodeOptimizedByteArray(bytes: ByteArray, maxDim: Int = 1024): Bitmap? {
    val boundsOpts = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

    val isOpaqueJpeg = boundsOpts.outMimeType.equals("image/jpeg", ignoreCase = true)

    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
        inPreferredConfig = if (isOpaqueJpeg) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
}

@Composable
actual fun WonderLoopImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    if (model.isNullOrBlank()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(SunSecondary.copy(alpha = 0.2f))
        ) {
            Text(text = "🎨 Ilustración mágica", fontSize = 16.sp, color = TextMuted)
        }
        return
    }

    val isLocalFile = model.startsWith("file://") || model.startsWith("/")
    val isBase64 = !isLocalFile && (model.startsWith("data:image") || model.contains(";base64,"))

    if (isLocalFile || isBase64) {
        val bitmapState = produceState<ImageBitmap?>(initialValue = null, model) {
            value = withContext(Dispatchers.IO) {
                try {
                    if (isLocalFile) {
                        val path = model.removePrefix("file://")
                        decodeOptimizedFile(path)?.asImageBitmap()
                    } else {
                        val base64Data = if (model.contains(";base64,")) {
                            model.substringAfter(";base64,")
                        } else {
                            model
                        }
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        decodeOptimizedByteArray(bytes)?.asImageBitmap()
                    }
                } catch (e: Throwable) {
                    println("Error decoding image asynchronously in WonderLoopImage: $e")
                    null
                }
            }
        }
        val bitmap = bitmapState.value

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.background(SunSecondary.copy(alpha = 0.2f))
            ) {
                Text(text = "🎨 Ilustración mágica", fontSize = 16.sp, color = TextMuted)
            }
        }
    } else {
        // Network URL (e.g. http:// or https://)
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
