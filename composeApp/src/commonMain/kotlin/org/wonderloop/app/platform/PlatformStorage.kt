package org.wonderloop.app.platform

expect object PlatformStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String, default: String = ""): String
    fun saveInt(key: String, value: Int)
    fun getInt(key: String, default: Int = 0): Int
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun saveLong(key: String, value: Long)
    fun getLong(key: String, default: Long = 0L): Long
    fun currentTimeMillis(): Long
    fun currentLocalDateKey(): String
    fun getDeviceLanguageCode(): String
    fun externalizeMediaIfNeeded(fileName: String, dataOrUri: String?): String?
    fun resolveDataUriIfNeeded(uriOrData: String?): String?
    fun deleteStoryMedia(storyId: String)
    fun purgeLegacyHeavyPrefs()
}
