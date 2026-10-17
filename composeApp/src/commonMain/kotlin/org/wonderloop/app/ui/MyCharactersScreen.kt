package org.wonderloop.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.tvFocusable

// New Design System Tokens
private val BoardCreamBg = Color(0xFFFFFBF5)
private val BoardInkDark = Color(0xFF2B2340)
private val BoardInkMuted = Color(0xFF6B6480)
private val BoardCoral = Color(0xFFD9384A)
private val BoardCoralSoft = Color(0xFFFDECE9)
private val BoardCoralBorder = Color(0xFFF9D0CA)
private val BoardMint = Color(0xFF0E7F64)
private val BoardMintSoft = Color(0xFFE4F6F0)
private val BoardMintBorder = Color(0xFFB8E6D6)
private val BoardGoldSoft = Color(0xFFFFF1CF)
private val BoardGoldBorder = Color(0xFFF3DC9B)
private val BoardGoldText = Color(0xFF9E6700)
private val BoardCardBorder = Color(0xFFEDE8E1)

// Corkboard & Frame Palette
private val CorkWarm = Color(0xFFE6C594)
private val CorkMottled = Color(0xFFD7AD73)
private val CorkSpeckDark = Color(0xFFB8884B)
private val CorkSpeckLight = Color(0xFFF3DAC2)
private val CorkFrameOuter = Color(0xFF8E4D20)
private val CorkFrameInner = Color(0xFFB97440)
private val PaperPolaroid = Color(0xFFFFFFFF)

// Pushpin Colors harmonized with the new palette
private val PinColors = listOf(
    Color(0xFFD9384A), // Coral Hero
    Color(0xFFFFB830), // Sunny Gold
    Color(0xFF0E7F64), // Mint Accent
    Color(0xFF3B82F6), // Sky Blue
    Color(0xFF9B59B6)  // Lavender Purple
)

data class CharacterWithStories(
    val character: CharacterProfile,
    val stories: List<FullStoryResponse>
)

