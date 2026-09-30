package org.wonderloop.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*

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

    Column(
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
                    radius = 900f
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: [ ← ] Mi Perfil
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    onBackClick()
                },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.5.dp, ChildCardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChildInkDark
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = AppStrings.myProfileTitle,
                fontFamily = TitleFont(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChildInkDark
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // -------------------------------------------------------------
        // 2. HERO IDENTITY CARD (Interactive Avatar & Creator Badges)
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, ChildCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big Tappable Avatar with floating customize badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clickable {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showAvatarPicker = !showAvatarPicker
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(ChildCoralSoft)
                            .border(2.5.dp, ChildCoralBorder, CircleShape)
                    ) {
                        Text(text = childAvatar, fontSize = 46.sp)
                    }

                    // Small edit badge at bottom-right
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ChildCoral)
                            .border(2.dp, Color.White, CircleShape)
                    ) {
                        Text(text = "🎨", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showAvatarPicker = !showAvatarPicker
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = ChildCoralSoft
                ) {
                    Text(
                        text = AppStrings.tapAvatarToCustomize,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChildCoral,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                // Expandable Inline Avatar Picker (8 avatars in 2x4)
                AnimatedVisibility(
                    visible = showAvatarPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ChildCreamBg)
                            .border(1.dp, ChildCardBorder, RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = AppStrings.chooseYourNewAvatar,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChildInkDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val avatarRows = availableAvatars.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            avatarRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { avatar ->
                                        val isSelected = avatar == childAvatar
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) ChildCoralSoft else Color.White)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) ChildCoral else ChildCardBorder,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                                    onAvatarChanged(avatar)
                                                    showAvatarPicker = false
                                                }
                                        ) {
                                            Text(text = avatar, fontSize = 26.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Child Name
                Text(
                    text = childName,
                    fontFamily = TitleFont(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChildInkDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Age & Pedagogical Rank Pills
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ChildGoldSoft,
                        border = BorderStroke(1.dp, ChildGoldBorder)
                    ) {
                        Text(
                            text = AppStrings.ageYears(childAge),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChildGoldText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ChildMintSoft,
                        border = BorderStroke(1.dp, ChildMintBorder)
                    ) {
                        Text(
                            text = pedagogicalBadge,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChildMint,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -------------------------------------------------------------
        // 3. THREE INTERACTIVE CREATOR STATS CARDS
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Cuentos creados -> Opens Mis Cuentos
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ChildCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onNavigateToStories()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📚", fontSize = 26.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$storiesCount",
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildCoral
                    )
                    Text(
                        text = AppStrings.storiesCreated(storiesCount),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChildInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            // Card 2: Dibujos con vida -> Opens Mis Amigos
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ChildCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onNavigateToCharacters()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎨", fontSize = 26.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$uniqueCharactersCount",
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildMint
                    )
                    Text(
                        text = AppStrings.aliveDrawings(uniqueCharactersCount),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChildInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            // Card 3: Nivel de Creador
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ChildCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🏆", fontSize = 26.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = AppStrings.levelTitle,
                        fontFamily = TitleFont(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildInkDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = creatorLevelName,
                        fontSize = 12.sp,
                        color = ChildCoral,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -------------------------------------------------------------
        // 4. CREATOR LEVEL PROGRESS BAR CARD
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, ChildCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "✨ Imagination Progress" else "✨ Progreso de Imaginación",
                        fontFamily = TitleFont(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildInkDark
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ChildCoralSoft
                    ) {
                        Text(
                            text = "$storiesCount / $nextGoalTarget",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChildCoral,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFF2ECE1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(levelProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(ChildCoral)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                Text(
                    text = motivationText,
                    fontSize = 12.5.sp,
                    color = ChildInkMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -------------------------------------------------------------
        // 5. MAGICAL MEDALS (2x2 Grid of Real Achievements)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = AppStrings.myMedalsTitle,
                fontFamily = TitleFont(),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChildInkDark
            )

            val unlockedCount = medals.count { it.isUnlocked }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = ChildGoldSoft,
                border = BorderStroke(1.dp, ChildGoldBorder)
            ) {
                Text(
                    text = "$unlockedCount / ${medals.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChildGoldText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val medalRows = medals.chunked(2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            medalRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { medal ->
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (medal.isUnlocked) Color.White else Color(0xFFF6F2EB)
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (medal.isUnlocked) ChildCoralBorder else ChildCardBorder
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (medal.isUnlocked) 2.dp else 0.dp
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Badge top-right
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (medal.isUnlocked) ChildMintSoft else Color(0xFFEAE5DC)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (medal.isUnlocked) "✓" else medal.progressText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (medal.isUnlocked) ChildMint else ChildInkMuted
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (medal.isUnlocked) ChildGoldSoft else Color(0xFFEAE5DC)
                                            )
                                    ) {
                                        Text(
                                            text = medal.emoji,
                                            fontSize = 24.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = medal.title,
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (medal.isUnlocked) ChildInkDark else ChildInkMuted,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = medal.description,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChildInkMuted,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp,
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}
