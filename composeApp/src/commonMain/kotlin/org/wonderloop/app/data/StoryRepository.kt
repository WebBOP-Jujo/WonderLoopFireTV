package org.wonderloop.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wonderloop.app.platform.PlatformStorage

data class SagaBook(
    val rootStory: FullStoryResponse,
    val chapters: List<FullStoryResponse>
) {
    val sagaId: String get() = rootStory.effectiveSagaId
    val chapterCount: Int get() = chapters.size
    val isMultiChapter: Boolean get() = chapters.size > 1
    val latestChapter: FullStoryResponse get() = chapters.lastOrNull() ?: rootStory
}

object StoryRepository {
    val savedStories = mutableStateListOf<FullStoryResponse>()
    var lastReadRevision by mutableIntStateOf(0)
        private set
    private val json = Json { ignoreUnknownKeys = true }
    private var isInitialized = false

    val totalChaptersCount: Int get() = getStoriesForProfile().size
    val totalSagasCount: Int get() = getBookshelfSagas().size

    fun externalizeStoryMedia(story: FullStoryResponse): FullStoryResponse {
        val sid = story.storyId
        fun externalizeCharacterWithPrefix(hero: CharacterProfile, prefix: String): CharacterProfile {
            return hero.copy(
                reinterpretedImageUrl = PlatformStorage.externalizeMediaIfNeeded("${prefix}_hero_portrait.jpg", hero.reinterpretedImageUrl),
                originalDrawingBase64 = PlatformStorage.externalizeMediaIfNeeded("${prefix}_hero_drawing.jpg", hero.originalDrawingBase64)
            )
        }

        val updatedCharacter = externalizeCharacterWithPrefix(story.character, sid)
        val updatedContinuation = story.continuation?.let { cont ->
            val sagaRootId = story.effectiveSagaId
            val rawSagaHero = cont.originalHero ?: cont.previousCharacter
            val updatedSagaHero = if (rawSagaHero.name.equals(updatedCharacter.name, ignoreCase = true) &&
                rawSagaHero.reinterpretedImageUrl == story.character.reinterpretedImageUrl
            ) {
                updatedCharacter
            } else {
                externalizeCharacterWithPrefix(rawSagaHero, sagaRootId)
            }
            cont.copy(
                previousCharacter = updatedSagaHero,
                originalHero = updatedSagaHero
            )
        }
        val updatedScenes = story.scenes.map { scene ->
            val rawAudio = scene.audioBase64
            val audioExt = when {
                rawAudio == null -> "mp3"
                rawAudio.startsWith("data:audio/mp3", ignoreCase = true) ||
                    rawAudio.startsWith("data:audio/mpeg", ignoreCase = true) ||
                    rawAudio.endsWith(".mp3", ignoreCase = true) -> "mp3"
                else -> "wav"
            }
            scene.copy(
                illustrationUrl = PlatformStorage.externalizeMediaIfNeeded("${sid}_img_p${scene.pageNumber}.jpg", scene.illustrationUrl) ?: scene.illustrationUrl,
                audioBase64 = PlatformStorage.externalizeMediaIfNeeded("${sid}_audio_p${scene.pageNumber}.$audioExt", rawAudio)
            )
        }

        return story.copy(
            character = updatedCharacter,
            continuation = updatedContinuation,
            scenes = updatedScenes
        )
    }

