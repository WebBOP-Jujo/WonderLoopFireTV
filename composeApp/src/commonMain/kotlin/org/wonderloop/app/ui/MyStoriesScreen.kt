package org.wonderloop.app.ui

import org.wonderloop.app.ui.components.WonderBackdrop
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.SagaBook
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.tvFocusable

// New Design System Tokens
private val ShelfCreamBg = Color(0xFFFFFBF5)
private val ShelfInkDark = Color(0xFF2B2340)
private val ShelfInkMuted = Color(0xFF6B6480)
private val ShelfCoral = Color(0xFFD9384A)
private val ShelfCoralSoft = Color(0xFFFDECE9)
private val ShelfCoralBorder = Color(0xFFF9D0CA)
private val ShelfMint = Color(0xFF0E7F64)
private val ShelfMintSoft = Color(0xFFE4F6F0)
private val ShelfGoldSoft = Color(0xFFFFF1CF)
private val ShelfGoldBorder = Color(0xFFF3DC9B)
private val ShelfGoldText = Color(0xFF9E6700)
private val ShelfCardBorder = Color(0xFFEDE8E1)

// Warm Oak Wood Palette for authentic children's bookshelf
private val WoodHighlight = Color(0xFFEDC18F)
private val WoodPrimary = Color(0xFFBF7A45)
private val WoodGrain = Color(0xFFA6612E)
private val WoodShadow = Color(0xFF743E17)
private val ShelfBracketWood = Color(0xFF8E4D20)

