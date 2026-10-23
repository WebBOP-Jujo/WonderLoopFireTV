package org.wonderloop.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wonderloop.app.platform.PlatformStorage

object ProfileRepository {
    val profiles = mutableStateListOf<ChildProfile>()
    var activeProfile by mutableStateOf<ChildProfile?>(null)
        private set

    private val json = Json { ignoreUnknownKeys = true }
    private var isInitialized = false

    private const val KEY_PROFILES = "child_profiles"
    private const val KEY_ACTIVE_PROFILE_ID = "active_child_profile_id"
    private const val KEY_STARTER_UNCLAIMED = "starter_profile_unclaimed"

    fun init() {
        if (isInitialized) return
        isInitialized = true

        try {
            val profilesRaw = PlatformStorage.getString(KEY_PROFILES, "")
            if (profilesRaw.isNotBlank()) {
                val list = json.decodeFromString<List<ChildProfile>>(profilesRaw)
                profiles.clear()
                profiles.addAll(list)
                println("👤 [ProfileRepository] Loaded ${profiles.size} profiles from storage")
            } else {
                // Legacy Migration: check if legacy profile keys exist
                val legacyName = PlatformStorage.getString("child_name", "")
                val legacyAge = PlatformStorage.getInt("child_age", 6)
                val legacyAvatar = PlatformStorage.getString("child_avatar", "🧒")

                if (legacyName.isNotBlank()) {
                    val defaultProfile = ChildProfile(
                        id = "profile_default",
                        name = legacyName,
                        age = if (legacyAge in 4..10) legacyAge else 6,
                        avatar = legacyAvatar.ifBlank { "🧒" },
                        createdAt = PlatformStorage.currentTimeMillis()
                    )
                    profiles.clear()
                    profiles.add(defaultProfile)
                    persistProfiles()
                    println("👤 [ProfileRepository] Migrated legacy profile '${defaultProfile.name}' (id: ${defaultProfile.id})")
                }
            }

            // If still empty (first run), provide default starter profile for instant TV experience
            if (profiles.isEmpty()) {
                val defaultProfile = ChildProfile(
                    id = "profile_default",
                    name = "Leo",
                    age = 6,
                    avatar = "🧒",
                    createdAt = PlatformStorage.currentTimeMillis()
                )
                profiles.add(defaultProfile)
                persistProfiles()
                // Placeholder only: the first profile the family creates takes it over (see addProfile)
                PlatformStorage.saveBoolean(KEY_STARTER_UNCLAIMED, true)
                println("👤 [ProfileRepository] Created default profile '${defaultProfile.name}' (id: ${defaultProfile.id})")
            }

            // Restore active profile
            val savedActiveId = PlatformStorage.getString(KEY_ACTIVE_PROFILE_ID, "")
            val found = profiles.find { it.id == savedActiveId } ?: profiles.firstOrNull()
            if (found != null) {
                setActiveProfile(found.id)
            }
        } catch (e: Throwable) {
            println("⚠️ [ProfileRepository] Error initializing profiles: ${e.message}")
        }
    }

    fun setActiveProfile(profileId: String): ChildProfile? {
        val found = profiles.find { it.id == profileId } ?: return null
        activeProfile = found
        PlatformStorage.saveString(KEY_ACTIVE_PROFILE_ID, found.id)

        // Keep legacy keys updated for maximum compatibility across older callers
        PlatformStorage.saveString("child_name", found.name)
        PlatformStorage.saveInt("child_age", found.age)
        PlatformStorage.saveString("child_avatar", found.avatar)

        org.wonderloop.app.theme.AppStrings.syncWithActiveProfile(found.preferredLanguage)

        println("🌟 [ProfileRepository] Active profile set to: ${found.name} (${found.age} yrs, id: ${found.id}, lang: ${getEffectiveLanguage()})")
        return found
    }

    /** True while the only profile is the "Leo" placeholder from the first run, not one the family created. */
    fun isStarterUnclaimed(): Boolean =
        profiles.size == 1 && PlatformStorage.getBoolean(KEY_STARTER_UNCLAIMED, false)

    fun addProfile(name: String, age: Int, avatar: String): ChildProfile {
        // The first real profile replaces the placeholder instead of appearing next to an empty "Leo"
        if (isStarterUnclaimed()) {
            val starter = profiles.first()
            updateProfile(starter.id, name, age, avatar)
            setActiveProfile(starter.id)
            return profiles.first()
        }
        val cleanName = name.trim().ifBlank { "Pequeño Autor" }
        val cleanAge = age.coerceIn(4, 10)
        val cleanAvatar = avatar.ifBlank { "🧒" }
        val id = "profile_${PlatformStorage.currentTimeMillis()}"

        val newProfile = ChildProfile(
            id = id,
            name = cleanName,
            age = cleanAge,
            avatar = cleanAvatar,
            createdAt = PlatformStorage.currentTimeMillis()
        )
        profiles.add(newProfile)
        persistProfiles()
        setActiveProfile(id)
        println("✨ [ProfileRepository] Added new profile: ${newProfile.name} (${newProfile.age} yrs)")
        return newProfile
    }

