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
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import org.wonderloop.app.ui.components.tvFocusable
import org.wonderloop.app.ui.components.TvFocusGold
import kotlin.math.absoluteValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.wonderloop.app.ui.tv.KeepScreenOn
import org.wonderloop.app.ui.tv.TvClickableSurface
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalFoundationApi::class)
private val NoOpBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

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
private val ReaderGoldHighlight = Color(0xFFFFD166)

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

/** Fallback pace for subtitles when the narration has no Polly speech marks (Gemini audio or device TTS). */
private const val ESTIMATED_MS_PER_WORD = 430L
/** About two lines of 28sp subtitle text inside the 10-foot safe zone. */
private const val SUBTITLE_MAX_CHARS = 100
private const val CONTROLS_AUTO_HIDE_MS = 3500L

@Composable
fun StoryReaderScreen(
    story: FullStoryResponse,
    onScanNextDrawing: (FullStoryResponse) -> Unit = {},
    onCloseClick: () -> Unit,
    onGoToBookshelf: () -> Unit = onCloseClick
) {
    val coroutineScope = rememberCoroutineScope()
    // No screensaver while a story is being read or narrated
    KeepScreenOn()

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
                list.add(SagaPage.RetoSuperadoPage(chapter = chapter, chapterIndex = chNum, nextChapter = nextChapter))
            } else {
                list.add(SagaPage.ChallengePromptPage(chapter = chapter, chapterIndex = chNum))
            }
        }
        list
    }

    // Map each chapter index to its first page index in `pages`
    val chapterStartIndices = remember(pages) {
        val map = mutableMapOf<Int, Int>()
        pages.forEachIndexed { pIdx, page ->
            val chNum = page.chapterNumber()
            if (!map.containsKey(chNum)) map[chNum] = pIdx
        }
        map
    }

    val initialPage = remember(story.storyId) {
        chapterStartIndices[story.chapterIndex ?: 1] ?: 0
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })
    var isPlayingAudio by remember { mutableStateOf(false) }
    // Bedtime mode: the story reads itself aloud from the first page; "Pause" turns it off for the session.
    var autoNarrateSession by remember { mutableStateOf(true) }
    var playbackPositionMs by remember { mutableStateOf(-1L) }
    var narrationElapsedMs by remember { mutableStateOf(0L) }
    var narrationDone by remember { mutableStateOf(false) }
    // Subtitle block on screen (index into the page's chunks)
    var shownChunk by remember { mutableStateOf(0) }
    // "Read it myself": the whole page text for a grown-up reading aloud
    var fullTextMode by remember { mutableStateOf(false) }
    // Video-player style overlay: hidden while the narrator reads, back on any key press
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }
    var swallowSelectRelease by remember { mutableStateOf(false) }
    val exitButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }
    val audioButtonFocusRequester = remember { FocusRequester() }

    val currentPage = pages.getOrNull(pagerState.currentPage) ?: pages.first()
    val currentChapter = currentPage.chapterStory()
    val currentChapterNum = currentPage.chapterNumber()
    val onScenePage = currentPage is SagaPage.ScenePage

    // Words and subtitle chunks of the page being read, synced with the narration clock
    val sceneText = (currentPage as? SagaPage.ScenePage)?.scene?.textContent.orEmpty()
    val sceneWords = remember(sceneText) { Regex("\\S+").findAll(sceneText).map { it.range }.toList() }
    val sceneChunks = remember(sceneText) { subtitleChunks(sceneWords.map { sceneText.substring(it) }) }
    val sceneTimings = (currentPage as? SagaPage.ScenePage)?.scene?.wordTimingsMs.orEmpty()
    val timingsValid = sceneTimings.isNotEmpty() && sceneTimings.size == sceneWords.size
    val activeWord = if (timingsValid && playbackPositionMs >= 0) sceneTimings.indexOfLast { it <= playbackPositionMs } else -1
    val spokenWord = when {
        activeWord >= 0 -> activeWord
        isPlayingAudio && !timingsValid && sceneWords.isNotEmpty() -> {
            val clockMs = if (playbackPositionMs >= 0) playbackPositionMs else narrationElapsedMs
            (clockMs / ESTIMATED_MS_PER_WORD).toInt().coerceIn(0, sceneWords.lastIndex)
        }
        else -> -1
    }
    LaunchedEffect(spokenWord, sceneChunks) {
        if (spokenWord >= 0) {
            val idx = sceneChunks.indexOfFirst { spokenWord in it }
            if (idx >= 0) shownChunk = idx
        }
    }

    val pageTurnSpec = remember { tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing) }

    fun goTo(target: Int) {
        if (target in pages.indices && target != pagerState.currentPage && !pagerState.isScrollInProgress) {
            coroutineScope.launch { pagerState.animateScrollToPage(page = target, animationSpec = pageTurnSpec) }
        }
    }

    fun playPage(page: SagaPage?) {
        narrationDone = false
        val challengeAudio = (page as? SagaPage.ChallengePromptPage)?.chapter?.creativeChallenge?.audioBase64
        when {
            page is SagaPage.ScenePage -> {
                isPlayingAudio = true
                PlatformAudioService.playStoryAudio(page.scene.textContent, page.scene.audioBase64)
            }
            page is SagaPage.ChallengePromptPage && page.canNarrateChallenge() -> {
                isPlayingAudio = true
                PlatformAudioService.playStoryAudio(page.challengeNarrationText(), challengeAudio)
            }
            else -> {
                PlatformAudioService.stop()
                isPlayingAudio = false
            }
        }
    }

    fun toggleAudio() {
        if (isPlayingAudio) {
            PlatformAudioService.stop()
            isPlayingAudio = false
            autoNarrateSession = false
        } else {
            autoNarrateSession = true
            if (fullTextMode) fullTextMode = false
            playPage(pages.getOrNull(pagerState.currentPage))
        }
    }

    fun setFullText(enabled: Boolean) {
        fullTextMode = enabled
        if (enabled && isPlayingAudio) {
            // A grown-up is reading aloud: the narrator steps aside
            PlatformAudioService.stop()
            isPlayingAudio = false
            autoNarrateSession = false
        }
    }

    // ◀ ▶ walk the subtitles at the child's own pace when nobody narrates, otherwise turn the page
    fun step(forward: Boolean) {
        val canStepChunk = onScenePage && !isPlayingAudio && !fullTextMode
        when {
            canStepChunk && forward && shownChunk < sceneChunks.lastIndex -> shownChunk++
            canStepChunk && !forward && shownChunk > 0 -> shownChunk--
            else -> goTo(pagerState.currentPage + if (forward) 1 else -1)
        }
    }

    // Fire TV guideline: BACK closes the open overlay first, then leaves (progress is saved automatically)
    PlatformBackHandler(enabled = true) {
        if (fullTextMode) {
            setFullText(false)
        } else {
            PlatformAudioService.stop()
            onCloseClick()
        }
    }

    DisposableEffect(Unit) {
        PlatformAudioService.setOnAudioCompletedListener {
            isPlayingAudio = false
            narrationDone = true
        }
        onDispose {
            PlatformAudioService.setOnAudioCompletedListener(null)
            PlatformAudioService.stop()
        }
    }

    LaunchedEffect(currentChapter.storyId) {
        StoryRepository.markLastReadStory(currentChapter.storyId)
    }

    // Page change (and first open): sound effect, narration and focus on the primary action
    var previousPage by remember { mutableStateOf(-1) }
    LaunchedEffect(pagerState.currentPage) {
        val page = pages.getOrNull(pagerState.currentPage)
        val firstOpen = previousPage == -1
        shownChunk = 0
        narrationDone = false
        if (!firstOpen) {
            PlatformAudioService.stop()
            isPlayingAudio = false
            if (page is SagaPage.ChallengePromptPage || page is SagaPage.RetoSuperadoPage) {
                PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
            } else {
                PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
            }
        }
        previousPage = pagerState.currentPage
        if (page is SagaPage.ScenePage) {
            delay(120)
            try { nextButtonFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        if (autoNarrateSession) {
            // let the page settle (and the celebration jingle finish) before the voice starts
            delay(
                when {
                    firstOpen -> 700L
                    page is SagaPage.ScenePage -> 250L
                    else -> 1100L
                }
            )
            if (pagerState.currentPage == pages.indexOf(page)) playPage(page)
        }
    }

    // Narration finished: rest on the last subtitle and invite the child to turn the page
    LaunchedEffect(narrationDone) {
        if (narrationDone && pages.getOrNull(pagerState.currentPage) is SagaPage.ScenePage) {
            shownChunk = sceneChunks.lastIndex.coerceAtLeast(0)
            try { nextButtonFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Narration clock for subtitles and word-by-word highlighting (Amazon Polly speech marks)
    LaunchedEffect(isPlayingAudio, pagerState.currentPage) {
        playbackPositionMs = -1L
        narrationElapsedMs = 0L
        while (isPlayingAudio) {
            playbackPositionMs = PlatformAudioService.currentPositionMs()
            delay(50)
            narrationElapsedMs += 50
        }
        playbackPositionMs = -1L
    }

    LaunchedEffect(interactionTick, isPlayingAudio, fullTextMode, onScenePage) {
        controlsVisible = true
        if (onScenePage && isPlayingAudio && !fullTextMode) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (!onScenePage || controlsVisible || fullTextMode) 1f else 0f,
        animationSpec = tween(durationMillis = if (controlsVisible) 200 else 500),
        label = "reader_overlay_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderCreamBg)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) interactionTick++
                val onScene = pages.getOrNull(pagerState.currentPage) is SagaPage.ScenePage
                val overlayHidden = onScene && !controlsVisible && !fullTextMode
                when (event.key) {
                    // ◀ ▶ always belong to the story (subtitle or page), never to focus movement
                    Key.DirectionRight, Key.DirectionLeft -> {
                        if (!onScene) {
                            false
                        } else {
                            if (event.type == KeyEventType.KeyDown) step(forward = event.key == Key.DirectionRight)
                            true
                        }
                    }
                    // Like a video player: OK while the overlay is hidden pauses the narration and shows the controls
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> when {
                        overlayHidden && event.type == KeyEventType.KeyDown && !swallowSelectRelease -> {
                            swallowSelectRelease = true
                            toggleAudio()
                            coroutineScope.launch {
                                delay(60)
                                try { audioButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                            true
                        }
                        swallowSelectRelease -> {
                            if (event.type == KeyEventType.KeyUp) swallowSelectRelease = false
                            true
                        }
                        else -> false
                    }
                    // First ▲ ▼ only wakes the overlay up
                    Key.DirectionUp, Key.DirectionDown -> overlayHidden
                    Key.MediaFastForward, Key.PageDown -> {
                        if (event.type == KeyEventType.KeyUp) goTo(pagerState.currentPage + 1)
                        true
                    }
                    Key.MediaRewind, Key.PageUp -> {
                        if (event.type == KeyEventType.KeyUp) goTo(pagerState.currentPage - 1)
                        true
                    }
                    Key.MediaPlayPause -> {
                        val narratable = onScene || pages.getOrNull(pagerState.currentPage) is SagaPage.ChallengePromptPage
                        if (event.type == KeyEventType.KeyUp && narratable) toggleAudio()
                        narratable
                    }
                    else -> false
                }
            }
    ) {
        // =================================================================
        // 1. PAGES: full-bleed 16:9 illustrations (cinema mode) or challenge pages
        // =================================================================
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoOpBringIntoViewSpec) {
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
                            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                            alpha = (1f - pageOffset.absoluteValue * 0.6f).coerceIn(0f, 1f)
                        }
                ) {
                    when (page) {
                        is SagaPage.ScenePage -> CinematicIllustration(
                            url = page.scene.illustrationUrl,
                            contentDescription = if (AppStrings.isEnglish) "Illustration for page ${page.scene.pageNumber}" else "Ilustración de la página ${page.scene.pageNumber}",
                            animate = isCurrentPage
                        )

                        is SagaPage.RetoSuperadoPage -> RetoSuperadoPageContent(
                            page = page,
                            isCurrentPage = isCurrentPage,
                            onTurnToNextChapter = { goTo(pagerState.currentPage + 1) },
                            onGoToBookshelf = {
                                PlatformAudioService.stop()
                                onGoToBookshelf()
                            },
                            onGoPrev = { goTo(pagerState.currentPage - 1) }
                        )

                        is SagaPage.ChallengePromptPage -> ChallengePromptPageContent(
                            page = page,
                            isCurrentPage = isCurrentPage,
                            isPlayingAudio = isPlayingAudio && isCurrentPage,
                            onToggleAudio = { toggleAudio() },
                            onScanNextDrawing = {
                                PlatformAudioService.stop()
                                PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                onScanNextDrawing(page.chapter)
                            },
                            onGoToBookshelf = {
                                PlatformAudioService.stop()
                                onGoToBookshelf()
                            },
                            onGoPrev = { goTo(pagerState.currentPage - 1) }
                        )
                    }
                }
            }
        }

        if (onScenePage) {
            // Soft top scrim so the top bar stays legible over any illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent)))
            )
            // Bottom scrim: the only part of the illustration the subtitles darken
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.28f),
                            1f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )
        }

        // =================================================================
        // 2. TOP BAR: Exit · Title · Progress (inside the 48dp/20dp safe zone)
        // =================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = overlayAlpha }
                .padding(horizontal = 48.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvClickableSurface(
                onClick = {
                    PlatformAudioService.stop()
                    onCloseClick()
                },
                modifier = Modifier
                    .height(44.dp)
                    .focusRequester(exitButtonFocusRequester),
                shape = RoundedCornerShape(50),
                containerColor = Color.White.copy(alpha = 0.92f),
                focusedContainerColor = Color.White,
                contentDescription = if (AppStrings.isEnglish) "Exit story" else "Salir del cuento"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp).align(Alignment.Center)
                ) {
                    Text(text = "✕", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ReaderInkDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Exit" else "Salir",
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        if (sagaChapters.size > 1) {
                            Text(
                                text = "🌟 ${AppStrings.sagaBadge(currentChapterNum)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ReaderCoral
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = currentChapter.title,
                            fontFamily = TitleFont(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReaderInkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            ReaderProgressPill(pages = pages, currentIndex = pagerState.currentPage, chapterNum = currentChapterNum)
        }

        // =================================================================
        // 3. SCENE OVERLAY: synced subtitles (or full text) + auto-hiding controls
        // =================================================================
        if (currentPage is SagaPage.ScenePage) {
            val nextIndex = pagerState.currentPage + 1
            val canGoNext = nextIndex < pages.size
            val canGoPrev = pagerState.currentPage > 0
            val nextIsChallenge = canGoNext && pages[nextIndex] !is SagaPage.ScenePage

            if (fullTextMode) {
                FullTextPanel(
                    text = sceneText,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 48.dp, end = 272.dp, bottom = 28.dp)
                )
            } else {
                SubtitleBlock(
                    text = sceneText,
                    words = sceneWords,
                    chunks = sceneChunks,
                    chunkIndex = shownChunk,
                    activeWord = activeWord,
                    pageIndex = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 72.dp, bottom = 34.dp)
                )
            }

            // Page-turn hints while the overlay rests
            if (canGoNext) {
                EdgeChevron(
                    glyph = "›",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                        .graphicsLayer { alpha = 1f - overlayAlpha }
                )
            }
            if (canGoPrev) {
                EdgeChevron(
                    glyph = "‹",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                        .graphicsLayer { alpha = (1f - overlayAlpha) * 0.6f }
                )
            }

            ReaderControls(
                isPlayingAudio = isPlayingAudio,
                narrationDone = narrationDone,
                fullTextMode = fullTextMode,
                canGoPrev = canGoPrev,
                nextLabel = when {
                    nextIsChallenge -> if (AppStrings.isEnglish) "Challenge →" else "Ir al reto →"
                    else -> if (AppStrings.isEnglish) "Next →" else "Siguiente →"
                },
                onNext = if (canGoNext) ({ goTo(nextIndex) }) else null,
                onPrev = { goTo(pagerState.currentPage - 1) },
                onToggleAudio = { toggleAudio() },
                onToggleFullText = { setFullText(!fullTextMode) },
                nextFocusRequester = nextButtonFocusRequester,
                audioFocusRequester = audioButtonFocusRequester,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp, top = 40.dp)
                    .graphicsLayer { alpha = overlayAlpha }
            )
        }
    }
}

