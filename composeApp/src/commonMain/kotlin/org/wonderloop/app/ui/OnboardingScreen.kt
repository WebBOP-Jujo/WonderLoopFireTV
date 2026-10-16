package org.wonderloop.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.DPadArrows
import org.wonderloop.app.ui.components.tvFocusable

private data class TutorialSlide(
    val stepIndex: Int,
    val chipText: String,
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val BrandRed = Color(0xFFD9384A)
    val BrandMint = Color(0xFF1FBE9C)
    val DeepInk = Color(0xFF2B2340)
    val MutedInk = Color(0xFF6B6480)

    val slides = remember(AppStrings.isEnglish) {
        listOf(
            TutorialSlide(
                stepIndex = 0,
                chipText = AppStrings.onboardingStep1Badge,
                title = AppStrings.onboardingStep1Title,
                description = AppStrings.onboardingStep1Desc,
                gradientColors = listOf(Color(0xFFFFF3D6), Color(0xFFFFE0AA))
            ),
            TutorialSlide(
                stepIndex = 1,
                chipText = AppStrings.onboardingStep2Badge,
                title = AppStrings.onboardingStep2Title,
                description = AppStrings.onboardingStep2Desc,
                gradientColors = listOf(Color(0xFFFFD9D3), Color(0xFFFF9E93))
            ),
            TutorialSlide(
                stepIndex = 2,
                chipText = AppStrings.onboardingStep3Badge,
                title = AppStrings.onboardingStep3Title,
                description = AppStrings.onboardingStep3Desc,
                gradientColors = listOf(Color(0xFFDFF6EE), Color(0xFFA9E1D0))
            ),
            TutorialSlide(
                stepIndex = 3,
                chipText = AppStrings.onboardingStep4Badge,
                title = AppStrings.onboardingStep4Title,
                description = AppStrings.onboardingStep4Desc,
                gradientColors = listOf(Color(0xFFE7EEFF), Color(0xFFCFC4F0))
            ),
            TutorialSlide(
                stepIndex = 4,
                chipText = AppStrings.onboardingStep5Badge,
                title = AppStrings.onboardingStep5Title,
                description = AppStrings.onboardingStep5Desc,
                gradientColors = listOf(Color(0xFFFBE7EC), Color(0xFFE1D6F0))
            )
        )
    }

    var currentPage by remember { mutableStateOf(0) }
    val isLastPage = currentPage == slides.size - 1

    val nextBtnFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(120)
        try {
            nextBtnFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    PlatformBackHandler(enabled = true) {
        if (currentPage > 0) {
            currentPage -= 1
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV 5% Safe Zone
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp) {
                    when (it.key) {
                        Key.DirectionLeft -> {
                            if (currentPage > 0) {
                                currentPage -= 1
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (currentPage < slides.size - 1) {
                                currentPage += 1
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // 1. TOP BAR: [ ← Anterior ]   Badge   [ Saltar tutorial ✕ ]
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Previous button (or spacer)
                if (currentPage > 0) {
                    val onGoPrev = {
                        currentPage -= 1
                    }
                    Surface(
                        onClick = { onGoPrev() },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFEDE8E1)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(42.dp)
                            .tvFocusable(shape = RoundedCornerShape(24.dp), focusedScale = 1.06f, onClick = { onGoPrev() })
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(text = "←", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DeepInk)
                            Text(
                                text = if (AppStrings.isEnglish) "Previous" else "Anterior",
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepInk
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(100.dp))
                }

                // Center: Welcome Tutorial Badge
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFFFF1CF),
                    border = BorderStroke(1.dp, Color(0xFFF3DC9B))
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "✨ WELCOME TUTORIAL ✨" else "✨ TUTORIAL DE BIENVENIDA ✨",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8A5A00),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Right: Skip button (hidden on last page)
                if (!isLastPage) {
                    Surface(
                        onClick = onFinish,
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFEDE8E1)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(42.dp)
                            .tvFocusable(shape = RoundedCornerShape(24.dp), focusedScale = 1.06f, onClick = onFinish)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "Skip tutorial" else "Saltar tutorial",
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedInk
                            )
                            Text(text = "✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedInk)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(100.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // 2. MAIN WIDESCREEN CAROUSEL (Left Stage · Right Narrative Card)
            // =================================================================
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> (width * 0.2f).toInt() } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally { width -> (-width * 0.2f).toInt() } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { width -> (-width * 0.2f).toInt() } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally { width -> (width * 0.2f).toInt() } + fadeOut(tween(180)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val slide = slides[pageIndex]

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN (44% width): Animated Illustration Stage
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = slide.gradientColors,
                                        radius = 900f
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            when (pageIndex) {
                                0 -> StageStep1DrawOnPaper()
                                1 -> StageStep2SnapPhoto(BrandRed)
                                2 -> StageStep3Storybook(BrandMint)
                                3 -> StageStep4Bilingual(BrandRed, BrandMint, MutedInk)
                                4 -> StageStep5CreativityLoop(BrandRed, BrandMint)
                            }
                        }
                    }

                    // RIGHT COLUMN (56% width): Information & Interactive Controls
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFEDE8E1)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                // Step Chip (.chip)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFCEAE7)
                                ) {
                                    Text(
                                        text = slide.chipText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandRed,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Headline (h2)
                                Text(
                                    text = slide.title,
                                    fontFamily = TitleFont(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepInk,
                                    lineHeight = 33.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Description (p)
                                Text(
                                    text = slide.description,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedInk,
                                    lineHeight = 24.sp
                                )
                            }

                            // Footer: Dots Indicator + Next / Finish Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TutorialDotsIndicator(
                                    totalDots = slides.size,
                                    currentIndex = pageIndex,
                                    activeColor = BrandRed
                                )

                                if (pageIndex < slides.size - 1) {
                                    // Siguiente paso
                                    Surface(
                                        onClick = {
                                            currentPage += 1
                                        },
                                        shape = RoundedCornerShape(50.dp),
                                        color = BrandRed,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .tvFocusable(
                                                shape = RoundedCornerShape(50.dp),
                                                focusedScale = 1.05f,
                                                focusRequester = nextBtnFocusRequester,
                                                onClick = {
                                                    currentPage += 1
                                                }
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        ) {
                                            Text(
                                                 text = if (AppStrings.isEnglish) "Next step" else "Siguiente paso",
                                                 fontFamily = TitleFont(),
                                                 fontSize = 15.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White
                                             )
                                            Text(text = "→", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        }
                                    }
                                } else {
                                    // Último paso: ¡Empezar mi aventura!
                                    Surface(
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                            onFinish()
                                        },
                                        shape = RoundedCornerShape(50.dp),
                                        color = BrandRed,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .tvFocusable(
                                                shape = RoundedCornerShape(50.dp),
                                                focusedScale = 1.05f,
                                                focusRequester = nextBtnFocusRequester,
                                                onClick = {
                                                    PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                                    onFinish()
                                                }
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 26.dp)
                                        ) {
                                            Text(
                                                text = "${AppStrings.startMyAdventure} ✨",
                                                fontFamily = TitleFont(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 3. BOTTOM HINT BAR: Symmetrical D-Pad arrows
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFEDE8E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            DPadArrows(color = DeepInk, width = 16.dp, height = 11.dp)
                            Text(
                                text = if (AppStrings.isEnglish) "Change step" else "Cambiar paso",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                        }

                        Text(text = "·", color = Color(0xFFEDE8E1), fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "OK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandRed)
                            Text(
                                text = if (AppStrings.isEnglish) "Continue" else "Continuar",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                        }

                        Text(text = "·", color = Color(0xFFEDE8E1), fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "↩", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedInk)
                            Text(
                                text = if (AppStrings.isEnglish) "Back / Exit" else "Volver / Salir",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialDotsIndicator(
    totalDots: Int,
    currentIndex: Int,
    activeColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDots) {
            val isActive = i == currentIndex
            val width by animateDpAsState(
                targetValue = if (isActive) 20.dp else 7.dp,
                animationSpec = tween(250),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) activeColor else Color(0xFFEADCE0),
                animationSpec = tween(250),
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

// =========================================================================
// STAGE 1: Draw on Paper (.paper1 + animated stroke + floating 🖍️ / 🖊️)
// =========================================================================
@Composable
private fun StageStep1DrawOnPaper() {
    val infiniteTransition = rememberInfiniteTransition(label = "step1")
    val drawProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drawProgress"
    )
    val crayonScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crayon1"
    )
    val crayonScale2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, delayMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crayon2"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Tilted Paper Sheet (.paper1)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .size(width = 148.dp, height = 176.dp)
                .rotate(-5f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val sx = size.width / 100f
                    val sy = size.height / 100f
                    val fullPath = Path().apply {
                        moveTo(15f * sx, 70f * sy)
                        cubicTo(20f * sx, 30f * sy, 45f * sx, 15f * sy, 55f * sx, 30f * sy)
                        cubicTo(65f * sx, 45f * sy, 40f * sx, 50f * sy, 45f * sx, 65f * sy)
                        cubicTo(50f * sx, 80f * sy, 75f * sx, 75f * sy, 80f * sx, 50f * sy)
                    }
                    val measure = PathMeasure()
                    measure.setPath(fullPath, false)
                    val partialPath = Path()
                    measure.getSegment(0f, measure.length * drawProgress, partialPath, true)

                    drawPath(
                        path = partialPath,
                        color = Color(0xFFFF6B6B),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Floating Crayon Top-Left (right next to the top-left corner of the drawing paper)
        Text(
            text = "🖍️",
            fontSize = 32.sp,
            modifier = Modifier
                .offset(x = (-92).dp, y = (-72).dp)
                .scale(crayonScale1)
        )

        // Floating Marker Bottom-Right (right next to the bottom-right corner of the drawing paper)
        Text(
            text = "🖊️",
            fontSize = 32.sp,
            modifier = Modifier
                .offset(x = 92.dp, y = 70.dp)
                .scale(crayonScale2)
        )
    }
}

// =========================================================================
// STAGE 2: Snap Photo (.flashring + .paper1 + .camb camera button)
// =========================================================================
@Composable
private fun StageStep2SnapPhoto(brandRed: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "step2")
    val flashScale by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flashScale"
    )
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flashAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Pulsing Flash Ring (.flashring)
        Box(
            modifier = Modifier
                .size(176.dp)
                .scale(flashScale)
                .alpha(flashAlpha)
                .border(6.dp, Color.White, RoundedCornerShape(28.dp))
        )

        // Tilted Paper Sheet (.paper1)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .size(width = 156.dp, height = 186.dp)
                .rotate(-5f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    val sx = size.width / 100f
                    val sy = size.height / 100f
                    val fullPath = Path().apply {
                        moveTo(15f * sx, 70f * sy)
                        cubicTo(20f * sx, 30f * sy, 45f * sx, 15f * sy, 55f * sx, 30f * sy)
                        cubicTo(65f * sx, 45f * sy, 40f * sx, 50f * sy, 45f * sx, 65f * sy)
                        cubicTo(50f * sx, 80f * sy, 75f * sx, 75f * sy, 80f * sx, 50f * sy)
                    }
                    drawPath(
                        path = fullPath,
                        color = Color(0xFFFF6B6B),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Camera Badge (.camb)
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset(x = 68.dp, y = 76.dp)
                .size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val w = size.width
                    val h = size.height
                    // Camera body rounded rect
                    drawRoundRect(
                        color = brandRed,
                        topLeft = Offset(w * 0.12f, h * 0.28f),
                        size = Size(w * 0.76f, h * 0.54f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                    // Top viewfinder bump
                    val topBump = Path().apply {
                        moveTo(w * 0.32f, h * 0.28f)
                        lineTo(w * 0.40f, h * 0.16f)
                        lineTo(w * 0.60f, h * 0.16f)
                        lineTo(w * 0.68f, h * 0.28f)
                    }
                    drawPath(
                        path = topBump,
                        color = brandRed,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Lens circle
                    drawCircle(
                        color = brandRed,
                        radius = w * 0.15f,
                        center = Offset(w * 0.5f, h * 0.55f),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
            }
        }
    }
}

// =========================================================================
// STAGE 3: Storybook & Voice (.book + turning page + .snd speaker waves)
// =========================================================================
@Composable
private fun StageStep3Storybook(brandMint: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "step3")
    val pageFlipAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pageFlipAngle"
    )
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Storybook (.book)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 160.dp, height = 120.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📖", fontSize = 48.sp)
                }
            }

            // Animated 3D turning page flap (.pg) anchored at center spine
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 7.dp, bottom = 7.dp, end = 7.dp)
                    .width(73.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = pageFlipAngle
                        cameraDistance = 10f * density
                    }
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFF3ECE1), Color.White)
                        )
                    )
            )
        }

        // Speaker Badge with pulsing soundwave ring (.snd)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(x = 74.dp, y = 68.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(ring1Scale)
                    .alpha(ring1Alpha)
                    .border(2.dp, brandMint, CircleShape)
            )
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val w = size.width
                        val h = size.height
                        val speakerPath = Path().apply {
                            moveTo(w * 0.16f, h * 0.38f)
                            lineTo(w * 0.16f, h * 0.62f)
                            lineTo(w * 0.34f, h * 0.62f)
                            lineTo(w * 0.56f, h * 0.80f)
                            lineTo(w * 0.56f, h * 0.20f)
                            lineTo(w * 0.34f, h * 0.38f)
                            close()
                        }
                        drawPath(
                            path = speakerPath,
                            color = Color(0xFF0E7F64),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawArc(
                            color = Color(0xFF0E7F64),
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(w * 0.48f, h * 0.28f),
                            size = Size(w * 0.34f, h * 0.44f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
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
        FloatingWordPill("Dibuja", brandRed, Modifier.align(Alignment.CenterStart).offset(x = w * 0.05f, y = (-24).dp - floatOffset.dp))
        FloatingWordPill("Dragon", brandMint, Modifier.align(Alignment.CenterEnd).offset(x = -(w * 0.05f), y = (-20).dp + floatOffset.dp))
        FloatingWordPill("Amigos", Color(0xFF6A4FC7), Modifier.align(Alignment.BottomStart).offset(x = w * 0.07f, y = -(h * 0.24f) + floatOffset.dp))
        FloatingWordPill("Friend", brandMint, Modifier.align(Alignment.BottomEnd).offset(x = -(w * 0.09f), y = -(h * 0.24f) - floatOffset.dp))
        FloatingWordPill("Dragón", Color(0xFFB8860B), Modifier.align(Alignment.BottomStart).offset(x = w * 0.18f, y = -(h * 0.07f) - floatOffset.dp))
        FloatingWordPill("Draw", Color(0xFFB8860B), Modifier.align(Alignment.BottomEnd).offset(x = -(w * 0.16f), y = -(h * 0.07f) + floatOffset.dp))

        // Center ES 🔁 EN Pill (.langwrap)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .scale(esScale)
                        .clip(CircleShape)
                        .background(brandRed)
                ) {
                    Text(
                        text = "ES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "🔁",
                    fontSize = 20.sp,
                    color = mutedInk
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .scale(enScale)
                        .clip(CircleShape)
                        .background(brandMint)
                ) {
                    Text(
                        text = "EN",
                        fontSize = 15.sp,
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
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// =========================================================================
// STAGE 5: The Creativity Loop (.loopwrap 📱 -> spinning arcs -> 🖍️)
// =========================================================================
@Composable
private fun StageStep5CreativityLoop(
    brandRed: Color,
    brandMint: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step5")
    val loopRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loopRotation"
    )
    val phoneAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phoneAlpha"
    )
    val crayonScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crayonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(196.dp)
    ) {
        // Rotating Dual-Arc Loop (.loopa)
        Canvas(
            modifier = Modifier
                .size(168.dp)
                .rotate(loopRotation)
        ) {
            val strokeW = 10.dp.toPx()
            val pad = strokeW * 1.5f
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

            // Red Arc
            drawArc(
                color = brandRed,
                startAngle = -90f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Mint Arc
            drawArc(
                color = brandMint,
                startAngle = 90f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Top: Dimming Phone (📱)
        Text(
            text = "📱",
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .alpha(phoneAlpha)
        )

        // Bottom: Glowing Crayon (🖍️)
        Text(
            text = "🖍️",
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .scale(crayonScale)
        )
    }
}
