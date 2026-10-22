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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wonderloop.app.data.ChildProfile
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.DPadArrows
import org.wonderloop.app.ui.components.MinimalistLockIcon
import org.wonderloop.app.ui.components.WonderBackdrop
import org.wonderloop.app.ui.components.profileAccent
import org.wonderloop.app.ui.components.tvFocusable

private val PickerInkDark = Color(0xFF2B2340)
private val PickerInkMuted = Color(0xFF6B6480)
private val PickerCoral = Color(0xFFD9384A)
private val PickerCoralSoft = Color(0xFFFDECE9)
private val PickerCardBorder = Color(0xFFEDE8E1)

@Composable
fun ProfilePickerScreen(
    canGoBack: Boolean = false,
    onSelectProfile: (ChildProfile) -> Unit,
    onAddNewProfileClick: () -> Unit,
    onParentsClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    PlatformBackHandler(enabled = canGoBack) {
        onBackClick()
    }

    val profiles = ProfileRepository.profiles
    val activeProfile = ProfileRepository.activeProfile

    val activeFocusRequester = remember { FocusRequester() }
    val addFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            if (profiles.isNotEmpty()) {
                activeFocusRequester.requestFocus()
            } else {
                addFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    WonderBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -------------------------------------------------------------
            // 1. TOP BAR: [ ← Volver ]                              [ 🔒 Padres ]
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canGoBack) {
                    PickerPill(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onBackClick()
                        }
                    ) {
                        Text(text = "←", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PickerInkDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (AppStrings.isEnglish) "Back" else "Volver",
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PickerInkDark
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                PickerPill(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onParentsClick()
                    }
                ) {
                    MinimalistLockIcon(tint = PickerInkDark, size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Parents" else "Padres",
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PickerInkDark
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. TITLE
            // -------------------------------------------------------------
            Text(
                text = AppStrings.whoIsImaginingClean,
                fontFamily = TitleFont(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = PickerInkDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.tapYourCharacter,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PickerInkMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // -------------------------------------------------------------
            // 3. PROFILE CARDS (up to 3) + "add profile"
            // -------------------------------------------------------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                profiles.forEachIndexed { index, profile ->
                    ProfileCard(
                        profile = profile,
                        accent = profileAccent(index),
                        isActive = profile.id == activeProfile?.id,
                        focusRequester = if (profile.id == activeProfile?.id) activeFocusRequester else null,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onSelectProfile(profile)
                        }
                    )
                }

                if (profiles.size < 3) {
                    AddProfileCard(
                        freeSlots = 3 - profiles.size,
                        focusRequester = if (profiles.isEmpty()) addFocusRequester else null,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onAddNewProfileClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // -------------------------------------------------------------
            // 4. REMOTE HINTS
            // -------------------------------------------------------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PickerHint(label = null, text = if (AppStrings.isEnglish) "Choose" else "Elegir")
                PickerHint(label = "OK", text = if (AppStrings.isEnglish) "Enter" else "Entrar")
                if (canGoBack) PickerHint(label = "↩", text = if (AppStrings.isEnglish) "Back" else "Volver")
            }
        }
    }
}

@Composable
private fun PickerPill(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, PickerCardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(50.dp), focusedScale = 1.08f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Child card: accent header, big avatar, and the friends this child has already drawn. */
@Composable
private fun ProfileCard(
    profile: ChildProfile,
    accent: Color,
    isActive: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    val stories = remember(profile.id, StoryRepository.savedStories.size) {
        StoryRepository.getStoriesForProfile(profile.id)
    }
    val friendPortraits = remember(stories) {
        stories
            .flatMap { story -> listOfNotNull(story.character, story.continuation?.effectiveHero) }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
            .mapNotNull { friend ->
                friend.reinterpretedImageUrl?.takeIf { it.isNotBlank() }
                    ?: stories.firstOrNull { it.character.name.equals(friend.name, ignoreCase = true) }
                        ?.scenes?.firstOrNull()?.illustrationUrl
            }
            .take(4)
    }
    val en = AppStrings.isEnglish

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(250.dp)
            .height(304.dp)
            .tvFocusable(
                shape = RoundedCornerShape(32.dp),
                focusedScale = 1.07f,
                focusRequester = focusRequester,
                onClick = onClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Accent header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f))))
            )
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Text(text = "✓", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 34.dp, start = 16.dp, end = 16.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(108.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, accent, CircleShape)
                ) {
                    Text(text = profile.avatar, fontSize = 56.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = profile.name,
                    fontFamily = TitleFont(),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = PickerInkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${profile.age} ${AppStrings.yearsOld} · " + when (stories.size) {
                        1 -> if (en) "1 story" else "1 cuento"
                        else -> if (en) "${stories.size} stories" else "${stories.size} cuentos"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PickerInkMuted
                )

                Spacer(modifier = Modifier.weight(1f))

                if (friendPortraits.isNotEmpty()) {
                    // Overlapping portraits of the characters this child has drawn
                    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                        friendPortraits.forEach { portrait ->
                            WonderLoopImage(
                                model = portrait,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color.White, CircleShape)
                                    .background(accent.copy(alpha = 0.12f))
                            )
                        }
                    }
                } else {
                    Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.10f)) {
                        Text(
                            text = if (en) "✨ Time to create!" else "✨ ¡Hora de crear!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProfileCard(freeSlots: Int, focusRequester: FocusRequester?, onClick: () -> Unit) {
    val en = AppStrings.isEnglish
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(250.dp)
            .height(304.dp)
            .tvFocusable(
                shape = RoundedCornerShape(32.dp),
                focusedScale = 1.07f,
                // Translucent slot: a focus shadow would show through as a grey veil
                focusedElevation = 0.dp,
                focusedContainerColor = Color.White.copy(alpha = 0.85f),
                focusRequester = focusRequester,
                onClick = onClick
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .drawBehind {
                // Dashed "empty slot" outline
                drawRoundRect(
                    color = PickerCoral.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()))
                    )
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(PickerCoralSoft)
            ) {
                Text(text = "+", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = PickerCoral)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (en) "Add profile" else "Añadir perfil",
                fontFamily = TitleFont(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PickerCoral
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (freeSlots == 1) {
                    if (en) "1 free slot" else "1 plaza libre"
                } else {
                    if (en) "$freeSlots free slots" else "$freeSlots plazas libres"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PickerInkMuted
            )
        }
    }
}

@Composable
private fun PickerHint(label: String?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, PickerCardBorder)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                if (label == null) {
                    DPadArrows(color = PickerInkDark)
                } else {
                    Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PickerInkDark)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PickerInkMuted)
    }
}
