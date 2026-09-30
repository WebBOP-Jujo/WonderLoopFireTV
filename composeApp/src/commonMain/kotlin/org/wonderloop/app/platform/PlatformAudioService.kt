package org.wonderloop.app.platform

enum class SoundEffect {
    POP,
    MAGIC_CHIME,
    PAGE_TURN,
    CELEBRATION
}

expect object PlatformAudioService {
    fun playStoryAudio(text: String, audioBase64: String?)
    fun speak(text: String)
    fun stop()
    fun isSpeaking(): Boolean
    fun playSfx(effect: SoundEffect)
    fun isSfxEnabled(): Boolean
    fun setSfxEnabled(enabled: Boolean)
    fun getVoiceStyle(): String
    fun setVoiceStyle(style: String)
}