private fun SagaPage.chapterNumber(): Int = when (this) {
    is SagaPage.ScenePage -> chapterIndex
    is SagaPage.RetoSuperadoPage -> chapterIndex
    is SagaPage.ChallengePromptPage -> chapterIndex
}

private fun SagaPage.chapterStory(): FullStoryResponse = when (this) {
    is SagaPage.ScenePage -> chapter
    is SagaPage.RetoSuperadoPage -> chapter
    is SagaPage.ChallengePromptPage -> chapter
}

private fun SagaPage.ChallengePromptPage.challengeNarrationText(): String {
    val challenge = chapter.creativeChallenge
    val opener = if (AppStrings.isEnglish) "Help ${chapter.sagaHero.name}!" else "¡Ayuda a ${chapter.sagaHero.name}!"
    return listOf(opener, challenge.prompt, challenge.encouragement).filter { it.isNotBlank() }.joinToString(" ")
}

/** Polly audio from the backend, or the Spanish device voice for stories created before challenge narration existed. */
private fun SagaPage.ChallengePromptPage.canNarrateChallenge(): Boolean =
    !chapter.creativeChallenge.audioBase64.isNullOrBlank() || !AppStrings.isEnglish

/**
 * Splits a page into subtitle blocks (ranges of word indices): whole sentences when they fit,
 * long sentences cut at the pause closest to their middle, short neighbours merged up to [maxChars].
 * A dialogue tag ("—¡Ayuda! —gritó el peluche.") stays with its line.
 */
