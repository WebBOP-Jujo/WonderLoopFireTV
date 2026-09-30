package org.wonderloop.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import java.io.ByteArrayOutputStream

@Composable
actual fun CameraScanScreen(
    initialArtStyle: String,
    onPhotoCaptured: (imageBase64: String, artStyle: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBase64 by remember { mutableStateOf<String?>(null) }
    var selectedArtStyle by remember(initialArtStyle) { mutableStateOf(initialArtStyle) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val brandRed = Color(0xFFD9384A)
    val deepInk = Color(0xFF2B2340)
    val mutedInk = Color(0xFF6B6480)
    val creamBg = Color(0xFFFFFBF5)
    val softBlush = Color(0xFFFDECE9)
    val softBlushBorder = Color(0xFFF6D5D0)
    val goldOutline = Color(0xFFFFC247)
    val mintColor = Color(0xFF0E7F64)
    val mintSoftBg = Color(0xFFE4F6F0)

    // Gentle floating animation for the paper guide illustration
    val infiniteTransition = rememberInfiniteTransition(label = "scan_guide")
    val sheetFloatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheetFloatY"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Auto-clear denial notice on resume if user granted camera permission in Settings
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    permissionDeniedNotice = false
                    showSettingsDialog = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val processAndSetBitmap: (Bitmap) -> Unit = { rawBitmap ->
        val maxDim = 1024
        val scaled = if (rawBitmap.width > maxDim || rawBitmap.height > maxDim) {
            val scaleFactor = minOf(maxDim.toFloat() / rawBitmap.width, maxDim.toFloat() / rawBitmap.height)
            Bitmap.createScaledBitmap(
                rawBitmap,
                (rawBitmap.width * scaleFactor).toInt(),
                (rawBitmap.height * scaleFactor).toInt(),
                true
            )
        } else {
            rawBitmap
        }
        previewBitmap = scaled
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        val bytes = outputStream.toByteArray()
        currentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
    }

    // Hardware Back Button: if photo is previewed, clear it; otherwise exit to Home
    BackHandler(enabled = previewBitmap != null) {
        previewBitmap = null
        currentBase64 = null
    }

    // Camera Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { processAndSetBitmap(it) }
    }

    // Permission Launcher for CAMERA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            permissionDeniedNotice = false
            showSettingsDialog = false
            try {
                takePictureLauncher.launch(null)
            } catch (e: Exception) {
                println("⚠️ Error launching camera after permission grant: ${e.message}")
            }
        } else {
            permissionDeniedNotice = true
            val canAskAgain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            if (!canAskAgain) {
                showSettingsDialog = true
            }
        }
    }

    val requestCameraOrSettings: () -> Unit = {
        PlatformAudioService.playSfx(SoundEffect.POP)
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            permissionDeniedNotice = false
            showSettingsDialog = false
            try {
                takePictureLauncher.launch(null)
            } catch (e: Exception) {
                println("⚠️ Error launching camera: ${e.message}")
            }
        } else {
            val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            if (permissionDeniedNotice && !shouldShowRationale) {
                showSettingsDialog = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Gallery Launcher with safe downsampling and EXIF orientation fix
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val orientation = try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        android.media.ExifInterface(stream).getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: android.media.ExifInterface.ORIENTATION_NORMAL
                } catch (_: Exception) {
                    android.media.ExifInterface.ORIENTATION_NORMAL
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                var sampleSize = 1
                val maxDim = 1024
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val rawBitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (rawBitmap != null) {
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    val rotatedBitmap = if (!matrix.isIdentity) {
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }
                    processAndSetBitmap(rotatedBitmap)
                }
            } catch (e: Exception) {
                println("⚠️ [CameraScan] Error reading gallery image: $e")
            }
        }
    }

    // Modal dialog when Android has suppressed the permission prompt
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(softBlush)
                    ) {
                        Text(text = "📷", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.cameraPermissionDialogTitle,
                        fontFamily = TitleFont(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deepInk
                    )
                }
            },
            text = {
                Text(
                    text = AppStrings.cameraPermissionDialogDesc,
                    fontSize = 14.sp,
                    color = mutedInk,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        try {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            println("⚠️ Error opening settings: ${e.message}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = AppStrings.openSettings,
                        fontFamily = TitleFont(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(
                        text = AppStrings.cancel,
                        fontFamily = TitleFont(),
                        color = mutedInk,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(creamBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // -----------------------------------------------------------------
        // 1. TOP BAR (.top: Back Button + Title + Speak Aloud Button)
        // -----------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Circle Button
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        if (previewBitmap != null) {
                            previewBitmap = null
                            currentBase64 = null
                        } else {
                            onBack()
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.65f, h * 0.18f)
                            lineTo(w * 0.30f, h * 0.50f)
                            lineTo(w * 0.65f, h * 0.82f)
                        }
                        drawPath(
                            path = path,
                            color = deepInk,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Screen Title
            Text(
                text = if (previewBitmap != null) {
                    if (AppStrings.isEnglish) "Drawing ready!" else "¡Dibujo listo!"
                } else {
                    if (AppStrings.isEnglish) "Frame your drawing" else "Enfoca tu dibujo"
                },
                fontFamily = TitleFont(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = deepInk,
                textAlign = TextAlign.Center
            )

            // Speak Aloud Circle Button (.vol)
            Surface(
                shape = CircleShape,
                color = softBlush,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        val msg = if (previewBitmap != null) {
                            if (AppStrings.isEnglish) "Your drawing looks amazing! Tap the button below to bring your character to life."
                            else "¡Tu dibujo ha quedado genial! Pulsa el botón rojo para darle vida a tu personaje."
                        } else {
                            if (AppStrings.isEnglish) "Place your drawing on a well-lit table and take a photo so the whole sheet is visible."
                            else "Coloca tu dibujo sobre una mesa con buena luz y hazle una foto para que se vea todo el folio."
                        }
                        PlatformAudioService.speak(msg)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val strokeW = 2.2.dp.toPx()
                        val speakerPath = Path().apply {
                            moveTo(4f * sx, 9f * sy)
                            lineTo(4f * sx, 15f * sy)
                            lineTo(8f * sx, 15f * sy)
                            lineTo(13f * sx, 19f * sy)
                            lineTo(13f * sx, 5f * sy)
                            lineTo(8f * sx, 9f * sy)
                            close()
                        }
                        drawPath(
                            path = speakerPath,
                            color = brandRed,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawArc(
                            color = brandRed,
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(12f * sx, 8f * sy),
                            size = Size(6f * sx, 8f * sy),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (previewBitmap != null) {
                if (AppStrings.isEnglish) "Ready to turn into a magical story" else "Listo para convertirse en un cuento mágico"
            } else {
                AppStrings.scanSubtitle
            },
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = mutedInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // -----------------------------------------------------------------
        // 2. FRAMED SHEET GUIDE (.gd: White sheet inside dashed yellow/mint outline)
        // -----------------------------------------------------------------
        val frameDashColor = if (previewBitmap != null) Color(0xFF1FBE9C) else goldOutline

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .drawBehind {
                    val strokeW = 3.5.dp.toPx()
                    val half = strokeW / 2f
                    drawRoundRect(
                        color = frameDashColor,
                        topLeft = Offset(half, half),
                        size = Size(size.width - strokeW, size.height - strokeW),
                        cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
                        )
                    )
                }
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        if (previewBitmap == null) {
                            requestCameraOrSettings()
                        }
                    }
            ) {
                if (previewBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = if (AppStrings.isEnglish) "Scanned drawing" else "Dibujo escaneado",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top-right "✓ Listo" Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(mintSoftBg)
                                .border(1.dp, mintColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "✓ Ready" else "✓ Listo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = mintColor
                            )
                        }
                    }
                } else {
                    // Empty State: Animated Paper Sheet Illustration
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(134.dp)
                                .offset(y = sheetFloatY.dp)
                        ) {
                            // Soft warm halo behind doodle
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF3D6))
                            )

                            // Tilted mini sheet with character sketch
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(width = 96.dp, height = 112.dp)
                                    .rotate(-4f)
                                    .border(1.5.dp, Color(0xFFF3E8D2), RoundedCornerShape(16.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🐲",
                                        fontSize = 52.sp
                                    )
                                }
                            }

                            // Floating camera badge on bottom-right of the sheet
                            Surface(
                                shape = CircleShape,
                                color = brandRed,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CameraVectorIcon(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = AppStrings.frameYourDrawing,
                            fontFamily = TitleFont(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = deepInk,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = AppStrings.frameDrawingHint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = mutedInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // -----------------------------------------------------------------
        // 3. HELPFUL TIP PILL (.tip)
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (previewBitmap != null) mintSoftBg else Color(0xFFFFF1CF))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (previewBitmap != null) {
                    if (AppStrings.isEnglish) "✨ Awesome! Now bring your character to life"
                    else "✨ ¡Genial! Ahora dale vida para crear su cuento"
                } else {
                    if (AppStrings.isEnglish) "💡 Make sure the whole sheet is visible with good light"
                    else "💡 Que se vea todo el folio y con buena luz"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (previewBitmap != null) mintColor else Color(0xFF8A5A00),
                textAlign = TextAlign.Center
            )
        }

        // Permission Denied Inline Card (if applicable)
        if (permissionDeniedNotice) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(softBlush)
                    .border(1.5.dp, softBlushBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = AppStrings.cameraPermissionRequired,
                        fontFamily = TitleFont(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = brandRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = AppStrings.cameraPermissionNotice,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = deepInk,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    println("⚠️ Error opening settings: ${e.message}")
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, softBlushBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = AppStrings.settingsButton,
                                fontFamily = TitleFont(),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = brandRed
                            )
                        }

                        Button(
                            onClick = requestCameraOrSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = AppStrings.askPermissionButton,
                                fontFamily = TitleFont(),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -----------------------------------------------------------------
        // 4. BOTTOM ACTION CONTROLS
        // -----------------------------------------------------------------
        if (previewBitmap == null) {
            // Primary Pill Button: Open Camera & Snap Photo
            Button(
                onClick = requestCameraOrSettings,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CameraVectorIcon(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Take Photo of Drawing" else "Hacer foto al dibujo",
                        fontFamily = TitleFont(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Pill Button: Choose from Gallery
            OutlinedButton(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    galleryLauncher.launch("image/*")
                },
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE5DEC9)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = AppStrings.chooseFromGallery,
                    fontFamily = TitleFont(),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = deepInk
                )
            }
        } else {
            // Art Style Segmented Selector Pill (Watercolor vs 3D Animation)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (AppStrings.isEnglish) "ILLUSTRATION STYLE" else "ESTILO DE ILUSTRACIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = mutedInk,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFEDE8E1))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isWatercolor = selectedArtStyle != "3d_animation"
                    Surface(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            selectedArtStyle = "watercolor"
                            org.wonderloop.app.data.ProfileRepository.setPreferredArtStyle("watercolor")
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isWatercolor) Color.White else Color.Transparent,
                        shadowElevation = if (isWatercolor) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (AppStrings.isEnglish) "🎨 Watercolor" else "🎨 Acuarela",
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isWatercolor) deepInk else mutedInk
                            )
                        }
                    }

                    val is3D = selectedArtStyle == "3d_animation"
                    Surface(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            selectedArtStyle = "3d_animation"
                            org.wonderloop.app.data.ProfileRepository.setPreferredArtStyle("3d_animation")
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = if (is3D) Color.White else Color.Transparent,
                        shadowElevation = if (is3D) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (AppStrings.isEnglish) "🎬 3D Animation" else "🎬 Animación 3D",
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (is3D) deepInk else mutedInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Pill Button: Bring Character to Life
            Button(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                    currentBase64?.let { onPhotoCaptured(it, selectedArtStyle) }
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = AppStrings.bringToLifeButton,
                    fontFamily = TitleFont(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Pill Button: Retake / Choose Another Photo
            OutlinedButton(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    previewBitmap = null
                    currentBase64 = null
                },
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.5.dp, softBlushBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = AppStrings.takeAnotherPhoto,
                    fontFamily = TitleFont(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = brandRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CameraVectorIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val strokeW = 2.2.dp.toPx()

        // Camera body path: <path d="M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/>
        val bodyPath = Path().apply {
            moveTo(4f * sx, 8f * sy)
            lineTo(7f * sx, 8f * sy)
            lineTo(9f * sx, 5f * sy)
            lineTo(15f * sx, 5f * sy)
            lineTo(17f * sx, 8f * sy)
            lineTo(20f * sx, 8f * sy)
            arcTo(
                rect = Rect(19f * sx, 8f * sy, 21f * sx, 10f * sy),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(21f * sx, 19f * sy)
            arcTo(
                rect = Rect(19f * sx, 18f * sy, 21f * sx, 20f * sy),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(4f * sx, 20f * sy)
            arcTo(
                rect = Rect(3f * sx, 18f * sy, 5f * sx, 20f * sy),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(3f * sx, 9f * sy)
            arcTo(
                rect = Rect(3f * sx, 8f * sy, 5f * sx, 10f * sy),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Camera lens: <circle cx="12" cy="13.5" r="3.5"/>
        drawCircle(
            color = color,
            radius = 3.5f * sx,
            center = Offset(12f * sx, 13.5f * sy),
            style = Stroke(width = strokeW)
        )
    }
}
