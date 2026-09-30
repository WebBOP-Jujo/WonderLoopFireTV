package org.wonderloop.app.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.wonderloop.app.platform.PlatformStorage

enum class SubscriptionPlan(
    val id: String,
    val monthlyQuota: Int,
    val maxRolloverCap: Int
) {
    FREE("free", monthlyQuota = 0, maxRolloverCap = 3),
    BASIC_MONTHLY("wonderloop_basic_6", monthlyQuota = 6, maxRolloverCap = 12),
    BASIC_ANNUAL("wonderloop_annual_basic_6", monthlyQuota = 6, maxRolloverCap = 12),
    PRO_MONTHLY("wonderloop_pro_15", monthlyQuota = 15, maxRolloverCap = 30),
    STAR_ANNUAL("wonderloop_annual_star_15", monthlyQuota = 15, maxRolloverCap = 30);

    companion object {
        fun fromId(id: String): SubscriptionPlan {
            return entries.firstOrNull { it.id == id } ?: FREE
        }
    }
}

object SubscriptionManager {
    private const val KEY_INITIALIZED = "sub_is_initialized"
    private const val KEY_AVAILABLE_STORIES = "sub_available_stories"
    private const val KEY_ACTIVE_PLAN_ID = "sub_active_plan_id"
    private const val KEY_LAST_GRANT_MS = "sub_last_grant_ms"
    private const val KEY_DAILY_LIMIT = "daily_story_limit"
    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    val ALLOWED_DAILY_LIMITS = listOf(2, 3, 5)

    private val _availableStories = mutableIntStateOf(3)
    val availableStories: State<Int> get() = _availableStories

    private val _dailyLimit = mutableIntStateOf(2)
    val dailyLimit: State<Int> get() = _dailyLimit

    private val _dailyUsageRevision = mutableIntStateOf(0)

    private val _activePlan = mutableStateOf(SubscriptionPlan.FREE)
    val activePlan: State<SubscriptionPlan> get() = _activePlan

    val isPremium: Boolean get() = _activePlan.value != SubscriptionPlan.FREE

    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        isInitialized = true

        val savedDaily = PlatformStorage.getInt(KEY_DAILY_LIMIT, 2)
        _dailyLimit.intValue = if (savedDaily in ALLOWED_DAILY_LIMITS) savedDaily else 2

        val wasInitialized = PlatformStorage.getBoolean(KEY_INITIALIZED, false)

