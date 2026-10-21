package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.DPadArrows
import org.wonderloop.app.ui.components.tvFocusable
import org.wonderloop.app.ui.components.TvFocusGold
import org.wonderloop.app.ui.components.WonderBackdrop
import org.wonderloop.app.platform.WonderLoopImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer

private val ChildCreamBg = Color(0xFFFFFBF5)
private val ChildInkDark = Color(0xFF2B2340)
private val ChildInkMuted = Color(0xFF6B6480)
private val ChildCoral = Color(0xFFD9384A)
private val ChildCoralSoft = Color(0xFFFDECE9)
private val ChildCoralBorder = Color(0xFFF9D0CA)
private val ChildMint = Color(0xFF0E7F64)
private val ChildMintSoft = Color(0xFFE4F6F0)
private val ChildMintBorder = Color(0xFFB8E6D6)
private val ChildGoldSoft = Color(0xFFFFF1CF)
private val ChildGoldBorder = Color(0xFFF3DC9B)
private val ChildGoldText = Color(0xFF9E6700)
private val ChildCardBorder = Color(0xFFEDE8E1)

private data class ChildMedal(
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun ChildProfileScreen(
    childName: String,
    childAge: Int,
    childAvatar: String,
    profileId: String? = null,
    onAvatarChanged: (String) -> Unit,
    onNavigateToStories: () -> Unit,
    onNavigateToCharacters: () -> Unit,
    onBackClick: () -> Unit
) {
    PlatformBackHandler(enabled = true) {
        onBackClick()
    }

    val availableAvatars = remember { listOf("👦", "👧", "🦁", "🦄", "🚀", "🎨", "🐱", "🐼") }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val profileStories = remember(profileId, StoryRepository.savedStories.size) {
        if (profileId != null) {
            StoryRepository.getStoriesForProfile(profileId)
        } else {
            StoryRepository.getStoriesForProfile()
        }
    }

    val storiesCount = profileStories.size
    val uniqueCharactersCount = remember(profileStories) {
        buildList {
            profileStories.forEach { story ->
                add(story.character)
                story.continuation?.effectiveHero?.let { add(it) }
            }
        }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
            .size
    }
    val hasContinuedSaga = remember(profileStories) {
        profileStories.any { (it.chapterIndex ?: 1) >= 2 }
    }

    val pedagogicalBadge = when {
        childAge <= 5 -> if (AppStrings.isEnglish) "🌱 Early Steps" else "🌱 Primeros Pasos"
        childAge <= 7 -> if (AppStrings.isEnglish) "🌟 Little Creator" else "🌟 Pequeño Creador"
        else -> if (AppStrings.isEnglish) "🚀 Great Explorer" else "🚀 Gran Explorador"
    }

    val creatorLevelName = when {
        storiesCount >= 5 -> if (AppStrings.isEnglish) "Master" else "Maestro"
        storiesCount >= 2 -> if (AppStrings.isEnglish) "Explorer" else "Explorador"
        else -> AppStrings.levelCurious
    }

    val nextGoalTarget = when {
        storiesCount < 2 -> 2
        storiesCount < 5 -> 5
        else -> 10
    }
    val levelProgress = (storiesCount.toFloat() / nextGoalTarget.toFloat()).coerceIn(0.08f, 1f)

    val medals = remember(storiesCount, uniqueCharactersCount, hasContinuedSaga, AppStrings.isEnglish) {
        listOf(
            ChildMedal(
                emoji = "🎨",
                title = AppStrings.medalFirstStoryTitle,
                description = AppStrings.medalFirstStoryDesc,
                isUnlocked = storiesCount >= 1,
                progressText = "${minOf(storiesCount, 1)}/1"
            ),
            ChildMedal(
                emoji = "🧸",
                title = AppStrings.medalThreeFriendsTitle,
                description = AppStrings.medalThreeFriendsDesc,
                isUnlocked = uniqueCharactersCount >= 3,
                progressText = "${minOf(uniqueCharactersCount, 3)}/3"
            ),
            ChildMedal(
                emoji = "🔗",
                title = AppStrings.medalSagaTitle,
                description = AppStrings.medalSagaDesc,
                isUnlocked = hasContinuedSaga,
                progressText = if (hasContinuedSaga) "1/1" else "0/1"
            ),
            ChildMedal(
                emoji = "👑",
                title = AppStrings.medalMasterTitle,
                description = AppStrings.medalMasterDesc,
                isUnlocked = storiesCount >= 5,
                progressText = "${minOf(storiesCount, 5)}/5"
            )
        )
    }

    val unlockedCount = medals.count { it.isUnlocked }

    val motivationText = when {
        storiesCount == 0 -> if (AppStrings.isEnglish)
            "Draw your first character on paper to unlock your first medal!"
        else
            "¡Dibuja tu primer personaje en papel para ganar tu primera medalla!"
        storiesCount < nextGoalTarget -> {
            val remaining = nextGoalTarget - storiesCount
            if (AppStrings.isEnglish)
                "Create $remaining more ${if (remaining == 1) "story" else "stories"} to level up!"
            else
                "¡Crea $remaining ${if (remaining == 1) "cuento" else "cuentos"} más para subir de nivel!"
        }
        else -> if (AppStrings.isEnglish)
            "You are a Master of Stories! Keep imagining new worlds."
        else
            "¡Eres un Maestro de Historias! Sigue imaginando nuevos mundos."
    }

    val storyCovers = remember(profileStories) {
        profileStories.mapNotNull { story -> story.scenes.firstOrNull { !it.illustrationUrl.isNullOrBlank() }?.illustrationUrl }.take(3)
    }
    val friendPortraits = remember(profileStories) {
        profileStories
            .flatMap { story -> listOfNotNull(story.character, story.continuation?.effectiveHero) }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
            .mapNotNull { it.reinterpretedImageUrl?.takeIf { url -> url.isNotBlank() } }
            .take(3)
    }

    // Main 1080p Widescreen Container (Zero vertical scrolling)
    WonderBackdrop {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV 5% Safe Zone
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // 1. TOP BAR: [ ← Volver ]  Mi Perfil de Creador   [ 🚀 Leo ]
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val onBack = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onBackClick()
                    }
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, ChildCardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(42.dp)
                            .tvFocusable(shape = RoundedCornerShape(24.dp), focusedScale = 1.06f, onClick = onBack)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "←",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ChildInkDark
                            )
                            Text(
                                text = if (AppStrings.isEnglish) "Back" else "Volver",
                                fontFamily = TitleFont(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChildInkDark
                            )
                        }
                    }

                    Text(
                        text = if (AppStrings.isEnglish) "Creator Profile" else "Mi Perfil de Creador",
                        fontFamily = TitleFont(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildInkDark
                    )
                }

                // Active Profile Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ChildCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(text = childAvatar.ifBlank { "👦" }, fontSize = 18.sp)
                        Text(
                            text = childName.ifBlank { "Leo" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChildInkDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // 2. MAIN 2-COLUMN CONTENT (Widescreen 1080p)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                // -------------------------------------------------------------
                // LEFT COLUMN (38% width): Identity, Level & Quick Action
                // -------------------------------------------------------------
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.5.dp, ChildCardBorder),
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .shadow(6.dp, RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(0f to ChildCoralSoft, 0.45f to Color.White),
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Avatar + Edit button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(106.dp)
                                    .tvFocusable(shape = CircleShape, focusedScale = 1.08f, onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        showAvatarPicker = true
                                    })
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(98.dp)
                                        .shadow(6.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(4.dp, ChildCoral, CircleShape)
                                ) {
                                    Text(text = childAvatar.ifBlank { "👦" }, fontSize = 52.sp)
                                }

                                // Floating edit badge
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ChildCoral)
                                        .border(2.dp, Color.White, CircleShape)
                                ) {
                                    Text(text = "🎨", fontSize = 16.sp)
                                }
                            }

                            // Child Name
                            Text(
                                text = childName.ifBlank { "Leo" },
                                fontFamily = TitleFont(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChildInkDark,
                                textAlign = TextAlign.Center
                            )

                            // Badges: Age + Stage
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = ChildGoldSoft,
                                    border = BorderStroke(1.dp, ChildGoldBorder)
                                ) {
                                    Text(
                                        text = AppStrings.ageYears(childAge),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ChildGoldText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = ChildMintSoft,
                                    border = BorderStroke(1.dp, ChildMintBorder)
                                ) {
                                    Text(
                                        text = pedagogicalBadge,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ChildMint,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Level Progress Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFFDF9),
                            border = BorderStroke(1.dp, ChildCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏆 $creatorLevelName",
                                        fontFamily = TitleFont(),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChildInkDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = ChildCoralSoft
                                    ) {
                                        Text(
                                            text = "$storiesCount / $nextGoalTarget",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ChildCoral,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Coral Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF2ECE1))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(levelProgress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.horizontalGradient(listOf(ChildCoral, Color(0xFFF2B630))))
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = motivationText,
                                    fontSize = 13.sp,
                                    color = ChildInkMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Quick Action: Ver biblioteca de cuentos
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ChildCoral,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(22.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        onNavigateToStories()
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📚", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (AppStrings.isEnglish) "View all stories ($storiesCount) →" else "Ver mis cuentos ($storiesCount) →",
                                    fontFamily = TitleFont(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // RIGHT COLUMN (62% width): Medals 2x2 Grid + Quick Stats
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "Your Medals and Achievements" else "Tus Medallas y Logros",
                            fontFamily = TitleFont(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChildInkDark
                        )

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = ChildGoldSoft,
                            border = BorderStroke(1.dp, ChildGoldBorder)
                        ) {
                            Text(
                                text = "✨ $unlockedCount / ${medals.size} " + if (AppStrings.isEnglish) "Unlocked" else "Desbloqueadas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ChildGoldText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 2x2 Medals Grid (weight 1f)
                    val medalRows = medals.chunked(2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        medalRows.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                row.forEach { medal ->
                                    MedalTile(
                                        medal = medal,
                                        tint = MedalTints[medals.indexOf(medal) % MedalTints.size],
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Quick Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Stat 1: Cuentos creados
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, ChildCardBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNavigateToStories()
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ArtworkStack(images = storyCovers.take(2), fallbackEmoji = "📚", square = true)
                                Column {
                                    Text(
                                        text = "$storiesCount " + if (AppStrings.isEnglish) "Stories created" else "Cuentos creados",
                                        fontFamily = TitleFont(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChildCoral
                                    )
                                    Text(
                                        text = if (AppStrings.isEnglish) "Open bookshelf →" else "Ver biblioteca →",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChildInkMuted
                                    )
                                }
                            }
                        }

                        // Stat 2: Dibujos con vida
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, ChildCardBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNavigateToCharacters()
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ArtworkStack(images = friendPortraits, fallbackEmoji = "🎨", square = false)
                                Column {
                                    Text(
                                        text = "$uniqueCharactersCount " + if (AppStrings.isEnglish) "Drawings alive" else "Dibujos con vida",
                                        fontFamily = TitleFont(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChildMint
                                    )
                                    Text(
                                        text = if (AppStrings.isEnglish) "Open friends →" else "Ver amigos →",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChildInkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 3. BOTTOM HINT BAR: Perfectly symmetrical DPad arrows
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
                    border = BorderStroke(1.dp, ChildCardBorder)
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
                            DPadArrows(color = ChildInkDark, width = 16.dp, height = 11.dp)
                            Text(
                                text = if (AppStrings.isEnglish) "Navigate" else "Moverse",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChildInkDark
                            )
                        }

                        Text(text = "·", color = ChildCardBorder, fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "OK", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ChildCoral)
                            Text(
                                text = if (AppStrings.isEnglish) "Select / Open" else "Seleccionar / Abrir",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChildInkDark
                            )
                        }

                        Text(text = "·", color = ChildCardBorder, fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "↩", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChildInkMuted)
                            Text(
                                text = if (AppStrings.isEnglish) "Back" else "Volver",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChildInkDark
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. FIRE TV AVATAR PICKER MODAL (480dp width, D-Pad focusable)
        // =====================================================================
        if (showAvatarPicker) {
            Dialog(
                onDismissRequest = { showAvatarPicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ChildCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                    modifier = Modifier.width(480.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "Choose your Avatar" else "Elige tu nuevo Avatar",
                            fontFamily = TitleFont(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChildInkDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val avatarRows = availableAvatars.chunked(4)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            avatarRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { avatar ->
                                        val isSelected = avatar == childAvatar
                                        Surface(
                                            onClick = {
                                                PlatformAudioService.playSfx(SoundEffect.POP)
                                                onAvatarChanged(avatar)
                                                showAvatarPicker = false
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) ChildCoralSoft else Color(0xFFFFFDF9),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.5.dp else 1.5.dp,
                                                color = if (isSelected) ChildCoral else ChildCardBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .tvFocusable(
                                                    shape = RoundedCornerShape(16.dp),
                                                    focusedScale = 1.12f,
                                                    onClick = {
                                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                                        onAvatarChanged(avatar)
                                                        showAvatarPicker = false
                                                    }
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = avatar, fontSize = 28.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = { showAvatarPicker = false },
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.5.dp, ChildCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = ChildInkDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(44.dp)
                                .tvFocusable(shape = RoundedCornerShape(50.dp), onClick = { showAvatarPicker = false })
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "Close" else "Cerrar",
                                fontFamily = TitleFont(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChildInkDark
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

/** Overlapping thumbnails of the child's real artwork (story covers or drawn friends). */
@Composable
private fun ArtworkStack(images: List<String>, fallbackEmoji: String, square: Boolean) {
    if (images.isEmpty()) {
        Text(text = fallbackEmoji, fontSize = 30.sp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
        images.forEach { image ->
            val shape = if (square) RoundedCornerShape(10.dp) else CircleShape
            WonderLoopImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = if (square) 58.dp else 46.dp, height = 46.dp)
                    .clip(shape)
                    .border(2.5.dp, Color.White, shape)
            )
        }
    }
}

private val MedalTints = listOf(
    Color(0xFFF2B630), // gold
    Color(0xFF14A37F), // mint
    Color(0xFF7C5CE0), // violet
    Color(0xFFE0485A)  // coral
)

/** A medal that looks like one: ribbon tails, metal-ring disc, and a soft tinted card when earned. */
@Composable
private fun MedalTile(medal: ChildMedal, tint: Color, modifier: Modifier = Modifier) {
    val unlocked = medal.isUnlocked
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (unlocked) {
                    Brush.linearGradient(listOf(tint.copy(alpha = 0.16f), Color.White))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFF3EFE8), Color(0xFFF9F6F0)))
                }
            )
            .border(1.5.dp, if (unlocked) tint.copy(alpha = 0.35f) else ChildCardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.size(width = 78.dp, height = 92.dp)) {
                // Ribbon tails
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    listOf(-12f, 12f).forEach { angle ->
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 30.dp)
                                .rotate(angle)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (unlocked) tint else Color(0xFFD9D3C8))
                        )
                    }
                }
                // Disc with a metallic ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(if (unlocked) 6.dp else 0.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) {
                                Brush.linearGradient(listOf(Color(0xFFFFE08A), Color(0xFFF2B630), Color(0xFFFFE9A8)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFE2DDD3), Color(0xFFCFC8BC)))
                            }
                        )
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Text(
                        text = medal.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier.graphicsLayer { alpha = if (unlocked) 1f else 0.35f }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medal.title,
                    fontFamily = TitleFont(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) ChildInkDark else ChildInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = medal.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChildInkMuted,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (unlocked) tint.copy(alpha = 0.14f) else Color(0xFFEAE5DC)
                ) {
                    Text(
                        text = if (unlocked) {
                            if (AppStrings.isEnglish) "✓ Earned" else "✓ Conseguida"
                        } else {
                            "🔒 ${medal.progressText}"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unlocked) tint else ChildInkMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
