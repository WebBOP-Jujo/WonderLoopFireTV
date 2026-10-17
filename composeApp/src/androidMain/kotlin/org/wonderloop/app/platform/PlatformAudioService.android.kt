package org.wonderloop.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

actual object PlatformAudioService {
    private var appContext: Context? = null
    private var tts: TextToSpeech? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isInitialized = false
    @Volatile
    private var isTtsSpeaking = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var onAudioCompletedListener: (() -> Unit)? = null

    actual fun setOnAudioCompletedListener(listener: (() -> Unit)?) {
        onAudioCompletedListener = listener
    }

    private val staticTracks = java.util.concurrent.ConcurrentHashMap<SoundEffect, AudioTrack>()
    private val sfxExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "WonderLoop-SfxWorker").apply { isDaemon = true }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts != null) return
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("es", "ES"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback to default Spanish or device language
                        tts?.setLanguage(Locale("es"))
                    }
                    tts?.setPitch(1.05f) // Slightly warmer, friendlier pitch for children
                    tts?.setSpeechRate(0.92f) // Slightly slower pacing for comfortable storybook reading
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isTtsSpeaking = true
                        }
                        override fun onDone(utteranceId: String?) {
                            isTtsSpeaking = false
                            mainHandler.post { onAudioCompletedListener?.invoke() }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isTtsSpeaking = false
                            mainHandler.post { onAudioCompletedListener?.invoke() }
                        }
                    })
                    isInitialized = true
                    println("🔊 [PlatformAudioService] Native Spanish Text-to-Speech initialized successfully")
                } else {
                    println("⚠️ [PlatformAudioService] TTS initialization failed with status: $status")
                }
            }

            // Warm up static SFX AudioTracks in background worker to eliminate all runtime thread churn
            sfxExecutor.execute {
                try {
                    val sampleRate = 22050
                    for (effect in SoundEffect.values()) {
                        val pcmData = when (effect) {
                            SoundEffect.POP -> generatePopPcm(sampleRate)
                            SoundEffect.MAGIC_CHIME -> generateMagicChimePcm(sampleRate)
                            SoundEffect.PAGE_TURN -> generatePageTurnPcm(sampleRate)
                            SoundEffect.CELEBRATION -> generateCelebrationPcm(sampleRate)
                        }
                        val track = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(pcmData.size * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()
                        track.write(pcmData, 0, pcmData.size)
                        staticTracks[effect] = track
                    }
                    android.util.Log.d("WonderLoopAudio", "✨ Pre-cached ${staticTracks.size} zero-allocation static SFX tracks")
                } catch (e: Throwable) {
                    android.util.Log.w("WonderLoopAudio", "Could not pre-cache static SFX tracks: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("⚠️ [PlatformAudioService] Error initializing TTS: ${e.message}")
        }
    }

    actual fun playStoryAudio(text: String, audioBase64: String?) {
        stop() // Stop any current speech or audio playback

        if (!audioBase64.isNullOrBlank()) {
            val success = playBase64Audio(audioBase64)
            if (success) {
                println("🔊 [PlatformAudioService] Playing Gemini AI generated voice")
                return
            }
        }

        // Fallback to Android native TTS
        println("🔊 [PlatformAudioService] Using Android native TTS fallback")
        speak(text)
    }

    private fun playBase64Audio(base64Data: String): Boolean {
        return try {
            if (base64Data.startsWith("file://") || base64Data.startsWith("/")) {
                val filePath = base64Data.removePrefix("file://")
                val audioFile = java.io.File(filePath)
                if (!audioFile.exists() || audioFile.length() == 0L) return false

                mediaPlayer?.release()
                java.io.FileInputStream(audioFile).use { fis ->
                    mediaPlayer = android.media.MediaPlayer().apply {
                        setDataSource(fis.fd)
                        prepare()
                        setOnCompletionListener {
                            mainHandler.post { onAudioCompletedListener?.invoke() }
                        }
                        start()
                    }
                }
                return true
            }

            val cleanBase64 = base64Data.substringAfter("base64,").trim()
            val audioBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            if (audioBytes.isEmpty()) return false

            val context = appContext ?: return false
            val ext = if (base64Data.startsWith("data:audio/mp3", ignoreCase = true) ||
                base64Data.startsWith("data:audio/mpeg", ignoreCase = true)
            ) ".mp3" else ".wav"
            val tempFile = java.io.File.createTempFile("gemini_narration_", ext, context.cacheDir)
            tempFile.deleteOnExit()
            java.io.FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer?.release()
            java.io.FileInputStream(tempFile).use { fis ->
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(fis.fd)
                    prepare()
                    setOnCompletionListener {
                        try {
                            tempFile.delete()
                        } catch (_: Exception) {}
                        mainHandler.post { onAudioCompletedListener?.invoke() }
                    }
                    start()
                }
            }
            true
        } catch (e: Throwable) {
            println("⚠️ [PlatformAudioService] Failed to play remote audio: ${e.message}")
            false
        }
    }

    private fun applyVoiceStyle(style: String) {
        when (style) {
            "soft" -> {
                tts?.setPitch(1.02f)
                tts?.setSpeechRate(0.85f)
            }
            "lively" -> {
                tts?.setPitch(1.12f)
                tts?.setSpeechRate(1.04f)
            }
            else -> {
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(0.95f)
            }
        }
    }

    actual fun isSfxEnabled(): Boolean {
        return PlatformStorage.getBoolean("sfx_enabled", true)
    }

    actual fun setSfxEnabled(enabled: Boolean) {
        PlatformStorage.saveBoolean("sfx_enabled", enabled)
    }

    actual fun getVoiceStyle(): String {
        return org.wonderloop.app.data.ProfileRepository.getEffectiveVoiceStyle()
    }

    actual fun setVoiceStyle(style: String) {
        org.wonderloop.app.data.ProfileRepository.setPreferredVoiceStyle(style)
        PlatformStorage.saveString("voice_style", style)
        applyVoiceStyle(style)
    }

    actual fun speak(text: String) {
        if (!isInitialized || tts == null) {
            println("⚠️ [PlatformAudioService] Native TTS engine is not initialized or unavailable on this Fire OS device")
            isTtsSpeaking = false
            mainHandler.post { onAudioCompletedListener?.invoke() }
            return
        }
        try {
            stop() // Interrupt any current speech
            val targetLocale = if (org.wonderloop.app.theme.AppStrings.isEnglish) Locale.US else Locale("es", "ES")
            tts?.language = targetLocale
            applyVoiceStyle(getVoiceStyle())
            val res = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "story_page_narration")
            if (res != TextToSpeech.SUCCESS) {
                println("⚠️ [PlatformAudioService] TTS speak call returned non-success code ($res)")
                isTtsSpeaking = false
                mainHandler.post { onAudioCompletedListener?.invoke() }
            } else {
                println("🔊 [PlatformAudioService] Speaking native text (${text.length} chars) in ${targetLocale.language}")
            }
        } catch (e: Exception) {
            println("⚠️ [PlatformAudioService] Error during speak: ${e.message}")
            isTtsSpeaking = false
            mainHandler.post { onAudioCompletedListener?.invoke() }
        }
    }

    actual fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            println("⚠️ [PlatformAudioService] Error stopping MediaPlayer: ${e.message}")
        }
        try {
            tts?.stop()
        } catch (e: Exception) {
            println("⚠️ [PlatformAudioService] Error during stop: ${e.message}")
        }
    }

    actual fun isSpeaking(): Boolean {
        return try {
            (mediaPlayer?.isPlaying == true) || isTtsSpeaking || (tts?.isSpeaking == true)
        } catch (_: Exception) {
            false
        }
    }

    actual fun playSfx(effect: SoundEffect) {
        if (!isSfxEnabled()) return
        val staticTrack = staticTracks[effect]
        if (staticTrack != null) {
            sfxExecutor.execute {
                try {
                    synchronized(staticTrack) {
                        staticTrack.stop()
                        staticTrack.reloadStaticData()
                        staticTrack.play()
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("WonderLoopAudio", "Error replaying static SFX $effect: ${e.message}")
                }
            }
        } else {
            // Immediate background fallback if called before pre-cache completes
            sfxExecutor.execute {
                try {
                    val sampleRate = 22050
                    val pcmData: ShortArray = when (effect) {
                        SoundEffect.POP -> generatePopPcm(sampleRate)
                        SoundEffect.MAGIC_CHIME -> generateMagicChimePcm(sampleRate)
                        SoundEffect.PAGE_TURN -> generatePageTurnPcm(sampleRate)
                        SoundEffect.CELEBRATION -> generateCelebrationPcm(sampleRate)
                    }
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcmData.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(pcmData, 0, pcmData.size)
                    staticTracks[effect] = track
                    track.play()
                } catch (e: Throwable) {
                    android.util.Log.e("WonderLoopAudio", "Fallback error playing SFX $effect: ${e.message}")
                }
            }
        }
    }

    // --- Programmatic SFX Waveform Generators ---

    private fun generatePopPcm(sampleRate: Int): ShortArray {
        val durationMs = 60
        val totalSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            // Downward pitch drop from 850Hz to 200Hz
            val freq = 850.0 - (progress * 650.0)
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = sin(2.0 * PI * freq * t) * envelope * 24000.0
            pcm[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return pcm
    }

    private fun generateMagicChimePcm(sampleRate: Int): ShortArray {
        // Pentatonic magical twinkle arpeggio (C6, E6, G6, B6, C7)
        val durationMs = 420
        val totalSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(totalSamples)
        val freqs = doubleArrayOf(1046.5, 1318.5, 1567.9, 1975.5, 2093.0)
        val noteSpacing = totalSamples / freqs.size

        for (i in 0 until totalSamples) {
            var sum = 0.0
            for (n in freqs.indices) {
                val noteStart = n * noteSpacing
                if (i >= noteStart) {
                    val dt = (i - noteStart).toDouble() / sampleRate
                    val decay = exp(-dt * 9.0)
                    sum += sin(2.0 * PI * freqs[n] * dt) * decay * 5500.0
                }
            }
            pcm[i] = sum.toInt().coerceIn(-32767, 32767).toShort()
        }
        return pcm
    }

    private fun generatePageTurnPcm(sampleRate: Int): ShortArray {
        val durationMs = 130
        val totalSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(totalSamples)
        var lastRandom = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Soft envelope: smooth swell and fade
            val envelope = sin(progress * PI)
            // Low-pass filtered noise to simulate paper rustle
            val whiteNoise = (Math.random() * 2.0 - 1.0)
            lastRandom = (lastRandom * 0.75) + (whiteNoise * 0.25)
            val sample = lastRandom * envelope * 18000.0
            pcm[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return pcm
    }

    private fun generateCelebrationPcm(sampleRate: Int): ShortArray {
        val durationMs = 650
        val totalSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(totalSamples)
        // Two-chord celebratory fanfare: C-E-G to C-G-C
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-progress * 3.5)
            val f1 = if (progress < 0.4) 523.25 else 1046.5 // C5 to C6
            val f2 = if (progress < 0.4) 659.25 else 783.99 // E5 to G5
            val sample = (sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)) * 0.5 * envelope * 24000.0
            pcm[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return pcm
    }

    fun shutdown() {
        stop()
        try {
            staticTracks.values.forEach { track ->
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
            staticTracks.clear()
        } catch (_: Throwable) {}
        try {
            sfxExecutor.shutdown()
        } catch (_: Throwable) {}
        try {
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
