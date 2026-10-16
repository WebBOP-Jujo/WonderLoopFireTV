package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.tvFocusable

@Composable
fun ParentalGateDialog(
    explanation: String? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var num1 by remember { mutableStateOf((6..9).random()) }
    var num2 by remember { mutableStateOf((4..9).random()) }
    var showError by remember { mutableStateOf(false) }

    val brandRed = CoralHero // #D9384A
    val deepInk = Color(0xFF2B2340)
    val mutedInk = Color(0xFF6B6480)
    val softBlush = Color(0xFFFDECE9)
    val softBlushBorder = Color(0xFFF6D5D0)

    val correctAnswer = num1 * num2
    val options = remember(num1, num2) {
        val list = mutableSetOf(correctAnswer)
        val candidates = listOf(
            correctAnswer + num1,
            correctAnswer - num1,
            correctAnswer + num2,
            correctAnswer - num2,
            correctAnswer + 10,
            correctAnswer - 10,
            (num1 + 1) * (num2 - 1),
            (num1 - 1) * (num2 + 1),
            correctAnswer + 4,
            correctAnswer - 4
        ).filter { it > 0 && it != correctAnswer }.shuffled()

        for (c in candidates) {
            list.add(c)
            if (list.size == 4) break
        }
        var offset = 1
        while (list.size < 4) {
            val cand = correctAnswer + offset
            if (cand > 0) list.add(cand)
            offset++
        }
        list.toList().shuffled()
    }

    fun selectAnswer(selected: Int) {
        if (selected == correctAnswer) {
            PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
            onSuccess()
        } else {
            PlatformAudioService.playSfx(SoundEffect.POP)
            showError = true
            num1 = (6..9).random()
            num2 = (4..9).random()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Blush Badge with Vector Padlock Icon (matches provided image 1:1)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(softBlush)
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val strokeW = 2.4.dp.toPx()

                        // Padlock body: <rect x="5" y="11" width="14" height="10" rx="2"/>
                        drawRoundRect(
                            color = brandRed,
                            topLeft = Offset(5f * sx, 11f * sy),
                            size = Size(14f * sx, 10f * sy),
                            cornerRadius = CornerRadius(2.4f * sx, 2.4f * sy),
                            style = Stroke(
                                width = strokeW,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Padlock shackle: <path d="M8 11V8a4 4 0 0 1 8 0v3"/>
                        val shacklePath = Path().apply {
                            moveTo(8f * sx, 11f * sy)
                            lineTo(8f * sx, 8f * sy)
                            arcTo(
                                rect = Rect(
                                    left = 8f * sx,
                                    top = 4f * sy,
                                    right = 16f * sx,
                                    bottom = 12f * sy
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(16f * sx, 11f * sy)
                        }
                        drawPath(
                            path = shacklePath,
                            color = brandRed,
                            style = Stroke(
                                width = strokeW,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = AppStrings.parentalGateTitle,
                    fontFamily = TitleFont(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = deepInk,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.parentalGatePrompt,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (!explanation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(softBlush)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = explanation,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Math Question Box (new redesign blush card + #D9384A typography)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(softBlush)
                        .border(1.5.dp, softBlushBorder, RoundedCornerShape(18.dp))
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = AppStrings.mathQuestion(num1, num2),
                        fontFamily = TitleFont(),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = brandRed
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Large D-Pad Navigable Option Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val firstOptionRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        try { firstOptionRequester.requestFocus() } catch (_: Exception) {}
                    }

                    options.forEachIndexed { index, option ->
                        Surface(
                            onClick = { selectAnswer(option) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFFBF9),
                            border = BorderStroke(1.5.dp, Color(0xFFE2DCEB)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .then(if (index == 0) Modifier.focusRequester(firstOptionRequester) else Modifier)
                                .tvFocusable(
                                    shape = RoundedCornerShape(18.dp),
                                    focusedScale = 1.08f,
                                    onClick = { selectAnswer(option) }
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = option.toString(),
                                    fontFamily = TitleFont(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = deepInk
                                )
                            }
                        }
                    }
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = AppStrings.wrongAnswer,
                        fontSize = 13.sp,
                        color = brandRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE2DCEB)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = deepInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(44.dp)
                        .tvFocusable(shape = RoundedCornerShape(50.dp), onClick = onDismiss)
                ) {
                    Text(
                        text = AppStrings.cancel,
                        fontFamily = TitleFont(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deepInk
                    )
                }
            }
        }
    }
}