        if (!wasInitialized) {
            // First time installation: grant 3 welcome stories
            val legacyPremium = PlatformStorage.getBoolean("is_premium", false)
            if (legacyPremium) {
                _activePlan.value = SubscriptionPlan.STAR_ANNUAL
                _availableStories.intValue = 15
                PlatformStorage.saveString(KEY_ACTIVE_PLAN_ID, SubscriptionPlan.STAR_ANNUAL.id)
            } else {
                _activePlan.value = SubscriptionPlan.FREE
                _availableStories.intValue = 3
                PlatformStorage.saveString(KEY_ACTIVE_PLAN_ID, SubscriptionPlan.FREE.id)
            }
            PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, _availableStories.intValue)
            PlatformStorage.saveLong(KEY_LAST_GRANT_MS, PlatformStorage.currentTimeMillis())
            PlatformStorage.saveBoolean(KEY_INITIALIZED, true)
            println("🌟 [SubscriptionManager] First-time setup complete: ${_availableStories.intValue} stories granted. Plan: ${_activePlan.value}")
        } else {
            // Load saved subscription state
            val planId = PlatformStorage.getString(KEY_ACTIVE_PLAN_ID, SubscriptionPlan.FREE.id)
            _activePlan.value = SubscriptionPlan.fromId(planId)
            _availableStories.intValue = PlatformStorage.getInt(KEY_AVAILABLE_STORIES, 3)

            // Check 30-day renewal / rollover cycle
            checkMonthlyRolloverGrant()
            println("🌟 [SubscriptionManager] Loaded active plan: ${_activePlan.value}, balance: ${_availableStories.intValue} stories, dailyLimit: ${_dailyLimit.intValue}")
        }
    }

    private fun checkMonthlyRolloverGrant() {
        val lastGrant = PlatformStorage.getLong(KEY_LAST_GRANT_MS, 0L)
        val now = PlatformStorage.currentTimeMillis()

        if (lastGrant > 0L && (now - lastGrant) >= THIRTY_DAYS_MS) {
            val currentPlan = _activePlan.value
            val currentStories = _availableStories.intValue

            val newStories = when (currentPlan) {
                SubscriptionPlan.FREE -> {
                    // 1 free story granted per month if user has 0
                    if (currentStories < 1) 1 else currentStories
                }
                SubscriptionPlan.BASIC_MONTHLY, SubscriptionPlan.BASIC_ANNUAL -> {
                    // +6 stories added, capped at 12
                    minOf(currentStories + currentPlan.monthlyQuota, currentPlan.maxRolloverCap)
                }
                SubscriptionPlan.PRO_MONTHLY, SubscriptionPlan.STAR_ANNUAL -> {
                    // +15 stories added, capped at 30
                    minOf(currentStories + currentPlan.monthlyQuota, currentPlan.maxRolloverCap)
                }
            }

            _availableStories.intValue = newStories
            PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, newStories)
            PlatformStorage.saveLong(KEY_LAST_GRANT_MS, now)
            println("🔄 [SubscriptionManager] Monthly rollover applied: was $currentStories, now $newStories")
        }
    }

    fun setDailyLimit(limit: Int) {
        val normalized = if (limit in ALLOWED_DAILY_LIMITS) limit else 2
        _dailyLimit.intValue = normalized
        PlatformStorage.saveInt(KEY_DAILY_LIMIT, normalized)
        _dailyUsageRevision.intValue += 1
        println("🌙 [SubscriptionManager] Updated daily story limit to $normalized/day")
    }

    fun getStoriesCreatedToday(profileId: String? = null): Int {
        // Read revision state so Compose UI recomposes when daily usage changes
        val rev = _dailyUsageRevision.intValue
        val targetProfileId = profileId ?: ProfileRepository.activeProfile?.id ?: "profile_default"
        val todayKey = PlatformStorage.currentLocalDateKey()
        val savedDate = PlatformStorage.getString("daily_date_$targetProfileId", "")
        if (savedDate != todayKey && rev >= 0) {
            return 0
        }
        return PlatformStorage.getInt("daily_count_$targetProfileId", 0).coerceAtLeast(0)
    }

    fun getRemainingDailyStories(profileId: String? = null): Int {
        return (_dailyLimit.intValue - getStoriesCreatedToday(profileId)).coerceAtLeast(0)
    }

    fun hasReachedDailyLimit(profileId: String? = null): Boolean {
        return getStoriesCreatedToday(profileId) >= _dailyLimit.intValue
    }

    fun recordDailyStoryCreation(profileId: String? = null) {
        val targetProfileId = profileId ?: ProfileRepository.activeProfile?.id ?: "profile_default"
        val todayKey = PlatformStorage.currentLocalDateKey()
        val currentCount = getStoriesCreatedToday(targetProfileId)
        PlatformStorage.saveString("daily_date_$targetProfileId", todayKey)
        PlatformStorage.saveInt("daily_count_$targetProfileId", currentCount + 1)
        _dailyUsageRevision.intValue += 1
        println("🌙 [SubscriptionManager] Daily stories created today for $targetProfileId ($todayKey): ${currentCount + 1}/${_dailyLimit.intValue}")
    }

    fun canCreateStory(): Boolean {
        return _availableStories.intValue > 0
    }

    fun consumeStoryCredit(): Boolean {
        if (_availableStories.intValue > 0) {
            _availableStories.intValue -= 1
            PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, _availableStories.intValue)
            recordDailyStoryCreation()
            println("📖 [SubscriptionManager] Consumed 1 credit. Remaining: ${_availableStories.intValue}")
            return true
        }
        return false
    }

    fun activatePlan(plan: SubscriptionPlan) {
        _activePlan.value = plan
        PlatformStorage.saveString(KEY_ACTIVE_PLAN_ID, plan.id)
        PlatformStorage.saveBoolean("is_premium", plan != SubscriptionPlan.FREE)

        // Grant monthly quota respecting rollover cap
        val current = _availableStories.intValue
        val updated = minOf(current + plan.monthlyQuota, plan.maxRolloverCap)
        _availableStories.intValue = updated
        PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, updated)
        PlatformStorage.saveLong(KEY_LAST_GRANT_MS, PlatformStorage.currentTimeMillis())

        println("🎉 [SubscriptionManager] Activated plan ${plan.name}. New balance: $updated stories")
    }

    fun syncRestoredPlan(plan: SubscriptionPlan) {
        val previousPlan = _activePlan.value
        _activePlan.value = plan
        PlatformStorage.saveString(KEY_ACTIVE_PLAN_ID, plan.id)
        PlatformStorage.saveBoolean("is_premium", plan != SubscriptionPlan.FREE)

        if (plan != SubscriptionPlan.FREE && previousPlan != plan) {
            val current = _availableStories.intValue
            val updated = minOf(maxOf(current, plan.monthlyQuota), plan.maxRolloverCap)
            _availableStories.intValue = updated
            PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, updated)
            PlatformStorage.saveLong(KEY_LAST_GRANT_MS, PlatformStorage.currentTimeMillis())
            println("🔄 [SubscriptionManager] Restored/synced plan ${plan.name}. Balance: $updated stories")
        }
    }

    fun buyPack(isSubscriber: Boolean) {
        // Consumable pack of 5 stories never expires and is not limited by subscription cap
        _availableStories.intValue += 5
        PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, _availableStories.intValue)
        println("🎁 [SubscriptionManager] Added 5-story pack (sub: $isSubscriber). New balance: ${_availableStories.intValue}")
    }

    fun redeemCode(code: String): Boolean {
        val validCodes = listOf("SHIPATON2026", "DEVPOSTVIP", "WONDERVIP", "HACKATHON2026")
        val normalized = code.trim().uppercase()
        if (validCodes.contains(normalized)) {
            _activePlan.value = SubscriptionPlan.STAR_ANNUAL
            _availableStories.intValue = maxOf(_availableStories.intValue + 30, 30)
            PlatformStorage.saveString(KEY_ACTIVE_PLAN_ID, SubscriptionPlan.STAR_ANNUAL.id)
            PlatformStorage.saveInt(KEY_AVAILABLE_STORIES, _availableStories.intValue)
            PlatformStorage.saveBoolean("is_premium", true)
            return true
        }
        return false
    }
}
