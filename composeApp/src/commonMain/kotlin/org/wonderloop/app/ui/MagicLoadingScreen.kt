package org.wonderloop.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*

@Composable
fun MagicLoadingScreen(
    imageBase64: String,
    api: WonderLoopApi,
    artStyle: String = "watercolor",
    onAnalysisComplete: (CharacterProfile) -> Unit,
    onError: (String) -> Unit
) {
    val brandRed = Color(0xFFD9384A)
    val deepInk = Color(0xFF2B2340)
    val mutedInk = Color(0xFF6B6480)
    val softBlush = Color(0xFFFDECE9)
    val softGold = Color(0xFFFFF1CF)
    val goldInk = Color(0xFFB87800)
    val mintColor = Color(0xFF0E7F64)
    val mintSoft = Color(0xFFE4F6F0)
    val cardBorder = Color(0xFFEDE8E1)

    val phrases = remember(AppStrings.isEnglish) {
        if (AppStrings.isEnglish) {
            listOf(
                "✨ Discovering the magical colors of your drawing...",
                "🔍 Finding your hero's unique superpowers...",
                "🌟 Learning your character's cheerful personality...",
                "🎨 Opening WonderLoop's enchanted storybook..."
            )
        } else {
            listOf(
                "✨ Buscando los colores mágicos de tu dibujo...",
                "🔍 Descubriendo los superpoderes de tu personaje...",
                "🌟 Conociendo la personalidad de tu héroe...",
                "🎨 Abriendo el libro encantado de WonderLoop..."
            )
        }
    }

    var phraseIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "magic_loading")

    val cardFloatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardFloatY"
    )

    val cardRotate by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardRotate"
    )

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    val progressBarAnim by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressBar"
    )

    // Play initial magical chime sound
    LaunchedEffect(Unit) {
        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
    }

    // Phrases cycler
    LaunchedEffect(Unit) {
        while (true) {
            delay(1900)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    // Call Backend Gateway with automatic client-side retry and zero fallback characters
    LaunchedEffect(imageBase64, retryTrigger) {
        errorMessage = null
        var lastError: Throwable? = null
        var analyzedChar: CharacterProfile? = null

        for (attempt in 1..2) {
            try {
                val (response, compactChar) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val res = api.analyzeDrawing(imageBase64, artStyle = artStyle)
                    val c = if (res.success) {
                        res.character.copy(
                            originalDrawingBase64 = org.wonderloop.app.platform.PlatformStorage.externalizeMediaIfNeeded(
                                "preview_latest_drawing.jpg",
                                res.character.originalDrawingBase64
                            ) ?: res.character.originalDrawingBase64,
                            reinterpretedImageUrl = org.wonderloop.app.platform.PlatformStorage.externalizeMediaIfNeeded(
                                "preview_latest_portrait.jpg",
                                res.character.reinterpretedImageUrl
                            ) ?: res.character.reinterpretedImageUrl
                        )
                    } else {
                        res.character
                    }
                    res to c
                }
                if (response.success) {
                    analyzedChar = compactChar
                    break
                } else {
                    lastError = IllegalStateException("AnalyzeDrawingResponse success=false")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: org.wonderloop.app.data.BackendOfflineException) {
                println("⚠️ [MagicLoadingScreen] Backend unreachable on attempt $attempt: ${e.message}")
                lastError = e
                break
            } catch (e: Throwable) {
                println("⚠️ [MagicLoadingScreen] Attempt $attempt/2 error analyzing drawing: $e")
                lastError = e
                if (attempt < 2) {
                    val reachable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        api.checkHealth()
                    }
                    if (!reachable) {
                        println("⚠️ [MagicLoadingScreen] Backend offline after health check, skipping retry")
                        break
                    }
                }
            }
            if (attempt < 2) {
                delay(1000)
            }
        }

        if (analyzedChar != null) {
            delay(500)
            onAnalysisComplete(analyzedChar)
        } else {
            println("❌ [MagicLoadingScreen] All attempts failed analyzing drawing: $lastError")
            errorMessage = if (api.isOnline == false || lastError is org.wonderloop.app.data.BackendOfflineException) {
                if (AppStrings.isEnglish) {
                    "Could not connect to the magic server. Make sure you have a connection and tap Retry!"
                } else {
                    "No hemos podido conectar con el servidor mágico. Comprueba la conexión y pulsa Reintentar."
                }
            } else {
                if (AppStrings.isEnglish) {
                    "The magic wand is very busy right now and couldn't analyze your drawing. Try again in a few seconds!"
                } else {
                    "La varita mágica está muy solicitada ahora mismo y no pudo analizar tu dibujo. ¡Vuelve a intentarlo en unos segundos!"
                }
            }
        }
    }

    // Magic Pause Dialog when drawing analysis fails after retries
    if (errorMessage != null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(softGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🪄", fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStrings.magicPause,
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deepInk,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = mutedInk,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = mintSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) {
                                "✨ Your photo is safe — no need to take it again"
                            } else {
                                "✨ Tu foto está guardada, no tienes que repetirla"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = mintColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                            errorMessage = null
                            retryTrigger++
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandRed,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "🔄 ${AppStrings.retry}",
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onError(errorMessage ?: "Error")
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = mutedInk
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "Back to camera" else "Volver a la cámara",
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedInk
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF3E0),
                        Color(0xFFFDF3EC),
                        Color(0xFFFFFBF5)
                    ),
                    center = Offset(400f, 500f),
                    radius = 1500f
                )
            )
    ) {
        // Soft ambient floating blobs
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 28.dp, y = (70 + cardFloatY).dp)
                .size(64.dp)
                .alpha(0.35f)
                .clip(CircleShape)
                .background(Color(0xFFFFB5A7))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-32).dp, y = (110 - cardFloatY).dp)
                .size(48.dp)
                .alpha(0.35f)
                .clip(CircleShape)
                .background(Color(0xFF9EE3D3))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 42.dp, y = (-120 - cardFloatY).dp)
                .size(44.dp)
                .alpha(0.45f)
                .clip(CircleShape)
                .background(Color(0xFFFFE099))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(softBlush)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (AppStrings.isEnglish) "✨ MAGICAL SCAN · READING DRAWING" else "✨ MAGIA EN MARCHA · MIRANDO TU DIBUJO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = brandRed,
                    letterSpacing = 0.4.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Central Framed Drawing Card with Magic Scanning Beam & Sparkles
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(246.dp)
            ) {
                // Soft outer halo
                Box(
                    modifier = Modifier
                        .size(226.dp)
                        .scale(1f + (sparkleScale - 0.95f) * 0.18f)
                        .clip(RoundedCornerShape(42.dp))
                        .background(softBlush.copy(alpha = 0.7f))
                )

                // White Polaroid / Paper Frame
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(204.dp)
                        .offset(y = cardFloatY.dp)
                        .rotate(cardRotate)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        val formattedModel = if (imageBase64.startsWith("data:")) imageBase64 else "data:image/jpeg;base64,$imageBase64"
                        WonderLoopImage(
                            model = formattedModel,
                            contentDescription = "Dibujo escaneado",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Animated Magical Scanning Beam
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val beamY = maxHeight * scanLineProgress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .offset(y = beamY - 16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFFFFD166).copy(alpha = 0.55f),
                                                Color.White.copy(alpha = 0.85f),
                                                Color(0xFFFFD166).copy(alpha = 0.55f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Floating 4-Point Vector Sparkles on Corners
                MagicSparkleStar(
                    color = Color(0xFFFFC93C),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 4.dp)
                        .size(28.dp)
                        .scale(sparkleScale)
                )
                MagicSparkleStar(
                    color = brandRed,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 8.dp, y = (-12).dp)
                        .size(22.dp)
                        .scale(2f - sparkleScale)
                )

                // Bottom overlapping pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = softGold,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                ) {
                    Text(
                        text = AppStrings.wakingUpDrawing,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = goldInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Main Title in Fredoka 600
            Text(
                text = AppStrings.bringingCreationToLife,
                fontFamily = TitleFont(),
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = deepInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Phrase Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFFEFE8DF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phrases[phraseIndex],
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Coral Progress Bar (.pr from Mockup)
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFF5DAD8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressBarAnim)
                        .clip(RoundedCornerShape(5.dp))
                        .background(brandRed)
                )
            }
        }
    }
}

@Composable
private fun MagicSparkleStar(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.55f, h * 0.36f, w * 0.64f, h * 0.45f, w, h * 0.5f)
            cubicTo(w * 0.64f, h * 0.55f, w * 0.55f, h * 0.64f, w * 0.5f, h)
            cubicTo(w * 0.45f, h * 0.64f, w * 0.36f, h * 0.55f, 0f, h * 0.5f)
            cubicTo(w * 0.36f, h * 0.45f, w * 0.45f, h * 0.36f, w * 0.5f, 0f)
            close()
        }
        drawPath(path = path, color = color)
    }
}
