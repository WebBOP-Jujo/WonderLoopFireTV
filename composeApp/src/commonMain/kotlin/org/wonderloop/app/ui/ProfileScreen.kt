package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.tvFocusable

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

    PlatformBackHandler(enabled = true) {
        if (showDeleteDialog) {
            showDeleteDialog = false
        } else {
            onBackClick()
        }
    }

    val avatarFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        try {
            avatarFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------
    // DELETE PROFILE MODAL
    // -------------------------------------------------------------
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
                    .width(480.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ProfCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗑️", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = AppStrings.deleteProfileQuestion(targetProfileName),
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.deleteProfileWarning(targetProfileName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                            .height(46.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showDeleteDialog = false
                                    onDeleteProfileClick()
                                }
                            )
                    ) {
                        Text(
                            text = AppStrings.confirmDelete,
                            fontFamily = TitleFont(),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showDeleteDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ProfCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ProfInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showDeleteDialog = false
                                }
                            )
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

    // -------------------------------------------------------------
    // MAIN 16:9 WIDESCREEN VIEWPORT (NO SCROLL)
    // -------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfCreamBg)
            .padding(horizontal = 48.dp, vertical = 22.dp)
    ) {
        // TOP NAVIGATION BAR: [ ← Cancelar ]     Título     [ Eliminar / Info ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, ProfCardBorder),
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
                        color = ProfInkDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Cancel" else "Cancelar",
                        fontFamily = TitleFont(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInkDark
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (isNewProfile) AppStrings.newProfileTitle else AppStrings.editProfile,
                fontFamily = TitleFont(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ProfInkDark
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!isNewProfile && canDeleteProfile) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = ProfCoralSoft,
                    border = BorderStroke(1.5.dp, ProfCoralBorder),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.08f,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showDeleteDialog = true
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🗑️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (AppStrings.isEnglish) "Delete profile" else "Eliminar",
                            fontFamily = TitleFont(),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfCoral
                        )
                    }
                }
            } else {
                val profileCount = ProfileRepository.profiles.size
                val profileNumber = if (isNewProfile) {
                    (profileCount + 1).coerceAtMost(3)
                } else {
                    val idx = ProfileRepository.profiles.indexOfFirst { it.id == profileId }
                    if (idx >= 0) idx + 1 else 1
                }
                val tagText = if (AppStrings.isEnglish) "Profile $profileNumber of 3" else "Perfil $profileNumber de 3"
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, ProfCoralBorder)
                ) {
                    Text(
                        text = tagText,
                        fontFamily = TitleFont(),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfCoral,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2-COLUMN WIDESCREEN LAYOUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // =========================================================
            // LEFT COLUMN (Live Preview Card + Age Selector + CTA)
            // =========================================================
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. LIVE PREVIEW CARD
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.5.dp, Color(0xFFF3DC9B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFF1CF), Color(0xFFFFE0E0))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(elevation = 8.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedAvatar, fontSize = 54.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = name.ifBlank { if (AppStrings.isEnglish) "Name" else "Nombre" },
                            fontFamily = TitleFont(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProfInkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$selectedAge ${AppStrings.yearsOld}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfInkMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pedagogical badge
                        val stageText = when {
                            selectedAge <= 5 -> if (AppStrings.isEnglish)
                                "🌱 Early Steps · Gentle stories & sweet rhymes"
                            else
                                "🌱 Primeros Pasos · Cuentos dulces y rimas"
                            selectedAge <= 7 -> if (AppStrings.isEnglish)
                                "🌟 Little Creator · Guided quests & creativity"
                            else
                                "🌟 Pequeño Creador · Retos guiados y magia"
                            else -> if (AppStrings.isEnglish)
                                "🚀 Great Explorer · Clever plots & rich quests"
                            else
                                "🚀 Gran Explorador · Tramas ricas e ingenio"
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, ProfMintBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stageText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfMint,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 2. AGE SELECTOR
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AppStrings.howOldAreYouTitle,
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInkDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 5, 6, 7, 8, 9, 10).forEach { age ->
                            val isSelected = age == selectedAge
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) ProfCoral else Color.White,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected) ProfCoral else ProfCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(14.dp),
                                        focusedScale = 1.12f,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            selectedAge = age
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$age",
                                        fontFamily = TitleFont(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ProfInkDark
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. CONFIRM CTA BUTTON [ ✓ ¡Listo! ]
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
                        .height(50.dp)
                        .tvFocusable(
                            shape = RoundedCornerShape(50.dp),
                            focusedScale = 1.05f,
                            onClick = {
                                val defaultName = if (AppStrings.isEnglish) "Little Hero" else "Pequeño Creador"
                                val finalName = if (name.isNotBlank()) name.trim() else defaultName
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onProfileConfirmed(finalName, selectedAge, selectedAvatar)
                            }
                        )
                ) {
                    Text(
                        text = "✓ " + AppStrings.allSetCta,
                        fontFamily = TitleFont(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // =========================================================
            // RIGHT COLUMN (Avatars + Name field + Alphabetical Keyboard)
            // =========================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // SECTION 1: CHOOSE AVATAR (Single Row of 8)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AppStrings.chooseYourAvatarTitle,
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInkDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        availableAvatars.forEach { avatar ->
                            val isSelected = avatar == selectedAvatar
                            val isFirstAvatar = avatar == availableAvatars.first()
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) ProfCoralSoft else Color.White,
                                border = BorderStroke(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) ProfCoral else ProfCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .tvFocusable(
                                        shape = CircleShape,
                                        focusedScale = 1.15f,
                                        focusRequester = if (isFirstAvatar) avatarFocusRequester else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            selectedAvatar = avatar
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = avatar, fontSize = 28.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(ProfCoral),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✓",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: NAME DISPLAY FIELD + KEYBOARD
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AppStrings.whatIsYourNameTitle,
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInkDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Box with Coral border and cursor
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(2.5.dp, ProfCoral),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = AppStrings.childNameMockupPlaceholder,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ProfInkMuted.copy(alpha = 0.5f)
                                )
                            } else {
                                Text(
                                    text = name,
                                    fontFamily = TitleFont(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfInkDark
                                )
                            }
                            Text(
                                text = " |",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfCoral
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ON-SCREEN KEYBOARD (4 ROWS)
                    val kbRow1 = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
                    val kbRow2 = listOf("J", "K", "L", "M", "N", "Ñ", "O", "P", "Q")
                    val kbRow3 = listOf("R", "S", "T", "U", "V", "W", "X", "Y", "Z")

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Row 1
                        KeyboardRow(
                            keys = kbRow1,
                            onKeyClick = { letter ->
                                if (name.length < 14) {
                                    name = if (name.isEmpty() || name.endsWith(" ")) {
                                        name + letter.uppercase()
                                    } else {
                                        name + letter.lowercase()
                                    }
                                }
                            }
                        )

                        // Row 2
                        KeyboardRow(
                            keys = kbRow2,
                            onKeyClick = { letter ->
                                if (name.length < 14) {
                                    name = if (name.isEmpty() || name.endsWith(" ")) {
                                        name + letter.uppercase()
                                    } else {
                                        name + letter.lowercase()
                                    }
                                }
                            }
                        )

                        // Row 3
                        KeyboardRow(
                            keys = kbRow3,
                            onKeyClick = { letter ->
                                if (name.length < 14) {
                                    name = if (name.isEmpty() || name.endsWith(" ")) {
                                        name + letter.uppercase()
                                    } else {
                                        name + letter.lowercase()
                                    }
                                }
                            }
                        )

                        // Row 4: Actions (⌫ Borrar, Espacio, Limpiar)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ⌫ Borrar (weight 3f)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, ProfCardBorder),
                                modifier = Modifier
                                    .weight(3f)
                                    .height(42.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.08f,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            if (name.isNotEmpty()) {
                                                name = name.dropLast(1)
                                            }
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "⌫ " + if (AppStrings.isEnglish) "Delete" else "Borrar",
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfInkDark
                                    )
                                }
                            }

                            // Espacio (weight 4f)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, ProfCardBorder),
                                modifier = Modifier
                                    .weight(4f)
                                    .height(42.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.08f,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            if (name.isNotEmpty() && !name.endsWith(" ") && name.length < 14) {
                                                name = "$name "
                                            }
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (AppStrings.isEnglish) "Space" else "Espacio",
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfInkDark
                                    )
                                }
                            }

                            // Limpiar (weight 2f)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, ProfCardBorder),
                                modifier = Modifier
                                    .weight(2f)
                                    .height(42.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.08f,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            name = ""
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (AppStrings.isEnglish) "Clear" else "Limpiar",
                                        fontFamily = TitleFont(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfCoral
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { key ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, ProfCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .tvFocusable(
                        shape = RoundedCornerShape(12.dp),
                        focusedScale = 1.15f,
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onKeyClick(key)
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = key,
                        fontFamily = TitleFont(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProfInkDark
                    )
                }
            }
        }
    }
}
