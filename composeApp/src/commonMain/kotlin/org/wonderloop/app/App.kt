package org.wonderloop.app

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wonderloop.app.ui.components.tvFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.ProfileRepository
import org.wonderloop.app.data.StoryContinuationContext
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.navigation.Screen
import org.wonderloop.app.platform.PlatformBackHandler
import org.wonderloop.app.platform.PlatformNotificationService
import org.wonderloop.app.platform.PlatformStorage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.*

object AppLifecycleObserver {
    var resumeCount by mutableStateOf(0)
        private set

    fun onAppResumed() {
        resumeCount++
    }
}

@Composable
fun App(
    onExitApp: () -> Unit = {}
) {
    WonderLoopTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
            val api = remember { WonderLoopApi() }

            // Navigation stack for smooth back handling and transitions
            var screenStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Splash)) }
            val currentScreen = screenStack.lastOrNull() ?: Screen.Home
            var isBackNav by remember { mutableStateOf(false) }

            fun navigateTo(screen: Screen) {
                isBackNav = false
                screenStack = screenStack + screen
            }

            fun navigateReplace(screen: Screen) {
                isBackNav = false
                screenStack = listOf(screen)
            }

            var childName by remember { mutableStateOf("Leo") }
            var childAge by remember { mutableStateOf(6) }
            var childAvatar by remember { mutableStateOf("🧒") }
            val backendOnline = api.isOnline
            var isPremium by remember { mutableStateOf(false) }
            var showParentalGate by remember { mutableStateOf(false) }
            var parentalGateExplanation by remember { mutableStateOf<String?>(null) }
            var parentalGateOnSuccess by remember { mutableStateOf<(() -> Unit)?>(null) }
            var showDailyLimitDialog by remember { mutableStateOf(false) }
            var dailyLimitHeroName by remember { mutableStateOf<String?>(null) }

            fun tryStartAdventure(sagaHeroName: String? = null, onProceed: () -> Unit) {
                if (!SubscriptionManager.canCreateStory()) {
                    parentalGateExplanation = AppStrings.quotaExhaustedPrompt
                    parentalGateOnSuccess = { navigateTo(Screen.Paywall) }
                    showParentalGate = true
                } else if (SubscriptionManager.hasReachedDailyLimit()) {
                    dailyLimitHeroName = sagaHeroName
                    showDailyLimitDialog = true
                } else {
                    onProceed()
                }
            }

            fun navigateBack() {
                if (showDailyLimitDialog) {
                    showDailyLimitDialog = false
                    dailyLimitHeroName = null
                    return
                }
                if (showParentalGate) {
                    showParentalGate = false
                    parentalGateExplanation = null
                    parentalGateOnSuccess = null
                    return
                }
                // Fire TV Guideline: BACK from root destinations must exit the app to TV launcher directly without a confirmation dialog
                if (currentScreen is Screen.Home || currentScreen is Screen.Splash || (currentScreen is Screen.ProfilePicker && !currentScreen.canGoBack) || screenStack.size <= 1) {
                    onExitApp()
                } else {
                    isBackNav = true
                    screenStack = screenStack.dropLast(1)
                }
            }

            // Global Android Back Button Interception
            PlatformBackHandler(enabled = true) {
                navigateBack()
            }

            // Load saved state and check backend health on launch without blocking UI animations
            LaunchedEffect(Unit) {
                val startTime = PlatformStorage.currentTimeMillis()

                // Warm up disk storage on background thread so Main UI thread never stalls
                withContext(Dispatchers.Default) {
                    PlatformStorage.getString("app_language", "")
                    PlatformStorage.getString("child_profiles", "")
                    PlatformStorage.getString("active_child_profile_id", "")
                    PlatformStorage.getBoolean("onboarding_completed", false)
                    PlatformStorage.getBoolean("is_premium", false)
                }

                ProfileRepository.init()
                AppStrings.init()
                SubscriptionManager.init()

                val active = ProfileRepository.activeProfile
                if (active != null) {
                    childName = active.name
                    childAge = active.age
                    childAvatar = active.avatar
                }
                isPremium = SubscriptionManager.isPremium

                // Decode saved stories asynchronously on background thread so SplashScreen animates from frame 1
                val storiesJob = launch {
                    StoryRepository.initAsync()
                }

                launch {
                    api.checkHealth()
                }

                val elapsed = PlatformStorage.currentTimeMillis() - startTime
                val remainingDelay = (2200L - elapsed).coerceAtLeast(1000L)
                delay(remainingDelay)
                storiesJob.join()

                val onboardingDone = PlatformStorage.getBoolean("onboarding_completed", false)
                if (!onboardingDone) {
                    screenStack = listOf(Screen.Onboarding)
                } else if (ProfileRepository.profiles.size >= 2) {
                    screenStack = listOf(Screen.ProfilePicker(canGoBack = false))
                } else {
                    screenStack = listOf(Screen.Home)
                }
            }

            // Re-verify backend connectivity automatically when returning from background
            val resumeCount = AppLifecycleObserver.resumeCount
            LaunchedEffect(resumeCount) {
                if (resumeCount > 1) {
                    withContext(Dispatchers.Default) {
                        api.checkHealth()
                    }
                }
            }

            // Re-verify backend connectivity when entering Home or while offline on Home
            LaunchedEffect(currentScreen, backendOnline) {
                if (currentScreen is Screen.Home) {
                    if (backendOnline == false) {
                        withContext(Dispatchers.Default) {
                            api.checkHealth()
                        }
                    }
                    while (api.isOnline == false) {
                        delay(5000L)
                        withContext(Dispatchers.Default) {
                            api.checkHealth()
                        }
                    }
                }
            }

            // Parental Gate Dialog for accessing financial/paywall areas
            if (showParentalGate) {
                ParentalGateDialog(
                    explanation = parentalGateExplanation,
                    onDismiss = {
                        showParentalGate = false
                        parentalGateExplanation = null
                        parentalGateOnSuccess = null
                    },
                    onSuccess = {
                        showParentalGate = false
                        parentalGateExplanation = null
                        val action = parentalGateOnSuccess ?: { navigateTo(Screen.Paywall) }
                        parentalGateOnSuccess = null
                        action()
                    }
                )
            }

            // Daily Creation Limit Reached Dialog (Child-friendly Paper & Rest Time)
            if (showDailyLimitDialog) {
                val limitVal = SubscriptionManager.dailyLimit.value
                val createdToday = SubscriptionManager.getStoriesCreatedToday()
                val heroName = dailyLimitHeroName
                Dialog(
                    onDismissRequest = {
                        showDailyLimitDialog = false
                        dailyLimitHeroName = null
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    val dailyLimitFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        try { dailyLimitFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFEDE8E1)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                        modifier = Modifier.width(480.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF1CF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🌙🖍️", fontSize = 30.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFFE4F6F0),
                                border = BorderStroke(1.dp, Color(0xFFB8E6D6))
                            ) {
                                Text(
                                    text = if (AppStrings.isEnglish)
                                        "✨ $createdToday OF $limitVal STORIES CREATED TODAY"
                                    else
                                        "✨ $createdToday DE $limitVal CUENTOS CREADOS HOY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0E7F64),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (heroName != null) {
                                    if (AppStrings.isEnglish) "Take your time drawing!" else "¡Tómate tu tiempo para dibujar!"
                                } else {
                                    if (AppStrings.isEnglish) "The magic rests until tomorrow!" else "¡La magia descansa hasta mañana!"
                                },
                                fontFamily = TitleFont(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2B2340),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (heroName != null) {
                                    if (AppStrings.isEnglish)
                                        "You've already created your $limitVal adventures for today! Grab paper and crayons to help $heroName calmly, and come back tomorrow to show us your drawing!"
                                    else
                                        "¡Hoy ya has creado tus $limitVal aventuras del día! Coge papel y pinturas para ayudar a $heroName sin prisa, ¡y vuelve mañana para enseñarnos tu dibujo!"
                                } else {
                                    if (AppStrings.isEnglish)
                                        "You've already created your $limitVal adventures for today! Grab paper and crayons to draw calmly. Come back tomorrow and show us your next hero!"
                                    else
                                        "¡Hoy ya has creado tus $limitVal aventuras del día! Prepara tus lápices y dibuja sin prisa en papel. ¡Vuelve mañana y muéstranos a tu próximo héroe!"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6B6480),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFFD9384A),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(dailyLimitFocusRequester)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = {
                                            showDailyLimitDialog = false
                                            dailyLimitHeroName = null
                                            if (currentScreen !is Screen.Home) {
                                                isBackNav = true
                                                screenStack = listOf(Screen.Home)
                                            }
                                        }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (AppStrings.isEnglish) "Okay, see you tomorrow! ✨" else "¡Vale, hasta mañana! ✨",
                                        fontFamily = TitleFont(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = RoundedCornerShape(50.dp),
                                        focusedScale = 1.05f,
                                        onClick = {
                                            showDailyLimitDialog = false
                                            dailyLimitHeroName = null
                                            parentalGateExplanation = null
                                            parentalGateOnSuccess = { navigateTo(Screen.ParentsZone) }
                                            showParentalGate = true
                                        }
                                    )
                            ) {
                                Text(
                                    text = if (AppStrings.isEnglish) "🔒 Parents Zone (adjust daily limit)" else "🔒 Zona de padres (ajustar límite)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B6480),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Animated Screen Transition
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isTargetModal = targetState is Screen.Profile || targetState is Screen.CameraScan || targetState is Screen.Paywall || targetState is Screen.MyStories || targetState is Screen.MyCharacters || targetState is Screen.ProfilePicker || targetState is Screen.ChildProfile || targetState is Screen.ParentsZone
                    val isInitialModal = initialState is Screen.Profile || initialState is Screen.CameraScan || initialState is Screen.Paywall || initialState is Screen.MyStories || initialState is Screen.MyCharacters || initialState is Screen.ProfilePicker || initialState is Screen.ChildProfile || initialState is Screen.ParentsZone

                    if (isTargetModal && !isBackNav) {
                        // Modal Enter: Bottom-up elevation with smooth spring/scale
                        (slideInVertically(
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                            initialOffsetY = { fullHeight -> (fullHeight * 0.25f).toInt() }
                        ) + fadeIn(animationSpec = tween(280)) + scaleIn(
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                            initialScale = 0.94f
                        )).togetherWith(
                            fadeOut(animationSpec = tween(220)) + scaleOut(
                                animationSpec = tween(280),
                                targetScale = 0.96f
                            )
                        )
                    } else if (isInitialModal && isBackNav) {
                        // Modal Exit: Graceful sink back down
                        (fadeIn(animationSpec = tween(220)) + scaleIn(
                            animationSpec = tween(280),
                            initialScale = 0.96f
                        )).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetY = { fullHeight -> (fullHeight * 0.25f).toInt() }
                            ) + fadeOut(animationSpec = tween(220)) + scaleOut(
                                animationSpec = tween(280),
                                targetScale = 0.94f
                            )
                        )
                    } else if (isBackNav) {
                        // Backward Pop: Previous enters from left (-x), current exits to right (+x)
                        (slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                        ) + fadeIn(animationSpec = tween(260))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                            ) + fadeOut(animationSpec = tween(220))
                        )
                    } else {
                        // Forward Push: Target enters from right (+x), previous exits to left (-x)
                        (slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + fadeIn(animationSpec = tween(260))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                            ) + fadeOut(animationSpec = tween(220))
                        )
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.Splash -> {
                        SplashScreen()
                    }

                    is Screen.Onboarding -> {
                        OnboardingScreen(
                            onFinish = {
                                PlatformStorage.saveBoolean("onboarding_completed", true)
                                val previousScreen = screenStack.dropLast(1).lastOrNull()
                                if (previousScreen is Screen.ParentsZone) {
                                    navigateBack()
                                } else if (ProfileRepository.profiles.isEmpty()) {
                                    navigateReplace(Screen.Profile(isNewProfile = true))
                                } else if (ProfileRepository.profiles.size >= 2) {
                                    navigateReplace(Screen.ProfilePicker(canGoBack = false))
                                } else {
                                    navigateReplace(Screen.Home)
                                }
                            }
                        )
                    }

                    is Screen.ProfilePicker -> {
                        ProfilePickerScreen(
                            canGoBack = screen.canGoBack,
                            onSelectProfile = { profile ->
                                ProfileRepository.setActiveProfile(profile.id)
                                childName = profile.name
                                childAge = profile.age
                                childAvatar = profile.avatar
                                navigateReplace(Screen.Home)
                            },
                            onAddNewProfileClick = {
                                parentalGateExplanation = AppStrings.gateForAuthorSettings
                                parentalGateOnSuccess = {
                                    navigateTo(Screen.Profile(isNewProfile = true))
                                }
                                showParentalGate = true
                            },
                            onParentsClick = {
                                parentalGateExplanation = null
                                parentalGateOnSuccess = {
                                    navigateTo(Screen.ParentsZone)
                                }
                                showParentalGate = true
                            },
                            onBackClick = { navigateBack() }
                        )
                    }

                    is Screen.ChildProfile -> {
                        ChildProfileScreen(
                            childName = childName,
                            childAge = childAge,
                            childAvatar = childAvatar,
                            onAvatarChanged = { newAvatar ->
                                val active = ProfileRepository.activeProfile
                                if (active != null) {
                                    ProfileRepository.updateProfile(active.id, active.name, active.age, newAvatar)
                                }
                                childAvatar = newAvatar
                            },
                            onNavigateToStories = { navigateTo(Screen.MyStories) },
                            onNavigateToCharacters = { navigateTo(Screen.MyCharacters) },
                            onBackClick = { navigateBack() }
                        )
                    }

                    is Screen.ParentsZone -> {
                        ParentsZoneScreen(
                            isPremium = isPremium,
                            onBackClick = { navigateBack() },
                            onManageSubscriptionClick = { navigateTo(Screen.Paywall) },
                            onEditProfileClick = { profileId ->
                                navigateTo(Screen.Profile(profileId = profileId, isNewProfile = false))
                            },
                            onAddProfileClick = {
                                navigateTo(Screen.Profile(isNewProfile = true))
                            },
                            onReplayOnboardingClick = {
                                navigateTo(Screen.Onboarding)
                            }
                        )
                    }

                    is Screen.Profile -> {
                        val active = if (screen.profileId != null) ProfileRepository.findProfile(screen.profileId) else ProfileRepository.activeProfile
                        val activeId = active?.id

                        ProfileScreen(
                            initialName = if (screen.isNewProfile) "" else (active?.name ?: childName),
                            initialAge = if (screen.isNewProfile) 8 else (active?.age ?: childAge),
                            initialAvatar = if (screen.isNewProfile) "👦" else (active?.avatar ?: childAvatar),
                            profileId = screen.profileId ?: activeId,
                            isNewProfile = screen.isNewProfile,
                            canDeleteProfile = ProfileRepository.profiles.size > 1 && !screen.isNewProfile,
                            onBackClick = { navigateBack() },
                            onDeleteProfileClick = {
                                val targetId = screen.profileId ?: ProfileRepository.activeProfile?.id
                                if (targetId != null && ProfileRepository.profiles.size > 1) {
                                    ProfileRepository.deleteProfile(targetId)
                                    val remaining = ProfileRepository.activeProfile
                                    if (remaining != null) {
                                        childName = remaining.name
                                        childAge = remaining.age
                                        childAvatar = remaining.avatar
                                    }
                                    val previousScreen = screenStack.dropLast(1).lastOrNull()
                                    if (previousScreen is Screen.ParentsZone) {
                                        navigateBack()
                                    } else if (ProfileRepository.profiles.size >= 2) {
                                        navigateReplace(Screen.ProfilePicker(canGoBack = false))
                                    } else {
                                        navigateReplace(Screen.Home)
                                    }
                                }
                            },
                            onProfileConfirmed = { name, age, avatar ->
                                if (screen.isNewProfile) {
                                    val newProfile = ProfileRepository.addProfile(name, age, avatar)
                                    ProfileRepository.setActiveProfile(newProfile.id)
                                    childName = newProfile.name
                                    childAge = newProfile.age
                                    childAvatar = newProfile.avatar
                                } else {
                                    val editId = screen.profileId ?: ProfileRepository.activeProfile?.id
                                    if (editId != null) {
                                        ProfileRepository.updateProfile(editId, name, age, avatar)
                                        val currentActive = ProfileRepository.activeProfile
                                        if (currentActive != null) {
                                            childName = currentActive.name
                                            childAge = currentActive.age
                                            childAvatar = currentActive.avatar
                                        }
                                    } else {
                                        val added = ProfileRepository.addProfile(name, age, avatar)
                                        ProfileRepository.setActiveProfile(added.id)
                                        childName = added.name
                                        childAge = added.age
                                        childAvatar = added.avatar
                                    }
                                }
                                PlatformStorage.saveBoolean("onboarding_completed", true)
                                val previousScreen = screenStack.dropLast(1).lastOrNull()
                                if (previousScreen is Screen.ParentsZone || previousScreen is Screen.Home) {
                                    navigateBack()
                                } else {
                                    navigateReplace(Screen.Home)
                                }
                            }
                        )
                    }

                    is Screen.Home -> {
                        val coroutineScope = rememberCoroutineScope()
                        HomeScreen(
                            childName = childName,
                            childAvatar = childAvatar,
                            backendOnline = backendOnline,
                            onSwitchProfileClick = {
                                navigateTo(Screen.ProfilePicker(canGoBack = true))
                            },
                            onNewAdventureClick = {
                                tryStartAdventure {
                                    navigateTo(Screen.CameraScan(scanSessionKey = PlatformStorage.currentTimeMillis()))
                                }
                            },
                            onEditProfileClick = {
                                navigateTo(Screen.ChildProfile)
                            },
                            onParentsClick = {
                                parentalGateExplanation = null
                                parentalGateOnSuccess = { navigateTo(Screen.ParentsZone) }
                                showParentalGate = true
                            },
                            onNavigateToStories = { navigateTo(Screen.MyStories) },
                            onNavigateToCharacters = { navigateTo(Screen.MyCharacters) },
                            onRetryConnection = {
                                coroutineScope.launch {
                                    api.resetOnlineStateForCheck()
                                    withContext(Dispatchers.Default) {
                                        api.checkHealth()
                                    }
                                }
                            },
                            onOpenStory = { story ->
                                navigateTo(Screen.StoryReader(story))
                            }
                        )
                    }

                    is Screen.MyStories -> {
                        MyStoriesScreen(
                            onBackClick = { navigateBack() },
                            onOpenStory = { story ->
                                navigateTo(Screen.StoryReader(story))
                            },
                            onNewAdventureClick = {
                                tryStartAdventure {
                                    navigateTo(Screen.CameraScan(scanSessionKey = PlatformStorage.currentTimeMillis()))
                                }
                            }
                        )
                    }

                    is Screen.MyCharacters -> {
                        MyCharactersScreen(
                            onBackClick = { navigateBack() },
                            onOpenStory = { story ->
                                navigateTo(Screen.StoryReader(story))
                            },
                            onNewAdventureClick = {
                                tryStartAdventure {
                                    navigateTo(Screen.CameraScan(scanSessionKey = PlatformStorage.currentTimeMillis()))
                                }
                            }
                        )
                    }

                    is Screen.CameraScan -> {
                        val initialArtStyle = screen.continuation?.artStyle
                            ?: ProfileRepository.getEffectiveArtStyle(childAge)
                        QrScanScreen(
                            initialArtStyle = initialArtStyle,
                            continuation = screen.continuation,
                            scanSessionKey = screen.scanSessionKey,
                            existingSessionId = screen.existingSessionId,
                            api = api,
                            onPhotoCaptured = { base64, chosenArtStyle, sessionId ->
                                if (!SubscriptionManager.canCreateStory()) {
                                    parentalGateExplanation = AppStrings.quotaExhaustedPrompt
                                    parentalGateOnSuccess = { navigateTo(Screen.Paywall) }
                                    showParentalGate = true
                                } else {
                                    ProfileRepository.setPreferredArtStyle(chosenArtStyle)
                                    val updatedContinuation = screen.continuation?.copy(artStyle = chosenArtStyle)
                                    navigateTo(Screen.MagicLoading(base64, updatedContinuation, chosenArtStyle, sessionId))
                                }
                            },
                            onBack = { navigateBack() }
                        )
                    }

                    is Screen.MagicLoading -> {
                        MagicLoadingScreen(
                            imageBase64 = screen.imageBase64,
                            api = api,
                            artStyle = screen.artStyle,
                            onAnalysisComplete = { hero ->
                                isBackNav = false
                                screenStack = screenStack.dropLast(1) + Screen.HeroConfirm(
                                    hero,
                                    screen.continuation,
                                    screen.artStyle,
                                    screen.sessionId
                                )
                            },
                            onError = {
                                navigateBack()
                            }
                        )
                    }

                    is Screen.HeroConfirm -> {
                        HeroConfirmScreen(
                            character = screen.character,
                            childName = childName,
                            continuation = screen.continuation,
                            sessionId = screen.sessionId,
                            api = api,
                            onStartStoryClick = { language ->
                                if (!SubscriptionManager.canCreateStory()) {
                                    parentalGateExplanation = AppStrings.quotaExhaustedPrompt
                                    parentalGateOnSuccess = { navigateTo(Screen.Paywall) }
                                    showParentalGate = true
                                } else {
                                    PlatformNotificationService.tagHeroCreated(
                                        heroName = screen.character.name,
                                        heroTrait = screen.character.personality
                                    )
                                    isBackNav = false
                                    screenStack = screenStack.dropLast(1) + Screen.StoryGenerating(screen.character, language, screen.continuation, screen.artStyle)
                                }
                            },
                            onNewPhotoReceived = { newBase64 ->
                                isBackNav = false
                                screenStack = screenStack.dropLast(1) + Screen.MagicLoading(
                                    newBase64,
                                    screen.continuation,
                                    screen.artStyle,
                                    screen.sessionId
                                )
                            },
                            onRescanClick = {
                                screenStack = screenStack.dropLast(1).filterNot { it is Screen.CameraScan } + Screen.CameraScan(
                                    continuation = screen.continuation,
                                    scanSessionKey = PlatformStorage.currentTimeMillis(),
                                    existingSessionId = screen.sessionId
                                )
                            },
                            onBackToHomeClick = {
                                navigateReplace(Screen.Home)
                            }
                        )
                    }

                    is Screen.StoryGenerating -> {
                        StoryGeneratingScreen(
                            character = screen.character,
                            childName = childName,
                            childAge = childAge,
                            api = api,
                            language = screen.language,
                            continuation = screen.continuation,
                            artStyle = screen.artStyle,
                            onStoryGenerated = { story ->
                                SubscriptionManager.consumeStoryCredit()
                                val savedStory = StoryRepository.saveStory(story)
                                screenStack = listOf(Screen.Home, Screen.MyStories, Screen.StoryReader(savedStory))
                            },
                            onError = {
                                navigateReplace(Screen.Home)
                            }
                        )
                    }

                    is Screen.StoryReader -> {
                        StoryReaderScreen(
                            story = screen.story,
                            onScanNextDrawing = { storyToContinue ->
                                val sagaOriginalHero = storyToContinue.sagaHero
                                tryStartAdventure(sagaHeroName = sagaOriginalHero.name) {
                                    StoryRepository.saveStory(storyToContinue)
                                    PlatformNotificationService.clearPendingChallenge()
                                    val inheritedArtStyle = storyToContinue.artStyle
                                        ?: storyToContinue.continuation?.artStyle
                                        ?: ProfileRepository.getEffectiveArtStyle(childAge)
                                    val continuationContext = StoryContinuationContext(
                                        sagaId = storyToContinue.effectiveSagaId,
                                        previousStoryId = storyToContinue.storyId,
                                        previousStoryTitle = storyToContinue.title,
                                        previousCharacter = sagaOriginalHero,
                                        originalHero = sagaOriginalHero,
                                        challengePrompt = storyToContinue.creativeChallenge.prompt,
                                        lastSceneText = storyToContinue.scenes.lastOrNull()?.textContent,
                                        chapterIndex = (storyToContinue.chapterIndex ?: 1) + 1,
                                        previousCompanionNames = buildList {
                                            storyToContinue.continuation?.previousCompanionNames?.let { addAll(it) }
                                            if (storyToContinue.continuation != null &&
                                                storyToContinue.character.name.isNotBlank() &&
                                                !storyToContinue.character.name.equals(sagaOriginalHero.name, ignoreCase = true)
                                            ) {
                                                if (storyToContinue.character.name !in this) add(storyToContinue.character.name)
                                            }
                                        },
                                        artStyle = inheritedArtStyle
                                    )
                                    navigateTo(Screen.CameraScan(continuationContext, scanSessionKey = PlatformStorage.currentTimeMillis()))
                                }
                            },
                            onCloseClick = {
                                if (screenStack.size > 1) {
                                    navigateBack()
                                } else {
                                    navigateReplace(Screen.Home)
                                }
                            },
                            onGoToBookshelf = {
                                PlatformNotificationService.tagPendingChallenge(
                                    heroName = screen.story.sagaHero.name,
                                    challengeType = screen.story.creativeChallenge.targetSubject
                                )
                                screenStack = listOf(Screen.Home, Screen.MyStories)
                            }
                        )
                    }

                    is Screen.Paywall -> {
                        PaywallScreen(
                            api = api,
                            userId = childName,
                            onPurchaseCompleted = {
                                isPremium = SubscriptionManager.isPremium
                                navigateBack()
                            },
                            onCloseClick = {
                                navigateBack()
                            }
                        )
                    }
                }
            }
            }
        }
    }
}
