package org.wonderloop.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.wonderloop.app.platform.PlatformStorage

class BackendOfflineException(message: String) : Exception(message)

class WonderLoopApi(
    // Default to LAN IP or localhost
    var baseUrl: String = "http://localhost:3000"
) {
    var appUserId: String = getOrCreateAppUserId()
    private val appSecretKey: String = "change-me-to-a-long-random-secret"

    var isOnline by mutableStateOf<Boolean?>(null)
        private set

    fun resetOnlineStateForCheck() {
        isOnline = null
    }

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 180000
            connectTimeoutMillis = 3000
            socketTimeoutMillis = 180000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    private fun CharacterProfile.withResolvedDataUris(): CharacterProfile {
        val resolvedReinterpreted = PlatformStorage.resolveDataUriIfNeeded(reinterpretedImageUrl)
        val hasReinterpreted = !resolvedReinterpreted.isNullOrBlank()
        return copy(
            reinterpretedImageUrl = resolvedReinterpreted,
            // Backend only uses originalDrawingBase64 if reinterpretedImageUrl is absent
            originalDrawingBase64 = if (hasReinterpreted) null else PlatformStorage.resolveDataUriIfNeeded(originalDrawingBase64)
        )
    }

    private fun StoryContinuationContext.withResolvedDataUris(): StoryContinuationContext {
        val heroSource = originalHero ?: previousCharacter
        val resolvedHero = heroSource.withResolvedDataUris()
        // Avoid sending the same Base64 portrait twice in both previousCharacter and originalHero
        val lightweightPrev = previousCharacter.copy(
            reinterpretedImageUrl = null,
            originalDrawingBase64 = null
        )
        return copy(
            previousCharacter = lightweightPrev,
            originalHero = resolvedHero
        )
    }

    private fun getOrCreateAppUserId(): String {
        val existing = PlatformStorage.getString("app_user_id", "")
        if (existing.isNotBlank()) return existing
        val newId = "wl_anon_" + (1..16).map { (('a'..'z') + ('0'..'9')).random() }.joinToString("")
        PlatformStorage.saveString("app_user_id", newId)
        return newId
    }

    suspend fun checkHealth(): Boolean {
        val savedRemote = PlatformStorage.getString("remote_backend_url", "")
        val candidates = buildList {
            if (savedRemote.isNotBlank()) add(savedRemote.trimEnd('/'))
            if (baseUrl.isNotBlank()) add(baseUrl.trimEnd('/'))
            add("http://localhost:3000")
            add("http://192.168.0.35:3000")
            add("http://10.0.2.2:3000")
        }.distinct()

        for (candidate in candidates) {
            try {
                val response = client.get("$candidate/api/story/health") {
                    timeout {
                        connectTimeoutMillis = 1200
                        requestTimeoutMillis = 1800
                    }
                }
                if (response.status.value == 200) {
                    baseUrl = candidate
                    isOnline = true
                    println("✅ [WonderLoopApi] Connected to backend at $baseUrl (User: $appUserId)")
                    return true
                }
            } catch (e: Exception) {
                println("⚠️ [WonderLoopApi] Candidate $candidate unreachable: ${e.message}")
            }
        }

        isOnline = false
        return false
    }

    suspend fun checkQuota(userId: String = appUserId): QuotaResponse {
        return client.get("$baseUrl/api/revenuecat/quota/$userId") {
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", userId)
        }.body()
    }

    suspend fun analyzeDrawing(
        imageBase64: String,
        mimeType: String = "image/jpeg",
        artStyle: String = "watercolor"
    ): AnalyzeDrawingResponse {
        if (isOnline != true) {
            val reconnected = checkHealth()
            if (!reconnected) {
                throw BackendOfflineException("Backend unreachable across all candidate URLs")
            }
        }
        val response: AnalyzeDrawingResponse = client.post("$baseUrl/api/story/analyze-drawing") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            setBody(AnalyzeDrawingRequest(imageBase64, mimeType, artStyle))
        }.body()
        if (response.success) {
            isOnline = true
        }
        return response
    }

    suspend fun generateStory(
        character: CharacterProfile,
        childName: String,
        childAge: Int,
        theme: String? = null,
        language: String = "es",
        continuation: StoryContinuationContext? = null,
        voiceStyle: String = org.wonderloop.app.platform.PlatformAudioService.getVoiceStyle(),
        artStyle: String = "watercolor"
    ): StoryGenerateResponse {
        if (isOnline != true) {
            val reconnected = checkHealth()
            if (!reconnected) {
                throw BackendOfflineException("Backend unreachable across all candidate URLs")
            }
        }
        val resolvedChar = character.withResolvedDataUris()
        val resolvedCont = continuation?.withResolvedDataUris()
        val response: StoryGenerateResponse = client.post("$baseUrl/api/story/generate") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            setBody(StoryGenerateRequest(resolvedChar, childName, childAge, theme, language, resolvedCont, voiceStyle, artStyle))
        }.body()
        if (response.success) {
            isOnline = true
        }

        // Preserve local file:// paths for this chapter's drawn character (hero in Ch 1, new companion in Ch 2+)
        val mergedCharacter = response.story.character.copy(
            reinterpretedImageUrl = character.reinterpretedImageUrl ?: response.story.character.reinterpretedImageUrl,
            originalDrawingBase64 = character.originalDrawingBase64 ?: response.story.character.originalDrawingBase64
        )
        // Also preserve local file:// paths for the saga founder hero inside continuation
        val localSagaHero = continuation?.originalHero ?: continuation?.previousCharacter
        val mergedCont = response.story.continuation?.let { respCont ->
            if (localSagaHero != null) {
                val mergedSagaHero = respCont.effectiveHero.copy(
                    reinterpretedImageUrl = localSagaHero.reinterpretedImageUrl ?: respCont.effectiveHero.reinterpretedImageUrl,
                    originalDrawingBase64 = localSagaHero.originalDrawingBase64 ?: respCont.effectiveHero.originalDrawingBase64
                )
                respCont.copy(
                    previousCharacter = mergedSagaHero,
                    originalHero = mergedSagaHero
                )
            } else {
                respCont
            }
        }
        return response.copy(
            story = response.story.copy(
                character = mergedCharacter,
                continuation = mergedCont
            )
        )
    }

    suspend fun redeemPromoCode(userId: String = appUserId, code: String): RedeemCodeResponse {
        return client.post("$baseUrl/api/subscription/redeem") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", userId)
            setBody(RedeemCodeRequest(userId, code))
        }.body()
    }
}
