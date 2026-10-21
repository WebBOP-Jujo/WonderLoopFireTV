package org.wonderloop.app.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.DPadArrows
import org.wonderloop.app.ui.components.QrCodeView
import org.wonderloop.app.ui.components.WonderBackdrop
import org.wonderloop.app.ui.tv.TvClickableSurface

private val TutInk = Color(0xFF2B2340)
private val TutInkMuted = Color(0xFF6B6480)
private val TutCoral = Color(0xFFD9384A)
private val TutCoralSoft = Color(0xFFFDECE9)
private val TutMint = Color(0xFF0E7F64)
private val TutMintBright = Color(0xFF1FBE9C)
private val TutGold = Color(0xFFFFD166)
private val TutGoldText = Color(0xFF9E6700)
private val TutViolet = Color(0xFF7C5CE0)
private val TutCardBorder = Color(0xFFEDE8E1)

private data class TutorialSlide(
    val icon: String,
    val shortLabel: String,
    val chipText: String,
    val title: String,
    val description: String,
    val accent: Color,
    val stageColors: List<Color>
)

/** The real crayon dog bundled for the "no phone" demo ("Pepe", see [DemoPepeAssets]): the tutorial shows the actual product. */
private val tutorialDrawing: String get() = DemoDrawings.items[1].base64DataUri

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val en = AppStrings.isEnglish
    val slides = remember(en) {
        listOf(
            TutorialSlide(
                icon = "🖍️",
                shortLabel = if (en) "Draw" else "Dibuja",
                chipText = AppStrings.onboardingStep1Badge,
                title = AppStrings.onboardingStep1Title,
                description = AppStrings.onboardingStep1Desc,
                accent = Color(0xFFF2B630),
                stageColors = listOf(Color(0xFFFFF6DE), Color(0xFFFFE3A3))
            ),
            TutorialSlide(
                icon = "📱",
                shortLabel = if (en) "Scan" else "Escanea",
                chipText = AppStrings.onboardingStep2Badge,
                title = AppStrings.onboardingStep2Title,
                description = AppStrings.onboardingStep2Desc,
                accent = TutCoral,
                stageColors = listOf(Color(0xFFFFEDEA), Color(0xFFFFC9C1))
            ),
            TutorialSlide(
                icon = "📖",
                shortLabel = if (en) "Listen" else "Escucha",
                chipText = AppStrings.onboardingStep3Badge,
                title = AppStrings.onboardingStep3Title,
                description = AppStrings.onboardingStep3Desc,
                accent = TutMint,
                stageColors = listOf(Color(0xFFE6F8F2), Color(0xFFB3E6D6))
            ),
            TutorialSlide(
                icon = "🌍",
                shortLabel = if (en) "Languages" else "Idiomas",
                chipText = AppStrings.onboardingStep4Badge,
                title = AppStrings.onboardingStep4Title,
                description = AppStrings.onboardingStep4Desc,
                accent = TutViolet,
                stageColors = listOf(Color(0xFFF0ECFF), Color(0xFFD5CBF5))
            ),
            TutorialSlide(
                icon = "🌱",
                shortLabel = if (en) "Create" else "Crea",
                chipText = AppStrings.onboardingStep5Badge,
                title = AppStrings.onboardingStep5Title,
                description = AppStrings.onboardingStep5Desc,
                accent = TutCoral,
                stageColors = listOf(Color(0xFFFFF0F3), Color(0xFFE6DBF5))
            )
        )
    }

    var currentPage by remember { mutableStateOf(0) }
    val isLastPage = currentPage == slides.size - 1

    fun goNext() {
        if (currentPage < slides.size - 1) {
            PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
            currentPage += 1
        }
    }

    fun goPrev() {
        if (currentPage > 0) {
            PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
            currentPage -= 1
        }
    }

    val nextBtnFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(120)
        try {
            nextBtnFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    PlatformBackHandler(enabled = true) {
        if (currentPage > 0) goPrev() else onFinish()
    }

    WonderBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV safe zone
                .onPreviewKeyEvent {
                    // ◀ ▶ always change step, wherever the focus is
                    when (it.key) {
                        Key.DirectionLeft -> {
                            if (it.type == KeyEventType.KeyDown) goPrev()
                            true
                        }
                        Key.DirectionRight -> {
                            if (it.type == KeyEventType.KeyDown) goNext()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // =================================================================
            // 1. TOP BAR: brand · 5-step journey · skip
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(190.dp)) {
                    Text(
                        text = if (en) "✨ WELCOME TO" else "✨ BIENVENIDO A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = TutGoldText
                    )
                    Row {
                        Text(text = "Wonder", fontFamily = TitleFont(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TutCoral)
                        Text(text = "Loop", fontFamily = TitleFont(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TutMint)
                    }
                }

                TutorialJourney(
                    slides = slides,
                    currentIndex = currentPage,
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) {
                    if (!isLastPage) {
                        TvClickableSurface(
                            onClick = onFinish,
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(50),
                            containerColor = Color.White,
                            borderColor = TutCardBorder,
                            contentDescription = if (en) "Skip tutorial" else "Saltar tutorial"
                        ) {
                            Text(
                                text = if (en) "Skip  ✕" else "Saltar  ✕",
                                fontFamily = TitleFont(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TutInkMuted,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 2. SLIDE: animated stage (left) · story of the step (right)
            // =================================================================
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> (width * 0.15f).toInt() } + fadeIn(tween(260)))
                            .togetherWith(slideOutHorizontally { width -> (-width * 0.15f).toInt() } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { width -> (-width * 0.15f).toInt() } + fadeIn(tween(260)))
                            .togetherWith(slideOutHorizontally { width -> (width * 0.15f).toInt() } + fadeOut(tween(180)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "tutorial_slide"
            ) { pageIndex ->
                val slide = slides[pageIndex]

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: illustration stage
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .shadow(10.dp, RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp))
                            .background(Brush.linearGradient(slide.stageColors))
                            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
                    ) {
                        FitStage {
                            when (pageIndex) {
                                0 -> StageStep1DrawOnPaper()
                                1 -> StageStep2ScanToTv()
                                2 -> StageStep3Storybook()
                                3 -> StageStep4Bilingual(TutCoral, TutMintBright, TutInkMuted)
                                4 -> StageStep5CreativityLoop()
                            }
                        }
                    }

                    // RIGHT: the step, in 10-foot type
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.94f))
                            .border(2.dp, slide.accent.copy(alpha = 0.35f), RoundedCornerShape(32.dp))
                            .padding(horizontal = 28.dp, vertical = 22.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(shape = RoundedCornerShape(50), color = slide.accent.copy(alpha = 0.12f)) {
                                Text(
                                    text = slide.chipText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (slide.accent == Color(0xFFF2B630)) TutGoldText else slide.accent,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = slide.title,
                                fontFamily = TitleFont(),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = TutInk,
                                lineHeight = 35.sp,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = slide.description,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TutInkMuted,
                                lineHeight = 25.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pageIndex < slides.size - 1) Text(
                                text = "${pageIndex + 1} / ${slides.size}",
                                fontFamily = TitleFont(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TutInkMuted
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (pageIndex > 0) {
                                TvClickableSurface(
                                    onClick = { goPrev() },
                                    modifier = Modifier.height(52.dp),
                                    shape = RoundedCornerShape(50),
                                    containerColor = Color.White,
                                    borderColor = TutCardBorder,
                                    playClickSound = false,
                                    contentDescription = if (en) "Previous step" else "Paso anterior"
                                ) {
                                    Text(
                                        text = "←",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TutInkMuted,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            TvClickableSurface(
                                onClick = {
                                    if (pageIndex < slides.size - 1) {
                                        goNext()
                                    } else {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        onFinish()
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .focusRequester(nextBtnFocusRequester),
                                shape = RoundedCornerShape(50),
                                containerColor = TutCoral,
                                focusedContainerColor = TutCoral,
                                contentColor = Color.White,
                                glowColor = TutCoral.copy(alpha = 0.4f),
                                playClickSound = false,
                                contentDescription = if (pageIndex < slides.size - 1) {
                                    if (en) "Next step" else "Siguiente paso"
                                } else {
                                    AppStrings.startMyAdventure
                                }
                            ) {
                                Text(
                                    text = if (pageIndex < slides.size - 1) {
                                        if (en) "Next  →" else "Siguiente  →"
                                    } else {
                                        AppStrings.startMyAdventure
                                    },
                                    fontFamily = TitleFont(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 26.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =================================================================
            // 3. REMOTE HINTS
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TutorialHint(label = null, text = if (en) "Change step" else "Cambiar paso")
                Spacer(modifier = Modifier.width(28.dp))
                TutorialHint(label = "OK", text = if (en) "Continue" else "Continuar")
                Spacer(modifier = Modifier.width(28.dp))
                TutorialHint(label = "↩", text = if (en) "Back" else "Volver")
            }
        }
    }
}

/** Five icons joined by a line: where the child is in the journey, and what comes next. */
@Composable
private fun TutorialJourney(slides: List<TutorialSlide>, currentIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        slides.forEachIndexed { i, slide ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .width(16.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= currentIndex) TutMintBright else TutCardBorder)
                )
            }
            val active = i == currentIndex
            val done = i < currentIndex
            val scale by animateFloatAsState(if (active) 1.12f else 1f, tween(250), label = "journey_scale")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(scale)
                        .shadow(if (active) 6.dp else 0.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> Color.White
                                done -> TutMintBright
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                        )
                        .border(3.dp, if (active) slide.accent else Color.Transparent, CircleShape)
                ) {
                    Text(
                        text = if (done) "✓" else slide.icon,
                        fontSize = if (done) 18.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.alpha(if (active || done) 1f else 0.55f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slide.shortLabel,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (active) TutInk else TutInkMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TutorialHint(label: String?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, TutCardBorder)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                if (label == null) {
                    DPadArrows(color = TutInk)
                } else {
                    Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TutInk)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TutInkMuted)
    }
}

/** Stages are designed on a fixed 440x360 dp canvas and scaled down to fit their card, so nothing is ever cut. */
@Composable
private fun FitStage(content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth / 440.dp, maxHeight / 360.dp, 1.2f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(440.dp, 360.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            content = content
        )
    }
}

/** A sheet of paper holding the child's real crayon drawing. */
@Composable
private fun PaperWithDrawing(modifier: Modifier = Modifier, rotation: Float = -5f, drawingAlpha: Float = 1f) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier.rotate(rotation)
    ) {
        WonderLoopImage(
            model = tutorialDrawing,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize()
                .alpha(drawingAlpha)
        )
    }
}

// =========================================================================
// STAGE 1: a real crayon drawing on paper, crayons dancing around it
// =========================================================================
@Composable
private fun StageStep1DrawOnPaper() {
    val transition = rememberInfiniteTransition(label = "step1")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val reveal by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reveal"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        PaperWithDrawing(
            modifier = Modifier.size(width = 210.dp, height = 240.dp),
            drawingAlpha = reveal
        )
        Text(text = "🖍️", fontSize = 48.sp, modifier = Modifier.offset(x = 145.dp, y = (85 + bob).dp).rotate(-20f))
        Text(text = "🖍️", fontSize = 40.sp, modifier = Modifier.offset(x = (-150).dp, y = (-95 - bob).dp).rotate(30f))
        Text(text = "✏️", fontSize = 36.sp, modifier = Modifier.offset(x = (-150).dp, y = (105 + bob).dp))
        Text(text = "✨", fontSize = 30.sp, modifier = Modifier.offset(x = 145.dp, y = (-120).dp).scale(reveal))
    }
}

// =========================================================================
// STAGE 2: a phone scans the TV's QR code, then photographs the drawing
// =========================================================================
@Composable
private fun StageStep2ScanToTv() {
    val transition = rememberInfiniteTransition(label = "step2")
    val flash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "flash"
    )
    val float by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // The TV, showing the QR code
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = (-70).dp, y = (-50).dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 250.dp, height = 160.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(TutInk)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFFBF5))
            ) {
                QrCodeView(
                    content = "https://wonderloop.tv/upload/DEMO",
                    modifier = Modifier.size(110.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 10.dp)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(TutInk.copy(alpha = 0.8f))
            )
        }

        // The phone in front, photographing the drawing
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = 125.dp, y = (55 + float).dp)
                .rotate(8f)
                .size(width = 125.dp, height = 220.dp)
                .shadow(14.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(TutInk)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            WonderLoopImage(
                model = tutorialDrawing,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize()
            )
            // Viewfinder corners
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val l = 22.dp.toPx()
                val sw = 4.dp.toPx()
                val c = TutCoral
                listOf(
                    Offset(0f, 0f) to Offset(1f, 1f),
                    Offset(size.width, 0f) to Offset(-1f, 1f),
                    Offset(0f, size.height) to Offset(1f, -1f),
                    Offset(size.width, size.height) to Offset(-1f, -1f)
                ).forEach { (p, d) ->
                    drawLine(c, p, Offset(p.x + d.x * l, p.y), sw, StrokeCap.Round)
                    drawLine(c, p, Offset(p.x, p.y + d.y * l), sw, StrokeCap.Round)
                }
            }
            // Camera flash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = if (flash < 0.12f) 0.85f * (1f - flash / 0.12f) else 0f))
            )
        }

        Surface(
            shape = CircleShape,
            color = TutCoral,
            shadowElevation = 6.dp,
            border = BorderStroke(3.dp, Color.White),
            modifier = Modifier
                .offset(x = 185.dp, y = 140.dp)
                .size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "📸", fontSize = 22.sp)
            }
        }
    }
}

