package org.wonderloop.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core. animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.StoryScene
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.ConfettiEffect
import kotlin.math.absoluteValue

private val ReaderCreamBg = Color(0xFFFFFBF5)
private val ReaderInkDark = Color(0xFF2B2340)
private val ReaderInkMuted = Color(0xFF6B6480)
private val ReaderCoral = Color(0xFFD9384A)
private val ReaderCoralSoft = Color(0xFFFDECE9)
private val ReaderMint = Color(0xFF0E7F64)
private val ReaderMintSoft = Color(0xFFE4F6F0)
private val ReaderMintBorder = Color(0xFFB8E6D6)
private val ReaderGoldSoft = Color(0xFFFFF1CF)
private val ReaderGoldBorder = Color(0xFFF3DC9B)
private val ReaderGoldText = Color(0xFF9E6700)
private val ReaderCardBorder = Color(0xFFEDE8E1)
private val ReaderDotInactive = Color(0xFFDCD5CA)

sealed class SagaPage {
    data class ScenePage(
        val chapter: FullStoryResponse,
        val chapterIndex: Int,
        val scene: StoryScene,
        val sceneIndex: Int,
        val totalScenesInChapter: Int
    ) : SagaPage()

    data class RetoSuperadoPage(
        val chapter: FullStoryResponse,
        val chapterIndex: Int,
        val nextChapter: FullStoryResponse
    ) : SagaPage()

    data class ChallengePromptPage(
        val chapter: FullStoryResponse,
        val chapterIndex: Int
    ) : SagaPage()
}

@Composable
fun AudioSpeakerIcon(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = w * 0.24f
            val barH = h * 0.65f
            val gap = w * 0.20f
            val startX = (w - (barW * 2 + gap)) / 2f
            val startY = (h - barH) / 2f
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
            )
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(startX + barW + gap, startY),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
            )
        } else {
            val path = androidx.compose.ui.graphics.Path().apply {
                val boxW = w * 0.22f
                val boxH = h * 0.36f
                val boxY = (h - boxH) / 2f
                val startX = w * 0.12f
                moveTo(startX, boxY)
                lineTo(startX + boxW, boxY)
                lineTo(w * 0.52f, h * 0.14f)
                lineTo(w * 0.52f, h * 0.86f)
                lineTo(startX + boxW, boxY + boxH)
                lineTo(startX, boxY + boxH)
                close()
            }
            drawPath(path, color = tint)

            val waveStroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w * 0.09f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.27f),
                size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.46f),
                style = waveStroke
            )
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.76f),
                style = waveStroke
            )
        }
    }
}

