package org.wonderloop.app.platform

expect object PlatformNotificationService {
    fun tagHeroCreated(heroName: String, heroTrait: String)
    fun tagPendingChallenge(heroName: String, challengeType: String)
    fun clearPendingChallenge()
}
