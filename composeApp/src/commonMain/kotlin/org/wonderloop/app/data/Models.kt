package org.wonderloop.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CharacterProfile(
    val name: String,
    val species: String,
    val primaryColors: List<String> = emptyList(),
    val distinctiveFeatures: List<String> = emptyList(),
    val personality: String = "",
    val originalDrawingBase64: String? = null,
    val reinterpretedImageUrl: String? = null,
    val isValidDrawing: Boolean = true,
    val confidence: Float = 1.0f,
    // Visual identity from the drawing analysis. They must round-trip back to /generate:
    // the story and the 5 illustrations use them to keep the hero's exact colors and silhouette.
    val namePhonetic: String? = null,
    val shapeLanguage: String? = null,
    val mood: String? = null,
    val colorPalette: List<ColorDef> = emptyList(),
    val colorHexes: List<String> = emptyList(),
    val visualDescriptionEn: String? = null
)

@Immutable
@Serializable
data class ColorDef(
    val name: String = "",
    val hex: String = ""
)

@Immutable
@Serializable
data class StoryScene(
    val pageNumber: Int,
    val textContent: String,
    val visualPrompt: String,
    val illustrationUrl: String? = null,
    val audioBase64: String? = null,
    /** Start time (ms) of each whitespace-separated word of textContent in the narration (karaoke highlight). */
    val wordTimingsMs: List<Int> = emptyList()
)

@Immutable
@Serializable
data class CreativeChallenge(
    val prompt: String,
    val targetSubject: String = "friend",
    val encouragement: String = "",
    val audioBase64: String? = null
)

@Immutable
@Serializable
data class StoryContinuationContext(
    val previousStoryId: String,
    val previousStoryTitle: String,
    val previousCharacter: CharacterProfile,
    val challengePrompt: String,
    val lastSceneText: String? = null,
    val chapterIndex: Int = 2,
    val sagaId: String? = null,
    val originalHero: CharacterProfile? = null,
    val previousCompanionNames: List<String> = emptyList(),
    val artStyle: String? = null,
    /** Saga memory: 1-2 sentence summary of every previous chapter, oldest first. */
    val previousChapterSummaries: List<String> = emptyList(),
    /** World of the previous chapter, so the backend can keep or change the setting on purpose. */
    val worldId: String? = null
) {
    val effectiveHero: CharacterProfile get() = originalHero ?: previousCharacter
    val previousHeroName: String get() = effectiveHero.name
}

@Immutable
@Serializable
data class ChildProfile(
    val id: String,
    val name: String,
    val age: Int,
    val avatar: String = "🧒",
    val createdAt: Long = 0L,
    val preferredArtStyle: String? = null,
    val preferredVoiceStyle: String? = null,
    val preferredLanguage: String? = null
)

@Immutable
@Serializable
data class FullStoryResponse(
    val storyId: String,
    val title: String,
    val childName: String,
    val childAge: Int,
    val character: CharacterProfile,
    val scenes: List<StoryScene>,
    val creativeChallenge: CreativeChallenge,
    val createdAt: String,
    val continuation: StoryContinuationContext? = null,
    val chapterIndex: Int? = null,
    val sagaId: String? = null,
    val nextStoryId: String? = null,
    val companionName: String? = null,
    val nextStoryTitle: String? = null,
    val childProfileId: String? = null,
    val artStyle: String? = null,
    val chapterSummary: String? = null,
    val worldId: String? = null
) {
    val effectiveSagaId: String get() = sagaId ?: continuation?.sagaId ?: continuation?.previousStoryId ?: storyId
    val isMultiChapter: Boolean get() = (chapterIndex ?: 1) > 1 || !nextStoryId.isNullOrBlank()
    val effectiveArtStyle: String get() = artStyle ?: continuation?.artStyle ?: "watercolor"
    val sagaHero: CharacterProfile get() = continuation?.originalHero ?: continuation?.previousCharacter ?: character
}

@Immutable
@Serializable
data class QuotaResponse(
    val success: Boolean,
    val hasActiveSubscription: Boolean,
    val entitlements: List<String> = emptyList(),
    val remainingFreeStories: Int = 0,
    val canCreateStory: Boolean = true,
    val activePlan: String? = null
)

@Serializable
data class AnalyzeDrawingRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val artStyle: String = "watercolor",
    val language: String = "es"
)

@Serializable
data class AnalyzeDrawingResponse(
    val success: Boolean,
    val character: CharacterProfile
)

@Serializable
data class StoryGenerateRequest(
    val character: CharacterProfile,
    val childName: String,
    val childAge: Int,
    val theme: String? = null,
    val language: String = "es",
    val continuation: StoryContinuationContext? = null,
    val voiceStyle: String = "soft",
    val artStyle: String = "watercolor"
)

@Serializable
data class StoryGenerateResponse(
    val success: Boolean,
    val story: FullStoryResponse
)

@Serializable
data class RedeemCodeRequest(
    val userId: String,
    val code: String
)

@Serializable
data class RedeemCodeResponse(
    val success: Boolean,
    val message: String,
    val status: QuotaResponse? = null
)

@Serializable
data class CreateTvSessionRequest(
    val language: String = "es"
)

@Immutable
@Serializable
data class TvSessionResponse(
    val success: Boolean = true,
    val sessionId: String,
    val shortCode: String = "",
    val uploadUrl: String,
    val lanIp: String = "",
    val expiresInSeconds: Int = 900
)

@Immutable
@Serializable
data class TvSessionStatusResponse(
    val success: Boolean = true,
    val sessionId: String = "",
    val shortCode: String = "",
    val status: String = "waiting",
    val language: String = "es",
    val imageBase64: String? = null,
    val mimeType: String? = null,
    val expiresInSeconds: Int = 900
)


