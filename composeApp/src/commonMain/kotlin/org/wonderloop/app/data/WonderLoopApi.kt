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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.wonderloop.app.platform.PlatformStorage
import org.wonderloop.app.platform.platformAppSecretKey
import org.wonderloop.app.platform.platformDefaultBackendUrl
import org.wonderloop.app.theme.AppStrings

class BackendOfflineException(message: String) : Exception(message)

class WonderLoopApi(
    // Build-time default: the public HTTPS backend on Amazon EC2 (override with wonderloop.backendUrl)
    var baseUrl: String = platformDefaultBackendUrl()
) {
    var appUserId: String = getOrCreateAppUserId()
    private val appSecretKey: String = platformAppSecretKey()

    var isOnline by mutableStateOf<Boolean?>(null)
        private set

    fun resetOnlineStateForCheck() {
        isOnline = null
    }

    fun updateCustomBackendUrl(newUrl: String) {
        val normalized = newUrl.trim().trimEnd('/').let {
            if (it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")) {
                val isLanAddress = Regex("""^(\d{1,3}(\.\d{1,3}){3}|localhost)(:\d+)?$""").matches(it)
                when {
                    !isLanAddress -> "https://$it" // a hostname (e.g. the public EC2 backend) is served over HTTPS
                    it.contains(":") -> "http://$it"
                    else -> "http://$it:3000"
                }
            } else {
                it
            }
        }
        if (normalized.isNotBlank()) {
            PlatformStorage.saveString("remote_backend_url", normalized)
            baseUrl = normalized
            isOnline = null
        }
    }

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 180000
            connectTimeoutMillis = 10000
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
            add(platformDefaultBackendUrl())
            add("http://10.0.2.2:3000")
            add("http://localhost:3000")
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

    suspend fun analyzeDrawing(
        imageBase64: String,
        mimeType: String = "image/jpeg",
        artStyle: String = "watercolor",
        language: String = ProfileRepository.getEffectiveLanguage()
    ): AnalyzeDrawingResponse {
        if (isOnline != true) {
            val reconnected = checkHealth()
            if (!reconnected) {
                throw BackendOfflineException("Backend unreachable across all candidate URLs")
            }
        }
        val httpResponse = client.post("$baseUrl/api/story/analyze-drawing") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            setBody(AnalyzeDrawingRequest(imageBase64, mimeType, artStyle, language))
        }
        if (!httpResponse.status.isSuccess()) {
            val errBody = try { httpResponse.bodyAsText() } catch (_: Exception) { "" }
            println("❌ [WonderLoopApi] /api/story/analyze-drawing failed (${httpResponse.status.value}): $errBody")
            throw Exception("HTTP ${httpResponse.status.value}: $errBody")
        }
        val response: AnalyzeDrawingResponse = httpResponse.body()
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
        val httpResponse = client.post("$baseUrl/api/story/generate") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            setBody(StoryGenerateRequest(resolvedChar, childName, childAge, theme, language, resolvedCont, voiceStyle, artStyle))
        }
        if (!httpResponse.status.isSuccess()) {
            val errBody = try { httpResponse.bodyAsText() } catch (_: Exception) { "" }
            println("❌ [WonderLoopApi] /api/story/generate failed (${httpResponse.status.value}): $errBody")
            throw Exception("HTTP ${httpResponse.status.value}: $errBody")
        }
        val response: StoryGenerateResponse = httpResponse.body()
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

    /**
     * Fire TV hackathon build: the backend billing/coupon routes are disabled (unlimited mode),
     * so coupons are validated locally by SubscriptionManager.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun redeemPromoCode(userId: String = appUserId, code: String): RedeemCodeResponse {
        val redeemed = SubscriptionManager.redeemCode(code)
        return RedeemCodeResponse(
            success = redeemed,
            message = if (redeemed) AppStrings.vipActivated else AppStrings.couponValidationFailed
        )
    }

    /**
     * Creates a new QR pairing session on the backend for Fire TV -> Mobile drawing upload.
     */
    suspend fun createTvSession(language: String = "es"): TvSessionResponse {
        if (isOnline != true) {
            val reconnected = checkHealth()
            if (!reconnected) {
                throw BackendOfflineException("Backend unreachable across all candidate URLs")
            }
        }
        val response: TvSessionResponse = client.post("$baseUrl/api/tv-session") {
            contentType(ContentType.Application.Json)
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            timeout {
                connectTimeoutMillis = 3000
                requestTimeoutMillis = 6000
            }
            setBody(CreateTvSessionRequest(language = language))
        }.body()
        if (response.success) {
            isOnline = true
        }
        return response
    }

    /**
     * Polls the status of an active QR pairing session every ~1.2s from the Fire TV screen.
     */
    suspend fun pollTvSession(sessionId: String): TvSessionStatusResponse {
        return client.get("$baseUrl/api/tv-session/$sessionId") {
            header("X-WonderLoop-Key", appSecretKey)
            header("X-User-Id", appUserId)
            timeout {
                connectTimeoutMillis = 2500
                requestTimeoutMillis = 6000
            }
        }.body()
    }

    /**
     * Resets an active session back to 'connected' or 'waiting' so a new photo can be uploaded
     * from the mobile phone without recreating the session or closing the mobile browser.
     */
    suspend fun resetTvSession(sessionId: String): TvSessionResponse? {
        return try {
            val response = client.post("$baseUrl/api/tv-session/$sessionId/reset") {
                header("X-WonderLoop-Key", appSecretKey)
                header("X-User-Id", appUserId)
                timeout {
                    connectTimeoutMillis = 2500
                    requestTimeoutMillis = 6000
                }
            }
            if (response.status.value in 200..299) {
                response.body<TvSessionResponse>()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Updates intermediate status of an active session (e.g. 'retry' when drawing is unclear).
     */
    suspend fun updateTvSessionStatus(sessionId: String, status: String): Boolean {
        return try {
            val response = client.post("$baseUrl/api/tv-session/$sessionId/status") {
                contentType(ContentType.Application.Json)
                header("X-WonderLoop-Key", appSecretKey)
                header("X-User-Id", appUserId)
                timeout {
                    connectTimeoutMillis = 2500
                    requestTimeoutMillis = 6000
                }
                setBody(mapOf("status" to status))
            }
            response.status.value in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
