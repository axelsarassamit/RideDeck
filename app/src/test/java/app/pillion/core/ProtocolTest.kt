package app.pillion.core

import app.pillion.protocol.*
import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    private class MemoryLink(private val bytes: ByteArray) : ByteChannel {
        var offset = 0
        override fun open() {}
        override fun close() {}
        override fun write(bytes: ByteArray) {}
        override fun read(buffer: ByteArray): Int {
            if (offset == bytes.size) return -1
            val count = minOf(3, bytes.size - offset)
            bytes.copyInto(buffer, 0, offset, offset + count); offset += count; return count
        }
    }
    @Test fun crcMatchesPublishedMpeg2Vector() {
        assertEquals(0x0376e6e7, Crc32Mpeg2.compute("123456789".toByteArray()))
    }
    @Test fun fragmentedFrameAndLeadingNoiseRoundTrip() {
        val payload = byteArrayOf(4, 3, 2, 1)
        val bytes = byteArrayOf(99, 98) + NaviLiteCodec.build(6, 83, 1, payload)
        val frame = FrameReader(MemoryLink(bytes)).next()
        assertEquals(83, frame.serviceType); assertArrayEquals(payload, frame.payload)
    }
    @Test(expected = IllegalStateException::class) fun badChecksumRejected() {
        val bytes = NaviLiteCodec.build(6, 80, 0, byteArrayOf(1, 0))
        bytes[16] = 2
        FrameReader(MemoryLink(bytes)).next()
    }
    @Test(expected = IllegalStateException::class) fun hugeFrameRejectedBeforeAllocation() {
        val bytes = NaviLiteCodec.build(6, 80, 0, byteArrayOf())
        bytes[10] = 127
        FrameReader(MemoryLink(bytes)).next()
    }
    @Test(expected = IllegalArgumentException::class) fun shortAuthRejected() { Auth.partNumber(byteArrayOf(1)) }
    @Test fun xmaxSizeAndChallenge() {
        val decoded = "006-B3952-01".toByteArray() + byteArrayOf(1, 2, 3, 4)
        val encoded = decoded.map { (it.toInt() xor 10).toByte() }.toByteArray()
        assertEquals("006-B3952-01", Auth.partNumber(encoded))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), Auth.secDataAckPayload(encoded))
        assertEquals(DashSize(480, 234), NaviLiteDisplay.forCcuPartNumber(Auth.partNumber(encoded)))
    }
    @Test fun handshakeEchoesNonceAndSetsUpXmax() {
        val decoded = "006-B3952-01".toByteArray() + byteArrayOf(7, 8, 9, 10)
        val input = MemoryLink(NaviLiteCodec.build(5, 66, 0, byteArrayOf()) +
            NaviLiteCodec.build(5, 83, 1, decoded.map { (it.toInt() xor 10).toByte() }.toByteArray()))
        val sent = mutableListOf<ByteArray>()
        val channel = object : ByteChannel {
            override fun open() {}
            override fun close() {}
            override fun read(buffer: ByteArray) = input.read(buffer)
            override fun write(bytes: ByteArray) { sent += bytes }
        }
        assertEquals(DashSize(480, 234), Handshake(channel, FrameReader(channel)).perform())
        assertEquals(14, sent.size)
        assertEquals(81, NaviLiteCodec.serviceTypeAt(sent[0], 0))
        assertEquals(84, NaviLiteCodec.serviceTypeAt(sent[2], 0))
        assertArrayEquals(byteArrayOf(7, 8, 9, 10), NaviLiteCodec.payloadAt(sent[2], 0))
    }
}
