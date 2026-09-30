package org.wonderloop.app.platform

import com.onesignal.OneSignal

actual object PlatformNotificationService {
    actual fun tagHeroCreated(heroName: String, heroTrait: String) {
        try {
            OneSignal.User.addTag("last_hero", heroName)
            OneSignal.User.addTag("last_hero_trait", heroTrait)
            OneSignal.User.addTag("stories_created", "1")
            println("🏷️ OneSignal tagged hero: $heroName, trait: $heroTrait")
        } catch (e: Exception) {
            println("⚠️ OneSignal tagHeroCreated skipped: ${e.message}")
        }
    }

    actual fun tagPendingChallenge(heroName: String, challengeType: String) {
        try {
            OneSignal.User.addTag("pending_challenge", challengeType)
            OneSignal.User.addTag("pending_challenge_hero", heroName)
            println("🏷️ OneSignal tagged pending challenge: $challengeType for hero: $heroName")
        } catch (e: Exception) {
            println("⚠️ OneSignal tagPendingChallenge skipped: ${e.message}")
        }
    }

    actual fun clearPendingChallenge() {
        try {
            OneSignal.User.removeTag("pending_challenge")
            println("🏷️ OneSignal cleared pending challenge tag")
        } catch (e: Exception) {
            println("⚠️ OneSignal clearPendingChallenge skipped: ${e.message}")
        }
    }
}