internal fun subtitleChunks(words: List<String>, maxChars: Int = SUBTITLE_MAX_CHARS): List<IntRange> {
    if (words.isEmpty()) return emptyList()
    fun length(range: IntRange): Int = range.sumOf { words[it].length + 1 } - 1

    val sentences = mutableListOf<IntRange>()
    var start = 0
    for (i in words.indices) {
        val bare = words[i].trimEnd('»', '"', '”', '\'', ')')
        val endsSentence = bare.endsWith('.') || bare.endsWith('!') || bare.endsWith('?') || bare.endsWith('…')
        val next = words.getOrNull(i + 1)
        val dialogueTagFollows = next != null && next.startsWith("—") && next.drop(1).firstOrNull()?.isLowerCase() == true
        if (i == words.lastIndex || (endsSentence && !dialogueTagFollows)) {
            sentences += start..i
            start = i + 1
        }
    }

    val pieces = mutableListOf<IntRange>()
    fun split(range: IntRange) {
        if (length(range) <= maxChars || range.first == range.last) {
            pieces += range
            return
        }
        val target = length(range) / 2
        val pauses = (range.first until range.last).filter { i ->
            val w = words[i]
            w.endsWith(',') || w.endsWith(';') || w.endsWith(':') || w.endsWith('!') || w.endsWith('?') ||
                w.endsWith('—') || words[i + 1].startsWith("—")
        }
        val cut = pauses.minByOrNull { kotlin.math.abs(length(range.first..it) - target) }
            ?: run {
                var acc = 0
                range.first + (range.first until range.last).indexOfFirst { acc += words[it].length + 1; acc > target }.coerceAtLeast(0)
            }
        split(range.first..cut)
        split((cut + 1)..range.last)
    }
    sentences.forEach { split(it) }

    val chunks = mutableListOf<IntRange>()
    for (piece in pieces) {
        val last = chunks.lastOrNull()
        if (last != null && length(last.first..piece.last) <= maxChars) {
            chunks[chunks.lastIndex] = last.first..piece.last
        } else {
            chunks += piece
        }
    }
    return chunks
}

