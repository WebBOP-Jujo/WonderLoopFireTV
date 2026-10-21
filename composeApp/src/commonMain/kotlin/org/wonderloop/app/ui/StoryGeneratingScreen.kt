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
import androidx.compose.ui.draw.drawBehind
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
import kotlinx.coroutines.launch
import org.wonderloop.app.ui.components.WonderBackdrop
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
    org.wonderloop.app.ui.tv.KeepScreenOn()

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

    // Steps, subtitle and progress follow the real pipeline: story text (~6 s), then illustrations + voice
    val storyProgress = remember { Animatable(0.08f) }
    LaunchedEffect(retryTrigger) {
        activeStepIndex = 2
        phraseIndex = 0
        storyProgress.snapTo(0.08f)
        launch { storyProgress.animateTo(0.5f, tween(6500, easing = LinearEasing)) }
        delay(6500)
        activeStepIndex = 3
        phraseIndex = 1
        launch { storyProgress.animateTo(0.94f, tween(14000, easing = LinearOutSlowInEasing)) }
        while (true) {
            delay(3500)
            phraseIndex = if (phraseIndex >= phrases.lastIndex) 1 else phraseIndex + 1
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
                activeStepIndex = checklistSteps.size
                storyProgress.animateTo(1f, tween(450))
                delay(500)
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

    // Main Screen Content: hero polaroid on the left, live progress on the right (16:9, 48/24dp safe zone)
    val heroDisplayImage = character.reinterpretedImageUrl ?: character.originalDrawingBase64
    val en = AppStrings.isEnglish
    val waitingTips = remember(en, character.name) {
        if (en) {
            listOf(
                "💡 Meanwhile: which friend could help ${character.name} in chapter 2?",
                "🖍️ Keep paper and crayons handy: a drawing challenge is coming!",
                "🔊 The story will read itself aloud with a narrator voice",
                "📺 Use ◀ ▶ on the remote to turn the pages"
            )
        } else {
            listOf(
                "💡 Mientras tanto: ¿qué amigo podría ayudar a ${character.name} en el capítulo 2?",
                "🖍️ Ten a mano papel y ceras: ¡al final habrá un reto de dibujo!",
                "🔊 El cuento se leerá solo, con voz de narrador",
                "📺 Usa ◀ ▶ del mando para pasar las páginas"
            )
        }
    }
    var tipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(waitingTips) {
        while (true) {
            delay(4500)
            tipIndex = (tipIndex + 1) % waitingTips.size
        }
    }

    WonderBackdrop {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ---------------------------------------------------------
            // LEFT: the hero, floating on a glowing polaroid
            // ---------------------------------------------------------
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .offset(y = floatY.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .scale(badgePulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GenGoldSoft.copy(alpha = 0.9f),
                                        GenCoralSoft.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(width = 214.dp, height = 238.dp)
                            .rotate(-3f)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 36.dp)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFFFF6DE), Color(0xFFFFE8EC)))),
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
                                Text(text = "📚", fontSize = 64.sp)
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = GenCoral,
                        shadowElevation = 6.dp,
                        border = BorderStroke(3.dp, Color.White),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-14).dp, y = (-18).dp)
                            .size(62.dp)
                            .scale(badgePulse)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "📖", fontSize = 30.sp)
                        }
                    }

                    StorySparkleStar(
                        color = GenGoldAccent,
                        size = 30.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 14.dp, y = 14.dp)
                            .scale(sparkleScale)
                    )
                    StorySparkleStar(
                        color = GenCoral,
                        size = 22.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = 24.dp)
                            .scale(1.8f - sparkleScale * 0.6f)
                    )
                    StorySparkleStar(
                        color = GenMint,
                        size = 22.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 18.dp, y = (-24).dp)
                            .scale(sparkleScale * 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 3.dp) {
                    Text(
                        text = "✨ ${character.name}",
                        fontFamily = TitleFont(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenInkDark,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                }
            }

            // ---------------------------------------------------------
            // RIGHT: what the magic is doing right now
            // ---------------------------------------------------------
            Column(modifier = Modifier.weight(0.6f)) {
                Surface(shape = RoundedCornerShape(50.dp), color = GenCoralSoft) {
                    Text(
                        text = if (en) "📖 STORY WORKSHOP" else "📖 TALLER DE CUENTOS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GenCoral,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = AppStrings.creatingDigitalBook,
                    fontFamily = TitleFont(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenInkDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = phrases[phraseIndex],
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                    label = "story_phrase_anim"
                ) { currentPhrase ->
                    Text(
                        text = currentPhrase,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenInkMuted,
                        lineHeight = 25.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress that only moves forward
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White)
                        .border(1.dp, GenCardBorder, RoundedCornerShape(50.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(storyProgress.value)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Brush.horizontalGradient(listOf(GenCoral, Color(0xFFFFB347))))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Horizontal stepper
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    checklistSteps.forEachIndexed { index, stepText ->
                        val isDone = index < activeStepIndex
                        val isActive = index == activeStepIndex
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(if (isActive) badgePulse else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> GenMint
                                            isActive -> GenCoral
                                            else -> Color.White
                                        }
                                    )
                                    .border(2.dp, if (isDone || isActive) Color.White else GenCardBorder, CircleShape)
                            ) {
                                Text(
                                    text = when {
                                        isDone -> "✓"
                                        isActive -> "✨"
                                        else -> "${index + 1}"
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDone || isActive) Color.White else GenInkMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stepText,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                color = when {
                                    isActive -> GenCoral
                                    isDone -> GenInkDark
                                    else -> GenInkMuted.copy(alpha = 0.7f)
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Rotating tip while waiting
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.5.dp, GenCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = waitingTips[tipIndex],
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "waiting_tip"
                    ) { tip ->
                        Text(
                            text = tip,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenInkDark,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                org.wonderloop.app.ui.tv.TvClickableSurface(
                    onClick = { showCancelConfirmDialog = true },
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(50),
                    containerColor = Color.White,
                    borderColor = GenCardBorder,
                    contentDescription = if (en) "Cancel" else "Cancelar"
                ) {
                    Text(
                        text = if (en) "✕  Cancel" else "✕  Cancelar",
                        fontFamily = TitleFont(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GenInkMuted,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    )
                }
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
