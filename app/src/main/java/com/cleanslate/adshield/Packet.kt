package com.cleanslate.adshield

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Packet {
    data class UdpDatagram(
        val sourceAddress: Int,
        val destinationAddress: Int,
        val sourcePort: Int,
        val destinationPort: Int,
        val payload: ByteArray
    )

    fun parseUdpDatagram(frame: ByteArray, length: Int): UdpDatagram? {
        if (length < 28) return null
        val version = frame[0].toInt().ushr(4) and 0x0f
        if (version != 4) return null
        val headerLength = (frame[0].toInt() and 0x0f) * 4
        if (headerLength < 20 || length < headerLength + 8) return null
        val protocol = frame[9].toInt() and 0xff
        if (protocol != 17) return null

        val sourceAddress = readInt(frame, 12)
        val destinationAddress = readInt(frame, 16)
        val udpOffset = headerLength
        val sourcePort = readShort(frame, udpOffset)
        val destinationPort = readShort(frame, udpOffset + 2)
        val udpLength = readShort(frame, udpOffset + 4)
        if (udpLength < 8 || udpOffset + udpLength > length) return null

        val payloadOffset = udpOffset + 8
        val payloadLength = udpLength - 8
        val payload = frame.copyOfRange(payloadOffset, payloadOffset + payloadLength)
        return UdpDatagram(sourceAddress, destinationAddress, sourcePort, destinationPort, payload)
    }

    fun buildUdpResponse(request: UdpDatagram, payload: ByteArray): ByteArray {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + payload.size
        val buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)

        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0)
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(17.toByte())
        buffer.putShort(0)
        buffer.putInt(request.destinationAddress)
        buffer.putInt(request.sourceAddress)

        val ipChecksum = checksum(buffer.array(), 0, ipHeaderLength)
        buffer.putShort(10, ipChecksum.toShort())

        val udpOffset = ipHeaderLength
        buffer.position(udpOffset)
        buffer.putShort(request.destinationPort.toShort())
        buffer.putShort(request.sourcePort.toShort())
        buffer.putShort((udpHeaderLength + payload.size).toShort())
        buffer.putShort(0)
        buffer.put(payload)

        val udpChecksum = udpChecksum(
            packet = buffer.array(),
            udpOffset = udpOffset,
            udpLength = udpHeaderLength + payload.size,
            sourceAddress = request.destinationAddress,
            destinationAddress = request.sourceAddress
        )
        buffer.putShort(udpOffset + 6, udpChecksum.toShort())
        return buffer.array()
    }

    private fun readShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 24) or
            ((bytes[offset + 1].toInt() and 0xff) shl 16) or
            ((bytes[offset + 2].toInt() and 0xff) shl 8) or
            (bytes[offset + 3].toInt() and 0xff)

    private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        val end = offset + length
        while (index + 1 < end) {
            sum += readShort(bytes, index).toLong()
            index += 2
        }
        if (index < end) sum += ((bytes[index].toInt() and 0xff) shl 8).toLong()
        while ((sum ushr 16) != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun udpChecksum(packet: ByteArray, udpOffset: Int, udpLength: Int, sourceAddress: Int, destinationAddress: Int): Int {
        val pseudoHeader = ByteBuffer.allocate(12 + udpLength + (udpLength % 2)).order(ByteOrder.BIG_ENDIAN)
        pseudoHeader.putInt(sourceAddress)
        pseudoHeader.putInt(destinationAddress)
        pseudoHeader.put(0)
        pseudoHeader.put(17.toByte())
        pseudoHeader.putShort(udpLength.toShort())
        pseudoHeader.put(packet, udpOffset, udpLength)
        if (udpLength % 2 != 0) pseudoHeader.put(0)
        val value = checksum(pseudoHeader.array(), 0, pseudoHeader.position())
        return if (value == 0) 0xffff else value
    }
}
