package com.cleanslate.adshield

import java.util.LinkedHashMap

class DnsCache(
    private val maxEntries: Int = AppConfig.DNS_CACHE_MAX_ENTRIES,
    private val ttlMs: Long = AppConfig.DNS_CACHE_TTL_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry(val createdAt: Long, val response: ByteArray)

    private val entries = object : LinkedHashMap<String, Entry>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > maxEntries
    }

    @Synchronized
    fun get(key: String): ByteArray? {
        val entry = entries[key] ?: return null
        if (clock() - entry.createdAt > ttlMs) {
            entries.remove(key)
            return null
        }
        return entry.response.copyOf()
    }

    @Synchronized
    fun put(key: String, response: ByteArray) {
        entries[key] = Entry(clock(), response.copyOf())
    }

    @Synchronized
    fun clear() = entries.clear()
}
