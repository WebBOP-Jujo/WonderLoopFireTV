package org.wonderloop.app.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import org.wonderloop.app.ui.components.tvFocusable
import org.wonderloop.app.ui.components.TvFocusGold
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
    if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_eq")
        val bar1 by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b1"
        )
        val bar2 by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b2"
        )
        val bar3 by infiniteTransition.animateFloat(
            initialValue = 0.40f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b3"
        )
        val bar4 by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b4"
        )
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bars = listOf(bar1, bar2, bar3, bar4)
            bars.forEach { hFraction ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(hFraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tint)
                )
            }
        }
    } else {
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
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
    val exitButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }
    val audioButtonFocusRequester = remember { FocusRequester() }
    val prevButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(pagerState.currentPage) {
        delay(120)
        try {
            val page = pages.getOrNull(pagerState.currentPage)
            if (page is SagaPage.ScenePage) {
                nextButtonFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

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
        PlatformAudioService.setOnAudioCompletedListener {
            isPlayingAudio = false
        }
        onDispose {
            PlatformAudioService.setOnAudioCompletedListener(null)
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

    // Redesigned Exit Reader Confirmation Modal
    if (showExitDialog) {
        Dialog(
            onDismissRequest = { showExitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, ReaderCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                        modifier = Modifier.width(480.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 26.dp),
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
                            val keepReadingFocusRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                try { keepReadingFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = ReaderCoral,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(keepReadingFocusRequester)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = { showExitDialog = false }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${AppStrings.keepReading} ✨",
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary: Exit to Home / Bookshelf
                            val onExitReader = {
                                showExitDialog = false
                                PlatformAudioService.stop()
                                onCloseClick()
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, ReaderCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = onExitReader
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
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
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderCreamBg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ReaderGoldSoft.copy(alpha = 0.45f),
                            ReaderCoralSoft.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxOf(size.width, size.height) * 0.65f
                    )
                )
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && !showExitDialog) {
                    when (event.key) {
                        Key.MediaFastForward, Key.PageDown -> {
                            if (pagerState.currentPage < pages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = pageTurnSpec
                                    )
                                }
                                true
                            } else false
                        }
                        Key.MediaRewind, Key.PageUp -> {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage - 1,
                                        animationSpec = pageTurnSpec
                                    )
                                }
                                true
                            } else false
                        }
                        Key.MediaPlayPause -> {
                            val curP = pages.getOrNull(pagerState.currentPage)
                            if (curP is SagaPage.ScenePage) {
                                if (isPlayingAudio) {
                                    PlatformAudioService.stop()
                                    isPlayingAudio = false
                                } else {
                                    isPlayingAudio = true
                                    PlatformAudioService.playStoryAudio(curP.scene.textContent, curP.scene.audioBase64)
                                }
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val screenHorizontalPadding = if (isLandscape) 48.dp else 16.dp
        val screenVerticalPadding = if (isLandscape) 24.dp else 10.dp

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
                    .padding(vertical = if (isLandscape) 4.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button: [✕ Salir]
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, ReaderCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .height(44.dp)
                        .focusRequester(exitButtonFocusRequester)
                        .tvFocusable(shape = RoundedCornerShape(50.dp), focusedScale = 1.06f, onClick = { showExitDialog = true })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (AppStrings.isEnglish) "Exit" else "Salir",
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark
                        )
                    }
                }

                // Center: Story Title (+ Saga Chapter badge if multi-chapter)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (sagaChapters.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = ReaderCoralSoft,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = "🌟 ${AppStrings.sagaBadge(currentChapterNum).uppercase()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderCoral,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = currentChapter.title,
                        fontFamily = TitleFont(),
                        fontSize = if (isLandscape) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReaderInkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Right: Symmetric spacer matching Exit button width so title stays centered
                Spacer(modifier = Modifier.width(96.dp))
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
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    val isCurrentPage = pageIndex == pagerState.currentPage

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                                val absOffset = pageOffset.absoluteValue
                                alpha = (1f - (absOffset * 0.35f)).coerceIn(0.25f, 1f)
                            }
                    ) {
                        when (page) {
                            is SagaPage.ScenePage -> {
                                ScenePageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    isCurrentPage = isCurrentPage,
                                    isPlayingAudio = isPlayingAudio,
                                    onGoPrev = {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage - 1,
                                                    animationSpec = pageTurnSpec
                                                )
                                            }
                                        }
                                    },
                                    onGoNext = {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage + 1,
                                                    animationSpec = pageTurnSpec
                                                )
                                            }
                                        }
                                    },
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
                                    },
                                    onFocusTop = {
                                        try { exitButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                    onFocusBottom = {
                                        try {
                                            if (pagerState.currentPage < pages.size - 1) {
                                                nextButtonFocusRequester.requestFocus()
                                            } else {
                                                audioButtonFocusRequester.requestFocus()
                                            }
                                        } catch (_: Exception) {}
                                    }
                                )
                            }

                            is SagaPage.RetoSuperadoPage -> {
                                RetoSuperadoPageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    isCurrentPage = isCurrentPage,
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
                                    },
                                    onGoPrev = {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage - 1,
                                                    animationSpec = pageTurnSpec
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            is SagaPage.ChallengePromptPage -> {
                                ChallengePromptPageContent(
                                    page = page,
                                    isLandscape = isLandscape,
                                    isCurrentPage = isCurrentPage,
                                    onScanNextDrawing = {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        onScanNextDrawing(page.chapter)
                                    },
                                    onGoToBookshelf = {
                                        PlatformAudioService.stop()
                                        onGoToBookshelf()
                                    },
                                    onGoPrev = {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage - 1,
                                                    animationSpec = pageTurnSpec
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

            // =================================================================
            // 3. BOTTOM READER CONTROLS: Prev + Audio · Segments · Next / Reto
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: [ ◀ Anterior ]  and  [ 🔊 Narrando / Escuchar ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val canGoPrev = pagerState.currentPage > 0
                    val onGoPrev: () -> Unit = {
                        if (canGoPrev) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage - 1,
                                    animationSpec = pageTurnSpec
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.4f),
                        border = BorderStroke(1.5.dp, if (canGoPrev) ReaderCardBorder else ReaderCardBorder.copy(alpha = 0.4f)),
                        shadowElevation = if (canGoPrev) 2.dp else 0.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .focusRequester(prevButtonFocusRequester)
                            .then(
                                if (canGoPrev) {
                                    Modifier
                                        .tvFocusable(shape = RoundedCornerShape(50.dp), focusedScale = 1.06f, onClick = onGoPrev)
                                        .onKeyEvent {
                                            if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                                onGoPrev()
                                                true
                                            } else false
                                        }
                                } else Modifier
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "←",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canGoPrev) ReaderInkDark else ReaderInkMuted.copy(alpha = 0.35f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (AppStrings.isEnglish) "Previous" else "Anterior",
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canGoPrev) ReaderInkDark else ReaderInkMuted.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Audio Narrando / Escuchar Button (only on scene pages)
                    if (currentPage is SagaPage.ScenePage) {
                        val toggleAudio: () -> Unit = {
                            if (isPlayingAudio) {
                                PlatformAudioService.stop()
                                isPlayingAudio = false
                                autoNarrateSession = false
                            } else {
                                autoNarrateSession = true
                                isPlayingAudio = true
                                PlatformAudioService.playStoryAudio(currentPage.scene.textContent, currentPage.scene.audioBase64)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isPlayingAudio) ReaderMintSoft else Color.White,
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isPlayingAudio) ReaderMintBorder else ReaderCardBorder
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .height(46.dp)
                                .focusRequester(audioButtonFocusRequester)
                                .tvFocusable(shape = RoundedCornerShape(50.dp), focusedScale = 1.06f, onClick = toggleAudio)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                AudioSpeakerIcon(
                                    isPlaying = isPlayingAudio,
                                    tint = if (isPlayingAudio) ReaderMint else ReaderCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPlayingAudio) {
                                        if (AppStrings.isEnglish) "Playing..." else "Narrando"
                                    } else {
                                        if (AppStrings.isEnglish) "Listen" else "Escuchar"
                                    },
                                    fontFamily = TitleFont(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlayingAudio) ReaderMint else ReaderInkDark
                                )
                            }
                        }
                    }
                }

                // CENTER: Single Segmented Bar + Label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    val currentChapterPages = pages.mapIndexedNotNull { idx, p ->
                        val chNum = when (p) {
                            is SagaPage.ScenePage -> p.chapterIndex
                            is SagaPage.RetoSuperadoPage -> p.chapterIndex
                            is SagaPage.ChallengePromptPage -> p.chapterIndex
                        }
                        if (chNum == currentChapterNum) Pair(idx, p) else null
                    }
                    val totalScenes = currentChapterPages.count { it.second is SagaPage.ScenePage }
                    val currentSceneIndex = (currentPage as? SagaPage.ScenePage)?.sceneIndex ?: totalScenes

                    // Segmented Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(if (isLandscape) 0.55f else 0.85f)
                            .height(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalScenes) {
                            val isRead = i < currentSceneIndex
                            val isActive = i == currentSceneIndex && currentPage is SagaPage.ScenePage
                            val segColor = when {
                                isActive -> ReaderCoral
                                isRead -> ReaderMint
                                else -> ReaderCardBorder
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(segColor)
                            )
                        }

                        // Challenge or Next Chapter segment
                        val hasChallenge = currentChapterPages.any { it.second is SagaPage.ChallengePromptPage || it.second is SagaPage.RetoSuperadoPage }
                        if (hasChallenge) {
                            val isChallengeActive = currentPage is SagaPage.ChallengePromptPage || currentPage is SagaPage.RetoSuperadoPage
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isChallengeActive) ReaderGoldText else ReaderGoldSoft)
                                    .border(1.dp, ReaderGoldBorder, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "★",
                                    fontSize = 12.sp,
                                    color = if (isChallengeActive) Color.White else ReaderGoldText,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress label
                    val hasChallenge = currentChapterPages.any { it.second is SagaPage.ChallengePromptPage || it.second is SagaPage.RetoSuperadoPage }
                    val progressLabel = when (currentPage) {
                        is SagaPage.ScenePage -> {
                            val base = "Página ${currentPage.sceneIndex + 1} de ${currentPage.totalScenesInChapter}"
                            if (hasChallenge) "$base · ${if (AppStrings.isEnglish) "Challenge at end" else "Reto al final"}" else base
                        }
                        is SagaPage.ChallengePromptPage -> if (AppStrings.isEnglish) "Creative Challenge" else "Reto Creativo"
                        is SagaPage.RetoSuperadoPage -> if (AppStrings.isEnglish) "Challenge Completed!" else "¡Reto Superado!"
                    }

                    Text(
                        text = progressLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReaderInkMuted
                    )
                }

                // RIGHT: Next Page Pill / Ir al Reto Button
                val canGoNext = pagerState.currentPage < pages.size - 1
                if (canGoNext) {
                    val onGoNext: () -> Unit = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = pageTurnSpec
                            )
                        }
                    }
                    val isLastSceneBeforeChallenge = currentPage is SagaPage.ScenePage &&
                        pagerState.currentPage + 1 < pages.size &&
                        (pages[pagerState.currentPage + 1] is SagaPage.ChallengePromptPage || pages[pagerState.currentPage + 1] is SagaPage.RetoSuperadoPage)

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ReaderCoral,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .focusRequester(nextButtonFocusRequester)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.06f,
                                onClick = onGoNext
                            )
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionRight) {
                                    onGoNext()
                                    true
                                } else false
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Text(
                                text = if (isLastSceneBeforeChallenge) {
                                    if (AppStrings.isEnglish) "Go to Challenge →" else "Ir al Reto →"
                                } else {
                                    if (AppStrings.isEnglish) "Next →" else "Siguiente →"
                                },
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Last page: Spacer to balance layout width
                    Spacer(modifier = Modifier.width(130.dp))
                }
            }
        }
    }
}