    suspend fun initAsync() {
        if (isInitialized) return
        isInitialized = true
        try {
            ProfileRepository.init()
            val decodedList = withContext(Dispatchers.Default) {
                val list = mutableListOf<FullStoryResponse>()
                val idsRaw = PlatformStorage.getString("story_ids", "")
                if (idsRaw.isNotBlank()) {
                    val ids = idsRaw.split(",").filter { it.isNotBlank() }
                    for (id in ids) {
                        try {
                            val storyJson = PlatformStorage.getString("story_$id", "")
                            if (storyJson.isNotBlank()) {
                                val rawStory = json.decodeFromString<FullStoryResponse>(storyJson)
                                val compactStory = externalizeStoryMedia(rawStory)
                                if (compactStory != rawStory || storyJson.length > 50_000) {
                                    persistSingleStory(compactStory)
                                }
                                list.add(compactStory)
                            }
                        } catch (e: Throwable) {
                            println("⚠️ [StoryRepository] Error decoding story $id: ${e.message}")
                        }
                    }
                } else {
                    val legacyRaw = PlatformStorage.getString("saved_stories", "")
                    if (legacyRaw.isNotBlank()) {
                        val legacyList = json.decodeFromString<List<FullStoryResponse>>(legacyRaw)
                        for (rawStory in legacyList) {
                            try {
                                val compactStory = externalizeStoryMedia(rawStory)
                                list.add(compactStory)
                                persistSingleStory(compactStory)
                            } catch (e: Throwable) {
                                println("⚠️ [StoryRepository] Error migrating story ${rawStory.storyId}: ${e.message}")
                            }
                        }
                        val migratedIds = list.joinToString(",") { it.storyId }
                        PlatformStorage.saveString("story_ids", migratedIds)
                        PlatformStorage.saveString("saved_stories", "")
                    }
                }
                list
            }
            savedStories.clear()
            savedStories.addAll(decodedList)
            println("📚 [StoryRepository] Async loaded ${savedStories.size} compact stories")
            autoLinkRetroactiveChapters()
            migrateStoriesToProfiles()
            withContext(Dispatchers.Default) {
                PlatformStorage.purgeLegacyHeavyPrefs()
            }
        } catch (e: Throwable) {
            println("⚠️ [StoryRepository] Error in initAsync: ${e.message}")
        }
    }

    fun init() {
        if (isInitialized) return
        isInitialized = true
        try {
            // Ensure ProfileRepository is initialized first
            ProfileRepository.init()

            // 1. Check for modern index-based storage
            val idsRaw = PlatformStorage.getString("story_ids", "")
            if (idsRaw.isNotBlank()) {
                val ids = idsRaw.split(",").filter { it.isNotBlank() }
                savedStories.clear()
                for (id in ids) {
                    try {
                        val storyJson = PlatformStorage.getString("story_$id", "")
                        if (storyJson.isNotBlank()) {
                            val rawStory = json.decodeFromString<FullStoryResponse>(storyJson)
                            val compactStory = externalizeStoryMedia(rawStory)
                            if (compactStory != rawStory || storyJson.length > 50_000) {
                                persistSingleStory(compactStory)
                            }
                            savedStories.add(compactStory)
                        }
                    } catch (e: Throwable) {
                        println("⚠️ [StoryRepository] Error decoding story $id: ${e.message}")
                    }
                }
                println("📚 [StoryRepository] Loaded ${savedStories.size} compact stories")
                autoLinkRetroactiveChapters()
                migrateStoriesToProfiles()
                PlatformStorage.purgeLegacyHeavyPrefs()
                return
            }

            // 2. Fallback / Migration from legacy monolithic 'saved_stories'
            val legacyRaw = PlatformStorage.getString("saved_stories", "")
            if (legacyRaw.isNotBlank()) {
                val list = json.decodeFromString<List<FullStoryResponse>>(legacyRaw)
                savedStories.clear()
                println("📚 [StoryRepository] Migrating ${list.size} stories from legacy storage to individual files...")

                for (rawStory in list) {
                    try {
                        val compactStory = externalizeStoryMedia(rawStory)
                        savedStories.add(compactStory)
                        persistSingleStory(compactStory)
                    } catch (e: Throwable) {
                        println("⚠️ [StoryRepository] Error migrating story ${rawStory.storyId}: ${e.message}")
                    }
                }
                val migratedIds = savedStories.joinToString(",") { it.storyId }
                PlatformStorage.saveString("story_ids", migratedIds)
                PlatformStorage.saveString("saved_stories", "")
                println("✅ [StoryRepository] Migration to compact file storage completed successfully!")
                autoLinkRetroactiveChapters()
                PlatformStorage.purgeLegacyHeavyPrefs()
            }
        } catch (e: Throwable) {
            println("⚠️ [StoryRepository] Error initializing saved stories: ${e.message}")
        }
    }

