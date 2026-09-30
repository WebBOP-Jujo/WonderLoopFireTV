package org.wonderloop.app.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*

private val ProfCreamBg = Color(0xFFFFFBF5)
private val ProfInkDark = Color(0xFF2B2340)
private val ProfInkMuted = Color(0xFF6B6480)
private val ProfCoral = Color(0xFFD9384A)
private val ProfCoralSoft = Color(0xFFFDECE9)
private val ProfCoralBorder = Color(0xFFF9D0CA)
private val ProfMint = Color(0xFF0E7F64)
private val ProfMintSoft = Color(0xFFE4F6F0)
private val ProfMintBorder = Color(0xFFB8E6D6)
private val ProfGoldSoft = Color(0xFFFFF1CF)
private val ProfCardBorder = Color(0xFFEDE8E1)

@Composable
fun ProfileScreen(
    initialName: String = "Leo",
    initialAge: Int = 6,
    initialAvatar: String = "🧒",
    profileId: String? = null,
    isNewProfile: Boolean = false,
    canDeleteProfile: Boolean = false,
    onBackClick: () -> Unit = {},
    onDeleteProfileClick: () -> Unit = {},
    onProfileConfirmed: (name: String, age: Int, avatar: String) -> Unit
) {
    val availableAvatars = listOf("👦", "👧", "🦁", "🦄", "🚀", "🎨", "🐱", "🐼")
    var name by remember(profileId, isNewProfile) { mutableStateOf(if (isNewProfile) "" else initialName) }
    var selectedAge by remember(profileId, isNewProfile) { mutableStateOf(if (isNewProfile) 8 else initialAge) }
    var selectedAvatar by remember(profileId, isNewProfile) {
        mutableStateOf(
            if (isNewProfile) "👦"
            else if (initialAvatar in availableAvatars) initialAvatar
            else availableAvatars.first()
        )
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Custom Delete Profile Confirmation Modal
    if (showDeleteDialog) {
        val targetProfileName = name.ifBlank { initialName }
        Dialog(
            onDismissRequest = { showDeleteDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ProfCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ProfCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗑️", fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStrings.deleteProfileQuestion(targetProfileName),
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.deleteProfileWarning(targetProfileName),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showDeleteDialog = false
                            onDeleteProfileClick()
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfCoral,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = AppStrings.confirmDelete,
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ProfCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ProfInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.cancel,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfInkMuted
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfCreamBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ProfGoldSoft.copy(alpha = 0.35f),
                        ProfCoralSoft.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    radius = 900f
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Navigation Bar: [ ← ] Nuevo perfil / Editar perfil
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    onBackClick()
                },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.5.dp, ProfCardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProfInkDark
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (isNewProfile) AppStrings.newProfileTitle else AppStrings.editProfile,
                fontFamily = TitleFont(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfInkDark
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 1. "Elige tu avatar"
        Text(
            text = AppStrings.chooseYourAvatarTitle,
            fontFamily = TitleFont(),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfInkDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2 rows of 4 avatars
        val avatarRows = availableAvatars.chunked(4)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            avatarRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { avatar ->
                        val isSelected = avatar == selectedAvatar
                        Surface(
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                selectedAvatar = avatar
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) ProfCoralSoft else Color.White,
                            border = BorderStroke(
                                width = if (isSelected) 2.2.dp else 1.5.dp,
                                color = if (isSelected) ProfCoral else ProfCardBorder
                            ),
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = avatar, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. "¿Cómo te llamas?"
        Text(
            text = AppStrings.whatIsYourNameTitle,
            fontFamily = TitleFont(),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfInkDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = {
                Text(
                    text = AppStrings.childNameMockupPlaceholder,
                    color = ProfInkMuted.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ProfCoral,
                unfocusedBorderColor = ProfCardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = ProfInkDark,
                unfocusedTextColor = ProfInkDark,
                cursorColor = ProfCoral
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. "¿Cuántos años tienes?"
        Text(
            text = AppStrings.howOldAreYouTitle,
            fontFamily = TitleFont(),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfInkDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: 4, 5, 6, 7
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(4, 5, 6, 7).forEach { age ->
                val isSelected = age == selectedAge
                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        selectedAge = age
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ProfCoral else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isSelected) ProfCoral else ProfCardBorder
                    ),
                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$age",
                            fontFamily = TitleFont(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else ProfInkDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: 8, 9, 10
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(8, 9, 10).forEach { age ->
                val isSelected = age == selectedAge
                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        selectedAge = age
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ProfCoral else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isSelected) ProfCoral else ProfCardBorder
                    ),
                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$age",
                            fontFamily = TitleFont(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else ProfInkDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Pedagogical context note + Default Art Style
        val stageNote = when {
            selectedAge <= 5 -> if (AppStrings.isEnglish)
                "🌱 Early Steps: gentle stories, sweet rhymes, and simple visual challenges."
            else
                "🌱 Primeros Pasos: lenguaje dulce, rimas y desafíos visuales sencillos."
            selectedAge <= 7 -> if (AppStrings.isEnglish)
                "🌟 Little Creator: fun adventures, rich vocabulary, and guided creative quests."
            else
                "🌟 Pequeño Creador: tramas divertidas, vocabulario enriquecedor y retos guiados."
            else -> if (AppStrings.isEnglish)
                "🚀 Great Explorer: clever plots, problem solving, and creative challenges."
            else
                "🚀 Gran Explorador: tramas con ingenio y retos creativos."
        }
        val styleNote = when {
            selectedAge <= 6 -> if (AppStrings.isEnglish)
                "🎨 Default: Magical Watercolor · 🎙️ Soft voice"
            else
                "🎨 Por defecto: Acuarela Mágica · 🎙️ Voz Suave"
            selectedAge <= 8 -> if (AppStrings.isEnglish)
                "🎬 Default: 3D Animation · 🎙️ Normal voice"
            else
                "🎬 Por defecto: Animación 3D · 🎙️ Voz Normal"
            else -> if (AppStrings.isEnglish)
                "🎬 Default: 3D Animation · 🎙️ Lively voice"
            else
                "🎬 Por defecto: Animación 3D · 🎙️ Voz Alegre"
        }
        val contextNote = "$stageNote\n$styleNote"

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ProfMintSoft,
            border = BorderStroke(1.5.dp, ProfMintBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = contextNote,
                fontSize = 13.sp,
                color = ProfInkDark,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Large Bottom CTA: [ ¡Listo! ]
        Button(
            onClick = {
                val defaultName = if (AppStrings.isEnglish) "Little Hero" else "Pequeño Creador"
                val finalName = if (name.isNotBlank()) name.trim() else defaultName
                PlatformAudioService.playSfx(SoundEffect.POP)
                onProfileConfirmed(finalName, selectedAge, selectedAvatar)
            },
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfCoral,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = AppStrings.allSetCta,
                fontFamily = TitleFont(),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        if (!isNewProfile && canDeleteProfile) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    PlatformAudioService.playSfx(SoundEffect.POP)
                    showDeleteDialog = true
                },
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.5.dp, ProfCoralBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ProfCoralSoft.copy(alpha = 0.6f),
                    contentColor = ProfCoral
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "🗑️ " + AppStrings.deleteProfileQuestion(name.ifBlank { initialName }),
                    fontFamily = TitleFont(),
                    fontSize = 14.sp,
                    color = ProfCoral,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
