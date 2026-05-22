package com.cleanslate.adshield

import java.nio.ByteBuffer
import java.util.Locale

object DnsMessage {
    data class Query(
        val id: Int,
        val hostname: String,
        val questionEnd: Int,
        val qType: Int,
        val qClass: Int
    )

    fun parseQuery(packet: ByteArray): Query? {
        if (packet.size < DNS_HEADER_LENGTH) return null
        val buffer = ByteBuffer.wrap(packet)
        val id = buffer.short.toInt() and 0xffff
        val flags = buffer.short.toInt() and 0xffff
        val questionCount = buffer.short.toInt() and 0xffff
        if ((flags and QR_RESPONSE_MASK) != 0 || questionCount == 0) return null

        buffer.position(DNS_HEADER_LENGTH)
        val labels = mutableListOf<String>()
        while (buffer.hasRemaining()) {
            val length = buffer.get().toInt() and 0xff
            when {
                length == 0 -> break
                (length and COMPRESSION_MASK) != 0 -> return null
                length > MAX_LABEL_LENGTH || buffer.remaining() < length -> return null
                else -> {
                    val label = ByteArray(length)
                    buffer.get(label)
                    labels += label.toString(Charsets.UTF_8)
                }
            }
        }
        if (labels.isEmpty() || buffer.remaining() < QUESTION_FOOTER_LENGTH) return null
        val qType = buffer.short.toInt() and 0xffff
        val qClass = buffer.short.toInt() and 0xffff
        return Query(
            id = id,
            hostname = labels.joinToString(".").lowercase(Locale.US),
            questionEnd = buffer.position(),
            qType = qType,
            qClass = qClass
        )
    }

    fun buildBlockedResponse(query: ByteArray): ByteArray? {
        val parsed = parseQuery(query) ?: return null
        val response = ByteArray(parsed.questionEnd)
        query.copyInto(response, 0, 0, parsed.questionEnd)

        // Standard DNS response: QR=1, opcode=0, AA=0, TC=0, RD copied, RA=1, RCODE=3 NXDOMAIN.
        response[2] = ((query[2].toInt() and RD_MASK) or QR_RESPONSE_MASK).toByte()
        response[3] = (RA_MASK or NXDOMAIN_RCODE).toByte()
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

    fun copyResponseWithQueryId(response: ByteArray, query: ByteArray): ByteArray? {
        if (response.size < 2 || query.size < 2) return null
        val patched = response.copyOf()
        patched[0] = query[0]
        patched[1] = query[1]
        return patched
    }

    private const val DNS_HEADER_LENGTH = 12
    private const val QUESTION_FOOTER_LENGTH = 4
    private const val MAX_LABEL_LENGTH = 63
    private const val COMPRESSION_MASK = 0xc0
    private const val QR_RESPONSE_MASK = 0x80
    private const val RD_MASK = 0x01
    private const val RA_MASK = 0x80
    private const val NXDOMAIN_RCODE = 0x03
}
