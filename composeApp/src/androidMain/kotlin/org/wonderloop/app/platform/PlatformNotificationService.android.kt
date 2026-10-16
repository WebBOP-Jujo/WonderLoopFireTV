package org.wonderloop.app.platform

actual object PlatformNotificationService {
    actual fun tagHeroCreated(heroName: String, heroTrait: String) {
        // No-op on Amazon Fire TV (push notification tags are disabled on 10-foot TV)
    }

    actual fun tagPendingChallenge(heroName: String, challengeType: String) {
        // No-op on Amazon Fire TV
    }

    actual fun clearPendingChallenge() {
        // No-op on Amazon Fire TV
    }
}
