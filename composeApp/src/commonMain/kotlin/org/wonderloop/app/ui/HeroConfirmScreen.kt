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
import kotlinx.coroutines.delay
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
    sessionId: String? = null,
    api: org.wonderloop.app.data.WonderLoopApi? = null,
    onStartStoryClick: (language: String) -> Unit,
    onRescanClick: () -> Unit,
    onNewPhotoReceived: ((imageBase64: String) -> Unit)? = null,
    onBackToHomeClick: (() -> Unit)? = null
) {
    val isBlankOrInvalid = !character.isValidDrawing ||
        character.confidence < 0.55f ||
        character.name.equals("Vacío", ignoreCase = true) ||
        character.name.equals("Vacio", ignoreCase = true) ||
        character.species.contains("sin dibujo", ignoreCase = true) ||
        character.species.contains("superficie", ignoreCase = true) ||
        character.species.contains("vacía", ignoreCase = true) ||
        character.species.contains("vacia", ignoreCase = true)

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
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isBlankOrInvalid) {
        if (isBlankOrInvalid) {
            PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
            delay(120)
            try {
                retryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            PlatformAudioService.playSfx(SoundEffect.CELEBRATION)
            delay(120)
            try {
                startStoryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Real-time mobile photo listener when drawing is blank/unclear:
    // If the parent takes another photo on their phone while looking at this screen,
    // the TV automatically picks it up and advances to MagicLoadingScreen without needing the remote!
    LaunchedEffect(isBlankOrInvalid, sessionId) {
        val sId = sessionId
        val apiClient = api
        if (isBlankOrInvalid && !sId.isNullOrBlank() && apiClient != null) {
            // Notify backend that TV is showing the retry screen so the phone displays retry guidance
            try {
                apiClient.updateTvSessionStatus(sId, "retry")
            } catch (_: Exception) {}

            var pollCount = 0
            while (pollCount < 300) { // Listen for up to 6 minutes
                delay(1200)
                pollCount++
                try {
                    val pollResult = apiClient.pollTvSession(sId)
                    if (pollResult.status == "ready" && !pollResult.imageBase64.isNullOrBlank()) {
                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                        onNewPhotoReceived?.invoke(pollResult.imageBase64)
                        break
                    }
                } catch (_: Exception) {}
            }
        }
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
        if (!isBlankOrInvalid) {
            ConfettiEffect()
        }

        val isLandscape = maxWidth > maxHeight

        if (isBlankOrInvalid) {
            if (isLandscape) {
                // =================================================================
                // 16:9 RETRY WIDESCREEN LAYOUT (BLANK OR UNCLEAR DRAWING)
                // =================================================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 18.dp), // 5% Safe Zone
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: Captured Camera Photo
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, HeroCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            val displayImage = character.originalDrawingBase64
                                ?: character.reinterpretedImageUrl

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
                                        contentDescription = "Foto capturada",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(text = "📄", fontSize = 64.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = HeroCoralSoft,
                                    border = BorderStroke(1.dp, HeroCoral.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = if (AppStrings.isEnglish) "📷 Camera Photo" else "📷 Foto Capturada",
                                        color = HeroCoral,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFBF7F0),
                                border = BorderStroke(1.dp, HeroCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔍", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (AppStrings.isEnglish)
                                            "No clear drawing detected on the paper surface."
                                        else
                                            "No se detectaron trazos claros de dibujo en esta foto.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HeroInkMuted
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Tips & D-Pad Actions
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HeroCoralSoft,
                            border = BorderStroke(1.5.dp, HeroCoral.copy(alpha = 0.4f)),
                            modifier = Modifier.scale(pulseScale)
                        ) {
                            Text(
                                text = AppStrings.retryScanBadge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeroCoral,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppStrings.retryScanTitle,
                                fontFamily = TitleFont(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeroInkDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.retryScanSubtitle,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeroInkMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(0.96f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Tip 1
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE8E2D8)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💡", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = AppStrings.retryTip1Title,
                                            fontFamily = TitleFont(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HeroInkDark
                                        )
                                        Text(
                                            text = AppStrings.retryTip1Desc,
                                            fontSize = 11.5.sp,
                                            color = HeroInkMuted
                                        )
                                    }
                                }
                            }

                            // Tip 2
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE8E2D8)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "📐", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = AppStrings.retryTip2Title,
                                            fontFamily = TitleFont(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HeroInkDark
                                        )
                                        Text(
                                            text = AppStrings.retryTip2Desc,
                                            fontSize = 11.5.sp,
                                            color = HeroInkMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions: Live Mobile Notice Card + Single Exit to Home button
                        Column(
                            modifier = Modifier.fillMaxWidth(0.96f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFFF9E6),
                                border = BorderStroke(1.5.dp, Color(0xFFFFD166)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEDBA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📱", fontSize = 22.sp, modifier = Modifier.scale(pulseScale))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = AppStrings.retryMobileNoticeTitle,
                                            fontFamily = TitleFont(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8A5A00)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppStrings.retryMobileNoticeSubtitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6B4800)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, HeroCardBorder),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.50f)
                                    .height(42.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        focusRequester = retryFocusRequester,
                                        onClick = {
                                            if (onBackToHomeClick != null) {
                                                onBackToHomeClick()
                                            } else {
                                                onRescanClick()
                                            }
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🏠 " + AppStrings.exitToHome,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeroInkDark
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // PORTRAIT RETRY FALLBACK
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = AppStrings.retryScanTitle,
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeroInkDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = AppStrings.retryScanSubtitle,
                        fontSize = 14.sp,
                        color = HeroInkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRescanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = HeroCoral)
                    ) {
                        Text(text = AppStrings.retryScanButton)
                    }
                }
            }
        } else if (isLandscape) {
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
                                    fontSize = 13.5.sp,
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
