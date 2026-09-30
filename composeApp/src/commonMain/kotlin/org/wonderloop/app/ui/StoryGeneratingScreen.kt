package org.wonderloop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.StoryContinuationContext
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*

private val GenCreamBg = Color(0xFFFFFBF5)
private val GenInkDark = Color(0xFF2B2340)
private val GenInkMuted = Color(0xFF6B6480)
private val GenCoral = Color(0xFFD9384A)
private val GenCoralSoft = Color(0xFFFDECE9)
private val GenMint = Color(0xFF0E7F64)
private val GenMintSoft = Color(0xFFE4F6F0)
private val GenGoldSoft = Color(0xFFFFF1CF)
private val GenGoldAccent = Color(0xFFFFB830)
private val GenCardBorder = Color(0xFFEDE8E1)

@Composable
fun StoryGeneratingScreen(
    character: CharacterProfile,
    childName: String,
    childAge: Int,
    api: WonderLoopApi,
    language: String = "es",
    continuation: StoryContinuationContext? = null,
    artStyle: String = "watercolor",
    onStoryGenerated: (FullStoryResponse) -> Unit,
    onError: (String) -> Unit
) {
    val is3D = artStyle == "3d_animation"
    val phrases = remember(AppStrings.isEnglish, character.name, childName, childAge, continuation, is3D) {
        if (continuation != null) {
            if (AppStrings.isEnglish) {
                listOf(
                    AppStrings.writingChapter(continuation.chapterIndex, continuation.previousHeroName, character.name),
                    AppStrings.paintingBothHeroes,
                    AppStrings.resolvingChallenge,
                    "🖍️ Preparing the next Creativity Loop drawing mission..."
                )
            } else {
                listOf(
                    AppStrings.writingChapter(continuation.chapterIndex, continuation.previousHeroName, character.name),
                    AppStrings.paintingBothHeroes,
                    AppStrings.resolvingChallenge,
                    "🖍️ Preparando el siguiente reto de dibujo..."
                )
            }
        } else {
            if (AppStrings.isEnglish) {
                listOf(
                    "📖 Writing the great adventure of ${character.name}...",
                    if (is3D) "🎬 Creating 3D animated scenes for each page..." else "🎨 Painting watercolor illustrations for each page...",
                    "✨ Tailoring the adventure for $childName ($childAge yrs)...",
                    "🖍️ Preparing The Creativity Loop drawing mission..."
                )
            } else {
                listOf(
                    "📖 Escribiendo el gran cuento de ${character.name}...",
                    if (is3D) "🎬 Creando las escenas 3D de cada página..." else "🎨 Pintando las acuarelas de cada página...",
                    "✨ Adaptando la aventura para $childName ($childAge años)...",
                    "🖍️ Preparando el reto de dibujo de The Creativity Loop..."
                )
            }
        }
    }

    val checklistSteps = remember(AppStrings.isEnglish, character.name, is3D) {
        if (AppStrings.isEnglish) {
            listOf(
                "Looking at your drawing",
                "Bringing ${character.name} to life",
                "Writing the story pages",
                if (is3D) "Creating 3D scenes" else "Painting the watercolors"
            )
        } else {
            listOf(
                "Mirando tu dibujo",
                "Dando vida a ${character.name}",
                "Escribiendo la historia",
                "Pintando las ilustraciones"
            )
        }
    }

    var phraseIndex by remember { mutableStateOf(0) }
    var activeStepIndex by remember { mutableStateOf(2) } // Step 0 & 1 already completed, Step 2 active initially

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "story_gen_anim")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    val progressPulse by infiniteTransition.animateFloat(
        initialValue = 0.58f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress_pulse"
    )

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    var hasRequestedStory by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Intercept back button to prevent accidental exit during generation
    PlatformBackHandler(enabled = true) {
        showCancelConfirmDialog = true
    }

    // Cycle phrases & checklist progress
    LaunchedEffect(retryTrigger) {
        activeStepIndex = 2
        while (true) {
            delay(2400)
            phraseIndex = (phraseIndex + 1) % phrases.size
            if (activeStepIndex < 3 && phraseIndex >= 1) {
                activeStepIndex = 3
            }
        }
    }

    // Call API to generate story (guarded against duplicate executions)
    LaunchedEffect(character, retryTrigger) {
        if (hasRequestedStory && retryTrigger == 0) return@LaunchedEffect
        hasRequestedStory = true
        errorMessage = null
        try {
            val (success, compactStory) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val response = api.generateStory(
                    character = character,
                    childName = childName,
                    childAge = childAge,
                    theme = null,
                    language = language,
                    continuation = continuation,
                    artStyle = artStyle
                )
                if (response.success) {
                    val externalized = org.wonderloop.app.data.StoryRepository.externalizeStoryMedia(response.story)
                    true to externalized
                } else {
                    false to null
                }
            }
            if (success && compactStory != null) {
                delay(800)
                onStoryGenerated(compactStory)
            } else {
                errorMessage = if (AppStrings.isEnglish) {
                    "Could not complete the story at this time."
                } else {
                    "No pudimos completar la historia en este momento."
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: org.wonderloop.app.data.BackendOfflineException) {
            println("⚠️ [StoryGeneratingScreen] Backend unreachable: ${e.message}")
            errorMessage = if (AppStrings.isEnglish) {
                "Could not connect to the magic server to finish ${character.name}'s story. Check your connection and tap Retry."
            } else {
                "No hemos podido conectar con el servidor para terminar el cuento de ${character.name}. Comprueba tu conexión y pulsa Reintentar."
            }
        } catch (e: Throwable) {
            println("Error generating story: $e")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                api.checkHealth()
            }
            errorMessage = if (AppStrings.isEnglish) {
                "The magic wand needs a quick breather to finish ${character.name}'s story. Check your Internet connection and try again."
            } else {
                "La varita necesita un pequeño respiro para terminar el cuento de ${character.name}. Comprueba tu conexión y vuelve a intentarlo."
            }
        }
    }

    // Cancel Confirmation Dialog ("¿Cancelar cuento?")
    if (showCancelConfirmDialog) {
        Dialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, GenCardBorder),
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
                    // Top Circular Blush Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GenCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStrings.cancelStoryQuestion,
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GenInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.cancelStoryWarning(character.name),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GenInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary: Keep Waiting
                    Button(
                        onClick = { showCancelConfirmDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GenCoral,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "${AppStrings.keepWaiting} ✨",
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary: Exit to Home
                    OutlinedButton(
                        onClick = {
                            showCancelConfirmDialog = false
                            onError(if (AppStrings.isEnglish) "Cancelled by user" else "Cancelado por el usuario")
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, GenCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = GenInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.exitToHome,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GenInkMuted
                        )
                    }
                }
            }
        }
    }

    // Magic Pause Dialog ("Pausa Mágica")
    if (errorMessage != null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, GenCardBorder),
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
                    // Top Circular Soft Gold Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GenGoldSoft),
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
                        color = GenInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: (if (AppStrings.isEnglish) {
                            "An unexpected pause occurred while creating your story."
                        } else {
                            "Ocurrió una pequeña pausa mientras creábamos el cuento."
                        }),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GenInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reassuring pill that the drawing is safe
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GenMintSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) {
                                "✨ Your drawing of ${character.name} is safe — no need to scan again"
                            } else {
                                "✨ El dibujo de ${character.name} está guardado, no tienes que repetirlo"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenMint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary: Retry
                    Button(
                        onClick = {
                            errorMessage = null
                            retryTrigger++
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GenCoral,
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

                    // Secondary: Back to Home
                    OutlinedButton(
                        onClick = {
                            onError(errorMessage ?: (if (AppStrings.isEnglish) "Cancelled" else "Cancelado"))
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, GenCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = GenInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.backToHome,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GenInkMuted
                        )
                    }
                }
            }
        }
    }

    // Main Screen Content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GenCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        GenGoldSoft.copy(alpha = 0.6f),
                        GenCoralSoft.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    radius = 850f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top step pill
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = GenCoralSoft,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Text(
                    text = if (AppStrings.isEnglish) {
                        "📖 STORY WORKSHOP · STEP 2 OF 2"
                    } else {
                        "📖 TALLER DE CUENTOS · PASO 2 DE 2"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GenCoral,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Central Enchanted Storybook & Hero Showcase (no CSS wizard)
            val heroDisplayImage = character.reinterpretedImageUrl ?: character.originalDrawingBase64

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = floatY.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft outer golden-blush halo
                Box(
                    modifier = Modifier
                        .size(182.dp)
                        .scale(badgePulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GenGoldSoft.copy(alpha = 0.85f),
                                    GenCoralSoft.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Hero Storybook Cover Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    border = BorderStroke(3.dp, Color.White),
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(-2f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF6DE),
                                        Color(0xFFFFE8EC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!heroDisplayImage.isNullOrBlank()) {
                            WonderLoopImage(
                                model = heroDisplayImage,
                                contentDescription = character.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(text = "📚", fontSize = 56.sp)
                        }
                    }
                }

                // Floating Book & Brush Badge at Bottom-Right
                Surface(
                    shape = CircleShape,
                    color = GenCoral,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.5.dp, Color.White),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-6).dp)
                        .size(50.dp)
                        .scale(badgePulse)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📖", fontSize = 24.sp)
                    }
                }

                // Floating 4-point sparkles
                StorySparkleStar(
                    color = GenGoldAccent,
                    size = 24.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 8.dp)
                        .scale(sparkleScale)
                )
                StorySparkleStar(
                    color = GenCoral,
                    size = 18.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 14.dp)
                        .scale(1.8f - sparkleScale * 0.6f)
                )
                StorySparkleStar(
                    color = GenMint,
                    size = 18.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 12.dp, y = (-14).dp)
                        .scale(sparkleScale * 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title
            Text(
                text = AppStrings.creatingDigitalBook,
                fontFamily = TitleFont(),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = GenInkDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Animated current status subtitle
            AnimatedContent(
                targetState = phrases[phraseIndex],
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                label = "story_phrase_anim"
            ) { currentPhrase ->
                Text(
                    text = currentPhrase,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenInkMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coral Progress Bar (.pr)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(GenCardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressPulse)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GenCoral, Color(0xFFFF8A65))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4-Step Live Checklist Card (.sl)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.5.dp, GenCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    checklistSteps.forEachIndexed { index, stepText ->
                        val isDone = index < activeStepIndex
                        val isActive = index == activeStepIndex

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Step Icon Circle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> GenMintSoft
                                            isActive -> GenCoralSoft
                                            else -> Color(0xFFF3EFE8)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isDone -> "✓"
                                        isActive -> "✨"
                                        else -> "${index + 1}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        isDone -> GenMint
                                        isActive -> GenCoral
                                        else -> GenInkMuted
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = stepText,
                                fontSize = 13.sp,
                                fontWeight = when {
                                    isActive -> FontWeight.ExtraBold
                                    isDone -> FontWeight.Bold
                                    else -> FontWeight.SemiBold
                                },
                                color = when {
                                    isActive -> GenCoral
                                    isDone -> GenInkDark
                                    else -> GenInkMuted.copy(alpha = 0.65f)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button at the bottom
            TextButton(
                onClick = { showCancelConfirmDialog = true }
            ) {
                Text(
                    text = if (AppStrings.isEnglish) "Cancel" else "Cancelar",
                    fontFamily = TitleFont(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GenInkMuted
                )
            }
        }
    }
}

@Composable
private fun StorySparkleStar(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h)
            quadraticTo(w * 0.5f, h * 0.5f, 0f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, 0f)
            close()
        }
        drawPath(path = path, color = color)
    }
}
