package org.wonderloop.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.theme.AppStrings
import org.wonderloop.app.theme.TitleFont

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    // Floating background blobs (fl: translateY 0 -> -14dp, scale 1 -> 1.08)
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1Y"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2Y"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3Y"
    )
    val blob4Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob4Y"
    )

    // Center logo breathing & rocking (sc: scale 1 -> 1.06, rotate -2deg -> 2deg)
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val logoRotate by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoRotate"
    )

    // Top sparkle rotation & pulse
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleRot"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    // Twinkling dots around logo
    val twinkle1 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle1"
    )
    val twinkle2 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle2"
    )
    val twinkle3 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle3"
    )

    // 3 bouncing loading dots (sdots)
    val dot1OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Y"
    )
    val dot2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Y"
    )
    val dot3OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Y"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE9DC),
                            Color(0xFFFCEFEA),
                            Color(0xFFF7ECF4)
                        ),
                        center = Offset(size.width * 0.3f, size.height * 0.45f),
                        radius = maxOf(size.width, size.height) * 0.8f
                    )
                )
            }
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        // 1. Floating pastel blobs (.bl.b1 .. .b4)
        Box(
            modifier = Modifier
                .offset(x = screenW * 0.10f, y = screenH * 0.12f + blob1Y.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B).copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -(screenW * 0.10f), y = screenH * 0.18f + blob2Y.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1FBE9C).copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = screenW * 0.16f, y = -(screenH * 0.20f) + blob3Y.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD166).copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -(screenW * 0.14f), y = -(screenH * 0.14f) + blob4Y.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFFC9C3F2).copy(alpha = 0.5f))
        )

        // 2. Center Brand Logo + Title + Bouncing Dots
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo Container (.slogo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(152.dp)
                    .scale(logoScale)
                    .rotate(logoRotate)
            ) {
                // White rounded square halo (.halo)
                Surface(
                    shape = RoundedCornerShape(40.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxSize()
                ) {}

                // Twinkling colored dots (.d1, .d2, .d3)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 22.dp, y = 14.dp)
                        .size(10.dp)
                        .scale(twinkle1)
                        .alpha(twinkle1)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9466))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-18).dp, y = 16.dp)
                        .size(9.dp)
                        .scale(twinkle2)
                        .alpha(twinkle2)
                        .clip(CircleShape)
                        .background(Color(0xFF1FBE9C))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-12).dp)
                        .size(10.dp)
                        .scale(twinkle3)
                        .alpha(twinkle3)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD166))
                )

                // Top Golden 4-Point Sparkle (.spk)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 4.dp)
                        .size(26.dp)
                        .rotate(sparkleRotation)
                        .scale(sparkleScale)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        cubicTo(w * 0.54f, h * 0.36f, w * 0.64f, h * 0.46f, w, h * 0.5f)
                        cubicTo(w * 0.64f, h * 0.54f, w * 0.54f, h * 0.64f, w * 0.5f, h)
                        cubicTo(w * 0.46f, h * 0.64f, w * 0.36f, h * 0.54f, 0f, h * 0.5f)
                        cubicTo(w * 0.36f, h * 0.46f, w * 0.46f, h * 0.36f, w * 0.5f, 0f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFFC93C))
                }

                // Infinity Loop SVG (<svg viewBox="0 0 120 80">)
                Canvas(
                    modifier = Modifier.size(width = 96.dp, height = 64.dp)
                ) {
                    val scaleX = size.width / 120f
                    val scaleY = size.height / 80f
                    val s = minOf(scaleX, scaleY)

                    // Left loop: cx=34, cy=40, r=26, stroke=#FF6B6B, strokeWidth=15
                    drawCircle(
                        color = Color(0xFFFF6B6B),
                        radius = 26f * s,
                        center = Offset(34f * scaleX, 40f * scaleY),
                        style = Stroke(width = 15f * s)
                    )

                    // Right loop: cx=86, cy=40, r=26, stroke=#1FBE9C, strokeWidth=15
                    drawCircle(
                        color = Color(0xFF1FBE9C),
                        radius = 26f * s,
                        center = Offset(86f * scaleX, 40f * scaleY),
                        style = Stroke(width = 15f * s)
                    )

                    // Center rotated bridge: rect x=52, y=26, width=16, height=28, rx=8, fill=#FF9466, rotate(28, 60, 40)
                    rotate(degrees = 28f, pivot = Offset(60f * scaleX, 40f * scaleY)) {
                        drawRoundRect(
                            color = Color(0xFFFF9466),
                            topLeft = Offset(52f * scaleX, 26f * scaleY),
                            size = Size(16f * scaleX, 28f * scaleY),
                            cornerRadius = CornerRadius(8f * s, 8f * s)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Brand Name: Wonder (Gradient) + Loop (#2B2340)
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF6B6B), Color(0xFF1FBE9C))
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Wonder")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF2B2340),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Loop")
                    }
                },
                fontFamily = TitleFont(),
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: CUENTOS MÁGICOS
            Text(
                text = AppStrings.splashSubtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6B6480),
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 3 Bouncing Dots (.sdots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1OffsetY.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0B9BE))
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2OffsetY.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD9A0))
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3OffsetY.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9EE3D3))
                )
            }
        }
    }
}