// =========================================================================
// STAGE 3: the cinema reader in miniature (illustration + subtitle + voice)
// =========================================================================
@Composable
private fun StageStep3Storybook() {
    val en = AppStrings.isEnglish
    val words = remember(en) {
        if (en) listOf("Pepe", "found", "a", "branch", "full", "of", "flowers.") else listOf("Pepe", "encontró", "una", "rama", "llena", "de", "flores.")
    }
    val transition = rememberInfiniteTransition(label = "step3")
    val wordClock by transition.animateFloat(
        initialValue = 0f,
        targetValue = words.size.toFloat() + 1.5f,
        animationSpec = infiniteRepeatable(tween(words.size * 480 + 700, easing = LinearEasing), RepeatMode.Restart),
        label = "words"
    )
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val activeWord = wordClock.toInt()

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(width = 380.dp, height = 280.dp)
                .rotate(-2f)
                .shadow(14.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(TutInk)
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            WonderLoopImage(
                model = DemoPepeAssets.STORY_SCENE_BASE64,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
            )
            Text(
                text = buildAnnotatedString {
                    words.forEachIndexed { i, w ->
                        if (i > 0) append(" ")
                        if (i == activeWord) withStyle(SpanStyle(color = TutGold)) { append(w) } else append(w)
                    }
                },
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 8f)),
                fontSize = 21.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        // Narrator badge with sound rings
        Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(x = 180.dp, y = (-130).dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(1f + ring * 0.6f)
                    .alpha(1f - ring)
                    .border(3.dp, TutMint, CircleShape)
            )
            Surface(shape = CircleShape, color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🔊", fontSize = 28.sp)
                }
            }
        }
    }
}

