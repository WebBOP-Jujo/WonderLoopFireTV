package org.wonderloop.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.*

// Design System Colors from wonderloop-firetv.html
private val TvCreamBg = Color(0xFFFFFBF5)
private val TvIvoryGlow = Color(0xFFFFF6E3)
private val TvInkDark = Color(0xFF2B2340)
private val TvInkMuted = Color(0xFF6B6480)
private val TvCoral = Color(0xFFD9384A)
private val TvCoralGradientEnd = Color(0xFFE5566A)
private val TvCoralSoft = Color(0xFFFCEAE7)
private val TvCoralBorder = Color(0xFFE8A0A8)
private val TvTeal = Color(0xFF0E7F64)
private val TvTealSoft = Color(0xFFDDF5EE)
private val TvTealBorder = Color(0xFF8FD3C0)
private val TvGoldSoft = Color(0xFFFFF1CF)
private val TvGoldText = Color(0xFF6B4A00)
private val TvTagText = Color(0xFF8A6200)

/**
 * Helper extension to draw clean dashed borders matching Fire TV HTML specs
 */
private fun Modifier.tvDashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Float = 14f,
    gapLength: Float = 8f
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
    )
    drawRoundRect(
        color = color,
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}

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
    val lastReadStory = StoryRepository.getLastReadStoryForProfile()
    val availableStories by SubscriptionManager.availableStories
    val mainCtaFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            mainCtaFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Determine current display story (real last read, first story, or fallback)
    val displayStory = lastReadStory ?: profileStories.firstOrNull()

    // Determine current display character (or Grisin fallback)
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
    val displayCharacter = uniqueCharacters.firstOrNull()

    // Pulse animation for scan line in Step 2 icon and stroke animation for Step 1 drawing
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )
    val drawingProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drawing_progress"
    )

    // 100% WIDESCREEN 16:9 CANVAS (1920x1080) - NO VERTICAL SCROLL
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(TvIvoryGlow, TvCreamBg),
                    center = Offset(1300f, 250f),
                    radius = 900f
                )
            )
            .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV 5% Safe Zone
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // 1. TOP BAR (.top)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LOGO: WonderLoop with dual-ring infinity mark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(modifier = Modifier.size(width = 46.dp, height = 30.dp)) {
                        val strokeW = 5.dp.toPx()
                        val r = 11.dp.toPx()
                        // Left Coral Ring
                        drawCircle(
                            color = Color(0xFFFF6B6B),
                            radius = r,
                            center = Offset(14.dp.toPx(), 15.dp.toPx()),
                            style = Stroke(width = strokeW)
                        )
                        // Right Teal Ring
                        drawCircle(
                            color = Color(0xFF1FBE9C),
                            radius = r,
                            center = Offset(32.dp.toPx(), 15.dp.toPx()),
                            style = Stroke(width = strokeW)
                        )
                    }

                    Row {
                        Text(
                            text = "Wonder",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE5484D)
                        )
                        Text(
                            text = "Loop",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF12A58A)
                        )
                    }
                }

                // RIGHT CHIPS: [🚀 Leo ∨], [👤 Mi Perfil] and [MinimalistLock Padres]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Profile Switcher Chip (.chip2)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(24.dp),
                                focusedScale = 1.05f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onSwitchProfileClick()
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 6.dp, end = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE3E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (childAvatar.isNotBlank()) childAvatar else "🚀",
                                    fontSize = 18.sp
                                )
                            }

                            Text(
                                text = if (childName.isNotBlank()) childName else "Leo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TvInkDark
                            )

                            Text(
                                text = "∨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvInkDark
                            )
                        }
                    }

                    // 2. Child Profile Button ("Mi Perfil")
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(24.dp),
                                focusedScale = 1.05f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onEditProfileClick()
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(text = "👤", fontSize = 16.sp)
                            Text(
                                text = if (AppStrings.isEnglish) "Profile" else "Mi Perfil",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TvInkDark
                            )
                        }
                    }

                    // 3. Parents Chip with Minimalist Lock (.chip2.pd)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(24.dp),
                                focusedScale = 1.05f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onParentsClick()
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            MinimalistLockIcon(tint = TvInkDark, size = 18.dp)
                            Text(
                                text = if (AppStrings.isEnglish) "Parents" else "Padres",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TvInkDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 2. HERO BANNER (.hero)
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(TvCoral, TvCoralGradientEnd),
                            start = Offset(0f, 0f),
                            end = Offset(1200f, 400f)
                        )
                    )
            ) {
                // Background subtle decorative circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = 180.dp.toPx(),
                        center = Offset(size.width + 40.dp.toPx(), -20.dp.toPx())
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 34.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN (.hl): Title, subtitle, CTA Button
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Crea tu cuento",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 38.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Dibuja un personaje en un folio y hazle una foto con el móvil",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // CTA Button (.cta) with D-Pad focus halo
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .height(52.dp)
                                .focusRequester(mainCtaFocusRequester)
                                .tvFocusable(
                                    shape = RoundedCornerShape(26.dp),
                                    focusedScale = 1.05f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        onNewAdventureClick()
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 26.dp)
                            ) {
                                Text(text = "📱", fontSize = 22.sp)
                                Text(
                                    text = "Escanear dibujo con el móvil",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // RIGHT COLUMN (.hr): 3 Steps + Idea de hoy pill
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 3 Visual Step Boxes - Mathematically Aligned
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Step 1: Dibuja (Animated Stroke)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(46.dp, 36.dp)) {
                                        val fullPath = Path().apply {
                                            moveTo(size.width * 0.15f, size.height * 0.75f)
                                            cubicTo(
                                                size.width * 0.25f, size.height * 0.15f,
                                                size.width * 0.55f, size.height * 0.05f,
                                                size.width * 0.65f, size.height * 0.45f
                                            )
                                            cubicTo(
                                                size.width * 0.75f, size.height * 0.85f,
                                                size.width * 0.50f, size.height * 0.95f,
                                                size.width * 0.85f, size.height * 0.45f
                                            )
                                        }

                                        // 1. Faint guideline
                                        drawPath(
                                            fullPath,
                                            color = TvCoral.copy(alpha = 0.2f),
                                            style = Stroke(
                                                width = 3.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )

                                        // 2. Animated active drawing trace
                                        val pm = PathMeasure()
                                        pm.setPath(fullPath, false)
                                        val animPath = Path()
                                        val curLen = pm.length * drawingProgress
                                        pm.getSegment(0f, curLen, animPath, true)

                                        drawPath(
                                            animPath,
                                            color = TvCoral,
                                            style = Stroke(
                                                width = 3.5.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )

                                        // 3. Glowing drawing tip moving along path
                                        val tip = pm.getPosition(curLen)
                                        drawCircle(
                                            color = Color(0xFFFFD166),
                                            radius = 3.5.dp.toPx(),
                                            center = tip
                                        )
                                        drawCircle(
                                            color = TvCoral,
                                            radius = 2.dp.toPx(),
                                            center = tip
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(38.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "1 · Dibuja",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Dotted Connector 1
                            Box(
                                modifier = Modifier.height(66.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "· · ·",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Step 2: Foto con el móvil (Phone + Lion + Scan beam)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Mini phone frame with 🦁
                                    Box(
                                        modifier = Modifier
                                            .size(width = 32.dp, height = 48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFCEAE7))
                                            .border(2.5.dp, TvInkDark, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🦁", fontSize = 16.sp)
                                        // Red animated scan line
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .offset(y = (-14 + (28 * scanLineOffset)).dp)
                                                .background(TvCoral)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(38.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "2 · Foto con el móvil",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        maxLines = 2
                                    )
                                }
                            }

                            // Dotted Connector 2
                            Box(
                                modifier = Modifier.height(66.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "· · ·",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Step 3: ¡Magia en la tele!
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "📖✨", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(38.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "3 · ¡Magia en la tele!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Idea de hoy pill (.idea)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TvGoldSoft
                        ) {
                            Text(
                                text = "💡 Idea de hoy: un león con una gran melena amarilla",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TvGoldText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 3. BOTTOM BAND (.band): Mis Cuentos & Mis Amigos (Side-by-Side)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // -------------------------------------------------------------
                // LEFT GROUP: Mis cuentos (.grp)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "My Stories" else "Mis cuentos",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TvInkDark
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDE8E1)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    focusedScale = 1.06f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNavigateToStories()
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(text = "📚", fontSize = 13.sp)
                                Text(
                                    text = if (AppStrings.isEnglish) {
                                        "View library (${profileStories.size}) →"
                                    } else {
                                        "Ver biblioteca (${profileStories.size}) →"
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Seguir leyendo (.cont)
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .weight(1.4f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(26.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                        if (displayStory != null) {
                                            onOpenStory(displayStory)
                                        } else {
                                            onNewAdventureClick()
                                        }
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
                                // Cover Image (.cv)
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFFCEAE7))
                                        .shadow(3.dp, RoundedCornerShape(16.dp))
                                ) {
                                    val coverUri = displayStory?.scenes?.firstOrNull()?.illustrationUrl
                                        ?: displayStory?.character?.reinterpretedImageUrl
                                        ?: DemoGrisinAssets.COVER_BASE64

                                    WonderLoopImage(
                                        model = coverUri,
                                        contentDescription = "Portada",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Story Meta & Seguir Button (.tx)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Yellow Tag (.tag)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TvGoldSoft
                                    ) {
                                        Text(
                                            text = "SEGUIR LEYENDO",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TvTagText,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Title
                                    Text(
                                        text = displayStory?.title ?: "Grisín y el gran rescate del peluche flotante",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TvInkDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 19.sp
                                    )

                                    // Meta info
                                    val pageText = if (displayStory != null && displayStory.scenes.isNotEmpty()) {
                                        "Página 2 de ${displayStory.scenes.size}"
                                    } else {
                                        "Página 2 de 5"
                                    }
                                    val heroName = displayStory?.character?.name?.ifBlank { "Grisín" } ?: "Grisín"
                                    Text(
                                        text = "✨ $heroName · $pageText",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvInkMuted
                                    )

                                    // Single Clean Coral progress bar (.prog)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFF1E3E0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.4f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(TvCoral)
                                        )
                                    }

                                    // Visual affordance: Seguir (.btn) - Non-focusable inside focusable card
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = TvCoral,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Text(text = "▶", fontSize = 12.sp, color = Color.White)
                                            Text(
                                                text = "Seguir",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Card 2: Nuevo cuento (.new)
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .tvDashedBorder(2.5.dp, TvCoralBorder, 26.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(26.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNewAdventureClick()
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(TvCoralSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TvCoral
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Nuevo cuento",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvInkDark
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = TvTealSoft
                                ) {
                                    Text(
                                        text = "✨ $availableStories disponibles",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TvTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // RIGHT GROUP: Mis amigos (.grp)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish) "My Friends" else "Mis amigos",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TvInkDark
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDE8E1)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    focusedScale = 1.06f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNavigateToCharacters()
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(text = "🎨", fontSize = 13.sp)
                                Text(
                                    text = if (AppStrings.isEnglish) {
                                        "View all (${uniqueCharacters.size}) →"
                                    } else {
                                        "Ver todos (${uniqueCharacters.size}) →"
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvTeal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Polaroid Amigo (.pol)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer { rotationZ = -1.5f }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(22.dp),
                                        focusedScale = 1.04f,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            onNavigateToCharacters()
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Character photo
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFFFF6E3))
                                    ) {
                                        val characterUri = displayCharacter?.reinterpretedImageUrl
                                            ?: displayCharacter?.originalDrawingBase64
                                            ?: DemoGrisinAssets.POLAROID_BASE64

                                        WonderLoopImage(
                                            model = characterUri,
                                            contentDescription = displayCharacter?.name ?: "Grisín",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = displayCharacter?.name?.ifBlank { "Grisín" } ?: "Grisín",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TvInkDark
                                    )
                                }
                            }

                            // Red pin circle on top center of Polaroid
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-5).dp)
                                    .size(13.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(TvCoral, CircleShape)
                            )
                        }

                        // Card 2: Nuevo amigo (.newf)
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .tvDashedBorder(2.5.dp, TvTealBorder, 26.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(26.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        onNewAdventureClick()
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(TvTealSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TvTeal
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Nuevo amigo",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvTeal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
