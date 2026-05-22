package com.cleanslate.adshield

import java.nio.ByteBuffer
import java.util.Locale

object DnsMessage {
    data class Query(val id: Int, val hostname: String, val questionEnd: Int)

    fun parseQuery(packet: ByteArray): Query? {
        if (packet.size < 12) return null
        val buffer = ByteBuffer.wrap(packet)
        val id = buffer.short.toInt() and 0xffff
        val flags = buffer.short.toInt() and 0xffff
        val questionCount = buffer.short.toInt() and 0xffff
        if ((flags and 0x8000) != 0 || questionCount == 0) return null

        buffer.position(12)
        val labels = mutableListOf<String>()
        while (buffer.hasRemaining()) {
            val length = buffer.get().toInt() and 0xff
            when {
                length == 0 -> break
                (length and 0xc0) != 0 -> return null
                length > 63 || buffer.remaining() < length -> return null
                else -> {
                    val label = ByteArray(length)
                    buffer.get(label)
                    labels += label.toString(Charsets.UTF_8)
                }
            }
        }
        if (labels.isEmpty() || buffer.remaining() < 4) return null
        buffer.position(buffer.position() + 4)
        return Query(
            id = id,
            hostname = labels.joinToString(".").lowercase(Locale.US),
            questionEnd = buffer.position()
        )
    }

    fun buildBlockedResponse(query: ByteArray): ByteArray? {
        val parsed = parseQuery(query) ?: return null
        val response = ByteArray(parsed.questionEnd)
        query.copyInto(response, 0, 0, parsed.questionEnd)

        // Standard DNS response: QR=1, opcode=0, AA=0, TC=0, RD copied, RA=1, RCODE=3 NXDOMAIN.
        response[2] = ((query[2].toInt() and 0x01) or 0x80).toByte()
        response[3] = 0x83.toByte()
        response[4] = 0x00
        response[5] = 0x01
        response[6] = 0x00
        response[7] = 0x00
        response[8] = 0x00
        response[9] = 0x00
        response[10] = 0x00
        response[11] = 0x00
        return response
    }
}