@Composable
private fun ScenePageContent(
    page: SagaPage.ScenePage,
    isLandscape: Boolean,
    isCurrentPage: Boolean = true,
    isPlayingAudio: Boolean,
    onGoPrev: () -> Unit = {},
    onGoNext: () -> Unit = {},
    onToggleAudio: () -> Unit,
    onFocusTop: () -> Unit = {},
    onFocusBottom: () -> Unit = {}
) {
    val scene = page.scene
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Page: Illustration Card (Clean, artwork only)
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
                }
            }

            // Right Page: Story Text Card (Focusable with D-Pad scroll UP/DOWN and page turn LEFT/RIGHT)
            val textScrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ReaderCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tvFocusable(
                        enabled = isCurrentPage,
                        shape = RoundedCornerShape(24.dp),
                        focusedScale = 1.02f,
                        focusBorderColor = TvFocusGold,
                        focusBorderWidth = 3.dp,
                        playClickSound = false,
                        onClick = onToggleAudio
                    )
                    .onKeyEvent { keyEvent ->
                        if (!isCurrentPage) return@onKeyEvent false
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    if (textScrollState.value > 0) {
                                        coroutineScope.launch {
                                            textScrollState.animateScrollBy(-140f)
                                        }
                                        true
                                    } else {
                                        onFocusTop()
                                        true
                                    }
                                }
                                Key.DirectionDown -> {
                                    if (textScrollState.value < textScrollState.maxValue) {
                                        coroutineScope.launch {
                                            textScrollState.animateScrollBy(140f)
                                        }
                                        true
                                    } else {
                                        onFocusBottom()
                                        true
                                    }
                                }
                                Key.DirectionLeft -> {
                                    onGoPrev()
                                    true
                                }
                                Key.DirectionRight -> {
                                    onGoNext()
                                    true
                                }
                                else -> false
                            }
                        } else if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.key) {
                                Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter -> true
                                else -> false
                            }
                        } else false
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(textScrollState)
                        .padding(horizontal = 32.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = scene.textContent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkDark,
                        lineHeight = 35.sp,
                        textAlign = TextAlign.Start
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
                            fontSize = 13.sp,
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
        shape = RoundedCornerShape(50.dp),
        color = if (isPlayingAudio) ReaderMintSoft else ReaderCoralSoft,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPlayingAudio) ReaderMintBorder else Color(0xFFF9D0CA)
        ),
        onClick = onClick,
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
    isCurrentPage: Boolean = true,
    onTurnToNextChapter: () -> Unit,
    onGoToBookshelf: () -> Unit,
    onGoPrev: () -> Unit = {}
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
        if (isLandscape) {
            // Widescreen 2-Column TV Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Card: Companion Portrait & Congratulations
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, ReaderMintBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp, vertical = 18.dp),
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderMint,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        if (companionAvatar != null) {
                            Surface(
                                shape = CircleShape,
                                shadowElevation = 6.dp,
                                border = BorderStroke(3.dp, ReaderMint),
                                modifier = Modifier.size(76.dp)
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
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(ReaderMintSoft)
                                    .border(2.dp, ReaderMint, CircleShape)
                            ) {
                                Text(text = "🎨", fontSize = 32.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = companionName,
                            fontFamily = TitleFont(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = AppStrings.challengeAlreadyCompletedBody(companionName),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReaderInkMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Right Card: Next Chapter Banner & Actions
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ReaderCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ReaderGoldSoft,
                            border = BorderStroke(1.dp, ReaderGoldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🌟", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (AppStrings.isEnglish) "NEXT CHAPTER READY" else "SIGUIENTE CAPÍTULO LISTO",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ReaderGoldText,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Capítulo $nextChNum: $nextChTitle",
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ReaderInkDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val nextChapterFocusRequester = remember { FocusRequester() }
                        LaunchedEffect(isCurrentPage) {
                            if (isCurrentPage) {
                                kotlinx.coroutines.delay(120)
                                try { nextChapterFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        }
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
                                .focusRequester(nextChapterFocusRequester)
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onTurnToNextChapter)
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                        onGoPrev()
                                        true
                                    } else false
                                }
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "Read Chapter $nextChNum ➔" else "¡Leer Capítulo $nextChNum! ➔",
                                fontFamily = TitleFont(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onGoToBookshelf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onGoToBookshelf)
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
        } else {
            // Portrait fallback
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
                        .padding(horizontal = 22.dp, vertical = 22.dp),
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderMint,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    if (companionAvatar != null) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 6.dp,
                            border = BorderStroke(3.dp, ReaderMint),
                            modifier = Modifier.size(96.dp)
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
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(ReaderMintSoft)
                                .border(2.dp, ReaderMint, CircleShape)
                        ) {
                            Text(text = "🎨", fontSize = 40.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = companionName,
                        fontFamily = TitleFont(),
                        fontSize = 24.sp,
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
                                    fontSize = 13.sp,
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

                    val nextChapterFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(isCurrentPage) {
                        if (isCurrentPage) {
                            kotlinx.coroutines.delay(120)
                            try { nextChapterFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    }
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
                            .focusRequester(nextChapterFocusRequester)
                            .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onTurnToNextChapter)
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                    onGoPrev()
                                    true
                                } else false
                            }
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "Read Chapter $nextChNum ➔" else "¡Leer Capítulo $nextChNum! ➔",
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onGoToBookshelf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onGoToBookshelf)
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

@Composable
private fun ChallengePromptPageContent(
    page: SagaPage.ChallengePromptPage,
    isLandscape: Boolean,
    isCurrentPage: Boolean = true,
    onScanNextDrawing: () -> Unit,
    onGoToBookshelf: () -> Unit,
    onGoPrev: () -> Unit = {}
) {
    val hero = page.chapter.sagaHero
    val heroAvatar = hero.reinterpretedImageUrl
        ?: hero.originalDrawingBase64
        ?: page.chapter.scenes.firstOrNull()?.illustrationUrl
    val nextChapterNumber = (page.chapter.chapterIndex ?: page.chapterIndex) + 1
    val scanFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            kotlinx.coroutines.delay(120)
            try { scanFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            // Widescreen 2-Column TV Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Card: Mission Card (Avatar, Prompt, Encouragement)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, ReaderGoldBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = ReaderGoldSoft,
                            border = BorderStroke(1.dp, ReaderGoldBorder),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "🎨 DRAWING MISSION · CH. $nextChapterNumber" else "🎨 MISIÓN DE DIBUJO · CAP. $nextChapterNumber",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderGoldText,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        // Hero Avatar with Crayon Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (!heroAvatar.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(2.5.dp, ReaderCoral),
                                    modifier = Modifier.size(68.dp)
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
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ReaderGoldSoft)
                                ) {
                                    Text(text = "🖍️", fontSize = 30.sp)
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = ReaderCoral,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🖍️", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (AppStrings.isEnglish) "Help ${hero.name}!" else "¡Ayuda a ${hero.name}!",
                            fontFamily = TitleFont(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = page.chapter.creativeChallenge.prompt,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ReaderInkDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        if (page.chapter.creativeChallenge.encouragement.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ReaderMintSoft,
                                border = BorderStroke(1.dp, ReaderMintBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💡", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = page.chapter.creativeChallenge.encouragement,
                                        fontSize = 13.sp,
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

                // Right Card: Action & Steps Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ReaderCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "How to continue:" else "¿Cómo continuar?",
                            fontFamily = TitleFont(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF7F2EA),
                                border = BorderStroke(1.dp, ReaderCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(text = "🖍️", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (AppStrings.isEnglish) "1. Draw on paper" else "1. Dibuja en papel",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReaderInkDark,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF7F2EA),
                                border = BorderStroke(1.dp, ReaderCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(text = "📱", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (AppStrings.isEnglish) "2. Scan with phone" else "2. Escanea con móvil",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReaderInkDark,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onScanNextDrawing,
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReaderCoral,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .focusRequester(scanFocusRequester)
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onScanNextDrawing)
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                        onGoPrev()
                                        true
                                    } else false
                                }
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "📱 I've drawn it! Scan QR ➔" else "📱 ¡Ya lo he dibujado! Escanear QR ➔",
                                fontFamily = TitleFont(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                .height(42.dp)
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onGoToBookshelf)
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                        onGoPrev()
                                        true
                                    } else false
                                }
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
        } else {
            // Portrait fallback
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
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = ReaderGoldSoft,
                            border = BorderStroke(1.dp, ReaderGoldBorder),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "🎨 DRAWING MISSION · CHAPTER $nextChapterNumber" else "🎨 MISIÓN DE DIBUJO · CAPÍTULO $nextChapterNumber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderGoldText,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = if (AppStrings.isEnglish) "Help ${hero.name}!" else "¡Ayuda a ${hero.name}!",
                            fontFamily = TitleFont(),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                            fontSize = 13.5.sp,
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
                                    fontSize = 13.sp,
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
                                Text(text = "📱", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (AppStrings.isEnglish) "2. Scan QR with phone" else "2. Escanea QR con móvil",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ReaderInkDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                .focusRequester(scanFocusRequester)
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onScanNextDrawing)
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                        onGoPrev()
                                        true
                                    } else false
                                }
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "📱 I've drawn it! Scan QR ➔" else "📱 ¡Ya lo he dibujado! Escanear QR ➔",
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
                                .tvFocusable(enabled = isCurrentPage, shape = RoundedCornerShape(50.dp), onClick = onGoToBookshelf)
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionLeft) {
                                        onGoPrev()
                                        true
                                    } else false
                                }
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
        }

        ConfettiEffect(particleCount = 45)
    }
}
