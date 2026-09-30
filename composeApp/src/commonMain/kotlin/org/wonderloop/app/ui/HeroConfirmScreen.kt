package org.wonderloop.app.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.StoryContinuationContext
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.ConfettiEffect

private val HeroCreamBg = Color(0xFFFFFBF5)
private val HeroInkDark = Color(0xFF2B2340)
private val HeroInkMuted = Color(0xFF6B6480)
private val HeroCoral = Color(0xFFD9384A)
private val HeroCoralSoft = Color(0xFFFDECE9)
private val HeroMint = Color(0xFF0E7F64)
private val HeroMintSoft = Color(0xFFE4F6F0)
private val HeroMintBorder = Color(0xFFB8E6D6)
private val HeroGoldSoft = Color(0xFFFFF1CF)
private val HeroGoldText = Color(0xFF9E6700)
private val HeroCardBorder = Color(0xFFEDE8E1)

@Composable
fun HeroConfirmScreen(
    character: CharacterProfile,
    childName: String,
    continuation: StoryContinuationContext? = null,
    onStartStoryClick: (language: String) -> Unit,
    onRescanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_confirm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_badge_scale"
    )

    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_float_y"
    )

    // Play celebration sound when hero is born
    LaunchedEffect(Unit) {
        PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
    }

    DisposableEffect(Unit) {
        onDispose {
            PlatformAudioService.stop()
        }
    }

    val activeProfileId = org.wonderloop.app.data.ProfileRepository.activeProfile?.id
    var selectedLanguage by remember(activeProfileId) {
        mutableStateOf(org.wonderloop.app.data.ProfileRepository.getEffectiveLanguage())
    }

    val quoteText = if (character.personality.isNotBlank()) {
        "«${character.personality}»"
    } else {
        AppStrings.defaultHeroSpeech
    }

    val traitsList = remember(character) {
        (character.distinctiveFeatures + character.primaryColors)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HeroCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        HeroGoldSoft.copy(alpha = 0.55f),
                        HeroCoralSoft.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    radius = 900f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Confetti celebration overlay
        ConfettiEffect()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top celebratory badge / Saga badge
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = HeroGoldSoft,
                border = BorderStroke(1.dp, Color(0xFFF3DC9B)),
                modifier = Modifier
                    .scale(pulseScale)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (continuation != null) {
                        "🌟 ${AppStrings.sagaBadge(continuation.chapterIndex)} · ${continuation.previousHeroName} + ${character.name}"
                    } else if (AppStrings.isEnglish) {
                        "✨ NEW HERO DISCOVERED ✨"
                    } else {
                        "✨ NUEVO PROTAGONISTA MÁGICO ✨"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeroGoldText,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Title
            Text(
                text = if (continuation != null) {
                    AppStrings.heroConfirmContinuationTitle(continuation.previousHeroName)
                } else {
                    AppStrings.heroBornTitle
                },
                fontFamily = TitleFont(),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = HeroInkDark,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (continuation != null) {
                    AppStrings.heroConfirmContinuationSubtitle(continuation.previousHeroName, childName)
                } else {
                    AppStrings.heroBornSubtitle(childName)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HeroInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Hero Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, HeroCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reinterpreted Character Portrait
                    val displayImage = character.reinterpretedImageUrl
                        ?: character.originalDrawingBase64

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF6DE),
                                        Color(0xFFFFE8EC)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFF5E2B8), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!displayImage.isNullOrBlank()) {
                            WonderLoopImage(
                                model = displayImage,
                                contentDescription = if (AppStrings.isEnglish) {
                                    "Magical portrait of ${character.name}"
                                } else {
                                    "Retrato mágico de ${character.name}"
                                },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = floatOffsetY.dp)
                            )
                        } else {
                            Text(
                                text = "🎨",
                                fontSize = 64.sp,
                                modifier = Modifier.scale(pulseScale)
                            )
                        }

                        // Top-left Speaker Button (reads character greeting aloud)
                        Surface(
                            onClick = {
                                val speech = "${character.name}. ${character.species}. ${character.personality.ifBlank { AppStrings.defaultHeroSpeech }}"
                                PlatformAudioService.speak(speech)
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.94f),
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, HeroCardBorder),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🔊", fontSize = 17.sp)
                            }
                        }

                        // Top-right Badge "Tu Personaje"
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HeroCoral,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "✨ ${AppStrings.yourHero}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Character Name
                    Text(
                        text = character.name,
                        fontFamily = TitleFont(),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeroInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Species Pill
                    if (character.species.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HeroCoralSoft,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = character.species,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeroCoral,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Trait / Feature Chips
                    if (traitsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            traitsList.forEach { trait ->
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color(0xFFF7F2EA),
                                    border = BorderStroke(1.dp, HeroCardBorder)
                                ) {
                                    Text(
                                        text = trait,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeroInkMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Friendly Mint Speech Bubble
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = HeroMintSoft,
                        border = BorderStroke(1.dp, HeroMintBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlatformAudioService.speak(character.personality.ifBlank { AppStrings.defaultHeroSpeech })
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💬", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = quoteText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeroInkDark,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Story Language Segmented Selector Pill
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppStrings.storyLanguageLabel.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeroInkMuted,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .background(HeroCardBorder)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isEs = selectedLanguage == "es"
                    Surface(
                        onClick = {
                            selectedLanguage = "es"
                            AppStrings.setAppLanguage("es")
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isEs) Color.White else Color.Transparent,
                        shadowElevation = if (isEs) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = AppStrings.inSpanish,
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEs) HeroInkDark else HeroInkMuted
                            )
                        }
                    }

                    val isEn = selectedLanguage == "en"
                    Surface(
                        onClick = {
                            selectedLanguage = "en"
                            AppStrings.setAppLanguage("en")
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isEn) Color.White else Color.Transparent,
                        shadowElevation = if (isEn) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = AppStrings.inEnglish,
                                fontFamily = TitleFont(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEn) HeroInkDark else HeroInkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary & Secondary Pill Actions
            Button(
                onClick = { onStartStoryClick(selectedLanguage) },
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeroCoral,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (continuation != null) {
                        AppStrings.continueSagaButton(continuation.chapterIndex)
                    } else {
                        AppStrings.startStoryButton
                    },
                    fontFamily = TitleFont(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRescanClick,
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.5.dp, HeroCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = HeroInkMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = AppStrings.scanAnotherCharacter,
                    fontFamily = TitleFont(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HeroInkMuted
                )
            }
        }
    }
}
