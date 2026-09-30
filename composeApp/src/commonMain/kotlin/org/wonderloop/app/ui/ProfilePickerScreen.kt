package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.ChildProfile
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*

private val UnselectedCardBorderColor = Color(0xFFDCD3C8)

@Composable
fun ProfilePickerScreen(
    canGoBack: Boolean = false,
    onSelectProfile: (ChildProfile) -> Unit,
    onAddNewProfileClick: () -> Unit,
    onParentsClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val profiles = ProfileRepository.profiles
    val activeProfile = ProfileRepository.activeProfile

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        val isLandscape = maxWidth > maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isLandscape) 24.dp else 20.dp, vertical = if (isLandscape) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Back button if navigated from inside the Home screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canGoBack) {
                    IconButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onBackClick()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PaperCard)
                            .border(1.dp, Color(0xFFEDE8E1), CircleShape)
                    ) {
                        Text(text = "←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cheerful Clean Header ("¿Quién imagina hoy? Toca tu personaje")
            Text(
                text = AppStrings.whoIsImaginingClean,
                fontFamily = TitleFont(),
                fontSize = if (isLandscape) 24.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoralHero,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.tapYourCharacter,
                fontSize = 15.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 20.dp else 28.dp))

            // Profiles Grid: 2 columns in portrait, 3 in landscape
            val targetCols = if (isLandscape) 3 else 2
            val chunkedProfiles = remember(profiles.toList(), targetCols) {
                profiles.chunked(targetCols)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (row in chunkedProfiles) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (profile in row) {
                            val isCurrentActive = profile.id == activeProfile?.id
                            val storyCount = remember(profile.id, StoryRepository.savedStories.size) {
                                StoryRepository.getStoriesForProfile(profile.id).size
                            }

                            ProfileCardItem(
                                profile = profile,
                                isActive = isCurrentActive,
                                storyCount = storyCount,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onSelectProfile(profile)
                                }
                            )
                        }

                        // Fill row spacing if odd number in row
                        val remainingInRow = targetCols - row.size
                        if (remainingInRow > 0 && chunkedProfiles.last() == row) {
                            // Slot for "+ Añadir" if it fits in the current row!
                            NewProfileCardItem(
                                modifier = Modifier.weight(1f),
                                onClick = onAddNewProfileClick
                            )
                            for (i in 1 until remainingInRow) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // If "+ Añadir" wasn't placed in the last row, place it in a new row
                val lastRowSize = chunkedProfiles.lastOrNull()?.size ?: 0
                if (lastRowSize == targetCols || chunkedProfiles.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NewProfileCardItem(
                            modifier = Modifier.weight(1f),
                            onClick = onAddNewProfileClick
                        )
                        for (i in 1 until targetCols) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Spacer(modifier = Modifier.weight(1f))

            // Bottom Link: Zona de padres with new vector padlock icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onParentsClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawCircle(color = Color(0xFFFDECE9))
                    val s = size.width / 32f
                    val lockColor = Color(0xFFD9384A)
                    val strokeW = 2.2f * s
                    drawRoundRect(
                        color = lockColor,
                        topLeft = Offset(9f * s, 15f * s),
                        size = Size(14f * s, 10f * s),
                        cornerRadius = CornerRadius(2.4f * s, 2.4f * s),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val shacklePath = Path().apply {
                        moveTo(12f * s, 15f * s)
                        lineTo(12f * s, 12f * s)
                        arcTo(
                            rect = Rect(12f * s, 8f * s, 20f * s, 16f * s),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        lineTo(20f * s, 15f * s)
                    }
                    drawPath(
                        path = shacklePath,
                        color = lockColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.parentsZoneLink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ProfileCardItem(
    profile: ChildProfile,
    isActive: Boolean,
    storyCount: Int,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PaperCard),
        border = BorderStroke(
            width = if (isActive) 2.5.dp else 1.5.dp,
            color = if (isActive) CoralHero else UnselectedCardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 5.dp else 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onSelect() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Selected Checkmark Badge (Top Right)
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CoralHero)
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEAE8))
                ) {
                    Text(text = profile.avatar, fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Child Name
                Text(
                    text = profile.name,
                    fontFamily = TitleFont(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Clean Story Count: "7 cuentos"
                Text(
                    text = if (storyCount == 1) "1 cuento" else "$storyCount cuentos",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NewProfileCardItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PaperCard),
        border = BorderStroke(
            width = 1.5.dp,
            color = UnselectedCardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                PlatformAudioService.playSfx(SoundEffect.POP)
                onClick()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF2EE))
            ) {
                Text(
                    text = "+",
                    fontSize = 36.sp,
                    color = CoralHero,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = AppStrings.addProfileButton, // "Añadir"
                fontFamily = TitleFont(),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoralHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.newProfileSubtitle, // "Nuevo perfil"
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
