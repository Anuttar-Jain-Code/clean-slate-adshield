package com.cleanslate.adshield

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsMessageTest {
    @Test
    fun parseQuery_readsHostnameTypeAndClass() {
        val query = dnsQuery("demo.example.com", idHigh = 0x12, idLow = 0x34)

        val parsed = DnsMessage.parseQuery(query)

        assertNotNull(parsed)
        assertEquals(0x1234, parsed!!.id)
        assertEquals("demo.example.com", parsed.hostname)
        assertEquals(1, parsed.qType)
        assertEquals(1, parsed.qClass)
    }

    @Test
    fun buildBlockedResponse_returnsNxDomainWithSameQuestion() {
        val query = dnsQuery("demo.example.com", idHigh = 0x01, idLow = 0x02)

        val response = DnsMessage.buildBlockedResponse(query)

        assertNotNull(response)
        assertEquals(0x01.toByte(), response!![0])
        assertEquals(0x02.toByte(), response[1])
        assertEquals(0x81.toByte(), response[2])
        assertEquals(0x83.toByte(), response[3])
        assertEquals(0x00.toByte(), response[6])
        assertEquals(0x00.toByte(), response[7])
    }

    @Test
    fun copyResponseWithQueryId_patchesCachedResponseId() {
        val cachedResponse = byteArrayOf(0x01, 0x02, 0x81.toByte(), 0x80.toByte())
        val newQuery = byteArrayOf(0x55, 0x66, 0x01, 0x00)

        val patched = DnsMessage.copyResponseWithQueryId(cachedResponse, newQuery)

        assertArrayEquals(byteArrayOf(0x55, 0x66, 0x81.toByte(), 0x80.toByte()), patched)
    }

    @Test
    fun parseQuery_rejectsDnsResponses() {
        val response = dnsQuery("demo.example.com", flagsHigh = 0x81)

        assertNull(DnsMessage.parseQuery(response))
    }

    private fun dnsQuery(
        host: String,
        idHigh: Int = 0x00,
        idLow: Int = 0x01,
        flagsHigh: Int = 0x01,
        flagsLow: Int = 0x00
    ): ByteArray {
        val question = host.split('.').flatMap { label ->
            listOf(label.length.toByte()) + label.encodeToByteArray().toList()
        } + listOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x01.toByte())

        return byteArrayOf(
            idHigh.toByte(), idLow.toByte(),
            flagsHigh.toByte(), flagsLow.toByte(),
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00
        ) + question
    }
}
