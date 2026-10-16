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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
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
import org.wonderloop.app.ui.components.tvFocusable

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

    val startStoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
        try {
            startStoryFocusRequester.requestFocus()
        } catch (_: Exception) {}
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HeroCreamBg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HeroGoldSoft.copy(alpha = 0.55f),
                            HeroCoralSoft.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxOf(size.width, size.height) * 0.65f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ConfettiEffect()

        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // =================================================================
            // 16:9 WIDESCREEN 2-COLUMN LAYOUT FOR AMAZON FIRE TV (+42")
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp), // 5% Safe Zone
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN: Character Portrait Card & Traits
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, HeroCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayImage = character.reinterpretedImageUrl
                            ?: character.originalDrawingBase64

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFF6DE), Color(0xFFFFE8EC))
                                    )
                                )
                                .border(2.dp, Color(0xFFF5E2B8), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!displayImage.isNullOrBlank()) {
                                WonderLoopImage(
                                    model = displayImage,
                                    contentDescription = character.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(y = floatOffsetY.dp)
                                )
                            } else {
                                Text(text = "🎨", fontSize = 64.sp, modifier = Modifier.scale(pulseScale))
                            }

                            // Speaker Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.94f),
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, HeroCardBorder),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .size(40.dp)
                                    .tvFocusable(
                                        shape = CircleShape,
                                        onClick = {
                                            val speech = "${character.name}. ${character.species}. ${character.personality.ifBlank { AppStrings.defaultHeroSpeech }}"
                                            PlatformAudioService.speak(speech)
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🔊", fontSize = 18.sp)
                                }
                            }

                            // Your Hero Pill
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
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Name + Species
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = character.name,
                                fontFamily = TitleFont(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeroInkDark
                            )

                            if (character.species.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = HeroCoralSoft
                                ) {
                                    Text(
                                        text = character.species,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HeroCoral,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Speech Quote Bubble
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HeroMintSoft,
                            border = BorderStroke(1.dp, HeroMintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    onClick = {
                                        PlatformAudioService.speak(character.personality.ifBlank { AppStrings.defaultHeroSpeech })
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = quoteText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeroInkDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Celebration, Story Language Selector, Start Story CTA
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebration badge
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = HeroGoldSoft,
                        border = BorderStroke(1.dp, Color(0xFFF3DC9B)),
                        modifier = Modifier.scale(pulseScale)
                    ) {
                        Text(
                            text = if (continuation != null) {
                                "🌟 ${AppStrings.sagaBadge(continuation.chapterIndex)} · ${continuation.previousHeroName} + ${character.name}"
                            } else if (AppStrings.isEnglish) {
                                "✨ NEW HERO DISCOVERED ✨"
                            } else {
                                "✨ NUEVO PROTAGONISTA MÁGICO ✨"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HeroGoldText,
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Title & Subtitle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (continuation != null) {
                                AppStrings.heroConfirmContinuationTitle(continuation.previousHeroName)
                            } else {
                                AppStrings.heroBornTitle
                            },
                            fontFamily = TitleFont(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeroInkDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (continuation != null) {
                                AppStrings.heroConfirmContinuationSubtitle(continuation.previousHeroName, childName)
                            } else {
                                AppStrings.heroBornSubtitle(childName)
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HeroInkMuted,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Story Language Selector Pill
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = AppStrings.storyLanguageLabel.uppercase(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HeroInkMuted,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFFEDE8E1))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isEs = selectedLanguage == "es"
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (isEs) Color.White else Color.Transparent,
                                shadowElevation = if (isEs) 3.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = {
                                            selectedLanguage = "es"
                                            AppStrings.setAppLanguage("es")
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = AppStrings.inSpanish,
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEs) HeroInkDark else HeroInkMuted
                                    )
                                }
                            }

                            val isEn = selectedLanguage == "en"
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (isEn) Color.White else Color.Transparent,
                                shadowElevation = if (isEn) 3.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = {
                                            selectedLanguage = "en"
                                            AppStrings.setAppLanguage("en")
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = AppStrings.inEnglish,
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEn) HeroInkDark else HeroInkMuted
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Buttons (D-Pad focused)
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HeroCoral,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(50.dp),
                                    focusedScale = 1.05f,
                                    focusRequester = startStoryFocusRequester,
                                    onClick = { onStartStoryClick(selectedLanguage) }
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (continuation != null) {
                                        AppStrings.continueSagaButton(continuation.chapterIndex)
                                    } else {
                                        "✨ ¡Crear Cuento en la Tele!"
                                    },
                                    fontFamily = TitleFont(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, HeroCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(50.dp),
                                    focusedScale = 1.05f,
                                    onClick = onRescanClick
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "◀ ${AppStrings.scanAnotherCharacter}",
                                    fontFamily = TitleFont(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HeroInkMuted
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // PORTRAIT FALLBACK
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = character.name,
                    fontFamily = TitleFont(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HeroInkDark
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onStartStoryClick(selectedLanguage) },
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCoral)
                ) {
                    Text(text = "Comenzar Cuento")
                }
            }
        }
    }
}
