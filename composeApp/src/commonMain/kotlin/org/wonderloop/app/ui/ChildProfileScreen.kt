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

    // Main 1080p Widescreen Container (Zero vertical scrolling)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ChildGoldSoft.copy(alpha = 0.35f),
                        ChildCoralSoft.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    radius = 950f
                )
            )
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
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChildInkDark
                            )
                        }
                    }

                    Text(
                        text = if (AppStrings.isEnglish) "Creator Profile" else "Mi Perfil de Creador",
                        fontFamily = TitleFont(),
                        fontSize = 24.sp,
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ChildCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
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
                                    .size(92.dp)
                                    .tvFocusable(shape = CircleShape, focusedScale = 1.08f, onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        showAvatarPicker = true
                                    })
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(ChildCoralSoft)
                                        .border(2.5.dp, ChildCoralBorder, CircleShape)
                                ) {
                                    Text(text = childAvatar.ifBlank { "👦" }, fontSize = 44.sp)
                                }

                                // Floating edit badge
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ChildCoral)
                                        .border(2.dp, Color.White, CircleShape)
                                ) {
                                    Text(text = "🎨", fontSize = 13.sp)
                                }
                            }

                            // Child Name
                            Text(
                                text = childName.ifBlank { "Leo" },
                                fontFamily = TitleFont(),
                                fontSize = 24.sp,
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
                                        fontSize = 11.5.sp,
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
                                        fontSize = 11.5.sp,
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
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChildInkDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = ChildCoralSoft
                                    ) {
                                        Text(
                                            text = "$storiesCount / $nextGoalTarget",
                                            fontSize = 11.5.sp,
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
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF2ECE1))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(levelProgress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ChildCoral)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = motivationText,
                                    fontSize = 11.sp,
                                    color = ChildInkMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp
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
                                    fontSize = 14.sp,
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
                            fontSize = 20.sp,
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
                                fontSize = 12.sp,
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
                                    Card(
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (medal.isUnlocked) Color.White else Color(0xFFF9F6F0)
                                        ),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (medal.isUnlocked) ChildCoralBorder else ChildCardBorder
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (medal.isUnlocked) 3.dp else 1.dp
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .tvFocusable(
                                                shape = RoundedCornerShape(22.dp),
                                                focusedScale = 1.04f,
                                                onClick = {
                                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                                }
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Emoji Circle
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (medal.isUnlocked) ChildGoldSoft else Color(0xFFEAE5DC)
                                                    )
                                            ) {
                                                Text(text = medal.emoji, fontSize = 28.sp)
                                            }

                                            // Text + Status
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = medal.title,
                                                        fontFamily = TitleFont(),
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (medal.isUnlocked) ChildInkDark else ChildInkMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // Badge status
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (medal.isUnlocked) ChildMintSoft else Color(0xFFEAE5DC)
                                                    ) {
                                                        Text(
                                                            text = if (medal.isUnlocked) "✓" else medal.progressText,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (medal.isUnlocked) ChildMint else ChildInkMuted,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = medal.description,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ChildInkMuted,
                                                    lineHeight = 15.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Quick Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
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
                                Text(text = "📚", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = "$storiesCount " + if (AppStrings.isEnglish) "Stories created" else "Cuentos creados",
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChildCoral
                                    )
                                    Text(
                                        text = if (AppStrings.isEnglish) "Open bookshelf →" else "Ver biblioteca →",
                                        fontSize = 11.sp,
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
                                Text(text = "🎨", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = "$uniqueCharactersCount " + if (AppStrings.isEnglish) "Drawings alive" else "Dibujos con vida",
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChildMint
                                    )
                                    Text(
                                        text = if (AppStrings.isEnglish) "Open friends →" else "Ver amigos →",
                                        fontSize = 11.sp,
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
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChildInkDark
                            )
                        }

                        Text(text = "·", color = ChildCardBorder, fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "OK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ChildCoral)
                            Text(
                                text = if (AppStrings.isEnglish) "Select / Open" else "Seleccionar / Abrir",
                                fontSize = 11.5.sp,
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
                                fontSize = 11.5.sp,
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
                                fontSize = 14.sp,
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
