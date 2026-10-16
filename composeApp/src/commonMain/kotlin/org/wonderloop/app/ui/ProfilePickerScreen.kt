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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
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
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.DPadArrows
import org.wonderloop.app.ui.components.MinimalistLockIcon
import org.wonderloop.app.ui.components.tvFocusable

private val PickerCreamBg = Color(0xFFFFFBF5)
private val PickerInkDark = Color(0xFF2B2340)
private val PickerInkMuted = Color(0xFF6B6480)
private val PickerCoral = Color(0xFFD9384A)
private val PickerCoralSoft = Color(0xFFFDECE9)
private val PickerCoralBorder = Color(0xFFF9D0CA)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickerCreamBg)
            .padding(horizontal = 48.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: [ ← Volver ]       ¿Quién imagina hoy?        [ 🔒 Padres ]
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, PickerCardBorder),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.08f,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onBackClick()
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "←",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PickerInkDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (AppStrings.isEnglish) "Back" else "Volver",
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PickerInkDark
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(110.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = AppStrings.whoIsImaginingClean,
                    fontFamily = TitleFont(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PickerInkDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = AppStrings.tapYourCharacter,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PickerInkMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, PickerCardBorder),
                modifier = Modifier.tvFocusable(
                    shape = RoundedCornerShape(50.dp),
                    focusedScale = 1.08f,
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onParentsClick()
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalistLockIcon(tint = PickerInkDark, size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Parents" else "Padres",
                        fontFamily = TitleFont(),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PickerInkDark
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. CENTER PROFILE CARDS ROW (Up to 3, Wide 1080p layout)
        // -------------------------------------------------------------
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfile?.id
                val storyCount = remember(profile.id, StoryRepository.savedStories.size) {
                    StoryRepository.getStoriesForProfile(profile.id).size
                }

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isActive) 2.5.dp else 1.5.dp,
                        color = if (isActive) PickerCoral else PickerCardBorder
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .width(230.dp)
                        .height(270.dp)
                        .tvFocusable(
                            shape = RoundedCornerShape(32.dp),
                            focusedScale = 1.06f,
                            focusRequester = if (isActive) activeFocusRequester else null,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onSelectProfile(profile)
                            }
                        )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 16.dp, topEnd = 30.dp),
                                color = PickerCoral,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(PickerCoralSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = profile.avatar, fontSize = 46.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = profile.name,
                                fontFamily = TitleFont(),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = PickerInkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${profile.age} ${AppStrings.yearsOld}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PickerInkMuted
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (storyCount == 1) {
                                    if (AppStrings.isEnglish) "1 story" else "1 cuento"
                                } else {
                                    if (AppStrings.isEnglish) "$storyCount stories" else "$storyCount cuentos"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PickerCoral
                            )
                        }
                    }
                }
            }

            // Slot for "+ Añadir perfil" if fewer than 3
            if (profiles.size < 3) {
                val freeSlots = 3 - profiles.size
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, PickerCoralBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .width(230.dp)
                        .height(270.dp)
                        .tvFocusable(
                            shape = RoundedCornerShape(32.dp),
                            focusedScale = 1.06f,
                            focusRequester = if (profiles.isEmpty()) addFocusRequester else null,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onAddNewProfileClick()
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(PickerCoralSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PickerCoral
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (AppStrings.isEnglish) "Add profile" else "Añadir perfil",
                            fontFamily = TitleFont(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PickerCoral,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (freeSlots == 1) {
                                if (AppStrings.isEnglish) "1 free slot" else "1 plaza libre"
                            } else {
                                if (AppStrings.isEnglish) "$freeSlots free slots" else "$freeSlots plazas libres"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PickerInkMuted
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. BOTTOM HINT BAR
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PickerCardBorder)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    DPadArrows(color = PickerInkDark)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (AppStrings.isEnglish) "Select" else "Elegir",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PickerInkMuted
            )

            Spacer(modifier = Modifier.width(28.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PickerCardBorder)
            ) {
                Text(
                    text = "OK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PickerInkDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (AppStrings.isEnglish) "Enter" else "Entrar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PickerInkMuted
            )

            if (canGoBack) {
                Spacer(modifier = Modifier.width(28.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PickerCardBorder)
                ) {
                    Text(
                        text = "↩",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PickerInkDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (AppStrings.isEnglish) "Back" else "Volver",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PickerInkMuted
                )
            }
        }
    }
}
