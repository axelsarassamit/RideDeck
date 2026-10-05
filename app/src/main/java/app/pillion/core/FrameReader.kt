package app.pillion.core

import app.pillion.protocol.NaviLiteCodec
import app.pillion.protocol.Crc32Mpeg2

/** A frame parsed off the wire. */
data class NaviFrameView(val serviceType: Int, val payload: ByteArray, val frameType: Int = 0, val payloadDataType: Int = 0)

/**
 * Reads complete NaviLite frames from a [ByteChannel]. Single responsibility: framing/resync.
 * It owns no transport and no protocol semantics beyond locating frame boundaries.
 */
class FrameReader(private val channel: ByteChannel) {
    private var buf = ByteArray(0)
    private val tmp = ByteArray(8192)

    fun next(): NaviFrameView {
        fill(NaviLiteCodec.HEADER_SIZE + NaviLiteCodec.CRC_SIZE)
        var skipped = 0
        while (!NaviLiteCodec.hasMagicAt(buf, 0)) {
            check(++skipped <= 8192) { "Unrecognized Yamaha data" }
            dropOne()
        }
        fill(16)
        val len = NaviLiteCodec.frameLengthAt(buf, 0)
        check(len in 16..1048576) { "Invalid Yamaha frame length" }
        fill(len)
        val crc = (buf[12].toInt() and 255) or ((buf[13].toInt() and 255) shl 8) or
            ((buf[14].toInt() and 255) shl 16) or ((buf[15].toInt() and 255) shl 24)
        check(Crc32Mpeg2.compute(buf.copyOfRange(0, 12) + buf.copyOfRange(16, len)) == crc) {
            "Yamaha frame checksum failed"
        }
        val view = NaviFrameView(NaviLiteCodec.serviceTypeAt(buf, 0), NaviLiteCodec.payloadAt(buf, 0),
            buf[5].toInt() and 255, buf[11].toInt() and 255)
        buf = buf.copyOfRange(len, buf.size)
        return view
    }

    private fun fill(n: Int) {
        while (buf.size < n) {
            val r = channel.read(tmp)
            if (r < 0) error("channel closed")
            if (r > 0) buf += tmp.copyOf(r)
        }
    }

    private fun dropOne() {
        fill(4)
        buf = buf.copyOfRange(1, buf.size)
    }
}