/** Full-bleed 16:9 illustration with a slow Ken Burns drift on the page being read. */
@Composable
private fun CinematicIllustration(url: String?, contentDescription: String?, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "ken_burns")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ken_burns_drift"
    )
    val p = if (animate) drift else 0f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6EBD9))
    ) {
        WonderLoopImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1.03f + 0.06f * p
                    scaleX = scale
                    scaleY = scale
                    translationX = (p - 0.5f) * size.width * 0.03f
                }
        )
    }
}

@Composable
private fun ReaderProgressPill(pages: List<SagaPage>, currentIndex: Int, chapterNum: Int) {
    val chapterPages = pages.withIndex().filter { it.value.chapterNumber() == chapterNum }
    val scenes = chapterPages.filter { it.value is SagaPage.ScenePage }
    val current = pages.getOrNull(currentIndex)
    val sceneIdx = (current as? SagaPage.ScenePage)?.sceneIndex ?: scenes.size
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.9f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            scenes.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .size(width = if (i == sceneIdx) 26.dp else 16.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                i == sceneIdx && current is SagaPage.ScenePage -> ReaderCoral
                                i < sceneIdx -> ReaderMint
                                else -> ReaderDotInactive
                            }
                        )
                )
            }
            Text(
                text = "★",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (current !is SagaPage.ScenePage) ReaderGoldText else ReaderGoldBorder
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (current is SagaPage.ScenePage) "${sceneIdx + 1} / ${scenes.size}" else if (AppStrings.isEnglish) "Challenge" else "Reto",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ReaderInkDark
            )
        }
    }
}

