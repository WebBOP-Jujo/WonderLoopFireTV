package org.wonderloop.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.SagaBook
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*

private val HomeCreamBg = Color(0xFFFFFBF5)
private val HomeInkDark = Color(0xFF2B2340)
private val HomeInkMuted = Color(0xFF6B6480)
private val HomeCoral = Color(0xFFD9384A)
private val HomeCoralWarm = Color(0xFFE64C5E)
private val HomeCoralSoft = Color(0xFFFDECE9)
private val HomeCoralBorder = Color(0xFFF9D0CA)
private val HomeMint = Color(0xFF0E7F64)
private val HomeMintSoft = Color(0xFFE4F6F0)
private val HomeMintBorder = Color(0xFFB8E6D6)
private val HomeGoldSoft = Color(0xFFFFF1CF)
private val HomeGoldBorder = Color(0xFFF3DC9B)
private val HomeGoldText = Color(0xFF9E6700)
private val HomeCardBorder = Color(0xFFEDE8E1)

private val HomePinColors = listOf(
    Color(0xFFD9384A), // Coral Hero
    Color(0xFFFFB830), // Sunny Gold
    Color(0xFF0E7F64), // Mint Accent
    Color(0xFF3B82F6), // Sky Blue
    Color(0xFF9B59B6)  // Lavender Purple
)