@Composable
fun StoryReaderScreen(
    story: FullStoryResponse,
    onScanNextDrawing: (FullStoryResponse) -> Unit = {},
    onCloseClick: () -> Unit,
    onGoToBookshelf: () -> Unit = onCloseClick
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Load all chapters of this saga in chronological order
    val sagaChapters = remember(story.storyId, StoryRepository.savedStories.size) {
        StoryRepository.getSagaChapters(story)
    }

    // 2. Build linear sequence of pages across all chapters
    val pages = remember(sagaChapters) {
        val list = mutableListOf<SagaPage>()
        sagaChapters.forEachIndexed { chIdx, chapter ->
            val chNum = chapter.chapterIndex ?: (chIdx + 1)
            val nextChapter = sagaChapters.getOrNull(chIdx + 1)

            chapter.scenes.forEachIndexed { sIdx, scene ->
                list.add(
                    SagaPage.ScenePage(
                        chapter = chapter,
                        chapterIndex = chNum,
                        scene = scene,
                        sceneIndex = sIdx,
                        totalScenesInChapter = chapter.scenes.size
                    )
                )
            }

            if (nextChapter != null) {
                list.add(
                    SagaPage.RetoSuperadoPage(
                        chapter = chapter,
                        chapterIndex = chNum,
                        nextChapter = nextChapter
                    )
                )
            } else {
                list.add(
                    SagaPage.ChallengePromptPage(
                        chapter = chapter,
                        chapterIndex = chNum
                    )
                )
            }
        }
        list
    }

    // Map each chapter index to its first page index in `pages`
    val chapterStartIndices = remember(pages) {
        val map = mutableMapOf<Int, Int>()
        pages.forEachIndexed { pIdx, page ->
            val chNum = when (page) {
                is SagaPage.ScenePage -> page.chapterIndex
                is SagaPage.RetoSuperadoPage -> page.chapterIndex
                is SagaPage.ChallengePromptPage -> page.chapterIndex
            }
            if (!map.containsKey(chNum)) {
                map[chNum] = pIdx
            }
        }
        map
    }

    val initialPage = remember(story.storyId) {
        val targetChNum = story.chapterIndex ?: 1
        chapterStartIndices[targetChNum] ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )
    var isPlayingAudio by remember { mutableStateOf(false) }
    var autoNarrateSession by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val currentPage = pages.getOrNull(pagerState.currentPage) ?: pages.first()
    val currentChapter = when (currentPage) {
        is SagaPage.ScenePage -> currentPage.chapter
        is SagaPage.RetoSuperadoPage -> currentPage.chapter
        is SagaPage.ChallengePromptPage -> currentPage.chapter
    }
    val currentChapterNum = when (currentPage) {
        is SagaPage.ScenePage -> currentPage.chapterIndex
        is SagaPage.RetoSuperadoPage -> currentPage.chapterIndex
        is SagaPage.ChallengePromptPage -> currentPage.chapterIndex
    }

    PlatformBackHandler(enabled = true) {
        if (showExitDialog) {
            showExitDialog = false
        } else {
            showExitDialog = true
        }
    }

    val pageTurnSpec = remember {
        tween<Float>(
            durationMillis = 520,
            easing = FastOutSlowInEasing
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            PlatformAudioService.stop()
        }
    }

    LaunchedEffect(currentChapter.storyId) {
        StoryRepository.markLastReadStory(currentChapter.storyId)
    }

    var previousPage by remember { mutableStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != previousPage) {
            val page = pages.getOrNull(pagerState.currentPage)
            if (page is SagaPage.ChallengePromptPage || page is SagaPage.RetoSuperadoPage) {
                PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
            } else {
                PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
            }
            previousPage = pagerState.currentPage

            if (autoNarrateSession) {
                if (page is SagaPage.ScenePage) {
                    isPlayingAudio = true
                    PlatformAudioService.playStoryAudio(page.scene.textContent, page.scene.audioBase64)
                } else {
                    PlatformAudioService.stop()
                    isPlayingAudio = false
                }
            }
        }
    }

    LaunchedEffect(isPlayingAudio, pagerState.currentPage) {
        if (isPlayingAudio) {
            delay(750)
            while (isPlayingAudio) {
                if (!PlatformAudioService.isSpeaking()) {
                    isPlayingAudio = false
                    break
                }
                delay(350)
            }
        }
    }

    // Redesigned Exit Reader Confirmation Modal
    if (showExitDialog) {
        Dialog(
            onDismissRequest = { showExitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ReaderCardBorder),
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
                            .background(ReaderGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📖", fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStrings.exitReaderDialogTitle,
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.storySavedNotice(currentChapter.title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary: Keep Reading
                    Button(
                        onClick = { showExitDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReaderCoral,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "${AppStrings.keepReading} ✨",
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary: Exit to Home / Bookshelf
                    OutlinedButton(
                        onClick = {
                            showExitDialog = false
                            PlatformAudioService.stop()
                            onCloseClick()
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ReaderCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ReaderInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.exitReaderDialogConfirm,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkMuted
                        )
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ReaderGoldSoft.copy(alpha = 0.45f),
                        ReaderCoralSoft.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    radius = 950f
                )
            )
    ) {
        val isLandscape = maxWidth > maxHeight
        val screenHorizontalPadding = if (isLandscape) 14.dp else 16.dp
        val screenVerticalPadding = if (isLandscape) 6.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenHorizontalPadding, vertical = screenVerticalPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // 1. TOP BAR: Circular Close · Pill Badge & Title · Circular Audio
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                Surface(
                    onClick = { showExitDialog = true },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.5.dp, ReaderCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✕",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark
                        )
                    }
                }

                // Center: Page / Chapter Pill + Story Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    val badgeBg = when (currentPage) {
                        is SagaPage.ScenePage -> ReaderCoralSoft
                        is SagaPage.RetoSuperadoPage -> ReaderMintSoft
                        is SagaPage.ChallengePromptPage -> ReaderGoldSoft
                    }
                    val badgeTextCol = when (currentPage) {
                        is SagaPage.ScenePage -> ReaderCoral
                        is SagaPage.RetoSuperadoPage -> ReaderMint
                        is SagaPage.ChallengePromptPage -> ReaderGoldText
                    }
                    val badgeLabel = when (currentPage) {
                        is SagaPage.ScenePage -> {
                            val pageStr = AppStrings.pageOf(currentPage.sceneIndex + 1, currentPage.totalScenesInChapter).uppercase()
                            if (sagaChapters.size > 1) {
                                "🌟 ${AppStrings.sagaBadge(currentChapterNum).uppercase()} · $pageStr"
                            } else {
                                pageStr
                            }
                        }
                        is SagaPage.RetoSuperadoPage -> "🎉 ${AppStrings.challengeAlreadyCompletedTitle.uppercase()}"
                        is SagaPage.ChallengePromptPage -> "🎨 ${AppStrings.creativeChallengeNav.uppercase()}"
                    }

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = badgeBg,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextCol,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = currentChapter.title,
                        fontFamily = TitleFont(),
                        fontSize = if (isLandscape) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Right: Audio Narration Button (only shown on story scene pages, hidden on challenge pages)
                if (currentPage is SagaPage.ScenePage) {
                    Surface(
                        onClick = {
                            if (isPlayingAudio) {
                                PlatformAudioService.stop()
                                isPlayingAudio = false
                                autoNarrateSession = false
                            } else {
                                autoNarrateSession = true
                                isPlayingAudio = true
                                PlatformAudioService.playStoryAudio(currentPage.scene.textContent, currentPage.scene.audioBase64)
                            }
                        },
                        shape = CircleShape,
                        color = if (isPlayingAudio) ReaderMint else Color.White,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isPlayingAudio) ReaderMint else ReaderCardBorder
                        ),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            AudioSpeakerIcon(
                                isPlaying = isPlayingAudio,
                                tint = if (isPlayingAudio) Color.White else ReaderCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

            // =================================================================
            // 2. MAIN BOOK PAGER WITH 3D PAGE-CURL TRANSITION
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pages[pageIndex]

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                                val absOffset = pageOffset.absoluteValue
                                alpha = (1f - (absOffset * 0.22f)).coerceIn(0.4f, 1f)
                                cameraDistance = 16f * density

                                if (pageOffset > 0f) {
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                    rotationY = (-36f * pageOffset).coerceIn(-75f, 0f)
                                } else if (pageOffset < 0f) {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    rotationY = (-36f * pageOffset).coerceIn(0f, 75f)
                                } else {
                                    rotationY = 0f
                                }
                            }
                    ) {
                        when (page) {
                            is SagaPage.ScenePage -> {
                                ScenePageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    isPlayingAudio = isPlayingAudio,
                                    onToggleAudio = {
                                        if (isPlayingAudio) {
                                            PlatformAudioService.stop()
                                            isPlayingAudio = false
                                            autoNarrateSession = false
                                        } else {
                                            autoNarrateSession = true
                                            isPlayingAudio = true
                                            PlatformAudioService.playStoryAudio(page.scene.textContent, page.scene.audioBase64)
                                        }
                                    }
                                )
                            }

                            is SagaPage.RetoSuperadoPage -> {
                                RetoSuperadoPageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    onTurnToNextChapter = {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage + 1,
                                                    animationSpec = pageTurnSpec
                                                )
                                            }
                                        }
                                    },
                                    onGoToBookshelf = {
                                        PlatformAudioService.stop()
                                        onGoToBookshelf()
                                    }
                                )
                            }

                            is SagaPage.ChallengePromptPage -> {
                                ChallengePromptPageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    onScanNextDrawing = {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        onScanNextDrawing(page.chapter)
                                    },
                                    onGoToBookshelf = {
                                        PlatformAudioService.stop()
                                        onGoToBookshelf()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

            // =================================================================
            // 3. BOTTOM READER CONTROLS: Prev · Dots / Chapters · Next
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Previous Page Button
                val canGoPrev = pagerState.currentPage > 0
                Surface(
                    onClick = {
                        if (canGoPrev) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage - 1,
                                    animationSpec = pageTurnSpec
                                )
                            }
                        }
                    },
                    enabled = canGoPrev,
                    shape = CircleShape,
                    color = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.4f),
                    border = BorderStroke(1.5.dp, if (canGoPrev) ReaderCardBorder else ReaderCardBorder.copy(alpha = 0.4f)),
                    shadowElevation = if (canGoPrev) 2.dp else 0.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "←",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (canGoPrev) ReaderInkDark else ReaderInkMuted.copy(alpha = 0.35f)
                        )
                    }
                }

                // Center: Chapter Jump Pills + Scene Dots + Challenge Star
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (currentChapterNum > 1) {
                        val prevChNum = currentChapterNum - 1
                        val prevTargetIndex = chapterStartIndices[prevChNum] ?: 0
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(prevTargetIndex, animationSpec = pageTurnSpec)
                                }
                            },
                            shape = RoundedCornerShape(50.dp),
                            color = ReaderGoldSoft,
                            border = BorderStroke(1.dp, ReaderGoldBorder),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "◂ Cap. $prevChNum",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderGoldText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val currentChapterPages = pages.mapIndexedNotNull { idx, p ->
                        val chNum = when (p) {
                            is SagaPage.ScenePage -> p.chapterIndex
                            is SagaPage.RetoSuperadoPage -> p.chapterIndex
                            is SagaPage.ChallengePromptPage -> p.chapterIndex
                        }
                        if (chNum == currentChapterNum) Pair(idx, p) else null
                    }

                    for ((pIndex, p) in currentChapterPages) {
                        if (p is SagaPage.ScenePage) {
                            val isCurrent = pIndex == pagerState.currentPage
                            val dotWidth by animateDpAsState(
                                targetValue = if (isCurrent) 22.dp else 8.dp,
                                animationSpec = tween(250),
                                label = "reader_dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(dotWidth)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (isCurrent) ReaderCoral else ReaderDotInactive)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = pIndex,
                                                animationSpec = pageTurnSpec
                                            )
                                        }
                                    }
                            )
                        }
                    }

                    val nextChNum = currentChapterNum + 1
                    val nextChStartIndex = chapterStartIndices[nextChNum]

                    if (nextChStartIndex != null) {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(nextChStartIndex, animationSpec = pageTurnSpec)
                                }
                            },
                            shape = RoundedCornerShape(50.dp),
                            color = ReaderGoldSoft,
                            border = BorderStroke(1.dp, ReaderGoldBorder),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "Cap. $nextChNum ▸",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderGoldText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        val challengePageIndex = currentChapterPages.firstOrNull { it.second is SagaPage.ChallengePromptPage }?.first
                        if (challengePageIndex != null) {
                            val isCurrent = challengePageIndex == pagerState.currentPage
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = challengePageIndex,
                                            animationSpec = pageTurnSpec
                                        )
                                    }
                                },
                                shape = CircleShape,
                                color = if (isCurrent) ReaderGoldSoft else Color.Transparent,
                                border = if (isCurrent) BorderStroke(1.dp, ReaderGoldBorder) else null,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🎨",
                                        fontSize = if (isCurrent) 14.sp else 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Next Page Pill / Button
                val canGoNext = pagerState.currentPage < pages.size - 1
                if (canGoNext) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = pageTurnSpec
                                )
                            }
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = ReaderCoral,
                        shadowElevation = 3.dp,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "Next →" else "Siguiente →",
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }
            }
        }
    }
}