private val SubtitleShadow = Shadow(color = Color.Black.copy(alpha = 0.75f), offset = Offset(0f, 2f), blurRadius = 12f)

/**
 * Cinema subtitles: one or two sentences at a time over the illustration, the narrated word in gold.
 * Word timings come from Amazon Polly speech marks, aligned to the whitespace-separated words by the backend.
 */
@Composable
private fun SubtitleBlock(
    text: String,
    words: List<IntRange>,
    chunks: List<IntRange>,
    chunkIndex: Int,
    activeWord: Int,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    if (chunks.isEmpty()) return
    val index = chunkIndex.coerceIn(0, chunks.lastIndex)
    val chunk = chunks[index]
    val start = words[chunk.first].first
    val slice = text.substring(start, words[chunk.last].last + 1)
    AnimatedContent(
        targetState = Triple(pageIndex, index, slice),
        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
        contentAlignment = Alignment.BottomCenter,
        label = "subtitle",
        modifier = modifier
    ) { (shownPage, shownIndex, shownSlice) ->
        val live = shownPage == pageIndex && shownIndex == index
        val annotated = buildAnnotatedString {
            append(shownSlice)
            if (live && activeWord in chunk) {
                val range = words[activeWord]
                addStyle(SpanStyle(color = ReaderGoldHighlight), range.first - start, range.last + 1 - start)
            }
        }
        Text(
            text = annotated,
            style = TextStyle(shadow = SubtitleShadow),
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** "Read it myself" mode: the whole page for a grown-up reading aloud, next to the controls. */
@Composable
private fun FullTextPanel(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.5.dp, ReaderCardBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        Text(
            text = if (AppStrings.isEnglish) "📖 READ-ALOUD MODE" else "📖 MODO LEER YO",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = ReaderGoldText
        )
        Spacer(modifier = Modifier.height(8.dp))
        HighlightedStoryText(text = text, wordTimingsMs = emptyList(), positionMs = -1L)
    }
}

@Composable
private fun EdgeChevron(glyph: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
    ) {
        Text(
            text = glyph,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

/** Vertical control rail: ▲ ▼ move between actions, so ◀ ▶ stay free for turning pages. */
@Composable
private fun ReaderControls(
    isPlayingAudio: Boolean,
    narrationDone: Boolean,
    fullTextMode: Boolean,
    canGoPrev: Boolean,
    nextLabel: String,
    onNext: (() -> Unit)?,
    onPrev: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleFullText: () -> Unit,
    nextFocusRequester: FocusRequester,
    audioFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val nextPulse = if (narrationDone && onNext != null) {
        val transition = rememberInfiniteTransition(label = "next_pulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "next_pulse_scale"
        )
        scale
    } else {
        1f
    }

    Column(
        modifier = modifier.width(204.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onNext != null) {
            TvClickableSurface(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = nextPulse
                        scaleY = nextPulse
                    }
                    .focusRequester(nextFocusRequester),
                shape = RoundedCornerShape(50),
                containerColor = ReaderCoral,
                focusedContainerColor = ReaderCoral,
                contentColor = Color.White,
                focusedScale = 1.06f,
                glowColor = ReaderCoral.copy(alpha = 0.45f),
                contentDescription = nextLabel,
                playClickSound = false
            ) {
                Text(
                    text = nextLabel,
                    fontFamily = TitleFont(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        TvClickableSurface(
            onClick = onToggleAudio,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .focusRequester(audioFocusRequester),
            shape = RoundedCornerShape(50),
            containerColor = if (isPlayingAudio) ReaderMintSoft else Color.White,
            borderColor = if (isPlayingAudio) ReaderMintBorder else ReaderCardBorder,
            contentDescription = if (isPlayingAudio) AppStrings.pauseNarration else AppStrings.listenPageWithVoice
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.Center)
            ) {
                AudioSpeakerIcon(
                    isPlaying = isPlayingAudio,
                    tint = if (isPlayingAudio) ReaderMint else ReaderCoral,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPlayingAudio) {
                        if (AppStrings.isEnglish) "Pause" else "Pausar"
                    } else {
                        if (AppStrings.isEnglish) "Listen" else "Escuchar"
                    },
                    fontFamily = TitleFont(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlayingAudio) ReaderMint else ReaderInkDark
                )
            }
        }

        if (canGoPrev) {
            TvClickableSurface(
                onClick = onPrev,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(50),
                containerColor = Color.White,
                borderColor = ReaderCardBorder,
                playClickSound = false,
                contentDescription = if (AppStrings.isEnglish) "Previous page" else "Página anterior"
            ) {
                Text(
                    text = if (AppStrings.isEnglish) "← Previous" else "← Anterior",
                    fontFamily = TitleFont(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReaderInkMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        TvClickableSurface(
            onClick = onToggleFullText,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(50),
            containerColor = if (fullTextMode) ReaderGoldSoft else Color.White,
            borderColor = if (fullTextMode) ReaderGoldBorder else ReaderCardBorder,
            contentDescription = if (AppStrings.isEnglish) "Show the whole page text" else "Ver el texto completo"
        ) {
            Text(
                text = when {
                    fullTextMode -> if (AppStrings.isEnglish) "✕ Hide text" else "✕ Ocultar texto"
                    else -> if (AppStrings.isEnglish) "📖 I'll read" else "📖 Leer yo"
                },
                fontFamily = TitleFont(),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (fullTextMode) ReaderGoldText else ReaderInkDark,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = if (AppStrings.isEnglish) "◀ ▶ turn the page" else "◀ ▶ pasar página",
            style = TextStyle(shadow = SubtitleShadow),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Whole page text (read-aloud mode), with optional karaoke highlight of the narrated word. */
@Composable
private fun HighlightedStoryText(text: String, wordTimingsMs: List<Int>, positionMs: Long) {
    val words = remember(text) { Regex("\\S+").findAll(text).map { it.range }.toList() }
    val activeIndex = if (positionMs < 0 || wordTimingsMs.size != words.size) {
        -1
    } else {
        wordTimingsMs.indexOfLast { it <= positionMs }
    }
    val annotated = remember(text, activeIndex) {
        buildAnnotatedString {
            append(text)
            if (activeIndex in words.indices) {
                val range = words[activeIndex]
                addStyle(
                    SpanStyle(background = ReaderGoldHighlight, color = ReaderInkDark),
                    range.first,
                    range.last + 1
                )
            }
        }
    }
    val compact = words.size > 60
    Text(
        text = annotated,
        fontSize = if (compact) 20.sp else 22.sp,
        lineHeight = if (compact) 29.sp else 32.sp,
        fontWeight = FontWeight.SemiBold,
        color = ReaderInkDark
    )
}

// =====================================================================
// CHAPTER-END PAGES: drawing mission and "challenge completed"
// =====================================================================

/** The chapter's own artwork, washed in cream, so end pages feel part of the same story. */
@Composable
private fun ChapterBackdrop(imageUrl: String?, animate: Boolean) {
    CinematicIllustration(url = imageUrl, contentDescription = null, animate = animate)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(ReaderCreamBg.copy(alpha = 0.55f), ReaderCreamBg.copy(alpha = 0.9f))
                )
            )
    )
}

/** Tilted instant-photo frame for a character portrait, with a ribbon badge on top. */
@Composable
private fun PolaroidPortrait(
    imageUrl: String?,
    contentDescription: String,
    badge: String,
    accent: Color,
    rotation: Float,
    imageSize: Dp
) {
    Box(contentAlignment = Alignment.TopCenter) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .padding(top = 16.dp)
                .rotate(rotation)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp)
                    .size(imageSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f))
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    WonderLoopImage(
                        model = imageUrl,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = "🎨", fontSize = 72.sp)
                }
            }
        }
        Surface(shape = RoundedCornerShape(50), color = accent, shadowElevation = 4.dp) {
            Text(
                text = badge,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/** Draw → scan → new chapter: the creative loop at a glance. */
@Composable
private fun CreativeLoopSteps(nextChapterNumber: Int, modifier: Modifier = Modifier) {
    val steps = listOf(
        "🖍️" to (if (AppStrings.isEnglish) "Draw it" else "Dibújalo"),
        "📱" to (if (AppStrings.isEnglish) "Scan it" else "Escanéalo"),
        "✨" to (if (AppStrings.isEnglish) "Chapter $nextChapterNumber" else "Capítulo $nextChapterNumber")
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, (emoji, label) ->
            if (i > 0) {
                Text(text = "›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ReaderInkMuted)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ReaderInkDark,
                    maxLines = 1
                )
            }
        }
    }
}

/** ◀ on the left-most action goes back a page; the other keys keep moving focus normally. */
private fun Modifier.previousPageOnLeft(onGoPrev: () -> Unit): Modifier = onPreviewKeyEvent {
    if (it.key == Key.DirectionLeft) {
        if (it.type == KeyEventType.KeyDown) onGoPrev()
        true
    } else {
        false
    }
}

@Composable
private fun RetoSuperadoPageContent(
    page: SagaPage.RetoSuperadoPage,
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
    val nextCover = page.nextChapter.scenes.firstOrNull()?.illustrationUrl
    val nextChapterFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            delay(120)
            try { nextChapterFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChapterBackdrop(imageUrl = nextCover, animate = isCurrentPage)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 84.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: the new friend the child drew
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PolaroidPortrait(
                    imageUrl = companionAvatar,
                    contentDescription = companionName,
                    badge = AppStrings.challengeAlreadyCompletedTitle.uppercase(),
                    accent = ReaderMint,
                    rotation = 3f,
                    imageSize = 220.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = companionName,
                    fontFamily = TitleFont(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReaderInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: what happened + the next chapter
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.96f))
                    .border(2.dp, ReaderMintBorder, RoundedCornerShape(32.dp))
                    .padding(horizontal = 30.dp, vertical = 24.dp)
            ) {
                Text(
                    text = AppStrings.challengeAlreadyCompletedBody(companionName),
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReaderInkDark,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ReaderGoldSoft)
                        .border(1.dp, ReaderGoldBorder, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!nextCover.isNullOrBlank()) {
                        WonderLoopImage(
                            model = nextCover,
                            contentDescription = nextChTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 128.dp, height = 72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            text = if (AppStrings.isEnglish) "🌟 CHAPTER $nextChNum IS READY" else "🌟 CAPÍTULO $nextChNum LISTO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = ReaderGoldText
                        )
                        Text(
                            text = nextChTitle,
                            fontFamily = TitleFont(),
                            fontSize = 21.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                TvClickableSurface(
                    onClick = onTurnToNextChapter,
                    enabled = isCurrentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .focusRequester(nextChapterFocusRequester)
                        .onPreviewKeyEvent {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    if (it.type == KeyEventType.KeyDown) onGoPrev()
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (it.type == KeyEventType.KeyDown) onTurnToNextChapter()
                                    true
                                }
                                else -> false
                            }
                        },
                    shape = RoundedCornerShape(50),
                    containerColor = ReaderMint,
                    focusedContainerColor = ReaderMint,
                    contentColor = Color.White,
                    glowColor = ReaderMint.copy(alpha = 0.4f),
                    contentDescription = if (AppStrings.isEnglish) "Read chapter $nextChNum" else "Leer el capítulo $nextChNum"
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "Read Chapter $nextChNum ➔" else "¡Leer Capítulo $nextChNum! ➔",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TvClickableSurface(
                    onClick = onGoToBookshelf,
                    enabled = isCurrentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .previousPageOnLeft(onGoPrev),
                    shape = RoundedCornerShape(50),
                    containerColor = Color.White,
                    borderColor = ReaderCardBorder,
                    contentDescription = AppStrings.goToMyBookshelf
                ) {
                    Text(
                        text = AppStrings.goToMyBookshelf,
                        fontFamily = TitleFont(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderInkMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        ConfettiEffect(particleCount = 30)
    }
}

@Composable
private fun ChallengePromptPageContent(
    page: SagaPage.ChallengePromptPage,
    isCurrentPage: Boolean = true,
    isPlayingAudio: Boolean,
    onToggleAudio: () -> Unit,
    onScanNextDrawing: () -> Unit,
    onGoToBookshelf: () -> Unit,
    onGoPrev: () -> Unit = {}
) {
    val hero = page.chapter.sagaHero
    val heroAvatar = hero.reinterpretedImageUrl
        ?: hero.originalDrawingBase64
        ?: page.chapter.scenes.firstOrNull()?.illustrationUrl
    val nextChapterNumber = (page.chapter.chapterIndex ?: page.chapterIndex) + 1
    val challenge = page.chapter.creativeChallenge
    val canNarrate = page.canNarrateChallenge()
    val scanFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            delay(120)
            try { scanFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChapterBackdrop(imageUrl = page.chapter.scenes.lastOrNull()?.illustrationUrl, animate = isCurrentPage)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 84.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: the hero asking for help + how the loop works
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (AppStrings.isEnglish) "Help ${hero.name}!" else "¡Ayuda a ${hero.name}!",
                    fontFamily = TitleFont(),
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReaderInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                PolaroidPortrait(
                    imageUrl = heroAvatar,
                    contentDescription = hero.name,
                    badge = if (AppStrings.isEnglish) "🖍️ DRAWING MISSION" else "🖍️ MISIÓN DE DIBUJO",
                    accent = ReaderCoral,
                    rotation = -3f,
                    imageSize = 184.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CreativeLoopSteps(nextChapterNumber = nextChapterNumber, modifier = Modifier.fillMaxWidth())
            }

            // Right: the mission itself, big enough to read from the sofa
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.96f))
                    .border(2.dp, ReaderGoldBorder, RoundedCornerShape(32.dp))
                    .padding(horizontal = 30.dp, vertical = 20.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = ReaderGoldSoft, border = BorderStroke(1.dp, ReaderGoldBorder)) {
                    Text(
                        text = if (AppStrings.isEnglish) "🎨 TO UNLOCK CHAPTER $nextChapterNumber" else "🎨 PARA DESBLOQUEAR EL CAPÍTULO $nextChapterNumber",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = ReaderGoldText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = challenge.prompt,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ReaderInkDark,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (challenge.encouragement.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ReaderMintSoft)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = challenge.encouragement,
                            fontSize = 18.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReaderInkDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TvClickableSurface(
                    onClick = onScanNextDrawing,
                    enabled = isCurrentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .focusRequester(scanFocusRequester)
                        .previousPageOnLeft(onGoPrev),
                    shape = RoundedCornerShape(50),
                    containerColor = ReaderCoral,
                    focusedContainerColor = ReaderCoral,
                    contentColor = Color.White,
                    glowColor = ReaderCoral.copy(alpha = 0.45f),
                    contentDescription = if (AppStrings.isEnglish) "I've drawn it, scan the drawing" else "Ya lo he dibujado, escanear el dibujo"
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "📱 I've drawn it! Scan it" else "📱 ¡Ya lo he dibujado! Escanear",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canNarrate) {
                        TvClickableSurface(
                            onClick = onToggleAudio,
                            enabled = isCurrentPage,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .previousPageOnLeft(onGoPrev),
                            shape = RoundedCornerShape(50),
                            containerColor = if (isPlayingAudio) ReaderMintSoft else Color.White,
                            borderColor = if (isPlayingAudio) ReaderMintBorder else ReaderCardBorder,
                            contentDescription = if (AppStrings.isEnglish) "Listen to the challenge" else "Escuchar el reto"
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                AudioSpeakerIcon(
                                    isPlaying = isPlayingAudio,
                                    tint = if (isPlayingAudio) ReaderMint else ReaderCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isPlayingAudio -> if (AppStrings.isEnglish) "Pause" else "Pausar"
                                        else -> if (AppStrings.isEnglish) "Listen" else "Escuchar"
                                    },
                                    fontFamily = TitleFont(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlayingAudio) ReaderMint else ReaderInkDark
                                )
                            }
                        }
                    }

                    TvClickableSurface(
                        onClick = onGoToBookshelf,
                        enabled = isCurrentPage,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(if (canNarrate) Modifier else Modifier.previousPageOnLeft(onGoPrev)),
                        shape = RoundedCornerShape(50),
                        containerColor = Color.White,
                        borderColor = ReaderCardBorder,
                        contentDescription = AppStrings.goToMyBookshelf
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "📚 Library" else "📚 Biblioteca",
                            fontFamily = TitleFont(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ReaderInkMuted,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        ConfettiEffect(particleCount = 30)
    }
}
