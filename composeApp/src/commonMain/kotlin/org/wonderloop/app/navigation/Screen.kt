package org.wonderloop.app.navigation

import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.StoryContinuationContext

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    data class ProfilePicker(val canGoBack: Boolean = false) : Screen()
    data class Profile(val profileId: String? = null, val isNewProfile: Boolean = false) : Screen()
    object ChildProfile : Screen()
    object ParentsZone : Screen()
    object Home : Screen()
    object MyStories : Screen()
    object MyCharacters : Screen()
    data class CameraScan(val continuation: StoryContinuationContext? = null, val scanSessionKey: Long = 0L, val existingSessionId: String? = null) : Screen()
    data class MagicLoading(val imageBase64: String, val continuation: StoryContinuationContext? = null, val artStyle: String = "watercolor", val sessionId: String? = null) : Screen()
    data class HeroConfirm(val character: CharacterProfile, val continuation: StoryContinuationContext? = null, val artStyle: String = "watercolor", val sessionId: String? = null) : Screen()
    data class StoryGenerating(val character: CharacterProfile, val language: String = "es", val continuation: StoryContinuationContext? = null, val artStyle: String = "watercolor") : Screen()
    data class StoryReader(val story: FullStoryResponse) : Screen()
    object Paywall : Screen()
}