    fun defaultVoiceStyleForAge(age: Int): String = when {
        age <= 6 -> "soft"
        age <= 8 -> "normal"
        else -> "lively"
    }

    fun updateProfile(id: String, name: String, age: Int, avatar: String) {
        val index = profiles.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = profiles[index]
            val cleanAge = age.coerceIn(4, 10)
            val artBracketChanged = (old.age <= 6) != (cleanAge <= 6)
            val voiceBracketChanged = defaultVoiceStyleForAge(old.age) != defaultVoiceStyleForAge(cleanAge)
            val updated = old.copy(
                name = name.trim().ifBlank { old.name },
                age = cleanAge,
                avatar = avatar.ifBlank { old.avatar },
                preferredArtStyle = if (artBracketChanged) null else old.preferredArtStyle,
                preferredVoiceStyle = if (voiceBracketChanged) null else old.preferredVoiceStyle
            )
            profiles[index] = updated
            persistProfiles()
            // Once named by the family, the starter is a real profile
            PlatformStorage.saveBoolean(KEY_STARTER_UNCLAIMED, false)
            if (activeProfile?.id == id) {
                setActiveProfile(id)
            }
            println("✏️ [ProfileRepository] Updated profile ${updated.name} (id: $id)")
        }
    }

    fun getEffectiveArtStyle(fallbackAge: Int = 6): String {
        val saved = activeProfile?.preferredArtStyle
        if (saved == "watercolor" || saved == "3d_animation") {
            return saved
        }
        val age = activeProfile?.age ?: fallbackAge
        return if (age <= 6) "watercolor" else "3d_animation"
    }

    fun setPreferredArtStyle(artStyle: String) {
        val normalized = if (artStyle == "3d_animation") "3d_animation" else "watercolor"
        val current = activeProfile ?: return
        val index = profiles.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val updated = profiles[index].copy(preferredArtStyle = normalized)
            profiles[index] = updated
            activeProfile = updated
            persistProfiles()
            println("🎨 [ProfileRepository] Saved preferred art style '$normalized' for ${updated.name}")
        }
    }

    fun getEffectiveVoiceStyle(fallbackAge: Int = 6): String {
        val saved = activeProfile?.preferredVoiceStyle
        if (saved == "soft" || saved == "normal" || saved == "lively") {
            return saved
        }
        val age = activeProfile?.age ?: fallbackAge
        return defaultVoiceStyleForAge(age)
    }

    fun setPreferredVoiceStyle(voiceStyle: String) {
        val normalized = when (voiceStyle) {
            "soft", "normal", "lively" -> voiceStyle
            else -> "normal"
        }
        val current = activeProfile ?: return
        val index = profiles.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val updated = profiles[index].copy(preferredVoiceStyle = normalized)
            profiles[index] = updated
            activeProfile = updated
            persistProfiles()
            println("🎙️ [ProfileRepository] Saved preferred voice style '$normalized' for ${updated.name}")
        }
    }

    fun getEffectiveLanguage(): String {
        val saved = activeProfile?.preferredLanguage
        if (saved == "es" || saved == "en") {
            return saved
        }
        return PlatformStorage.getDeviceLanguageCode()
    }

    fun setPreferredLanguage(language: String) {
        val normalized = if (language == "en") "en" else "es"
        val current = activeProfile ?: return
        val index = profiles.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val updated = profiles[index].copy(preferredLanguage = normalized)
            profiles[index] = updated
            activeProfile = updated
            persistProfiles()
            org.wonderloop.app.theme.AppStrings.syncWithActiveProfile(normalized)
            println("🌍 [ProfileRepository] Saved preferred story language '$normalized' for ${updated.name}")
        }
    }

    fun deleteProfile(id: String): Boolean {
        if (profiles.size <= 1) {
            println("⚠️ [ProfileRepository] Cannot delete the only remaining profile!")
            return false
        }
        val index = profiles.indexOfFirst { it.id == id }
        if (index != -1) {
            val deletedName = profiles[index].name
            profiles.removeAt(index)
            persistProfiles()

            // If we deleted the active profile, switch to the first remaining profile
            if (activeProfile?.id == id) {
                profiles.firstOrNull()?.let { setActiveProfile(it.id) }
            }

            // Cascade delete stories belonging to this profile
            StoryRepository.deleteStoriesForProfile(id)
            println("🗑️ [ProfileRepository] Deleted profile '$deletedName' and its associated stories")
            return true
        }
        return false
    }

    fun findProfile(id: String): ChildProfile? = profiles.find { it.id == id }

    fun ensureActiveProfile(): ChildProfile {
        if (activeProfile == null) {
            if (profiles.isNotEmpty()) {
                setActiveProfile(profiles.first().id)
            } else {
                addProfile(name = "Leo", age = 6, avatar = "🧒")
            }
        }
        return activeProfile!!
    }

    private fun persistProfiles() {
        try {
            val raw = json.encodeToString(profiles.toList())
            PlatformStorage.saveString(KEY_PROFILES, raw)
        } catch (e: Throwable) {
            println("⚠️ [ProfileRepository] Error persisting profiles: ${e.message}")
        }
    }
}