    fun autoLinkRetroactiveChapters() {
        var hadLinks = false
        for (story in savedStories.toList()) {
            val prevId = story.continuation?.previousStoryId
            if (!prevId.isNullOrBlank()) {
                val prevIndex = savedStories.indexOfFirst { it.storyId == prevId }
                if (prevIndex != -1) {
                    val prev = savedStories[prevIndex]
                    val parentSagaId = prev.sagaId ?: story.sagaId ?: prev.storyId

                    // Self-heal continuation chapters where character was previously overwritten by sagaHero
                    val childIndex = savedStories.indexOfFirst { it.storyId == story.storyId }
                    if (childIndex != -1) {
                        var currentChild = savedStories[childIndex]
                        var childModified = false
                        val cont = currentChild.continuation
                        val expectedCompanionName = (currentChild.companionName ?: prev.companionName)?.trim()

                        if (cont != null && !expectedCompanionName.isNullOrBlank() &&
                            currentChild.character.name.equals(cont.effectiveHero.name, ignoreCase = true) &&
                            !expectedCompanionName.equals(cont.effectiveHero.name, ignoreCase = true)
                        ) {
                            val existingPortrait = currentChild.character.reinterpretedImageUrl
                            val mediaBaseDir = existingPortrait
                                ?.takeIf { it.startsWith("file://") && it.contains("/") }
                                ?.substringBeforeLast("/")
                            val healedPortrait = if (mediaBaseDir != null) {
                                PlatformStorage.externalizeMediaIfNeeded(
                                    "${currentChild.storyId}_hero_portrait.jpg",
                                    "$mediaBaseDir/preview_latest_portrait.jpg"
                                )?.takeIf { !it.endsWith("preview_latest_portrait.jpg") }
                            } else null
                            val healedDrawing = if (mediaBaseDir != null) {
                                PlatformStorage.externalizeMediaIfNeeded(
                                    "${currentChild.storyId}_hero_drawing.jpg",
                                    "$mediaBaseDir/preview_latest_drawing.jpg"
                                )?.takeIf { !it.endsWith("preview_latest_drawing.jpg") }
                            } else null

                            val founderHero = prev.sagaHero
                            val healedCharacter = currentChild.character.copy(
                                name = expectedCompanionName,
                                species = "",
                                reinterpretedImageUrl = healedPortrait
                                    ?: currentChild.scenes.firstOrNull()?.illustrationUrl
                                    ?: currentChild.character.reinterpretedImageUrl,
                                originalDrawingBase64 = healedDrawing ?: currentChild.character.originalDrawingBase64
                            )
                            val healedCont = cont.copy(
                                previousCharacter = founderHero,
                                originalHero = founderHero
                            )
                            currentChild = currentChild.copy(
                                character = healedCharacter,
                                continuation = healedCont
                            )
                            childModified = true
                            println("🩹 [StoryRepository] Self-healed companion character '$expectedCompanionName' for chapter ${currentChild.storyId}")
                        }

                        if (currentChild.sagaId != parentSagaId) {
                            currentChild = currentChild.copy(sagaId = parentSagaId)
                            childModified = true
                        }

                        if (childModified) {
                            savedStories[childIndex] = currentChild
                            persistSingleStory(currentChild)
                            hadLinks = true
                        }
                    }

                    val latestChild = if (childIndex != -1) savedStories[childIndex] else story
                    val resolvedCompanionName = latestChild.character.name.takeIf {
                        it.isNotBlank() && !it.equals(prev.sagaHero.name, ignoreCase = true)
                    } ?: latestChild.companionName ?: latestChild.character.name

                    if (prev.nextStoryId != story.storyId || prev.companionName != resolvedCompanionName || prev.sagaId != parentSagaId) {
                        val updatedPrev = prev.copy(
                            nextStoryId = story.storyId,
                            companionName = resolvedCompanionName,
                            nextStoryTitle = story.title,
                            sagaId = parentSagaId
                        )
                        savedStories[prevIndex] = updatedPrev
                        persistSingleStory(updatedPrev)
                        hadLinks = true
                    }
                }
            }
        }
        if (hadLinks) {
            println("✨ [StoryRepository] Auto-linked retroactive saga chapters successfully!")
        }
    }

    fun migrateStoriesToProfiles() {
        val defaultProfileId = ProfileRepository.activeProfile?.id ?: "profile_default"
        for (i in savedStories.indices) {
            val s = savedStories[i]
            if (s.childProfileId.isNullOrBlank()) {
                val matchingProfile = ProfileRepository.profiles.find { it.name.equals(s.childName, ignoreCase = true) }
                val assignedId = matchingProfile?.id ?: defaultProfileId
                val updated = s.copy(childProfileId = assignedId)
                savedStories[i] = updated
                persistSingleStory(updated)
                println("📚 [StoryRepository] Associated story '${s.title}' with profile '$assignedId' (${s.childName})")
            }
        }
    }

