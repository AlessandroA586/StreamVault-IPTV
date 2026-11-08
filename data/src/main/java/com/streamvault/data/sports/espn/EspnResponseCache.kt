package com.streamvault.data.sports.espn

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspnResponseCache @Inject constructor() {
    private data class Entry(val rawJson: String, val expiresAt: Instant, val bytes: Int)
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private var byteCount = 0

    @Synchronized
    fun get(key: String, now: Instant): String? {
        val entry = entries[key] ?: return null
        if (!now.isBefore(entry.expiresAt)) {
            remove(key, entry)
            return null
        }
        return entry.rawJson
    }

    @Synchronized
    fun put(key: String, rawJson: String, ttl: Duration, now: Instant) {
        require(!ttl.isNegative && !ttl.isZero)
        val bytes = rawJson.toByteArray(Charsets.UTF_8).size
        if (bytes > MAX_BYTES) return
        entries.remove(key)?.let { byteCount -= it.bytes }
        entries[key] = Entry(rawJson, now.plus(ttl), bytes)
        byteCount += bytes
        while (entries.size > MAX_ENTRIES || byteCount > MAX_BYTES) {
            val oldest = entries.entries.iterator().next()
            remove(oldest.key, oldest.value)
        }
    }

    private fun remove(key: String, entry: Entry) {
        entries.remove(key)
        byteCount -= entry.bytes
    }

    private companion object {
        const val MAX_ENTRIES = 64
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}
