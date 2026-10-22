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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
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
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

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

    val coroutineScope = rememberCoroutineScope()
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

    PlatformBackHandler(enabled = true) {
        if (showLanguageDialog) showLanguageDialog = false
        else if (showVoiceDialog) showVoiceDialog = false
        else if (showArtStyleDialog) showArtStyleDialog = false
        else if (showDailyLimitDialog) showDailyLimitDialog = false
        else if (showPrivacyDialog) showPrivacyDialog = false
        else if (showAboutDialog) showAboutDialog = false
        else if (showConfirmClearStories) showConfirmClearStories = false
        else onBackClick()
    }

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

    val activeProfileFocusRequester = remember { FocusRequester() }
    val addProfileFocusRequester = remember { FocusRequester() }
    val gestionarFocusRequester = remember { FocusRequester() }

    val dailyLimitRowRequester = remember { FocusRequester() }
    val languageRowRequester = remember { FocusRequester() }
    val voiceRowRequester = remember { FocusRequester() }
    val artStyleRowRequester = remember { FocusRequester() }
    val sfxRowRequester = remember { FocusRequester() }
    val replayRowRequester = remember { FocusRequester() }
    val privacyRowRequester = remember { FocusRequester() }
    val aboutRowRequester = remember { FocusRequester() }

    var lastFocusedSetting by remember { mutableStateOf<String?>(null) }

    val anyDialogOpen = showLanguageDialog || showVoiceDialog || showArtStyleDialog ||
        showDailyLimitDialog || showPrivacyDialog || showAboutDialog || showConfirmClearStories

    LaunchedEffect(anyDialogOpen) {
        if (!anyDialogOpen && lastFocusedSetting != null) {
            delay(120)
            try {
                when (lastFocusedSetting) {
                    "dailyLimit" -> dailyLimitRowRequester.requestFocus()
                    "language" -> languageRowRequester.requestFocus()
                    "voice" -> voiceRowRequester.requestFocus()
                    "artStyle" -> artStyleRowRequester.requestFocus()
                    "privacy" -> privacyRowRequester.requestFocus()
                    "about" -> aboutRowRequester.requestFocus()
                    else -> activeProfileFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
            lastFocusedSetting = null
        }
    }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            if (profiles.isNotEmpty()) {
                activeProfileFocusRequester.requestFocus()
            } else {
                addProfileFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    WonderBackdrop(mood = BackdropMood.Calm) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: [ ← Volver ]       Zona de padres        [ Spacer ]
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, ParentsCardBorder),
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
                        color = ParentsInkDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (AppStrings.isEnglish) "Back" else "Volver",
                        fontFamily = TitleFont(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentsInkDark
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = AppStrings.parentsZoneTitle,
                fontFamily = TitleFont(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ParentsInkDark
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(110.dp)) // balances the left back pill
        }

        Spacer(modifier = Modifier.height(14.dp))

        // -------------------------------------------------------------
        // 2. MAIN 2-COLUMN WIDESCREEN LAYOUT (NO SCROLL)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // LEFT COLUMN (Pro Card + Perfiles)
            Column(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2A. PRO CARD
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.5.dp, ParentsGoldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFF1CF), Color(0xFFFFE3A3))
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPremium) "👑" else "🌱",
                                fontSize = 28.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPremium) AppStrings.planProActiveLabel else AppStrings.planFreeActiveLabel,
                                fontFamily = TitleFont(),
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ParentsInkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = when {
                                    availableStories >= 999 -> if (AppStrings.isEnglish) "∞ Unlimited stories" else "∞ Cuentos ilimitados"
                                    AppStrings.isEnglish -> if (availableStories == 1) "1 story available" else "$availableStories stories available"
                                    else -> if (availableStories == 1) "1 cuento disponible" else "$availableStories cuentos disponibles"
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ParentsGoldText
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, ParentsGoldBorder),
                            modifier = Modifier.tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.08f,
                                focusRequester = gestionarFocusRequester,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    onManageSubscriptionClick()
                                }
                            )
                        ) {
                            Text(
                                text = AppStrings.manageButtonLabel,
                                fontFamily = TitleFont(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ParentsCoral,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                // 2B. PERFILES SECTION
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.profilesSectionHeader,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ParentsInkDark
                        )
                        Text(
                            text = if (AppStrings.isEnglish) "${profiles.size} of 3" else "${profiles.size} de 3",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentsInkMuted
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        profiles.forEachIndexed { index, profile ->
                            val isActive = profile.id == activeProfileId
                            val accent = profileAccent(index)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    width = if (isActive) 2.5.dp else 1.5.dp,
                                    color = if (isActive) accent else ParentsCardBorder
                                ),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(24.dp),
                                        focusedScale = 1.06f,
                                        focusRequester = if (isActive) activeProfileFocusRequester else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            onEditProfileClick(profile.id)
                                        }
                                    )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.6f))))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .shadow(elevation = 4.dp, shape = CircleShape)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(3.dp, accent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = profile.avatar, fontSize = 34.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = profile.name,
                                            fontFamily = TitleFont(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ParentsInkDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${profile.age} ${AppStrings.yearsOld}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ParentsInkMuted
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = if (isActive) accent.copy(alpha = 0.14f) else Color(0xFFF4F1EC)
                                        ) {
                                            Text(
                                                text = if (isActive) {
                                                    if (AppStrings.isEnglish) "● Active" else "● Activo"
                                                } else {
                                                    if (AppStrings.isEnglish) "Edit" else "Editar"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isActive) accent else ParentsInkMuted,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Add Profile Card if fewer than 3
                        if (profiles.size < 3) {
                            val freeSlots = 3 - profiles.size
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(24.dp),
                                        focusedScale = 1.06f,
                                        // Translucent slot: a focus shadow would show through as a grey veil
                                        focusedElevation = 0.dp,
                                        focusedContainerColor = Color.White.copy(alpha = 0.85f),
                                        focusRequester = addProfileFocusRequester,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            onAddProfileClick()
                                        }
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .drawBehind {
                                        drawRoundRect(
                                            color = ParentsCoral.copy(alpha = 0.5f),
                                            cornerRadius = CornerRadius(24.dp.toPx()),
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
                                            )
                                        )
                                    }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(ParentsCoralSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ParentsCoral
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (AppStrings.isEnglish) "Add profile" else "Añadir perfil",
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ParentsCoral,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (freeSlots == 1) {
                                            if (AppStrings.isEnglish) "1 free slot" else "1 plaza libre"
                                        } else {
                                            if (AppStrings.isEnglish) "$freeSlots free slots" else "$freeSlots plazas libres"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParentsInkMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 2C. FAMILY ACTIVITY: what the children have made so far
                FamilyActivityCard(modifier = Modifier.fillMaxWidth())
            }

            // RIGHT COLUMN (Ajustes Card)
            Column(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = AppStrings.settingsSectionHeader,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ParentsInkDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ParentsCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        // 1. Límite diario
                        ParentsSettingsRow(
                            iconEmoji = "⏱️",
                            iconBg = Color(0xFFFFF1CF),
                            title = if (AppStrings.isEnglish) "Daily limit" else "Límite diario",
                            valueText = dailyLimitDisplay,
                            focusRequester = dailyLimitRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "dailyLimit"
                                showDailyLimitDialog = true
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 2. Idioma de los cuentos
                        ParentsSettingsRow(
                            iconEmoji = "🌍",
                            iconBg = Color(0xFFDDF5EE),
                            title = AppStrings.storyLanguageRowLabel,
                            valueText = if (AppStrings.isStoryEnglish) "English" else "Español",
                            focusRequester = languageRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "language"
                                showLanguageDialog = true
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 3. Voz de lectura
                        ParentsSettingsRow(
                            iconEmoji = "🎙️",
                            iconBg = Color(0xFFE4EEFF),
                            title = AppStrings.readingVoiceRowLabel,
                            valueText = voiceStyleDisplay,
                            focusRequester = voiceRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "voice"
                                showVoiceDialog = true
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 4. Estilo de ilustración
                        ParentsSettingsRow(
                            iconEmoji = "🎨",
                            iconBg = Color(0xFFFCEAE7),
                            title = if (AppStrings.isEnglish) "Illustration style" else "Estilo de ilustración",
                            valueText = artStyleDisplay,
                            focusRequester = artStyleRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "artStyle"
                                showArtStyleDialog = true
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 5. Efectos de sonido
                        ParentsSettingsRow(
                            iconEmoji = "🔊",
                            iconBg = Color(0xFFE4F6F0),
                            title = AppStrings.soundEffectsRowLabel,
                            valueText = if (sfxEnabled) AppStrings.sfxActiveLabel else AppStrings.sfxMutedLabel,
                            focusRequester = sfxRowRequester,
                            onClick = {
                                val nextState = !sfxEnabled
                                sfxEnabled = nextState
                                PlatformAudioService.setSfxEnabled(nextState)
                                coroutineScope.launch {
                                    delay(50)
                                    try {
                                        sfxRowRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 6. Tutorial de bienvenida
                        ParentsSettingsRow(
                            iconEmoji = "💡",
                            iconBg = Color(0xFFFFF1CF),
                            title = AppStrings.replayTutorialRowLabel,
                            valueText = if (AppStrings.isEnglish) "View" else "Ver",
                            focusRequester = replayRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onReplayOnboardingClick()
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 7. Privacidad y datos
                        ParentsSettingsRow(
                            iconEmoji = "🔒",
                            iconBg = Color(0xFFEDE7F8),
                            title = AppStrings.privacyAndDataRowLabel,
                            valueText = "",
                            focusRequester = privacyRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "privacy"
                                showPrivacyDialog = true
                            }
                        )
                        HorizontalDivider(color = ParentsCardBorder.copy(alpha = 0.6f), thickness = 1.dp)

                        // 8. Ayuda y acerca de
                        ParentsSettingsRow(
                            iconEmoji = "❓",
                            iconBg = Color(0xFFFCEAE7),
                            title = AppStrings.helpAndAboutRowLabel,
                            valueText = "",
                            focusRequester = aboutRowRequester,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                lastFocusedSetting = "about"
                                showAboutDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // -------------------------------------------------------------
        // 3. BOTTOM HINT BAR
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HintChip(useDpadArrows = true, text = if (AppStrings.isEnglish) "Navigate" else "Moverse")
            Spacer(modifier = Modifier.width(28.dp))
            HintChip(label = "OK", text = if (AppStrings.isEnglish) "Open" else "Abrir")
            Spacer(modifier = Modifier.width(28.dp))
            HintChip(label = "↩", text = if (AppStrings.isEnglish) "Back" else "Volver")
        }
    }
    }

    // =============================================================
    // DIALOG 1: Language Picker
    // =============================================================
    if (showLanguageDialog) {
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(480.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌍", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStrings.storyLanguageRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) ParentsCoralSoft else ParentsCreamBg,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected) ParentsCoral else ParentsCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(16.dp),
                                        focusedScale = 1.04f,
                                        focusRequester = if (isSelected || (code == "es" && AppStrings.language != "en")) dialogInitialFocus else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            AppStrings.setAppLanguage(code)
                                            showLanguageDialog = false
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showLanguageDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showLanguageDialog = false
                                }
                            )
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(520.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎙️", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStrings.readingVoiceRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) ParentsCoralSoft else ParentsCreamBg,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected) ParentsCoral else ParentsCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(16.dp),
                                        focusedScale = 1.04f,
                                        focusRequester = if (isSelected || (code == "soft" && voiceStyle !in listOf("normal", "lively"))) dialogInitialFocus else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            voiceStyle = code
                                            PlatformAudioService.setVoiceStyle(code)
                                            showVoiceDialog = false
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontFamily = TitleFont(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showVoiceDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showVoiceDialog = false
                                }
                            )
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(520.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Illustration style" else "Estilo de ilustración",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) ParentsCoralSoft else ParentsCreamBg,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected) ParentsCoral else ParentsCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(16.dp),
                                        focusedScale = 1.04f,
                                        focusRequester = if (isSelected || (code == "watercolor" && artStyle != "3d_animation")) dialogInitialFocus else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            artStyle = code
                                            ProfileRepository.setPreferredArtStyle(code)
                                            showArtStyleDialog = false
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontFamily = TitleFont(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showArtStyleDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showArtStyleDialog = false
                                }
                            )
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(520.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌙", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Daily creation limit" else "Límite diario de creación",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "Encourages calm drawing on paper and healthy screen time. Reading saved stories is always unlimited."
                        else
                            "Fomenta que el peque dibuje con calma en papel y evita el uso excesivo. Leer cuentos guardados es siempre ilimitado.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val limitOptions = listOf(
                        Triple(
                            2,
                            if (AppStrings.isEnglish) "🌙  2 stories per day" else "🌙  2 cuentos al día",
                            if (AppStrings.isEnglish)
                                "Recommended · Create a hero and complete their mission"
                            else
                                "Recomendado · Ideal para crear un héroe y completar su reto"
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) ParentsCoralSoft else ParentsCreamBg,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected) ParentsCoral else ParentsCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(16.dp),
                                        focusedScale = 1.04f,
                                        focusRequester = if (isSelected || (limitVal == 2 && dailyLimit !in listOf(3, 5))) dialogInitialFocus else null,
                                        onClick = {
                                            PlatformAudioService.playSfx(SoundEffect.POP)
                                            SubscriptionManager.setDailyLimit(limitVal)
                                            showDailyLimitDialog = false
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontFamily = TitleFont(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showDailyLimitDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showDailyLimitDialog = false
                                }
                            )
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(520.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsMintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🛡️", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStrings.privacyAndDataRowLabel,
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ParentsMintSoft,
                        border = BorderStroke(1.dp, ParentsMintBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = AppStrings.privacyBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentsMint,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "All child profiles, names, and saved stories are stored locally on this device (Local-First architecture) for maximum family privacy."
                        else
                            "Todos los perfiles, nombres y cuentos guardados se almacenan localmente en este dispositivo (arquitectura Local-First) para máxima privacidad familiar.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    if (StoryRepository.savedStories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                showPrivacyDialog = false
                                showConfirmClearStories = true
                            },
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.5.dp, ParentsCoralBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ParentsCoralSoft.copy(alpha = 0.6f),
                                contentColor = ParentsCoral
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(50.dp),
                                    focusedScale = 1.04f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        showPrivacyDialog = false
                                        showConfirmClearStories = true
                                    }
                                )
                        ) {
                            Text(
                                text = if (AppStrings.isEnglish) "🗑️ Clear saved stories" else "🗑️ Borrar cuentos guardados",
                                fontFamily = TitleFont(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ParentsCoral,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showPrivacyDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentsCoral,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                focusRequester = dialogInitialFocus,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showPrivacyDialog = false
                                }
                            )
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(480.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(ParentsCoralSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗑️", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (AppStrings.isEnglish) "Delete all saved stories?" else "¿Borrar todos los cuentos?",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentsInkDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (AppStrings.isEnglish)
                            "This will remove all stories stored on this device. Profiles will remain intact."
                        else
                            "Se eliminarán todos los cuentos guardados en este dispositivo. Los perfiles de los niños se mantendrán intactos.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
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
                            .height(46.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    val allIds = StoryRepository.savedStories.map { it.storyId }
                                    allIds.forEach { StoryRepository.deleteStory(it) }
                                    showConfirmClearStories = false
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
                            showConfirmClearStories = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, ParentsCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ParentsInkMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                focusRequester = dialogInitialFocus,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showConfirmClearStories = false
                                }
                            )
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
        val dialogInitialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(120)
            try { dialogInitialFocus.requestFocus() } catch (_: Exception) {}
        }

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
                    .width(480.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ParentsGoldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✨", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "WonderLoop",
                        fontFamily = TitleFont(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParentsInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            PlatformAudioService.playSfx(SoundEffect.POP)
                            showAboutDialog = false
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentsCoral,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(50.dp),
                                focusedScale = 1.04f,
                                focusRequester = dialogInitialFocus,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.POP)
                                    showAboutDialog = false
                                }
                            )
                    ) {
                        Text(
                            text = AppStrings.close,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
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
    iconEmoji: String,
    iconBg: Color,
    title: String,
    valueText: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(14.dp),
                focusedScale = 1.02f,
                focusedContainerColor = Color(0xFFFFF8EE),
                focusedElevation = 0.dp,
                focusRequester = focusRequester,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconEmoji, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontFamily = TitleFont(),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = ParentsInkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valueText.isNotBlank()) {
                Text(
                    text = valueText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ParentsInkMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "›",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ParentsInkMuted
            )
        }
    }
}

@Composable
private fun HintChip(
    text: String,
    useDpadArrows: Boolean = false,
    label: String = ""
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ParentsCardBorder)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                if (useDpadArrows) {
                    DPadArrows(color = ParentsInkDark)
                } else {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParentsInkDark
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParentsInkMuted
        )
    }
}

/** Totals across every child profile, so grown-ups see what the family has made at a glance. */
@Composable
private fun FamilyActivityCard(modifier: Modifier = Modifier) {
    val stories = StoryRepository.savedStories
    val storyCount = stories.size
    val friendCount = remember(storyCount) {
        stories
            .flatMap { story -> listOfNotNull(story.character, story.continuation?.effectiveHero) }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
            .size
    }
    val sagaCount = remember(storyCount) {
        stories.groupBy { it.effectiveSagaId }.count { it.value.size > 1 }
    }
    val en = AppStrings.isEnglish

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, ParentsCardBorder),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                text = if (en) "📊 WHAT THE FAMILY HAS CREATED" else "📊 LO QUE HA CREADO LA FAMILIA",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = ParentsInkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    Triple("$storyCount", if (en) "stories" else "cuentos", ParentsCoral),
                    Triple("$friendCount", if (en) "friends" else "amigos", ParentsMint),
                    Triple("$sagaCount", if (en) "sagas" else "sagas", Color(0xFF7C5CE0))
                ).forEach { (value, label, color) ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            fontFamily = TitleFont(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentsInkDark,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