    fun getStoriesForProfile(profileId: String? = null): List<FullStoryResponse> {
        val targetProfileId = profileId ?: ProfileRepository.activeProfile?.id
        return if (targetProfileId != null) {
            savedStories.filter {
                it.childProfileId == targetProfileId ||
                (it.childProfileId.isNullOrBlank() && it.childName.equals(ProfileRepository.activeProfile?.name, ignoreCase = true))
            }
        } else {
            savedStories.toList()
        }
    }

    fun getBookshelfSagas(profileId: String? = null): List<SagaBook> {
        autoLinkRetroactiveChapters()
        val targetProfileId = profileId ?: ProfileRepository.activeProfile?.id
        val pool = getStoriesForProfile(targetProfileId)

        val processedIds = mutableSetOf<String>()
        val result = mutableListOf<SagaBook>()

        val sortedStories = pool.sortedBy { it.chapterIndex ?: 1 }
        for (story in sortedStories) {
            if (processedIds.contains(story.storyId)) continue
            val chapters = getSagaChapters(story).filter {
                targetProfileId == null ||
                it.childProfileId == targetProfileId ||
                (it.childProfileId.isNullOrBlank() && it.childName.equals(ProfileRepository.activeProfile?.name, ignoreCase = true))
            }
            processedIds.addAll(chapters.map { it.storyId })
            val root = chapters.firstOrNull { (it.chapterIndex ?: 1) == 1 } ?: chapters.firstOrNull() ?: story
            result.add(SagaBook(rootStory = root, chapters = if (chapters.isNotEmpty()) chapters else listOf(story)))
        }

        return result.sortedByDescending { it.latestChapter.createdAt }
    }

    fun getSagaChapters(story: FullStoryResponse): List<FullStoryResponse> {
        val sagaId = story.effectiveSagaId
        val direct = savedStories.filter { it.effectiveSagaId == sagaId }
        if (direct.size > 1) {
            return direct.sortedBy { it.chapterIndex ?: 1 }
        }

        // Bidirectional chain traversal fallback
        val chain = mutableListOf<FullStoryResponse>()
        var curr: FullStoryResponse? = story
        while (curr != null) {
            if (!chain.any { it.storyId == curr?.storyId }) {
                chain.add(0, curr)
            }
            val pId = curr.continuation?.previousStoryId
            curr = if (!pId.isNullOrBlank()) savedStories.find { it.storyId == pId } else null
        }
        curr = story
        while (curr != null) {
            val nId = curr.nextStoryId
            val nextStory = if (!nId.isNullOrBlank()) savedStories.find { it.storyId == nId } else null
            if (nextStory != null && !chain.any { it.storyId == nextStory.storyId }) {
                chain.add(nextStory)
                curr = nextStory
            } else {
                curr = null
            }
        }
        if (chain.size > 1) {
            return chain.sortedBy { it.chapterIndex ?: 1 }
        }
        return listOf(story)
    }

    fun getSagaChapters(sagaId: String): List<FullStoryResponse> {
        val root = savedStories.find { it.storyId == sagaId || it.effectiveSagaId == sagaId }
        return if (root != null) getSagaChapters(root) else savedStories.filter { it.effectiveSagaId == sagaId }.sortedBy { it.chapterIndex ?: 1 }
    }

    fun findStory(storyId: String): FullStoryResponse? {
        return savedStories.find { it.storyId == storyId }
    }

    fun markLastReadStory(storyId: String) {
        if (storyId.isBlank()) return
        val story = findStory(storyId)
        val targetPid = story?.childProfileId?.takeIf { it.isNotBlank() }
            ?: ProfileRepository.activeProfile?.id
            ?: "profile_default"
        PlatformStorage.saveString("last_read_story_$targetPid", storyId)
        lastReadRevision++
    }

    fun getLastReadStoryForProfile(profileId: String? = null): FullStoryResponse? {
        // Read lastReadRevision so Compose recomposes when last read story changes
        val rev = lastReadRevision
        val stories = getStoriesForProfile(profileId)
        if (stories.isEmpty() && rev >= 0) return null
        val targetPid = profileId ?: ProfileRepository.activeProfile?.id ?: "profile_default"
        val savedLastId = PlatformStorage.getString("last_read_story_$targetPid", "")
        if (savedLastId.isNotBlank()) {
            stories.find { it.storyId == savedLastId }?.let { return it }
        }
        return stories.firstOrNull()
    }