@Composable
private fun ScenePageContent(
    page: SagaPage.ScenePage,
    isLandscape: Boolean,
    isPlayingAudio: Boolean,
    onToggleAudio: () -> Unit
) {
    val scene = page.scene
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Page: Illustration Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ReaderCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFF6DE))
                ) {
                    WonderLoopImage(
                        model = scene.illustrationUrl,
                        contentDescription = "Ilustración de la página ${scene.pageNumber}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "✨ ${page.sceneIndex + 1} / ${page.totalScenesInChapter}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Right Page: Story Text Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ReaderCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = scene.textContent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReaderInkDark,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AudioNarrationPill(
                        isPlayingAudio = isPlayingAudio,
                        label = if (isPlayingAudio) AppStrings.pauseNarration else AppStrings.listenWithVoice,
                        onClick = onToggleAudio
                    )
                }
            }
        }
    } else {
        // Portrait Mode: Unified Open Storybook Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, ReaderCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Watercolor Illustration Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(275.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF6DE), Color(0xFFFFE8EC))
                            )
                        )
                        .border(1.5.dp, Color(0xFFF3E5C8), RoundedCornerShape(22.dp))
                ) {
                    WonderLoopImage(
                        model = scene.illustrationUrl,
                        contentDescription = "Ilustración de la página ${scene.pageNumber}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Page Number Pill
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "✨ ${page.sceneIndex + 1} / ${page.totalScenesInChapter}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Story Text
                Text(
                    text = scene.textContent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReaderInkDark,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Voice Pill inside the book card
                AudioNarrationPill(
                    isPlayingAudio = isPlayingAudio,
                    label = if (isPlayingAudio) AppStrings.pauseNarration else AppStrings.listenPageWithVoice,
                    onClick = onToggleAudio,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

@Composable
private fun AudioNarrationPill(
    isPlayingAudio: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = if (isPlayingAudio) ReaderMintSoft else ReaderCoralSoft,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPlayingAudio) ReaderMintBorder else Color(0xFFF9D0CA)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            AudioSpeakerIcon(
                isPlaying = isPlayingAudio,
                tint = if (isPlayingAudio) ReaderMint else ReaderCoral,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontFamily = TitleFont(),
                fontSize = 13.sp,
                color = if (isPlayingAudio) ReaderMint else ReaderCoral,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RetoSuperadoPageContent(
    page: SagaPage.RetoSuperadoPage,
    isLandscape: Boolean,
    onTurnToNextChapter: () -> Unit,
    onGoToBookshelf: () -> Unit
) {
    val nextChNum = page.nextChapter.chapterIndex ?: (page.chapterIndex + 1)
    val nextChTitle = page.nextChapter.title
    val companionName = page.nextChapter.character.name.takeIf {
        it.isNotBlank() && !it.equals(page.chapter.sagaHero.name, ignoreCase = true)
    } ?: page.chapter.companionName ?: page.nextChapter.character.name
    val companionAvatar: String? = page.nextChapter.character.reinterpretedImageUrl
        ?: page.nextChapter.character.originalDrawingBase64
        ?: page.nextChapter.scenes.firstOrNull()?.illustrationUrl

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, ReaderMintBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = if (isLandscape) 14.dp else 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = ReaderMintSoft,
                    border = BorderStroke(1.dp, ReaderMintBorder),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "🎉 ${AppStrings.challengeAlreadyCompletedTitle.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ReaderMint,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Companion Portrait Circle
                if (companionAvatar != null) {
                    Surface(
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        border = BorderStroke(3.dp, ReaderMint),
                        modifier = Modifier.size(if (isLandscape) 72.dp else 96.dp)
                    ) {
                        WonderLoopImage(
                            model = companionAvatar,
                            contentDescription = companionName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (isLandscape) 64.dp else 84.dp)
                            .clip(CircleShape)
                            .background(ReaderMintSoft)
                            .border(2.dp, ReaderMint, CircleShape)
                    ) {
                        Text(text = "🎨", fontSize = if (isLandscape) 30.sp else 40.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = companionName,
                    fontFamily = TitleFont(),
                    fontSize = if (isLandscape) 20.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReaderInkDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AppStrings.challengeAlreadyCompletedBody(companionName),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReaderInkMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Next Chapter Preview Box
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ReaderGoldSoft,
                    border = BorderStroke(1.dp, ReaderGoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌟", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (AppStrings.isEnglish) "NEXT CHAPTER READY" else "SIGUIENTE CAPÍTULO LISTO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderGoldText,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Capítulo $nextChNum: $nextChTitle",
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ReaderInkDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Action: Turn Page to Chapter N
                Button(
                    onClick = onTurnToNextChapter,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReaderMint,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) {
                            "Read Chapter $nextChNum ➔"
                        } else {
                            "¡Leer Capítulo $nextChNum! ➔"
                        },
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onGoToBookshelf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = AppStrings.goToMyBookshelf,
                        fontFamily = TitleFont(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkMuted
                    )
                }
            }
        }

        ConfettiEffect(particleCount = 45)
    }
}

@Composable
private fun ChallengePromptPageContent(
    page: SagaPage.ChallengePromptPage,
    isLandscape: Boolean,
    onScanNextDrawing: () -> Unit,
    onGoToBookshelf: () -> Unit
) {
    val hero = page.chapter.sagaHero
    val heroAvatar = hero.reinterpretedImageUrl
        ?: hero.originalDrawingBase64
        ?: page.chapter.scenes.firstOrNull()?.illustrationUrl
    val nextChapterNumber = (page.chapter.chapterIndex ?: page.chapterIndex) + 1

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, ReaderGoldBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = if (isLandscape) 12.dp else 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Mission Badge + Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ReaderGoldSoft,
                        border = BorderStroke(1.dp, ReaderGoldBorder),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) {
                                "🎨 DRAWING MISSION · CHAPTER $nextChapterNumber"
                            } else {
                                "🎨 MISIÓN DE DIBUJO · CAPÍTULO $nextChapterNumber"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderGoldText,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = if (AppStrings.isEnglish) {
                            "Help ${hero.name}!"
                        } else {
                            "¡Ayuda a ${hero.name}!"
                        },
                        fontFamily = TitleFont(),
                        fontSize = if (isLandscape) 22.sp else 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkDark,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Middle Section: Hero Avatar + Mission Prompt Box
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFFFFBF2),
                    border = BorderStroke(1.5.dp, ReaderGoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Hero Avatar with Crayon Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (!heroAvatar.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(2.5.dp, ReaderCoral),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    WonderLoopImage(
                                        model = heroAvatar,
                                        contentDescription = hero.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(ReaderGoldSoft)
                                ) {
                                    Text(text = "🖍️", fontSize = 34.sp)
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = ReaderCoral,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🖍️", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Challenge Prompt Text
                        Text(
                            text = page.chapter.creativeChallenge.prompt,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 23.sp
                        )

                        if (page.chapter.creativeChallenge.encouragement.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ReaderMintSoft,
                                border = BorderStroke(1.dp, ReaderMintBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = page.chapter.creativeChallenge.encouragement,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReaderInkDark,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2-Step Visual Guide: 1. Draw on paper · 2. Take a photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF7F2EA),
                        border = BorderStroke(1.dp, ReaderCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(text = "🖍️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (AppStrings.isEnglish) "1. Draw on paper" else "1. Dibuja en papel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderInkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF7F2EA),
                        border = BorderStroke(1.dp, ReaderCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(text = "📸", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (AppStrings.isEnglish) "2. Scan drawing" else "2. Hazle una foto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderInkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onScanNextDrawing,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReaderCoral,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) {
                                "📸 I've drawn it! Scan ➔"
                            } else {
                                "📸 ¡Ya lo he dibujado! ➔"
                            },
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onGoToBookshelf,
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ReaderCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ReaderInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = AppStrings.goToMyBookshelf,
                            fontFamily = TitleFont(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkMuted
                        )
                    }
                }
            }
        }

        ConfettiEffect(particleCount = 45)
    }
}