@Composable
fun MyCharactersScreen(
    onBackClick: () -> Unit,
    onOpenStory: (FullStoryResponse) -> Unit,
    onNewAdventureClick: () -> Unit
) {
    val stories = StoryRepository.getStoriesForProfile()

    // Group stories by character name (both chapter characters and saga founder heroes)
    val charactersWithStories = remember(stories) {
        val map = linkedMapOf<String, MutableList<FullStoryResponse>>()
        val charProfiles = linkedMapOf<String, CharacterProfile>()
        val defaultName = if (AppStrings.isEnglish) "Magic Hero" else "Héroe Mágico"
        stories.forEach { story ->
            val charKey = story.character.name.trim().ifBlank { defaultName }
            if (!charProfiles.containsKey(charKey)) {
                charProfiles[charKey] = story.character
            }
            val list = map.getOrPut(charKey) { mutableListOf() }
            if (list.none { it.storyId == story.storyId }) {
                list.add(story)
            }

            // Also include continuation chapters in the saga founder hero's story list
            val founderHero = story.continuation?.effectiveHero
            if (founderHero != null) {
                val founderKey = founderHero.name.trim().ifBlank { defaultName }
                if (!founderKey.equals(charKey, ignoreCase = true)) {
                    if (!charProfiles.containsKey(founderKey)) {
                        charProfiles[founderKey] = founderHero
                    }
                    val founderList = map.getOrPut(founderKey) { mutableListOf() }
                    if (founderList.none { it.storyId == story.storyId }) {
                        founderList.add(story)
                    }
                }
            }
        }
        map.map { (key, storyList) ->
            val sortedList = storyList.sortedBy { it.chapterIndex ?: 1 }
            CharacterWithStories(
                character = charProfiles[key] ?: sortedList.first().character,
                stories = sortedList
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoardCreamBg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BoardMintSoft.copy(alpha = 0.45f),
                            BoardGoldSoft.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxOf(size.width, size.height) * 0.65f
                    )
                )
            }
            .padding(horizontal = 48.dp, vertical = 20.dp) // Fire TV 5% Safe Zone
    ) {
        // =================================================================
        // 1. TOP BAR: Circular Back · Fredoka Title · Mint Count Pill
        // =================================================================
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
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.5.dp, BoardCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "←",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BoardInkDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val activeProfile = ProfileRepository.activeProfile
                Text(
                    text = if (activeProfile != null) AppStrings.friendsOf(activeProfile.name) else AppStrings.friendsBoard,
                    fontFamily = TitleFont(),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BoardInkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (charactersWithStories.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = BoardMintSoft,
                    border = BorderStroke(1.dp, BoardMintBorder)
                ) {
                    Text(
                        text = "🧸 ${AppStrings.friendsCount(charactersWithStories.size)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BoardMint,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle guide
        Text(
            text = AppStrings.boardSubtitle,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = BoardInkMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // =================================================================
        // 2. WOOD-FRAMED CORKBOARD BULLETIN BOARD
        // =================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(8.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(CorkFrameOuter, CorkFrameInner, CorkFrameOuter)
                    )
                )
                .padding(7.dp) // Outer wooden frame thickness
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CorkWarm,
                            CorkMottled,
                            CorkWarm
                        )
                    )
                )
                .border(1.5.dp, Color.Black.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
        ) {
            // Subtle cork grain texture speckles
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val cols = 9
                val rows = 14
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val fx = ((c * 37 + r * 19) % 100) / 100f
                        val fy = ((r * 29 + c * 13) % 100) / 100f
                        drawCircle(
                            color = if ((r + c) % 2 == 0) CorkSpeckDark.copy(alpha = 0.22f) else CorkSpeckLight.copy(alpha = 0.25f),
                            radius = if ((r * c) % 3 == 0) 3.2f else 2.1f,
                            center = Offset(w * fx, h * fy)
                        )
                    }
                }
            }

            if (charactersWithStories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Empty Board State (Pinned Note Card on the Corkboard)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, BoardCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BoardCoralSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📌", fontSize = 34.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = AppStrings.emptyBoardTitle,
                                fontFamily = TitleFont(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BoardInkDark,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = AppStrings.emptyBoardDesc,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BoardInkMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            val onPinFirst = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onNewAdventureClick()
                            }
                            Button(
                                onClick = onPinFirst,
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BoardCoral,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .tvFocusable(shape = RoundedCornerShape(50.dp), onClick = onPinFirst)
                            ) {
                                Text(
                                    text = AppStrings.pinFirstDrawing,
                                    fontFamily = TitleFont(),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                // Responsive Lazy Grid of Pinned Polaroid Drawings + "Add Friend" Polaroid
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val availableWidth = (maxWidth - 28.dp).coerceAtLeast(240.dp)

                    val columns = when {
                        availableWidth >= 740.dp -> 4
                        availableWidth >= 500.dp -> 3
                        else -> 2
                    }

                    val spacing = 12.dp
                    val totalSpacing = spacing * (columns + 1)
                    val calculatedWidth = (availableWidth - totalSpacing) / columns
                    val sheetWidth = calculatedWidth.coerceIn(134.dp, 158.dp)

                    val allItems: List<CharacterWithStories?> = remember(charactersWithStories) {
                        charactersWithStories + listOf(null)
                    }
                    val chunks = remember(allItems, columns) { allItems.chunked(columns) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        itemsIndexed(
                            items = chunks,
                            key = { rowIndex, rowItems ->
                                "board_row_${rowIndex}_${rowItems.firstOrNull()?.character?.name ?: "add"}"
                            }
                        ) { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowItems.forEachIndexed { itemIndex, item ->
                                    val overallIndex = (rowIndex * columns) + itemIndex
                                    val rotation = remember(overallIndex) {
                                        when (overallIndex % 4) {
                                            0 -> -1.6f
                                            1 -> 1.8f
                                            2 -> -1.1f
                                            else -> 1.5f
                                        }
                                    }
                                    val pinColor = PinColors[overallIndex % PinColors.size]

                                    if (item != null) {
                                        PinnedDrawingSheet(
                                            character = item.character,
                                            stories = item.stories,
                                            onOpenStory = onOpenStory,
                                            pinColor = pinColor,
                                            rotation = rotation,
                                            modifier = Modifier.width(sheetWidth)
                                        )
                                    } else {
                                        AddCharacterPolaroidSlot(
                                            pinColor = pinColor,
                                            rotation = rotation,
                                            onClick = {
                                                PlatformAudioService.playSfx(SoundEffect.POP)
                                                onNewAdventureClick()
                                            },
                                            modifier = Modifier.width(sheetWidth)
                                        )
                                    }
                                }

                                val remainder = columns - rowItems.size
                                if (remainder > 0) {
                                    repeat(remainder) {
                                        Spacer(modifier = Modifier.width(sheetWidth))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A pinned artwork Polaroid sheet with a colorful 3D pushpin on top,
 * photo area with tap-to-transform animation, Fredoka character name,
 * compact flip badge, and pill story button.
 */
@Composable
private fun PinnedDrawingSheet(
    character: CharacterProfile,
    stories: List<FullStoryResponse>,
    onOpenStory: (FullStoryResponse) -> Unit,
    pinColor: Color,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val originalDrawing = character.originalDrawingBase64?.takeIf { it.isNotBlank() }
    val reinterpretedImage = character.reinterpretedImageUrl?.takeIf { it.isNotBlank() }
        ?: stories.firstOrNull()?.scenes?.firstOrNull()?.illustrationUrl?.takeIf { it.isNotBlank() }

    val hasBothImages = !originalDrawing.isNullOrBlank() && !reinterpretedImage.isNullOrBlank()
    var showOriginal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(top = 8.dp)
    ) {
        // 1. Polaroid Paper Sheet
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperPolaroid),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.5.dp, BoardCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Photo Display (Tap to transform between AI watercolor & Original drawing)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.94f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BoardGoldSoft, BoardCoralSoft)
                            )
                        )
                        .border(1.dp, BoardCardBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = hasBothImages) {
                            PlatformAudioService.playSfx(if (showOriginal) SoundEffect.POP else SoundEffect.MAGIC_CHIME)
                            showOriginal = !showOriginal
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = showOriginal,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300)))
                                .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.05f, animationSpec = tween(300)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "pin_sheet_art_transition"
                    ) { isShowingOriginal ->
                        val activeImage = if (isShowingOriginal) originalDrawing else (reinterpretedImage ?: originalDrawing)

                        if (!activeImage.isNullOrBlank()) {
                            WonderLoopImage(
                                model = activeImage,
                                contentDescription = character.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(text = "🎨", fontSize = 36.sp)
                        }
                    }

                    // Top-Right Magic Wand / Stamp Indicator
                    if (hasBothImages) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(25.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (showOriginal) "🖍️" else "✨",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Bottom Scrim Label
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            BoardInkDark.copy(alpha = 0.82f)
                                        )
                                    )
                                )
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showOriginal) AppStrings.paperDrawingBadge else AppStrings.aiVersionBadge,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Character Name in Fredoka
                Text(
                    text = character.name,
                    fontFamily = TitleFont(),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BoardInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Concise Tap-to-Transform Pill that always fits on one line
                Spacer(modifier = Modifier.height(4.dp))

                val tapFeedbackText = if (hasBothImages) {
                    if (showOriginal) {
                        if (AppStrings.isEnglish) "✨ See magic" else "✨ Ver magia"
                    } else {
                        if (AppStrings.isEnglish) "👆 See drawing" else "👆 Ver dibujo"
                    }
                } else if (character.species.isNotBlank()) {
                    character.species
                } else {
                    AppStrings.magicalFriend
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (showOriginal) BoardCoralSoft else BoardMintSoft,
                    border = BorderStroke(1.dp, if (showOriginal) BoardCoralBorder else BoardMintBorder),
                    modifier = Modifier.clickable(enabled = hasBothImages) {
                        PlatformAudioService.playSfx(if (showOriginal) SoundEffect.POP else SoundEffect.MAGIC_CHIME)
                        showOriginal = !showOriginal
                    }
                ) {
                    Text(
                        text = tapFeedbackText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (showOriginal) BoardCoral else BoardMint,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                // Action: Read Story Pill Button
                val firstStory = stories.firstOrNull()
                if (firstStory != null) {
                    val onReadFirstStory = {
                        PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
                        onOpenStory(firstStory)
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = BoardCoral,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .tvFocusable(shape = RoundedCornerShape(50.dp), onClick = onReadFirstStory)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.readStoryButton(stories.size),
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. 3D Colorful Pushpin (Overlapping the top center of the Polaroid)
        Pushpin3D(
            pinColor = pinColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
        )
    }
}

/**
 * Clean, balanced pinned Polaroid slot on the corkboard to scan a new character drawing
 */
@Composable
private fun AddCharacterPolaroidSlot(
    pinColor: Color,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(top = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperPolaroid),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.5.dp, BoardCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(
                    shape = RoundedCornerShape(16.dp),
                    focusedScale = 1.06f,
                    onClick = onClick
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Photo area matching PinnedDrawingSheet aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.94f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BoardGoldSoft.copy(alpha = 0.55f),
                                    BoardCoralSoft.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(1.5.dp, BoardCoralBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, BoardCoralBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎨",
                            fontSize = 25.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title matching Character Name height
                Text(
                    text = if (AppStrings.isEnglish) "New friend" else "Nuevo amigo",
                    fontFamily = TitleFont(),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BoardInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Compact single-line pill matching the tap-feedback pill height
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = BoardGoldSoft,
                    border = BorderStroke(1.dp, BoardGoldBorder)
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "✨ Draw on paper" else "✨ Dibuja en papel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BoardGoldText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                // Action Pill Button ("📸 Escanear")
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BoardCoral,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "📸 Scan" else "📸 Escanear",
                        fontFamily = TitleFont(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Pushpin3D(
            pinColor = pinColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
        )
    }
}

@Composable
private fun Pushpin3D(
    pinColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        pinColor,
                        pinColor.copy(alpha = 0.82f)
                    )
                )
            )
            .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
    )
}
