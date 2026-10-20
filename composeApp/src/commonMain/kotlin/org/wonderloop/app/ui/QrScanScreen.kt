package org.wonderloop.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wonderloop.app.data.StoryContinuationContext
import org.wonderloop.app.data.TvSessionResponse
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.QrCodeView
import org.wonderloop.app.ui.components.tvFocusable
import org.wonderloop.app.ui.components.TvFocusGold

private val QrCreamBg = Color(0xFFFFFBF5)
private val QrPaper = Color(0xFFFFFFFF)
private val QrInkDark = Color(0xFF2B2340)
private val QrInkMuted = Color(0xFF6B6480)
private val QrCoral = Color(0xFFD9384A)
private val QrCoralSoft = Color(0xFFFDECE9)
private val QrMint = Color(0xFF0E7F64)
private val QrMintSoft = Color(0xFFE4F6F0)
private val QrGold = Color(0xFFFFD166)
private val QrGoldSoft = Color(0xFFFFF1CF)
private val QrCardBorder = Color(0xFFEDE8E1)

@Composable
fun QrScanScreen(
    initialArtStyle: String = "watercolor",
    continuation: StoryContinuationContext? = null,
    scanSessionKey: Long = 0L,
    existingSessionId: String? = null,
    api: WonderLoopApi,
    onPhotoCaptured: (imageBase64: String, artStyle: String, sessionId: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedArtStyle by remember(initialArtStyle) { mutableStateOf(initialArtStyle) }
    var session by remember { mutableStateOf<TvSessionResponse?>(null) }
    var sessionStatus by remember { mutableStateOf("waiting") }
    var isLoadingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showIpDialog by remember { mutableStateOf(false) }
    var customIpInput by remember { mutableStateOf(api.baseUrl.replace("http://", "").replace(":3000", "")) }
    var pollTrigger by remember { mutableStateOf(0) }

    val retryFocusRequester = remember { FocusRequester() }
    val dialogConfirmRequester = remember { FocusRequester() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(120)
            try {
                retryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showIpDialog) {
        if (showIpDialog) {
            delay(120)
            try {
                dialogConfirmRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Pulse animation for live indicators
    val infiniteTransition = rememberInfiniteTransition(label = "qr_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Scanning laser beam animation across QR viewfinder
    val scanTransition = rememberInfiniteTransition(label = "scan_beam")
    val scanOffsetY by scanTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_offset"
    )

    // Function to initialize or refresh the QR session
    fun startSession(forceNew: Boolean = false) {
        isLoadingSession = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val targetSessionId = if (!forceNew) (existingSessionId ?: session?.sessionId) else null
                if (targetSessionId != null) {
                    val resetSession = api.resetTvSession(targetSessionId)
                    if (resetSession != null) {
                        session = resetSession
                        sessionStatus = "waiting"
                        isLoadingSession = false
                        pollTrigger++
                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                        return@launch
                    }
                }
                val lang = if (AppStrings.isEnglish) "en" else "es"
                val newSession = api.createTvSession(language = lang)
                session = newSession
                sessionStatus = "waiting"
                isLoadingSession = false
                pollTrigger++
                PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
            } catch (e: Exception) {
                isLoadingSession = false
                errorMessage = e.message ?: "Error al conectar con el servidor"
            }
        }
    }

    LaunchedEffect(scanSessionKey) {
        startSession(forceNew = scanSessionKey != 0L && existingSessionId == null)
    }

    // Active polling loop while session is open (with timeout & expiry detection)
    LaunchedEffect(session?.sessionId, pollTrigger) {
        val sId = session?.sessionId ?: return@LaunchedEffect
        var pollCount = 0
        val maxPolls = 400 // ~8 minutes of polling before automatic timeout
        while (pollCount < maxPolls) {
            delay(1200)
            pollCount++
            try {
                val pollResult = api.pollTvSession(sId)
                sessionStatus = pollResult.status
                if (pollResult.status == "ready" && !pollResult.imageBase64.isNullOrBlank()) {
                    PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
                    onPhotoCaptured(pollResult.imageBase64, selectedArtStyle, sId)
                    break
                }
                if (pollResult.expiresInSeconds <= 0) {
                    sessionStatus = "expired"
                    errorMessage = if (AppStrings.isEnglish) "QR session expired. Click Retry to generate a new code." else "La sesión QR ha caducado. Pulsa Reintentar para generar un nuevo código."
                    break
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("404") || msg.contains("SESSION_NOT_FOUND")) {
                    sessionStatus = "expired"
                    errorMessage = if (AppStrings.isEnglish) "QR session expired. Click Retry to generate a new code." else "La sesión QR ha caducado. Pulsa Reintentar para generar un nuevo código."
                    break
                }
                // Ignore transient network poll blips
            }
        }
        if (pollCount >= maxPolls && sessionStatus != "ready") {
            sessionStatus = "expired"
            errorMessage = if (AppStrings.isEnglish) "Session timed out. Click Retry for a new QR." else "Tiempo de espera agotado. Pulsa Reintentar para generar un nuevo QR."
        }
    }

    // IP Configuration Dialog for Fire TV Stick 4K on different Wi-Fi networks
    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            title = {
                Text(
                    text = if (AppStrings.isEnglish) "Configure Server IP" else "Configurar IP del Servidor",
                    fontFamily = TitleFont(),
                    fontSize = 20.sp,
                    color = QrInkDark
                )
            },
            text = {
                Column {
                    Text(
                        text = if (AppStrings.isEnglish)
                            "Enter your PC's Wi-Fi IP address (e.g. 192.168.0.35) so your Fire TV and mobile phone can connect:"
                        else
                            "Introduce la dirección IP de tu ordenador en la red Wi-Fi (ej. 192.168.0.35):",
                        fontSize = 14.sp,
                        color = QrInkMuted,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customIpInput,
                        onValueChange = { customIpInput = it },
                        singleLine = true,
                        label = { Text("IP / Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = QrCoral,
                    modifier = Modifier
                        .focusRequester(dialogConfirmRequester)
                        .tvFocusable(
                            shape = RoundedCornerShape(20.dp),
                            focusBorderColor = TvFocusGold,
                            onClick = {
                                showIpDialog = false
                                api.updateCustomBackendUrl(customIpInput)
                                startSession(forceNew = true)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "Connect & Refresh" else "Conectar y Actualizar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .tvFocusable(
                            shape = RoundedCornerShape(20.dp),
                            focusBorderColor = QrCoral,
                            onClick = { showIpDialog = false }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = AppStrings.cancel, color = QrInkMuted, fontSize = 14.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(26.dp)
        )
    }

    // Full 16:9 Widescreen Layout with Fire TV Safe Zone Padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QrCreamBg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QrGoldSoft.copy(alpha = 0.5f),
                            QrCoralSoft.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.35f, size.height * 0.45f),
                        radius = maxOf(size.width, size.height) * 0.65f
                    )
                )
            }
            .padding(horizontal = 48.dp, vertical = 20.dp) // Fire TV 5% Overscan Safe Zone
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // =================================================================
            // LEFT COLUMN (44% width): THE MAGIC QR PORTAL
            // =================================================================
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = QrPaper,
                border = BorderStroke(1.5.dp, QrCardBorder),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header inside card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = QrGoldSoft,
                            border = BorderStroke(1.dp, QrGold.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = if (continuation != null)
                                    "✨ ${AppStrings.sagaBadge(continuation.chapterIndex)} · RETO DE DIBUJO ✨"
                                else
                                    "✨ EL PORTAL MÁGICO ✨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9E6700),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = if (continuation != null)
                                "¡Envía el dibujo para el Capítulo ${continuation.chapterIndex}!"
                            else
                                "¡Pasa tu dibujo del papel a la tele!",
                            fontFamily = TitleFont(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = QrInkDark,
                            textAlign = TextAlign.Center
                        )
                    }

                    // QR Code Area with Camera Viewfinder brackets & Scanning Laser beam
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // QR background container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFFFFFDF9))
                                .border(1.5.dp, Color(0xFFEEE6EF), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingSession) {
                                CircularProgressIndicator(
                                    color = QrCoral,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else if (!session?.uploadUrl.isNullOrBlank()) {
                                QrCodeView(
                                    content = session!!.uploadUrl,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                )

                                // Animated Scanning Laser Beam across the QR
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(4.dp)
                                        .align(Alignment.TopCenter)
                                        .offset(y = 200.dp * scanOffsetY)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    QrCoral.copy(alpha = 0.85f),
                                                    QrCoral,
                                                    QrCoral.copy(alpha = 0.85f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(text = "⚠️", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = errorMessage ?: "No se pudo crear la sesión QR",
                                        fontSize = 14.sp,
                                        color = QrCoral,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = QrCoral,
                                        modifier = Modifier
                                            .focusRequester(retryFocusRequester)
                                            .tvFocusable(
                                                shape = RoundedCornerShape(50.dp),
                                                focusBorderColor = TvFocusGold,
                                                focusedScale = 1.08f,
                                                onClick = { startSession(forceNew = true) }
                                            )
                                            .padding(horizontal = 24.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Reintentar",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Camera Viewfinder 4 Corner Brackets
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerLength = 26.dp.toPx()
                            val strokeW = 4.5.dp.toPx()
                            val cornerColor = QrCoral

                            // Top-Left
                            drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
                            drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

                            // Top-Right
                            drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
                            drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

                            // Bottom-Left
                            drawLine(cornerColor, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth = strokeW, cap = StrokeCap.Round)
                            drawLine(cornerColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

                            // Bottom-Right
                            drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth = strokeW, cap = StrokeCap.Round)
                            drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)
                        }
                    }

                    // Live Status Pill & Wi-Fi hint
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = when (sessionStatus) {
                                "connected" -> QrMintSoft
                                "uploading", "ready" -> QrGoldSoft
                                else -> QrCoralSoft
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when (sessionStatus) {
                                    "connected" -> QrMint
                                    "uploading", "ready" -> QrGold
                                    else -> QrCoral.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .scale(if (sessionStatus == "connected" || sessionStatus == "uploading") pulseScale else 1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (sessionStatus) {
                                        "connected" -> "✅ Móvil conectado · ¡haz la foto!"
                                        "uploading" -> "⬆️ Enviando dibujo a la tele..."
                                        "ready" -> "✨ ¡Dibujo recibido! Transformando..."
                                        else -> "⏳ Esperando escaneo con el móvil..."
                                    },
                                    fontFamily = TitleFont(),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (sessionStatus) {
                                        "connected" -> QrMint
                                        "uploading", "ready" -> Color(0xFF8A5A00)
                                        else -> QrCoral
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📶 Móvil y tele en la misma Wi-Fi",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QrInkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // =================================================================
            // RIGHT COLUMN (56% width): STEPS, STYLE, DEMO & CONTROLS
            // =================================================================
            Column(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. 3 Live Step Cards (Cycles 1 -> 2 -> 3 while waiting, then tracks live status)
                var demoStep by remember { mutableStateOf(1) }
                LaunchedEffect(sessionStatus) {
                    if (sessionStatus == "waiting") {
                        while (true) {
                            delay(2400L)
                            demoStep = (demoStep % 3) + 1
                        }
                    }
                }

                val currentStep = when (sessionStatus) {
                    "connected" -> 2
                    "uploading", "ready" -> 3
                    else -> demoStep
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QrStepCard(
                        number = "1",
                        icon = "📱",
                        title = "Escanea el QR",
                        desc = "Abre la cámara de tu móvil y enfoca la tele",
                        isActive = currentStep == 1,
                        modifier = Modifier.weight(1f)
                    )
                    QrStepCard(
                        number = "2",
                        icon = "📷",
                        title = "Haz la foto",
                        desc = "Fotografía el dibujo en papel de tu peque",
                        isActive = currentStep == 2,
                        modifier = Modifier.weight(1f)
                    )
                    QrStepCard(
                        number = "3",
                        icon = "✨",
                        title = "¡Mira la tele!",
                        desc = "El dibujo cobrará vida en tu salón",
                        isActive = currentStep == 3,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Illustration Art Style Selector (Primary initial D-Pad focus)
                val styleFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(120)
                    try { styleFocusRequester.requestFocus() } catch (_: Exception) {}
                }
                Column {
                    Text(
                        text = if (AppStrings.isEnglish) "Illustration Art Style" else "Estilo de las ilustraciones",
                        fontFamily = TitleFont(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = QrInkDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFEFE8DE))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isWatercolor = selectedArtStyle == "watercolor"
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isWatercolor) Color.White else Color.Transparent,
                            shadowElevation = if (isWatercolor) 4.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .focusRequester(styleFocusRequester)
                                .tvFocusable(
                                    shape = RoundedCornerShape(50.dp),
                                    focusedScale = 1.04f,
                                    focusBorderColor = TvFocusGold,
                                    focusBorderWidth = 3.dp,
                                    onClick = { selectedArtStyle = "watercolor" }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(text = "🎨", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (AppStrings.isEnglish) "Watercolor" else "Acuarela",
                                    fontFamily = TitleFont(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWatercolor) QrInkDark else QrInkMuted
                                )
                            }
                        }

                        val is3D = selectedArtStyle == "3d_animation"
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (is3D) Color.White else Color.Transparent,
                            shadowElevation = if (is3D) 4.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(50.dp),
                                    focusedScale = 1.04f,
                                    focusBorderColor = TvFocusGold,
                                    focusBorderWidth = 3.dp,
                                    onClick = { selectedArtStyle = "3d_animation" }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(text = "🎬", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (AppStrings.isEnglish) "3D Animation" else "Animación 3D",
                                    fontFamily = TitleFont(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (is3D) QrInkDark else QrInkMuted
                                )
                            }
                        }
                    }
                }

                // 3. Demo Drawings Card (Remote D-Pad Selector for judges & testing)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = QrPaper,
                    border = BorderStroke(1.5.dp, QrCardBorder),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🎮 ¿Sin móvil a mano?",
                                    fontFamily = TitleFont(),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QrInkDark
                                )
                                Text(
                                    text = "Prueba con un dibujo de ejemplo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = QrInkMuted
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF6F0E8)
                            ) {
                                Text(
                                    text = "OK para elegir",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QrInkMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DemoDrawings.items.forEach { demo ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFFFFF8EC),
                                    border = BorderStroke(1.5.dp, Color(0xFFF3E3C6)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .tvFocusable(
                                            shape = RoundedCornerShape(18.dp),
                                            focusedScale = 1.08f,
                                            focusBorderColor = TvFocusGold,
                                            focusBorderWidth = 3.dp,
                                            onClick = {
                                                PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                                onPhotoCaptured(demo.base64DataUri, selectedArtStyle, session?.sessionId ?: "")
                                            }
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = demo.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = demo.name,
                                            fontFamily = TitleFont(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = QrInkDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Bottom Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (D-Pad focused)
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(50.dp),
                        color = QrPaper,
                        border = BorderStroke(1.5.dp, Color(0xFFE9E1EA)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.06f,
                                focusBorderColor = TvFocusGold,
                                focusBorderWidth = 3.dp,
                                onClick = onBack
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text(text = "←", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = QrInkMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (AppStrings.isEnglish) "Back to Living Room" else "Volver al salón",
                                fontFamily = TitleFont(),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = QrInkDark
                            )
                        }
                    }

                    // Server IP configuration pill
                    Surface(
                        onClick = { showIpDialog = true },
                        shape = RoundedCornerShape(50.dp),
                        color = Color(0xFFEFE8DE),
                        modifier = Modifier
                            .height(42.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.06f,
                                focusBorderColor = TvFocusGold,
                                focusBorderWidth = 3.dp,
                                onClick = { showIpDialog = true }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(text = "📶", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi · ${api.baseUrl.replace("http://", "").replace(":3000", "")} · ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5B5470)
                            )
                            Text(
                                text = "Cambiar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = QrCoral
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrStepCard(
    number: String,
    icon: String,
    title: String,
    desc: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animScale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 0.98f,
        animationSpec = tween(450, easing = FastOutSlowInEasing)
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.65f,
        animationSpec = tween(450)
    )
    val animElevation by animateDpAsState(
        targetValue = if (isActive) 8.dp else 2.dp,
        animationSpec = tween(450)
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = QrPaper,
        border = BorderStroke(
            if (isActive) 2.5.dp else 1.5.dp,
            if (isActive) QrCoral else QrCardBorder
        ),
        shadowElevation = animElevation,
        modifier = modifier
            .height(138.dp)
            .graphicsLayer {
                alpha = animAlpha
                scaleX = animScale
                scaleY = animScale
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isActive) QrCoral else QrCoralSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        fontFamily = TitleFont(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActive) Color.White else QrCoral
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontFamily = TitleFont(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = QrInkDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A4260),
                    lineHeight = 17.sp
                )
            }
        }
    }
}
