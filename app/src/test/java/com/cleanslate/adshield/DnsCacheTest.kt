package com.cleanslate.adshield

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsCacheTest {
    @Test
    fun get_returnsStoredCopy() {
        var now = 1000L
        val cache = DnsCache(maxEntries = 2, ttlMs = 1000L, clock = { now })
        val response = byteArrayOf(1, 2, 3)

        cache.put("demo|1|1", response)
        response[0] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3), cache.get("demo|1|1"))
    }

    @Test
    fun get_expiresOldEntries() {
        var now = 1000L
        val cache = DnsCache(maxEntries = 2, ttlMs = 1000L, clock = { now })

        cache.put("demo|1|1", byteArrayOf(1))
        now = 2501L

        assertNull(cache.get("demo|1|1"))
    }

    @Test
    fun put_evictsOldestEntry() {
        val cache = DnsCache(maxEntries = 2, ttlMs = 1000L, clock = { 1000L })

        cache.put("one", byteArrayOf(1))
        cache.put("two", byteArrayOf(2))
        cache.put("three", byteArrayOf(3))

        assertNull(cache.get("one"))
    }
}