@Composable
fun HomeScreen(
    childName: String,
    childAvatar: String = "🧒",
    backendOnline: Boolean?,
    onNewAdventureClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSwitchProfileClick: () -> Unit = {},
    onNavigateToStories: () -> Unit = {},
    onNavigateToCharacters: () -> Unit = {},
    onParentsClick: () -> Unit = onEditProfileClick,
    onRetryConnection: () -> Unit = {},
    onOpenStory: (FullStoryResponse) -> Unit = {}
) {
    val profileStories = StoryRepository.getStoriesForProfile()
    val bookshelfSagas = StoryRepository.getBookshelfSagas()
    val lastReadStory = StoryRepository.getLastReadStoryForProfile()
    val availableStories by SubscriptionManager.availableStories
    var openingSaga by remember { mutableStateOf<SagaBook?>(null) }

    // Trigger navigation once the 3D book-opening animation finishes
    LaunchedEffect(openingSaga) {
        val target = openingSaga ?: return@LaunchedEffect
        delay(540)
        onOpenStory(target.rootStory)
        delay(120)
        openingSaga = null
    }

    val uniqueCharacters = remember(profileStories) {
        buildList {
            profileStories.forEach { story ->
                add(story.character)
                story.continuation?.effectiveHero?.let { add(it) }
            }
        }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
    }

    Scaffold(
        containerColor = HomeCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            HomeGoldSoft.copy(alpha = 0.4f),
                            HomeCoralSoft.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP BAR: Profile Pill · Available Stories / Status · Child Profile · Parental Lock
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Pill [🚀 Leo ▾] -> Switches profile
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, HomeCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                        .clickable {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onSwitchProfileClick()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HomeCoralSoft)
                        ) {
                            Text(text = childAvatar, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = childName,
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeInkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "▾",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeInkMuted
                        )
                    }
                }

                // Right controls: Offline Pill + Child Profile Button + Parental Lock Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (backendOnline == false) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HomeCoralSoft,
                            border = BorderStroke(1.dp, HomeCoralBorder),
                            modifier = Modifier.clickable { onRetryConnection() }
                        ) {
                            Text(
                                text = "🔴 Sin IA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HomeCoral,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Child Profile Button ("Mi Perfil")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, HomeCardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onEditProfileClick()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    color = HomeCoralSoft,
                                    radius = w * 0.52f,
                                    center = Offset(w * 0.5f, h * 0.5f)
                                )
                                drawCircle(
                                    color = HomeCoral,
                                    radius = w * 0.19f,
                                    center = Offset(w * 0.5f, h * 0.35f)
                                )
                                val shouldersPath = Path().apply {
                                    moveTo(w * 0.22f, h * 0.78f)
                                    cubicTo(
                                        w * 0.22f, h * 0.56f,
                                        w * 0.78f, h * 0.56f,
                                        w * 0.78f, h * 0.78f
                                    )
                                    close()
                                }
                                drawPath(
                                    path = shouldersPath,
                                    color = HomeCoral
                                )
                            }
                        }
                    }

                    // Parental Lock Button ("Zona de padres") -> Vector padlock matching ParentalGateDialog
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, HomeCardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onParentsClick()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    color = HomeCoralSoft,
                                    radius = w * 0.52f,
                                    center = Offset(w * 0.5f, h * 0.5f)
                                )

                                val iconSize = w * 0.68f
                                val ox = (w - iconSize) / 2f
                                val oy = (h - iconSize) / 2f
                                val sx = iconSize / 24f
                                val sy = iconSize / 24f
                                val strokeW = 2.1.dp.toPx()

                                drawRoundRect(
                                    color = HomeCoral,
                                    topLeft = Offset(ox + 5f * sx, oy + 11f * sy),
                                    size = androidx.compose.ui.geometry.Size(14f * sx, 10f * sy),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.4f * sx, 2.4f * sy),
                                    style = Stroke(
                                        width = strokeW,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                val shacklePath = Path().apply {
                                    moveTo(ox + 8f * sx, oy + 11f * sy)
                                    lineTo(ox + 8f * sx, oy + 8f * sy)
                                    arcTo(
                                        rect = androidx.compose.ui.geometry.Rect(
                                            left = ox + 8f * sx,
                                            top = oy + 4f * sy,
                                            right = ox + 16f * sx,
                                            bottom = oy + 12f * sy
                                        ),
                                        startAngleDegrees = 180f,
                                        sweepAngleDegrees = 180f,
                                        forceMoveTo = false
                                    )
                                    lineTo(ox + 16f * sx, oy + 11f * sy)
                                }
                                drawPath(
                                    path = shacklePath,
                                    color = HomeCoral,
                                    style = Stroke(
                                        width = strokeW,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // 2. HERO CARD: "Crea tu cuento" (Coral Gradient #D9384A)
            // -------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = HomeCoral),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HomeCoral, HomeCoralWarm)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header: Title + Speaker Read-Aloud
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.createStoryTitle,
                                fontFamily = TitleFont(),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            Surface(
                                onClick = {
                                    PlatformAudioService.speak(AppStrings.audioCreateStory)
                                },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Canvas(modifier = Modifier.size(20.dp)) {
                                        val sx = size.width / 24f
                                        val sy = size.height / 24f
                                        val strokeW = 2.1.dp.toPx()
                                        val strokeStyle = Stroke(
                                            width = strokeW,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )

                                        // Speaker body & cone outline matching padlock vector style
                                        val conePath = Path().apply {
                                            moveTo(11f * sx, 5f * sy)
                                            lineTo(6f * sx, 9f * sy)
                                            lineTo(2f * sx, 9f * sy)
                                            lineTo(2f * sx, 15f * sy)
                                            lineTo(6f * sx, 15f * sy)
                                            lineTo(11f * sx, 19f * sy)
                                            close()
                                        }
                                        drawPath(
                                            path = conePath,
                                            color = Color.White,
                                            style = strokeStyle
                                        )

                                        // Inner sound wave arc
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -45f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = Offset(7f * sx, 7f * sy),
                                            size = androidx.compose.ui.geometry.Size(10f * sx, 10f * sy),
                                            style = strokeStyle
                                        )

                                        // Outer sound wave arc
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -45f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = Offset(2f * sx, 2f * sy),
                                            size = androidx.compose.ui.geometry.Size(20f * sx, 20f * sy),
                                            style = strokeStyle
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = AppStrings.createStorySubtitle,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.94f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Step Pills: 1 Dibuja · 2 Foto · 3 Magia
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White
                            ) {
                                Text(
                                    text = AppStrings.stepDrawShort,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HomeCoral,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = AppStrings.stepPhotoShort,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = AppStrings.stepMagicShort,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full-width White Pill CTA Button
                        Button(
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onNewAdventureClick()
                            },
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = HomeCoral
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📷", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${AppStrings.scanDrawingShort} ($availableStories)",
                                    fontFamily = TitleFont(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HomeCoral
                                )
                            }
                        }
                    }
                }
            }

            // If the child has not created/read any story yet, show "Idea de hoy" right under "Crea tu cuento"
            // to spark their imagination for their very first drawing.
            if (lastReadStory == null) {
                Spacer(modifier = Modifier.height(14.dp))
                DailySparkCard()
            }

            // -------------------------------------------------------------
            // 3. CONTINUE READING CARD: "Continuar leyendo" (Last read story)
            // -------------------------------------------------------------
            if (lastReadStory != null) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.continueReadingTitle,
                        fontFamily = TitleFont(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeInkDark
                    )

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = HomeGoldSoft,
                        border = BorderStroke(1.dp, HomeGoldBorder)
                    ) {
                        Text(
                            text = AppStrings.continueReadingBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeGoldText,
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, HomeCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
                            onOpenStory(lastReadStory)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini Hardcover Book Cover Thumbnail
                        val miniBookShape = RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        )
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(80.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = miniBookShape,
                                    ambientColor = Color.Black.copy(alpha = 0.25f),
                                    spotColor = Color.Black.copy(alpha = 0.35f)
                                )
                                .clip(miniBookShape)
                                .background(HomeCoralSoft)
                                .border(1.dp, Color.White.copy(alpha = 0.85f), miniBookShape)
                        ) {
                            val coverUrl = lastReadStory.character.reinterpretedImageUrl
                                ?: lastReadStory.character.originalDrawingBase64
                                ?: lastReadStory.scenes.firstOrNull()?.illustrationUrl
                            if (!coverUrl.isNullOrBlank()) {
                                WonderLoopImage(
                                    model = coverUrl,
                                    contentDescription = lastReadStory.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(text = "📖", fontSize = 26.sp)
                                }
                            }

                            // Mini Book Left Spine Crease
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(8.dp)
                                    .align(Alignment.CenterStart)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.40f),
                                                Color.White.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            val chIdx = lastReadStory.chapterIndex ?: 1
                            if (lastReadStory.isMultiChapter || chIdx > 1) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = HomeGoldSoft.copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, HomeGoldBorder),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "Cap. $chIdx",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HomeGoldText,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Story Title + Hero & Pages Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lastReadStory.title,
                                fontFamily = TitleFont(),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeInkDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.heroAndPagesSubtitle(
                                    heroName = lastReadStory.character.name,
                                    pages = lastReadStory.scenes.size
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Coral Pill CTA
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HomeCoral,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = AppStrings.continueReadingCta,
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -------------------------------------------------------------
            // 4. CAROUSEL: "Mis cuentos" (Hardcover Storybooks without Shelf Planks)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.myStories,
                    fontFamily = TitleFont(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeInkDark
                )

                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onNavigateToStories()
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = HomeCoralSoft
                ) {
                    Text(
                        text = AppStrings.seeAll,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HomeCoral,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val displayedSagas = remember(bookshelfSagas) { bookshelfSagas.take(8) }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Existing Storybooks (grouped by saga just like MyStoriesScreen)
                items(
                    items = displayedSagas,
                    key = { it.sagaId }
                ) { saga ->
                    val isThisBookOpening = openingSaga?.sagaId == saga.sagaId
                    HomeBookCoverItem(
                        saga = saga,
                        isOpening = isThisBookOpening,
                        onClick = {
                            if (openingSaga == null) {
                                PlatformAudioService.playSfx(SoundEffect.PAGE_TURN)
                                openingSaga = saga
                            }
                        },
                        modifier = Modifier
                            .width(114.dp)
                            .zIndex(if (isThisBookOpening) 10f else 0f)
                    )
                }

                // "+ Nuevo" Book-Shaped Card
                item(key = "home_new_book_placeholder") {
                    HomeNewBookPlaceholder(
                        onClick = {
                            if (openingSaga == null) {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onNewAdventureClick()
                            }
                        },
                        modifier = Modifier.width(114.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // -------------------------------------------------------------
            // 5. CAROUSEL: "Mis amigos"
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.myFriends,
                    fontFamily = TitleFont(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeInkDark
                )

                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onNavigateToCharacters()
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = HomeMintSoft
                ) {
                    Text(
                        text = AppStrings.seeAll,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HomeMint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val displayedCharacters = remember(uniqueCharacters) { uniqueCharacters.take(8) }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Unique Friends / Characters as Mini-Polaroids with 3D Pushpin (no corkboard)
                itemsIndexed(
                    items = displayedCharacters,
                    key = { index, character -> "friend_${character.name}_$index" }
                ) { index, character ->
                    val rotation = when (index % 4) {
                        0 -> -1.4f
                        1 -> 1.5f
                        2 -> -1.0f
                        else -> 1.3f
                    }
                    val pinColor = HomePinColors[index % HomePinColors.size]
                    val characterImage = character.reinterpretedImageUrl?.takeIf { it.isNotBlank() }
                        ?: character.originalDrawingBase64?.takeIf { it.isNotBlank() }
                        ?: profileStories.firstOrNull {
                            it.character.name.trim().equals(character.name.trim(), ignoreCase = true)
                        }?.scenes?.firstOrNull()?.illustrationUrl

                    HomeFriendPolaroidItem(
                        character = character,
                        imageUrl = characterImage,
                        pinColor = pinColor,
                        rotation = rotation,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onNavigateToCharacters()
                        },
                        modifier = Modifier.width(96.dp)
                    )
                }

                // "+ Nuevo" Friend Mini-Polaroid Card
                item(key = "home_new_friend_placeholder") {
                    val newIndex = displayedCharacters.size
                    val newPinColor = HomePinColors[newIndex % HomePinColors.size]
                    val newRotation = if (newIndex % 2 == 0) -1.2f else 1.3f

                    HomeNewFriendPolaroidPlaceholder(
                        pinColor = newPinColor,
                        rotation = newRotation,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onNewAdventureClick()
                        },
                        modifier = Modifier.width(96.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 6. DAILY SPARK CARD: "Idea de hoy" (at the bottom when user has stories)
            // -------------------------------------------------------------
            if (lastReadStory != null) {
                Spacer(modifier = Modifier.height(22.dp))
                DailySparkCard()
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Daily Spark Card ("💡 Idea de hoy") — Creative drawing prompt banner
 */
@Composable
private fun DailySparkCard() {
    val sparkIndex = remember { kotlin.random.Random.nextInt(12) }
    val ideas = AppStrings.todaySparkIdeas
    val currentSpark = ideas[sparkIndex % ideas.size]

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeGoldSoft),
        border = BorderStroke(1.5.dp, HomeGoldBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💡", fontSize = 21.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = AppStrings.todaySparkTitle.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HomeGoldText,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentSpark,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeInkDark,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, HomeGoldBorder)
            ) {
                Text(
                    text = AppStrings.drawNowAction,
                    fontFamily = TitleFont(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeGoldText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * Hardcover Storybook Item for the HomeScreen "Mis cuentos" Carousel (no wooden shelf plank)
 */
@Composable
private fun HomeBookCoverItem(
    saga: SagaBook,
    isOpening: Boolean,
    onClick: () -> Unit,
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
        topEnd = 16.dp,
        bottomEnd = 16.dp
    )

    val coverRotationY by animateFloatAsState(
        targetValue = if (isOpening) -135f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "home_book_cover_swing_y"
    )

    val bookScale by animateFloatAsState(
        targetValue = if (isOpening) 1.05f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "home_book_lift_scale"
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
                .clickable { onClick() }
        ) {
            // LAYER 1 (UNDERNEATH): First Inside Page revealed on 3D cover swing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 5.dp,
                        shape = bookShape,
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(bookShape)
                    .background(HomeCreamBg)
                    .border(1.5.dp, HomeCardBorder, bookShape)
            ) {
                // Right page-block paper edges effect
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
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
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = HomeCoralSoft
                    ) {
                        Text(
                            text = "✨ PÁG. 1",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeCoral,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HomeGoldSoft)
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeInkDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }

                // Inner spine shadow on left edge
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // LAYER 2 (ON TOP): 3D Swinging Hardcover Front Cover
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = 16f * density
                        rotationY = coverRotationY
                    }
                    .shadow(
                        elevation = if (isOpening) 10.dp else 6.dp,
                        shape = bookShape,
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.40f)
                    )
                    .clip(bookShape)
                    .background(Color.White)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), bookShape)
            ) {
                if (coverRotationY > -90f) {
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
                                        colors = listOf(HomeGoldSoft, HomeCoralSoft)
                                    )
                                )
                        ) {
                            Text(text = "📖", fontSize = 36.sp)
                        }
                    }

                    // Realistic Book Spine Effect on Left Edge
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(11.dp)
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
                            .padding(start = 11.dp, end = 7.dp, top = 10.dp, bottom = 7.dp)
                    ) {
                        Column {
                            Text(
                                text = story.title,
                                fontFamily = TitleFont(),
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (saga.isMultiChapter) "🌟 Saga · ${story.character.name}" else "✨ ${story.character.name}",
                                color = HomeGoldSoft,
                                fontSize = 9.5.sp,
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
                                    colors = listOf(HomeGoldSoft, HomeCreamBg)
                                )
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // Soft floating shadow underneath the book (no shelf plank)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Book-Shaped "+ Nuevo" Card in the HomeScreen "Mis cuentos" Carousel
 */
@Composable
private fun HomeNewBookPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookShape = RoundedCornerShape(
        topStart = 5.dp,
        bottomStart = 5.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = bookShape,
            color = Color.White,
            border = BorderStroke(1.8.dp, HomeCoralBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Subtle left book spine tint
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    HomeCoralSoft,
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HomeCoralSoft)
                    ) {
                        Text(
                            text = "+",
                            fontFamily = TitleFont(),
                            fontSize = 24.sp,
                            color = HomeCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = AppStrings.newStoryBookstand,
                        fontFamily = TitleFont(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeInkDark,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )

                    val availableStories = SubscriptionManager.availableStories.value
                    val hasStories = availableStories > 0
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = if (hasStories) HomeMintSoft else HomeCoralSoft,
                        border = BorderStroke(1.dp, if (hasStories) HomeMintBorder else HomeCoralBorder)
                    ) {
                        val badgeText = if (AppStrings.isEnglish) {
                            if (hasStories) "✨ $availableStories left" else "⚠️ 0 left"
                        } else {
                            if (hasStories) {
                                if (availableStories == 1) "✨ 1 restante" else "✨ $availableStories restantes"
                            } else {
                                "⚠️ 0 restantes"
                            }
                        }
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            color = if (hasStories) HomeMint else HomeCoral,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }

        // Matching soft ground shadow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Mini-Polaroid with 3D Pushpin for the HomeScreen "Mis amigos" Carousel (no corkboard behind)
 */
@Composable
private fun HomeFriendPolaroidItem(
    character: CharacterProfile,
    imageUrl: String?,
    pinColor: Color,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(top = 7.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.5.dp, HomeCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Polaroid Square Photo Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HomeMintSoft, HomeGoldSoft.copy(alpha = 0.55f))
                            )
                        )
                        .border(1.dp, HomeCardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        WonderLoopImage(
                            model = imageUrl,
                            contentDescription = character.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = "🧸", fontSize = 30.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Polaroid White Bottom Strip Caption
                Text(
                    text = character.name,
                    fontFamily = TitleFont(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeInkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3D Colorful Pushpin at the top center of the Mini-Polaroid
        HomePushpin3D(
            pinColor = pinColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
        )
    }
}

/**
 * Mini-Polaroid "+ Nuevo" Slot for the HomeScreen "Mis amigos" Carousel
 */
@Composable
private fun HomeNewFriendPolaroidPlaceholder(
    pinColor: Color,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(top = 7.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.5.dp, HomeMintBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    HomeMintSoft.copy(alpha = 0.75f),
                                    HomeGoldSoft.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .border(1.2.dp, HomeMintBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, HomeMintBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontFamily = TitleFont(),
                            fontSize = 22.sp,
                            color = HomeMint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AppStrings.newCard,
                    fontFamily = TitleFont(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeMint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        HomePushpin3D(
            pinColor = pinColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
        )
    }
}

@Composable
private fun HomePushpin3D(
    pinColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(15.dp)
            .shadow(3.dp, CircleShape)
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
            .border(1.dp, Color.Black.copy(alpha = 0.16f), CircleShape)
    )
}


