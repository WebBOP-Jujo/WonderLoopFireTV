package org.wonderloop.app.platform

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual object PlatformStorage {
    private var appContext: Context? = null
    private var fastPrefs: SharedPreferences? = null
    private var heavyPrefs: SharedPreferences? = null
    private var storiesDir: File? = null
    private var mediaDir: File? = null

    private fun isHeavyStoryKey(key: String): Boolean {
        return (key.startsWith("story_") && key != "story_ids" && key != "story_language") || key == "saved_stories"
    }

    private fun isImageFileName(fileName: String): Boolean {
        return fileName.endsWith(".jpg", ignoreCase = true) ||
            fileName.endsWith(".jpeg", ignoreCase = true) ||
            fileName.endsWith(".png", ignoreCase = true)
    }

    private fun isRiffWav(bytes: ByteArray): Boolean {
        return bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
    }

    private fun isAlreadyCompressedAudio(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val isM4a = bytes[4] == 'f'.code.toByte() &&
            bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() &&
            bytes[7] == 'p'.code.toByte()
        val isId3Mp3 = bytes[0] == 'I'.code.toByte() &&
            bytes[1] == 'D'.code.toByte() &&
            bytes[2] == '3'.code.toByte()
        val isRawMp3 = (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xE0) == 0xE0
        return isM4a || isId3Mp3 || isRawMp3
    }

    /**
     * Compresses uncompressed 16-bit PCM WAV or raw 24kHz PCM buffers (> 350 KB) into 64 kbps AAC (.m4a container)
     * using Android's hardware MediaCodec + MediaMuxer, shrinking ~1.3 MB WAV files to ~210 KB (-84%).
     */
    private fun compressWavBytesIfNeeded(bytes: ByteArray, bitrate: Int = 64_000): ByteArray {
        if (bytes.size <= 350_000 || isAlreadyCompressedAudio(bytes)) return bytes
        val ctx = appContext ?: return bytes
        var tempFile: File? = null
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        return try {
            val hasRiff = isRiffWav(bytes)
            val numChannels: Int
            val sampleRate: Int
            val pcmStart: Int
            if (hasRiff) {
                val headerBuf = ByteBuffer.wrap(bytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
                numChannels = headerBuf.getShort(22).toInt().coerceIn(1, 2)
                sampleRate = headerBuf.getInt(24).let { if (it in 8000..48000) it else 24000 }
                pcmStart = 44
            } else {
                numChannels = 1
                sampleRate = 24000
                pcmStart = 0
            }
            val bytesPerSampleFrame = 2 * numChannels

            tempFile = File.createTempFile("wl_aac_opt_", ".m4a", ctx.cacheDir)
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, numChannels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec = enc
            enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            enc.start()

            val mux = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux

            val bufferInfo = MediaCodec.BufferInfo()
            var inputOffset = pcmStart
            var inputDone = false
            var outputDone = false
            var trackIndex = -1

            while (!outputDone) {
                while (!inputDone) {
                    val inBufIndex = enc.dequeueInputBuffer(0L)
                    if (inBufIndex < 0) break
                    val inBuf = enc.getInputBuffer(inBufIndex)!!
                    inBuf.clear()
                    val remaining = bytes.size - inputOffset
                    if (remaining <= 0) {
                        val totalFrames = (bytes.size - pcmStart) / bytesPerSampleFrame
                        val ptsUs = (totalFrames * 1_000_000L) / sampleRate
                        enc.queueInputBuffer(inBufIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val toWrite = minOf(remaining, inBuf.remaining())
                        val framesSoFar = (inputOffset - pcmStart) / bytesPerSampleFrame
                        val ptsUs = (framesSoFar * 1_000_000L) / sampleRate
                        inBuf.put(bytes, inputOffset, toWrite)
                        enc.queueInputBuffer(inBufIndex, 0, toWrite, ptsUs, 0)
                        inputOffset += toWrite
                    }
                }

                val outBufIndex = enc.dequeueOutputBuffer(bufferInfo, 2_000L)
                when {
                    outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = mux.addTrack(enc.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                    }
                    outBufIndex >= 0 -> {
                        val outBuf = enc.getOutputBuffer(outBufIndex)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted && outBuf != null) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            mux.writeSampleData(trackIndex, outBuf, bufferInfo)
                        }
                        enc.releaseOutputBuffer(outBufIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            try { enc.stop() } catch (_: Throwable) {}
            try { enc.release() } catch (_: Throwable) {}
            codec = null

            if (muxerStarted) {
                try { mux.stop() } catch (_: Throwable) {}
            }
            try { mux.release() } catch (_: Throwable) {}
            muxer = null

            val compressed = tempFile.readBytes()
            tempFile.delete()
            if (compressed.isNotEmpty() && compressed.size < bytes.size) {
                compressed
            } else {
                bytes
            }
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] WAV compression skipped: ${e.message}")
            try { codec?.release() } catch (_: Throwable) {}
            try { if (muxerStarted) muxer?.stop(); muxer?.release() } catch (_: Throwable) {}
            try { tempFile?.delete() } catch (_: Throwable) {}
            bytes
        }
    }

    /**
     * Compresses oversized raw Gemini/camera images (often 750 KB - 1 MB+) into optimized
     * JPEG at 82% quality (capped at 1024px max dimension), reducing disk footprint to ~110-140 KB
     * without visible quality loss on mobile screens.
     */
    private fun compressImageBytesIfNeeded(
        bytes: ByteArray,
        maxDim: Int = 1024,
        quality: Int = 82
    ): ByteArray {
        if (bytes.size <= 180_000) return bytes
        return try {
            val boundsOpts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return bytes

            var sampleSize = 1
            while (srcW / (sampleSize * 2) >= maxDim || srcH / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return bytes

            val finalBitmap = if (decoded.width > maxDim || decoded.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) {
                    decoded.recycle()
                }
                scaled
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream(131_072)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            finalBitmap.recycle()

            val compressed = baos.toByteArray()
            if (compressed.isNotEmpty() && compressed.size < bytes.size) {
                compressed
            } else {
                bytes
            }
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Image compression skipped: ${e.message}")
            bytes
        }
    }

    private fun getLegacyHeavyPrefs(): SharedPreferences? {
        val ctx = appContext ?: return null
        if (heavyPrefs != null) return heavyPrefs
        val prefsFile = File(ctx.applicationInfo.dataDir, "shared_prefs/wonderloop_local_db.xml")
        if (prefsFile.exists() && prefsFile.length() > 0L) {
            heavyPrefs = ctx.getSharedPreferences("wonderloop_local_db", Context.MODE_PRIVATE)
        }
        return heavyPrefs
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        if (fastPrefs == null) {
            fastPrefs = appCtx.getSharedPreferences("wonderloop_fast_prefs", Context.MODE_PRIVATE)
        }
        if (storiesDir == null) {
            storiesDir = File(appCtx.filesDir, "stories").apply { if (!exists()) mkdirs() }
        }
        if (mediaDir == null) {
            mediaDir = File(appCtx.filesDir, "media").apply { if (!exists()) mkdirs() }
        }
    }

    actual fun saveString(key: String, value: String) {
        if (isHeavyStoryKey(key)) {
            val dir = storiesDir ?: return
            val file = File(dir, "$key.json")
            try {
                if (value.isBlank()) {
                    if (file.exists()) file.delete()
                } else {
                    file.writeText(value, Charsets.UTF_8)
                }
            } catch (e: Throwable) {
                println("⚠️ [PlatformStorage] Error writing story file ${file.name}: ${e.message}")
            }
            return
        }
        fastPrefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun getString(key: String, default: String): String {
        if (isHeavyStoryKey(key)) {
            val dir = storiesDir
            if (dir != null) {
                val file = File(dir, "$key.json")
                if (file.exists()) {
                    try {
                        return file.readText(Charsets.UTF_8)
                    } catch (e: Throwable) {
                        println("⚠️ [PlatformStorage] Error reading story file ${file.name}: ${e.message}")
                    }
                }
            }
            // Fallback to legacy heavyPrefs for one-time migration
            val hp = getLegacyHeavyPrefs()
            if (hp != null && hp.contains(key)) {
                return hp.getString(key, default) ?: default
            }
            return default
        }

        val fp = fastPrefs
        if (fp != null && fp.contains(key)) {
            return fp.getString(key, default) ?: default
        }
        val hp = getLegacyHeavyPrefs()
        if (hp != null && hp.contains(key)) {
            val migrated = hp.getString(key, default) ?: default
            fp?.edit()?.putString(key, migrated)?.apply()
            return migrated
        }
        return default
    }

    actual fun externalizeMediaIfNeeded(fileName: String, dataOrUri: String?): String? {
        if (dataOrUri.isNullOrBlank()) return dataOrUri
        if (dataOrUri.startsWith("http://") || dataOrUri.startsWith("https://")) {
            return dataOrUri
        }
        val dir = mediaDir ?: return dataOrUri
        val outFile = File(dir, fileName)

        if (dataOrUri.startsWith("file://") || dataOrUri.startsWith("/")) {
            return try {
                val srcFile = File(dataOrUri.removePrefix("file://"))
                if (srcFile.absolutePath == outFile.absolutePath && outFile.exists()) {
                    "file://${outFile.absolutePath}"
                } else if (srcFile.exists() && srcFile.length() > 0L) {
                    val rawBytes = srcFile.readBytes()
                    val finalBytes = when {
                        isImageFileName(fileName) -> compressImageBytesIfNeeded(rawBytes)
                        isRiffWav(rawBytes) -> compressWavBytesIfNeeded(rawBytes)
                        else -> rawBytes
                    }
                    outFile.writeBytes(finalBytes)
                    "file://${outFile.absolutePath}"
                } else {
                    dataOrUri
                }
            } catch (e: Throwable) {
                println("⚠️ [PlatformStorage] Failed to copy/compress local media $fileName: ${e.message}")
                dataOrUri
            }
        }

        return try {
            val base64Part = if (dataOrUri.contains(";base64,")) {
                dataOrUri.substringAfter(";base64,").trim()
            } else if (dataOrUri.startsWith("data:")) {
                dataOrUri.substringAfter(",").trim()
            } else {
                dataOrUri.trim()
            }
            val rawBytes = Base64.decode(base64Part, Base64.DEFAULT)
            if (rawBytes.isEmpty()) return dataOrUri
            val finalBytes = when {
                isImageFileName(fileName) -> compressImageBytesIfNeeded(rawBytes)
                isRiffWav(rawBytes) -> compressWavBytesIfNeeded(rawBytes)
                else -> rawBytes
            }
            outFile.writeBytes(finalBytes)
            "file://${outFile.absolutePath}"
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Failed to externalize media $fileName: ${e.message}")
            dataOrUri
        }
    }

    actual fun resolveDataUriIfNeeded(uriOrData: String?): String? {
        if (uriOrData.isNullOrBlank()) return uriOrData
        if (!uriOrData.startsWith("file://") && !uriOrData.startsWith("/")) {
            return uriOrData
        }
        return try {
            val path = uriOrData.removePrefix("file://")
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return uriOrData
            val bytes = file.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mime = when {
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                file.name.endsWith(".mp3", ignoreCase = true) -> "audio/mp3"
                file.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                file.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                else -> "image/jpeg"
            }
            "data:$mime;base64,$b64"
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Failed to resolve file URI $uriOrData: ${e.message}")
            uriOrData
        }
    }

    actual fun deleteStoryMedia(storyId: String) {
        if (storyId.isBlank()) return
        try {
            val dir = mediaDir ?: return
            val prefix = "${storyId}_"
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix)) {
                    file.delete()
                }
            }
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Error deleting media for $storyId: ${e.message}")
        }
    }

    actual fun purgeLegacyHeavyPrefs() {
        val ctx = appContext ?: return
        try {
            val prefsFile = File(ctx.applicationInfo.dataDir, "shared_prefs/wonderloop_local_db.xml")
            if (prefsFile.exists() || heavyPrefs != null) {
                println("🧹 [PlatformStorage] Purging legacy wonderloop_local_db.xml (${prefsFile.length() / (1024 * 1024)} MB)...")
                heavyPrefs?.edit()?.clear()?.commit()
                heavyPrefs = null
                ctx.deleteSharedPreferences("wonderloop_local_db")
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
                System.gc()
                println("✅ [PlatformStorage] Legacy wonderloop_local_db.xml purged and memory reclaimed!")
            }
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Error purging legacy heavyPrefs: ${e.message}")
        }

        // Clean up orphaned media/story files and retroactively optimize any remaining uncompressed media in files/media/
        try {
            val validStoryIds = fastPrefs?.getString("story_ids", "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()

            if (validStoryIds.isNotEmpty()) {
                storiesDir?.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("story_") && file.name.endsWith(".json")) {
                        val rawName = file.name.removeSuffix(".json")
                        val storyId = rawName.removePrefix("story_")
                        if (rawName != "story_ids" && storyId !in validStoryIds && rawName !in validStoryIds) {
                            file.delete()
                        }
                    }
                }
            }

            val dir = mediaDir
            if (dir != null && dir.exists()) {
                var optimizedImgCount = 0
                var optimizedAudioCount = 0
                var deletedOrphanCount = 0
                var savedBytes = 0L
                val now = System.currentTimeMillis()

                dir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val name = file.name
                        val len = file.length()

                        val isRedundantHeroCopy = name.contains("_prev_hero_") || name.contains("_orig_hero_")
                        val isStalePreview = name.startsWith("preview_latest_") && (now - file.lastModified() > 86_400_000L)
                        val isOrphanStoryMedia = validStoryIds.isNotEmpty() &&
                            name.startsWith("story_") &&
                            validStoryIds.none { id -> name.startsWith("${id}_") }

                        if (isRedundantHeroCopy || isStalePreview || isOrphanStoryMedia) {
                            if (file.delete()) {
                                deletedOrphanCount++
                                savedBytes += len
                            }
                        } else if ((name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) && len > 250_000L) {
                            val originalBytes = file.readBytes()
                            val compressedBytes = compressImageBytesIfNeeded(originalBytes)
                            if (compressedBytes.size < originalBytes.size) {
                                file.writeBytes(compressedBytes)
                                optimizedImgCount++
                                savedBytes += (originalBytes.size - compressedBytes.size)
                            }
                        } else if (name.endsWith(".wav", ignoreCase = true) && len > 350_000L) {
                            val originalBytes = file.readBytes()
                            if (!isAlreadyCompressedAudio(originalBytes)) {
                                val compressedBytes = compressWavBytesIfNeeded(originalBytes)
                                if (compressedBytes.size < originalBytes.size) {
                                    file.writeBytes(compressedBytes)
                                    optimizedAudioCount++
                                    savedBytes += (originalBytes.size - compressedBytes.size)
                                }
                            }
                        }
                    }
                }
                if (optimizedImgCount > 0 || optimizedAudioCount > 0 || deletedOrphanCount > 0) {
                    println("🗜️ [PlatformStorage] Media maintenance: $optimizedImgCount imgs, $optimizedAudioCount audios compressed, $deletedOrphanCount orphans removed (saved ${savedBytes / 1024} KB)")
                }
            }
        } catch (e: Throwable) {
            println("⚠️ [PlatformStorage] Error optimizing existing media files: ${e.message}")
        }
    }

    actual fun saveInt(key: String, value: Int) {
        fastPrefs?.edit()?.putInt(key, value)?.apply()
    }

    actual fun getInt(key: String, default: Int): Int {
        val fp = fastPrefs
        if (fp != null && fp.contains(key)) {
            return fp.getInt(key, default)
        }
        val hp = getLegacyHeavyPrefs()
        if (hp != null && hp.contains(key)) {
            val migrated = hp.getInt(key, default)
            fp?.edit()?.putInt(key, migrated)?.apply()
            return migrated
        }
        return default
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        fastPrefs?.edit()?.putBoolean(key, value)?.apply()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        val fp = fastPrefs
        if (fp != null && fp.contains(key)) {
            return fp.getBoolean(key, default)
        }
        val hp = getLegacyHeavyPrefs()
        if (hp != null && hp.contains(key)) {
            val migrated = hp.getBoolean(key, default)
            fp?.edit()?.putBoolean(key, migrated)?.apply()
            return migrated
        }
        return default
    }

    actual fun saveLong(key: String, value: Long) {
        fastPrefs?.edit()?.putLong(key, value)?.apply()
    }

    actual fun getLong(key: String, default: Long): Long {
        val fp = fastPrefs
        if (fp != null && fp.contains(key)) {
            return fp.getLong(key, default)
        }
        val hp = getLegacyHeavyPrefs()
        if (hp != null && hp.contains(key)) {
            val migrated = hp.getLong(key, default)
            fp?.edit()?.putLong(key, migrated)?.apply()
            return migrated
        }
        return default
    }

    actual fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    actual fun currentLocalDateKey(): String {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(java.util.Locale.US, year, month, day)
    }

    actual fun getDeviceLanguageCode(): String {
        val sysLang = java.util.Locale.getDefault().language.lowercase(java.util.Locale.US)
        return if (sysLang.startsWith("en")) "en" else "es"
    }
}