    fun saveStory(story: FullStoryResponse): FullStoryResponse {
        val activePid = ProfileRepository.activeProfile?.id ?: "profile_default"
        val profileIdToUse = story.childProfileId?.ifBlank { null } ?: activePid
        val prevId = story.continuation?.previousStoryId
        var storyToSave = externalizeStoryMedia(story.copy(childProfileId = profileIdToUse))
        if (!prevId.isNullOrBlank()) {
            val prevIndex = savedStories.indexOfFirst { it.storyId == prevId }
            if (prevIndex != -1) {
                val prevStory = savedStories[prevIndex]
                val parentSagaId = prevStory.sagaId ?: story.sagaId ?: prevStory.storyId
                val resolvedCompanionName = storyToSave.character.name.takeIf {
                    it.isNotBlank() && !it.equals(prevStory.sagaHero.name, ignoreCase = true)
                } ?: storyToSave.companionName ?: storyToSave.character.name
                val updatedPrev = prevStory.copy(
                    nextStoryId = storyToSave.storyId,
                    companionName = resolvedCompanionName,
                    nextStoryTitle = storyToSave.title,
                    sagaId = parentSagaId
                )
                savedStories[prevIndex] = updatedPrev
                persistSingleStory(updatedPrev)
                storyToSave = storyToSave.copy(sagaId = parentSagaId)
                println("🔗 [StoryRepository] Bidirectionally linked chapter «${prevStory.title}» -> «${storyToSave.title}» with sagaId=$parentSagaId")
            }
        }

        val existingIndex = savedStories.indexOfFirst { it.storyId == storyToSave.storyId }
        if (existingIndex == -1) {
            savedStories.add(0, storyToSave) // Newest first
        } else {
            savedStories[existingIndex] = storyToSave
        }

        persistSingleStory(storyToSave)
        updateStoryIdsIndex()
        markLastReadStory(storyToSave.storyId)
        println("💾 [StoryRepository] Saved story «${storyToSave.title}» (${storyToSave.storyId}) for profile=$profileIdToUse, total stories: ${savedStories.size}")
        return storyToSave
    }

    fun deleteStoriesForProfile(profileId: String) {
        val toDelete = savedStories.filter { it.childProfileId == profileId }
        if (toDelete.isNotEmpty()) {
            for (story in toDelete) {
                PlatformStorage.saveString("story_${story.storyId}", "")
                PlatformStorage.deleteStoryMedia(story.storyId)
            }
            savedStories.removeAll(toDelete)
            updateStoryIdsIndex()
            println("🗑️ [StoryRepository] Deleted ${toDelete.size} stories for profile $profileId, remaining: ${savedStories.size}")
        }
    }

    fun deleteSaga(sagaId: String) {
        val toDelete = savedStories.filter { it.effectiveSagaId == sagaId }
        if (toDelete.isNotEmpty()) {
            for (story in toDelete) {
                PlatformStorage.saveString("story_${story.storyId}", "")
                PlatformStorage.deleteStoryMedia(story.storyId)
            }
            savedStories.removeAll(toDelete)
            updateStoryIdsIndex()
            println("🗑️ [StoryRepository] Deleted entire saga $sagaId (${toDelete.size} chapters), remaining: ${savedStories.size}")
        }
    }

    fun deleteStory(storyId: String) {
        val story = findStory(storyId)
        if (story != null && story.effectiveSagaId == storyId && story.isMultiChapter) {
            // Root story of a multi-chapter saga -> delete entire saga
            deleteSaga(story.effectiveSagaId)
        } else {
            val removed = savedStories.removeAll { it.storyId == storyId }
            if (removed) {
                PlatformStorage.saveString("story_$storyId", "")
                PlatformStorage.deleteStoryMedia(storyId)
                updateStoryIdsIndex()
                println("🗑️ [StoryRepository] Deleted story $storyId, remaining: ${savedStories.size}")
            }
        }
    }

    private fun persistSingleStory(story: FullStoryResponse) {
        try {
            val singleJson = json.encodeToString(story)
            PlatformStorage.saveString("story_${story.storyId}", singleJson)
        } catch (e: Throwable) {
            println("⚠️ [StoryRepository] Error saving story ${story.storyId}: ${e.message}")
        }
    }

    private fun updateStoryIdsIndex() {
        try {
            val ids = savedStories.joinToString(",") { it.storyId }
            PlatformStorage.saveString("story_ids", ids)
        } catch (e: Throwable) {
            println("⚠️ [StoryRepository] Error updating index: ${e.message}")
        }
    }
}
