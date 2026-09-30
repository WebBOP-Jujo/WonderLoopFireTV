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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*

private val ParentsCreamBg = Color(0xFFFFFBF5)
private val ParentsInkDark = Color(0xFF2B2340)
private val ParentsInkMuted = Color(0xFF6B6480)
private val ParentsCoral = Color(0xFFD9384A)
private val ParentsCoralSoft = Color(0xFFFDECE9)
private val ParentsCoralBorder = Color(0xFFF9D0CA)
private val ParentsMint = Color(0xFF0E7F64)
private val ParentsMintSoft = Color(0xFFE4F6F0)
private val ParentsMintBorder = Color(0xFFB8E6D6)
private val ParentsGoldSoft = Color(0xFFFFF1CF)
private val ParentsGoldBorder = Color(0xFFF3DC9B)
private val ParentsGoldText = Color(0xFF9E6700)
private val ParentsCardBorder = Color(0xFFEDE8E1)

@Composable
fun ParentsZoneScreen(
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onManageSubscriptionClick: () -> Unit,
    onEditProfileClick: (profileId: String) -> Unit,
    onAddProfileClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit
) {
    val profiles = ProfileRepository.profiles
    val activeProfileId = ProfileRepository.activeProfile?.id
    val availableStories by SubscriptionManager.availableStories
    val dailyLimit by SubscriptionManager.dailyLimit

    var sfxEnabled by remember { mutableStateOf(PlatformAudioService.isSfxEnabled()) }
    var voiceStyle by remember(activeProfileId, ProfileRepository.activeProfile?.age, ProfileRepository.activeProfile?.preferredVoiceStyle) {
        mutableStateOf(ProfileRepository.getEffectiveVoiceStyle())
    }
    var artStyle by remember(activeProfileId, ProfileRepository.activeProfile?.age, ProfileRepository.activeProfile?.preferredArtStyle) {
        mutableStateOf(ProfileRepository.getEffectiveArtStyle())
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showArtStyleDialog by remember { mutableStateOf(false) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showConfirmClearStories by remember { mutableStateOf(false) }

    val voiceStyleDisplay = when (voiceStyle) {
        "soft" -> AppStrings.voiceSoftLabel
        "lively" -> AppStrings.voiceLivelyLabel
        else -> AppStrings.voiceNormalLabel
    }

    val artStyleDisplay = if (artStyle == "3d_animation") {
        "3D"
    } else {
        if (AppStrings.isEnglish) "Watercolor" else "Acuarela"
    }

    val dailyLimitDisplay = if (AppStrings.isEnglish) "$dailyLimit / day" else "$dailyLimit al día"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParentsCreamBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: [ ← ] Zona de padres
        // -------------------------------------------------------------
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
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParentsInkDark
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = AppStrings.parentsZoneTitle,
                fontFamily = TitleFont(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ParentsInkDark
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // -------------------------------------------------------------
        // 2. SUBSCRIPTION BANNER CARD (👑 Plan Pro · activo [Gestionar])
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ParentsGoldSoft),
            border = BorderStroke(1.5.dp, ParentsGoldBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPremium) "👑" else "🌱",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPremium) AppStrings.planProActiveLabel else AppStrings.planFreeActiveLabel,
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkDark,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (AppStrings.isEnglish) {
                            if (availableStories == 1) "1 story credit available" else "$availableStories story credits available"
                        } else {
                            if (availableStories == 1) "1 cuento disponible" else "$availableStories cuentos disponibles"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentsGoldText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onManageSubscriptionClick()
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ParentsGoldBorder),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = AppStrings.manageButtonLabel,
                        fontFamily = TitleFont(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsCoral,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // -------------------------------------------------------------
        // 3. SECTION: PERFILES
        // -------------------------------------------------------------
        Text(
            text = AppStrings.profilesSectionHeader,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ParentsInkMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, ParentsCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfileId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onEditProfileClick(profile.id)
                            }
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ParentsCoralSoft)
                            ) {
                                Text(text = profile.avatar, fontSize = 19.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = profile.name,
                                fontFamily = TitleFont(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ParentsInkDark
                            )

                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(ParentsCoralSoft)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (AppStrings.isEnglish) "Active" else "Activo",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ParentsCoral
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${profile.age} ${AppStrings.yearsOld}  ›",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkMuted
                        )
                    }

                    HorizontalDivider(
                        color = ParentsCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }

                // + Añadir perfil row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            onAddProfileClick()
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.addProfileRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsCoral
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // -------------------------------------------------------------
        // 4. SECTION: AJUSTES
        // -------------------------------------------------------------
        Text(
            text = AppStrings.settingsSectionHeader,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ParentsInkMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, ParentsCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Idioma de los cuentos
                ParentsSettingsRow(
                    title = AppStrings.storyLanguageRowLabel,
                    valueText = "${if (AppStrings.isStoryEnglish) "English" else "Español"}  ›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showLanguageDialog = true
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 2: Voz de lectura
                ParentsSettingsRow(
                    title = AppStrings.readingVoiceRowLabel,
                    valueText = "$voiceStyleDisplay  ›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showVoiceDialog = true
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 2b: Estilo de ilustración
                ParentsSettingsRow(
                    title = if (AppStrings.isEnglish) "Illustration style" else "Estilo de ilustración",
                    valueText = "$artStyleDisplay  ›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showArtStyleDialog = true
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 2c: Límite diario
                ParentsSettingsRow(
                    title = if (AppStrings.isEnglish) "Daily limit" else "Límite diario",
                    valueText = "$dailyLimitDisplay  ›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showDailyLimitDialog = true
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 3: Efectos de sonido (Real Toggle)
                ParentsSettingsRow(
                    title = AppStrings.soundEffectsRowLabel,
                    valueText = "${if (sfxEnabled) AppStrings.sfxActiveLabel else AppStrings.sfxMutedLabel}  ›",
                    onClick = {
                        val nextState = !sfxEnabled
                        sfxEnabled = nextState
                        PlatformAudioService.setSfxEnabled(nextState)
                        if (nextState) {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                        }
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 4: Tutorial de bienvenida
                ParentsSettingsRow(
                    title = AppStrings.replayTutorialRowLabel,
                    valueText = "${if (AppStrings.isEnglish) "View" else "Ver"}  ›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        onReplayOnboardingClick()
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 5: Privacidad y datos
                ParentsSettingsRow(
                    title = AppStrings.privacyAndDataRowLabel,
                    valueText = "›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showPrivacyDialog = true
                    }
                )

                HorizontalDivider(
                    color = ParentsCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                // Row 6: Ayuda y acerca de
                ParentsSettingsRow(
                    title = AppStrings.helpAndAboutRowLabel,
                    valueText = "›",
                    onClick = {
                        PlatformAudioService.playSfx(SoundEffect.POP)
                        showAboutDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // =============================================================
    // DIALOG 1: Language Picker
    // =============================================================
    if (showLanguageDialog) {
        Dialog(
            onDismissRequest = { showLanguageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌍", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = AppStrings.storyLanguageRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = AppStrings.languageSubtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val options = listOf("es" to "🇪🇸  Español", "en" to "🇬🇧  English")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEach { (code, label) ->
                            val isSelected = code == AppStrings.language
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ParentsCoralSoft else ParentsCreamBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) ParentsCoral else ParentsCardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        AppStrings.setAppLanguage(code)
                                        showLanguageDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = TitleFont(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ParentsInkDark
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ParentsCoral
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { showLanguageDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkMuted
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 2: Reading Voice Style Picker
    // =============================================================
    if (showVoiceDialog) {
        Dialog(
            onDismissRequest = { showVoiceDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎙️", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = AppStrings.readingVoiceRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val voiceOptions = listOf(
                        Triple(
                            "soft",
                            "🌙  ${AppStrings.voiceSoftLabel}",
                            if (AppStrings.isEnglish) "Calm, gentle pacing · Default for ages 4–6" else "Ritmo pausado y cálido · Por defecto de 4 a 6 años"
                        ),
                        Triple(
                            "normal",
                            "📖  ${AppStrings.voiceNormalLabel}",
                            if (AppStrings.isEnglish) "Natural storybook narration · Default for ages 7–8" else "Ritmo natural de cuentacuentos · Por defecto de 7 a 8 años"
                        ),
                        Triple(
                            "lively",
                            "☀️  ${AppStrings.voiceLivelyLabel}",
                            if (AppStrings.isEnglish) "Upbeat, energetic voice · Default for ages 9–10" else "Voz más viva y dinámica · Por defecto de 9 a 10 años"
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        voiceOptions.forEach { (code, title, subtitle) ->
                            val isSelected = voiceStyle == code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ParentsCoralSoft else ParentsCreamBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) ParentsCoral else ParentsCardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        voiceStyle = code
                                        PlatformAudioService.setVoiceStyle(code)
                                        showVoiceDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ParentsCoral
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { showVoiceDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkMuted
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 2B: Illustration Art Style Picker
    // =============================================================
    if (showArtStyleDialog) {
        Dialog(
            onDismissRequest = { showArtStyleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Illustration style" else "Estilo de ilustración",
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    val activeName = ProfileRepository.activeProfile?.name
                    if (!activeName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (AppStrings.isEnglish) "Active profile: $activeName" else "Perfil activo: $activeName",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentsInkMuted,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val artStyleOptions = listOf(
                        Triple(
                            "watercolor",
                            if (AppStrings.isEnglish) "🎨  Magical Watercolor" else "🎨  Acuarela Mágica",
                            if (AppStrings.isEnglish)
                                "Hand-painted classic storybook look (default for ages 4–6)"
                            else
                                "Ilustración de cuento clásico pintado a mano (por defecto 4–6 años)"
                        ),
                        Triple(
                            "3d_animation",
                            if (AppStrings.isEnglish) "🎬  3D Animation" else "🎬  Animación 3D",
                            if (AppStrings.isEnglish)
                                "Cinematic 3D animated movie style with volume (default for ages 7–10)"
                            else
                                "Estilo película de animación 3D con volumen (por defecto 7–10 años)"
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        artStyleOptions.forEach { (code, title, subtitle) ->
                            val isSelected = artStyle == code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ParentsCoralSoft else ParentsCreamBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) ParentsCoral else ParentsCardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        artStyle = code
                                        ProfileRepository.setPreferredArtStyle(code)
                                        showArtStyleDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ParentsCoral
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { showArtStyleDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkMuted
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 2C: Daily Creation Limit Picker (2, 3, 5)
    // =============================================================
    if (showDailyLimitDialog) {
        val createdToday = SubscriptionManager.getStoriesCreatedToday()
        Dialog(
            onDismissRequest = { showDailyLimitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌙", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Daily creation limit" else "Límite diario de creación",
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "Encourages calm drawing on paper and healthy screen time. Reading saved stories is always unlimited."
                        else
                            "Fomenta que el peque dibuje con calma en papel y evita el uso excesivo del móvil. Leer cuentos guardados es siempre ilimitado.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val activeName = ProfileRepository.activeProfile?.name ?: ""
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ParentsMintSoft,
                        border = BorderStroke(1.dp, ParentsMintBorder)
                    ) {
                        Text(
                            text = if (AppStrings.isEnglish)
                                "🖍️ Today${if (activeName.isNotBlank()) " ($activeName)" else ""}: $createdToday / $dailyLimit created"
                            else
                                "🖍️ Hoy${if (activeName.isNotBlank()) " ($activeName)" else ""}: $createdToday de $dailyLimit creados",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ParentsMint,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val limitOptions = listOf(
                        Triple(
                            2,
                            if (AppStrings.isEnglish) "🌙  2 stories per day" else "🌙  2 cuentos al día",
                            if (AppStrings.isEnglish)
                                "Recommended · Create a hero and complete their drawing mission"
                            else
                                "Recomendado · Ideal para crear un héroe y completar su reto en papel"
                        ),
                        Triple(
                            3,
                            if (AppStrings.isEnglish) "🌟  3 stories per day" else "🌟  3 cuentos al día",
                            if (AppStrings.isEnglish)
                                "Creative afternoon · Extra space for drawing and exploring"
                            else
                                "Tarde creativa · Para días con más tiempo de dibujo en casa"
                        ),
                        Triple(
                            5,
                            if (AppStrings.isEnglish) "🎨  5 stories per day" else "🎨  5 cuentos al día",
                            if (AppStrings.isEnglish)
                                "Weekends or holidays · Healthy daily maximum"
                            else
                                "Fin de semana o vacaciones · Máximo diario saludable"
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        limitOptions.forEach { (limitVal, title, subtitle) ->
                            val isSelected = dailyLimit == limitVal
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ParentsCoralSoft else ParentsCreamBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) ParentsCoral else ParentsCardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        SubscriptionManager.setDailyLimit(limitVal)
                                        showDailyLimitDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ParentsCoral
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { showDailyLimitDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentsInkMuted
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 3: Privacy & Local Data
    // =============================================================
    if (showPrivacyDialog) {
        Dialog(
            onDismissRequest = { showPrivacyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsMintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🛡️", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = AppStrings.privacyAndDataRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ParentsMintSoft,
                        border = BorderStroke(1.dp, ParentsMintBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = AppStrings.privacyBadge,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentsMint,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "All child profiles, names, and saved stories are stored locally on this device (Local-First architecture) for maximum family privacy."
                        else
                            "Todos los perfiles, nombres y cuentos guardados se almacenan localmente en este dispositivo (arquitectura Local-First) para máxima privacidad familiar.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    if (StoryRepository.savedStories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                showPrivacyDialog = false
                                showConfirmClearStories = true
                            },
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.5.dp, ParentsCoralBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ParentsCoralSoft.copy(alpha = 0.6f),
                                contentColor = ParentsCoral
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "🗑️ Clear saved stories" else "🗑️ Borrar cuentos guardados",
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ParentsCoral,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showPrivacyDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentsCoral,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 3B: Confirm Clear Saved Stories
    // =============================================================
    if (showConfirmClearStories) {
        Dialog(
            onDismissRequest = { showConfirmClearStories = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
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
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗑️", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Delete all saved stories?" else "¿Borrar todos los cuentos?",
                        fontFamily = TitleFont(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "This will remove all stories stored on this phone. Profiles will remain intact."
                        else
                            "Se eliminarán todos los cuentos guardados en este teléfono. Los perfiles de los niños se mantendrán intactos.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            val allIds = StoryRepository.savedStories.map { it.storyId }
                            allIds.forEach { StoryRepository.deleteStory(it) }
                            showConfirmClearStories = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentsCoral,
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
                        onClick = { showConfirmClearStories = false },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
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
                            color = ParentsInkMuted
                        )
                    }
                }
            }
        }
    }

    // =============================================================
    // DIALOG 4: Help & About WonderLoop
    // =============================================================
    if (showAboutDialog) {
        Dialog(
            onDismissRequest = { showAboutDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, ParentsCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✨", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "WonderLoop",
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = ParentsCoralSoft
                    ) {
                        Text(
                            text = AppStrings.appVersionInfo,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ParentsCoral,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStrings.appDescriptionInfo,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAboutDialog = false },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentsCoral,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentsSettingsRow(
    title: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = ParentsInkDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 10.dp)
        )
        Text(
            text = valueText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParentsInkMuted,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End
        )
    }
}