@Composable
fun MyStoriesScreen(
    onBackClick: () -> Unit,
    onOpenStory: (FullStoryResponse) -> Unit,
    onNewAdventureClick: () -> Unit
) {
    var sagaToDelete by remember { mutableStateOf<SagaBook?>(null) }
    // Deleting is an adult action: the remote's Menu key on a book asks for the parental gate first
    var sagaPendingGate by remember { mutableStateOf<SagaBook?>(null) }
    var refocusTick by remember { mutableStateOf(0) }
    var openingSaga by remember { mutableStateOf<SagaBook?>(null) }
    val sagas = StoryRepository.getBookshelfSagas()
    val firstBookFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(140)
        try {
            firstBookFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Give D-pad focus back to the shelf after the parental gate closes
    LaunchedEffect(refocusTick) {
        if (refocusTick > 0) {
            delay(250)
            try { firstBookFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Trigger navigation once the 3D book-opening animation finishes
    LaunchedEffect(openingSaga) {
        val target = openingSaga ?: return@LaunchedEffect
        delay(580)
        onOpenStory(target.rootStory)
        delay(120)
        openingSaga = null
    }

    sagaPendingGate?.let { saga ->
        ParentalGateDialog(
            explanation = if (AppStrings.isEnglish) "Grown-ups only: solve it to delete this book." else "Solo adultos: resuélvelo para borrar este libro.",
            onDismiss = {
                sagaPendingGate = null
                refocusTick++
            },
            onSuccess = {
                sagaPendingGate = null
                sagaToDelete = saga
            }
        )
    }

    // Story / Saga Deletion Confirmation Modal (New Design)
    sagaToDelete?.let { saga ->
        val isMulti = saga.isMultiChapter
        Dialog(
            onDismissRequest = { sagaToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                    val cancelFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, ShelfCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                        modifier = Modifier.width(480.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Circular Blush Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ShelfCoralSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🗑️", fontSize = 34.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Story Title Pill
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = ShelfGoldSoft,
                                border = BorderStroke(1.dp, ShelfGoldBorder)
                            ) {
                                Text(
                                    text = "📖 ${saga.rootStory.title}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ShelfGoldText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isMulti) AppStrings.deleteSagaQuestion else AppStrings.deleteStoryQuestion,
                                fontFamily = TitleFont(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ShelfInkDark,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isMulti) {
                                    AppStrings.deleteSagaWarning(saga.rootStory.title, saga.chapterCount)
                                } else {
                                    AppStrings.deleteStoryWarning(saga.rootStory.title)
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ShelfInkMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Primary: Cancel (Safer for TV remote)
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, ShelfCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(cancelFocusRequester)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = { sagaToDelete = null }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = AppStrings.cancel,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ShelfInkDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary: Confirm Delete
                            val onDeleteConfirmed = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                StoryRepository.deleteSaga(saga.sagaId)
                                sagaToDelete = null
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = ShelfCoral,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = onDeleteConfirmed
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = AppStrings.confirmDelete,
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
        }
    }

    WonderBackdrop {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val availableWidth = (maxWidth - 96.dp).coerceAtLeast(260.dp)

        val columns = when {
            availableWidth >= 780.dp -> 4
            availableWidth >= 520.dp -> 3
            else -> 2
        }

        val spacing = 14.dp
        val totalSpacing = spacing * (columns + 1)
        val calculatedWidth = (availableWidth - totalSpacing) / columns
        val bookWidth = calculatedWidth.coerceIn(122.dp, 152.dp)

        val chunks = remember(sagas, columns) { sagas.chunked(columns) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 22.dp)
        ) {
            // =================================================================
            // 1. TOP BAR: Circular Back · Fredoka Title · Count Pill
            // =================================================================
            item(key = "top_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val onBack = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onBackClick()
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.5.dp, ShelfCardBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .tvFocusable(shape = CircleShape, focusedScale = 1.08f, onClick = onBack)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "←",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ShelfInkDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val activeProfile = ProfileRepository.activeProfile
                        Text(
                            text = if (activeProfile != null) AppStrings.bookshelfOf(activeProfile.name) else AppStrings.myBookshelf,
                            fontFamily = TitleFont(),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ShelfInkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (sagas.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = ShelfCoralSoft,
                            border = BorderStroke(1.dp, ShelfCoralBorder)
                        ) {
                            Text(
                                text = "📚 ${AppStrings.booksCount(sagas.size)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ShelfCoral,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (sagas.isEmpty()) {
                item(key = "empty_bookshelf") {
                    // Empty Bookshelf State (New Design)
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, ShelfCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(ShelfGoldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📖", fontSize = 38.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = AppStrings.emptyBookshelfTitle,
                                fontFamily = TitleFont(),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ShelfInkDark,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = AppStrings.emptyBookshelfDesc,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ShelfInkMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onNewAdventureClick()
                                },
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShelfCoral,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = AppStrings.createFirstBook,
                                    fontFamily = TitleFont(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    WoodenShelfPlank(modifier = Modifier.fillMaxWidth())
                }
            } else {
                item(key = "bookshelf_subtitle") {
                    Text(
                        text = AppStrings.bookshelfSubtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShelfInkMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(
                    items = chunks,
                    key = { shelfIndex, shelfSagas ->
                        "shelf_${shelfIndex}_${shelfSagas.firstOrNull()?.sagaId ?: shelfIndex}"
                    }
                ) { shelfIndex, shelfSagas ->
                    val shelfHasOpeningBook = shelfSagas.any { it.sagaId == openingSaga?.sagaId }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (shelfHasOpeningBook) 10f else 0f)
                            .padding(bottom = 22.dp)
                    ) {
                        // Row of books standing on the wooden shelf
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            shelfSagas.forEachIndexed { bookIndex, saga ->
                                val isThisBookOpening = openingSaga?.sagaId == saga.sagaId
                                BookCoverItem(
                                    saga = saga,
                                    isOpening = isThisBookOpening,
                                    focusRequester = if (shelfIndex == 0 && bookIndex == 0) firstBookFocusRequester else null,
                                    onClick = {
                                        if (openingSaga == null) {
                                            PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
                                            openingSaga = saga
                                        }
                                    },
                                    onDelete = {
                                        if (openingSaga == null) {
                                            sagaPendingGate = saga
                                        }
                                    },
                                    modifier = Modifier
                                        .width(bookWidth)
                                        .zIndex(if (isThisBookOpening) 10f else 0f)
                                )
                            }

                            val isLastShelf = shelfIndex == chunks.size - 1
                            if (isLastShelf && shelfSagas.size < columns) {
                                AddBookPlaceholder(
                                    focusRequester = if (sagas.isEmpty()) firstBookFocusRequester else null,
                                    onClick = {
                                        if (openingSaga == null) {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            onNewAdventureClick()
                                        }
                                    },
                                    modifier = Modifier.width(bookWidth)
                                )
                            }
                        }

                        // Authentic Wooden Shelf Plank directly beneath the books
                        WoodenShelfPlank(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }

                // If all shelves are full, add a dedicated shelf with the "New Book" bookstand
                if (sagas.size % columns == 0) {
                    item(key = "extra_new_book_shelf") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                AddBookPlaceholder(
                                    focusRequester = if (sagas.isEmpty()) firstBookFocusRequester else null,
                                    onClick = {
                                        if (openingSaga == null) {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            onNewAdventureClick()
                                        }
                                    },
                                    modifier = Modifier.width(bookWidth)
                                )
                            }
                            WoodenShelfPlank(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * Children's Picture Book Stand on the Wooden Shelf with 3D Cover-Opening Transition
 */
@Composable
private fun BookCoverItem(
    saga: SagaBook,
    isOpening: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val story = saga.rootStory
    val coverImageUrl = story.character.reinterpretedImageUrl
        ?: story.character.originalDrawingBase64
        ?: story.scenes.firstOrNull()?.illustrationUrl
    val firstPageIllustration = story.scenes.firstOrNull()?.illustrationUrl ?: coverImageUrl

    val bookShape = RoundedCornerShape(
        topStart = 5.dp,
        bottomStart = 5.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp
    )

    // 3D cover swing angle (0° closed -> -135° open hinged on the left spine)
    val coverRotationY by animateFloatAsState(
        targetValue = if (isOpening) -135f else 0f,
        animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
        label = "book_cover_swing_y"
    )

    // Slight lift scale when opening
    val bookScale by animateFloatAsState(
        targetValue = if (isOpening) 1.07f else 1f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "book_lift_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.graphicsLayer {
            scaleX = bookScale
            scaleY = bookScale
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .onKeyEvent {
                    if (it.key == Key.Menu && it.type == KeyEventType.KeyUp) {
                        onDelete()
                        true
                    } else false
                }
                .tvFocusable(
                    shape = bookShape,
                    focusedScale = 1.06f,
                    focusRequester = focusRequester,
                    onClick = onClick
                )
        ) {
            // =================================================================
            // LAYER 1 (UNDERNEATH): First Inside Page of the Story
            // Revealed as the hardcover front cover swings open!
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 6.dp,
                        shape = bookShape,
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(bookShape)
                    .background(ShelfCreamBg)
                    .border(1.5.dp, ShelfCardBorder, bookShape)
            ) {
                // Right page-block paper edges effect
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE5DEC9),
                                    Color.White
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ShelfCoralSoft
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "✨ PAGE 1" else "✨ PÁG. 1",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ShelfCoral,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ShelfGoldSoft)
                    ) {
                        if ((isOpening || coverRotationY < -1f) && !firstPageIllustration.isNullOrBlank()) {
                            WonderLoopImage(
                                model = firstPageIllustration,
                                contentDescription = story.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = story.title,
                        fontFamily = TitleFont(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShelfInkDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }

                // Inner spine shadow on the left edge of the page
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // =================================================================
            // LAYER 2 (ON TOP): 3D Swinging Front Cover (hinged on left spine)
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = 16f * density
                        rotationY = coverRotationY
                    }
                    .shadow(
                        elevation = if (isOpening) 12.dp else 8.dp,
                        shape = bookShape,
                        ambientColor = Color.Black.copy(alpha = 0.32f),
                        spotColor = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(bookShape)
                    .background(Color.White)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), bookShape)
            ) {
                if (coverRotationY > -90f) {
                    // Front side of the book cover
                    if (!coverImageUrl.isNullOrBlank()) {
                        WonderLoopImage(
                            model = coverImageUrl,
                            contentDescription = story.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ShelfGoldSoft, ShelfCoralSoft)
                                    )
                                )
                        ) {
                            Text(text = "📖", fontSize = 42.sp)
                        }
                    }

                    // Realistic Book Spine Effect on Left Edge
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(13.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.42f),
                                        Color.White.copy(alpha = 0.28f),
                                        Color.Black.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Top Ribbon Bookmark (Pages count or Saga Chapters count)
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        color = if (saga.isMultiChapter) ShelfGoldSoft else ShelfCoral,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = if (saga.isMultiChapter) {
                                "🌟 ${AppStrings.sagaChaptersBadge(saga.chapterCount)}"
                            } else {
                                AppStrings.pagesCountRibbon(story.scenes.size)
                            },
                            color = if (saga.isMultiChapter) ShelfGoldText else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }


                    // Bottom Scrim & Title on the Book Cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF2B2340).copy(alpha = 0.78f),
                                        Color(0xFF2B2340).copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = story.title,
                                fontFamily = TitleFont(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 15.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (saga.isMultiChapter) "🌟 Saga · ${story.character.name}" else "✨ ${story.character.name}",
                                color = ShelfGoldSoft,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    // Inside lining of the front cover when swung past -90°
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ShelfGoldSoft, ShelfCreamBg)
                                )
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✨", fontSize = 22.sp)
                        }
                    }
                }
            }
        }

        // Contact shadow right at the foot of the book resting on the shelf
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Whimsical bookstand slot inviting the child to create a new story on the shelf
 */
@Composable
private fun AddBookPlaceholder(
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookShape = RoundedCornerShape(
        topStart = 5.dp,
        bottomStart = 5.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = bookShape,
            color = Color.White.copy(alpha = 0.88f),
            border = BorderStroke(2.dp, ShelfCoralBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .tvFocusable(
                    shape = bookShape,
                    focusedScale = 1.06f,
                    focusRequester = focusRequester,
                    onClick = onClick
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ShelfCoralSoft)
                ) {
                    Text(text = "📸", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = AppStrings.newStoryBookstand,
                    fontFamily = TitleFont(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ShelfInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (AppStrings.isEnglish) "📱 Scan a drawing" else "📱 Escanea un dibujo",
                    fontSize = 13.sp,
                    color = ShelfMint,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // Contact shadow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Authentic Wooden Shelf Plank (Balda de madera)
 */
@Composable
fun WoodenShelfPlank(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Shelf Top Surface / Highlight Bevel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WoodHighlight,
                            WoodPrimary
                        )
                    )
                )
        )

        // Shelf Front Plank (Thick Wooden Beam)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WoodPrimary,
                            WoodGrain,
                            WoodShadow
                        )
                    )
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Bracket Detail
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(ShelfBracketWood)
            )

            // Subtle wood grain highlight across the front
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(WoodHighlight.copy(alpha = 0.18f))
            )

            // Right Bracket Detail
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(ShelfBracketWood)
            )
        }

        // Deep 3D Cast Shadow on the wall underneath the shelf
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
