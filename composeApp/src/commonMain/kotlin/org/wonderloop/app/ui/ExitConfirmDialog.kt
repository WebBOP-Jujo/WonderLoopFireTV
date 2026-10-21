package org.wonderloop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.AppStrings
import org.wonderloop.app.theme.TitleFont
import org.wonderloop.app.ui.tv.TvClickableSurface

private val ExitInk = Color(0xFF2B2340)
private val ExitInkMuted = Color(0xFF6B6480)
private val ExitCoral = Color(0xFFD9384A)
private val ExitMint = Color(0xFF0E7F64)
private val ExitCardBorder = Color(0xFFEDE8E1)

/**
 * "Are you sure you want to exit?" on the main screen, as the Fire TV controller guidelines suggest.
 * Focus starts on "keep creating" so an accidental BACK + OK never closes the app; BACK closes the dialog.
 */
@Composable
fun ExitConfirmDialog(
    childName: String,
    childAvatar: String,
    onStay: () -> Unit,
    onExit: () -> Unit
) {
    val en = AppStrings.isEnglish
    val stayFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        try { stayFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(520.dp)
                .shadow(18.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(0f to Color(0xFFFFF1E6), 0.45f to Color.White))
                .border(1.5.dp, ExitCardBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 36.dp, vertical = 30.dp)
        ) {
            // Sleepy moon with the child's avatar: "see you soon", not an error
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, ExitCoral, CircleShape)
                ) {
                    Text(text = childAvatar.ifBlank { "🧒" }, fontSize = 50.sp)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2340))
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    Text(text = "🌙", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (en) "Leaving already, ${childName.ifBlank { "friend" }}?" else "¿Ya te vas, ${childName.ifBlank { "amigo" }}?",
                fontFamily = TitleFont(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ExitInk,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (en) "Your stories and drawn friends stay saved for the next adventure."
                else "Tus cuentos y tus amigos dibujados se quedan guardados para la próxima aventura.",
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = ExitInkMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            TvClickableSurface(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    onStay()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .focusRequester(stayFocusRequester),
                shape = RoundedCornerShape(50),
                containerColor = ExitMint,
                focusedContainerColor = ExitMint,
                contentColor = Color.White,
                glowColor = ExitMint.copy(alpha = 0.4f),
                contentDescription = if (en) "Keep creating" else "Seguir creando",
                playClickSound = false
            ) {
                Text(
                    text = if (en) "✨ Keep creating" else "✨ Seguir creando",
                    fontFamily = TitleFont(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TvClickableSurface(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                containerColor = Color.White,
                borderColor = ExitCardBorder,
                contentDescription = if (en) "Exit WonderLoop" else "Salir de WonderLoop",
                playClickSound = false
            ) {
                Text(
                    text = if (en) "Exit WonderLoop" else "Salir de WonderLoop",
                    fontFamily = TitleFont(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ExitCoral,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