// =========================================================================
// STAGE 4: Two Languages (.langwrap ES 🔁 EN + floating bilingual words)
// =========================================================================
@Composable
private fun StageStep4Bilingual(
    brandRed: Color,
    brandMint: Color,
    mutedInk: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step4")
    val esScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "esScale"
    )
    val enScale by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "enScale"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wordFloat"
    )

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val w = maxWidth
        val h = maxHeight

        // Floating bilingual word chips (.fw)
        FloatingWordPill("Hola", brandRed, Modifier.align(Alignment.TopStart).offset(x = w * 0.10f, y = h * 0.09f + floatOffset.dp))
        FloatingWordPill("Hello", brandMint, Modifier.align(Alignment.TopEnd).offset(x = -(w * 0.10f), y = h * 0.11f - floatOffset.dp))
        FloatingWordPill("Dibuja", brandRed, Modifier.align(Alignment.TopCenter).offset(y = h * 0.02f - floatOffset.dp))
        FloatingWordPill("Dragon", brandMint, Modifier.align(Alignment.BottomCenter).offset(x = 20.dp, y = -(h * 0.02f) + floatOffset.dp))
        FloatingWordPill("Amigos", Color(0xFF6A4FC7), Modifier.align(Alignment.BottomStart).offset(x = w * 0.04f, y = -(h * 0.19f) + floatOffset.dp))
        FloatingWordPill("Friend", brandMint, Modifier.align(Alignment.BottomEnd).offset(x = -(w * 0.09f), y = -(h * 0.24f) - floatOffset.dp))
        FloatingWordPill("Dragón", Color(0xFFB8860B), Modifier.align(Alignment.BottomStart).offset(x = w * 0.10f, y = -(h * 0.05f) - floatOffset.dp))
        FloatingWordPill("Draw", Color(0xFFB8860B), Modifier.align(Alignment.BottomEnd).offset(x = -(w * 0.05f), y = -(h * 0.08f) + floatOffset.dp))

        // Center ES 🔁 EN Pill (.langwrap)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(esScale)
                        .clip(CircleShape)
                        .background(brandRed)
                ) {
                    Text(
                        text = "ES",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "🔁",
                    fontSize = 30.sp,
                    color = mutedInk
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(enScale)
                        .clip(CircleShape)
                        .background(brandMint)
                ) {
                    Text(
                        text = "EN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingWordPill(
    word: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Text(
            text = word,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

// =========================================================================
// STAGE 5: the Creativity Loop: draw on paper → story on the TV → new drawing mission
// =========================================================================
@Composable
private fun StageStep5CreativityLoop() {
    val en = AppStrings.isEnglish
    val transition = rememberInfiniteTransition(label = "step5")
    val loopRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "loopRotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "nodePulse"
    )
    val activeNode = pulse.toInt()

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Rotating dashed ring that connects the three moments
        Canvas(modifier = Modifier.size(270.dp).rotate(loopRotation)) {
            drawCircle(
                color = TutCoral.copy(alpha = 0.45f),
                radius = size.minDimension / 2f - 6.dp.toPx(),
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 14.dp.toPx()))
                )
            )
        }

        Text(
            text = if (en) "♾️\nThe\nloop" else "♾️\nEl\nciclo",
            fontFamily = TitleFont(),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TutInkMuted,
            textAlign = TextAlign.Center
        )

        LoopNode(
            label = if (en) "Draw" else "Dibuja",
            active = activeNode == 0,
            modifier = Modifier.offset(x = 0.dp, y = (-112).dp)
        ) {
            WonderLoopImage(model = tutorialDrawing, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
        }
        LoopNode(
            label = if (en) "Story" else "Cuento",
            active = activeNode == 1,
            modifier = Modifier.offset(x = 140.dp, y = 75.dp)
        ) {
            WonderLoopImage(model = DemoPepeAssets.JUMP_SCENE_BASE64, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        LoopNode(
            label = if (en) "New mission" else "Nueva misión",
            active = activeNode == 2,
            modifier = Modifier.offset(x = (-140).dp, y = 75.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color(0xFFFFF6DE))) {
                Text(text = "🖍️", fontSize = 38.sp)
            }
        }
    }
}

@Composable
private fun LoopNode(label: String, active: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(if (active) 1.12f else 1f, tween(400), label = "loop_node")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(if (active) 12.dp else 6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(4.dp, if (active) TutGold else Color.White, RoundedCornerShape(24.dp))
                .padding(4.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(50), color = if (active) TutCoral else Color.White, shadowElevation = 3.dp) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (active) Color.White else TutInk,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}
