package app.pillion.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DashContentCommandTest {
    private fun frame(service: Int, payload: ByteArray = byteArrayOf(1, 0), type: Int = 1, pdt: Int = 0) =
        NaviFrameView(service, payload, type, pdt)
    @Test fun acceptsImageStartAndStop() {
        assertEquals(DashContentCommand.Action.START, DashContentCommand.classify(frame(55)))
        assertEquals(DashContentCommand.Action.STOP, DashContentCommand.classify(frame(56, byteArrayOf(1))))
    }
    @Test fun rejectsOtherContentAndMalformedRequests() {
        for (payload in listOf(byteArrayOf(), byteArrayOf(2, 0), byteArrayOf(1, 5), byteArrayOf(1, 0, 0)))
            assertEquals(DashContentCommand.Action.IGNORE, DashContentCommand.classify(frame(55, payload)))
    }
    @Test fun rejectsWrongDirectionTypeAndUnknownService() {
        assertEquals(DashContentCommand.Action.IGNORE, DashContentCommand.classify(frame(55, type = 6)))
        assertEquals(DashContentCommand.Action.IGNORE, DashContentCommand.classify(frame(55, pdt = 1)))
        assertEquals(DashContentCommand.Action.IGNORE, DashContentCommand.classify(frame(51)))
    }
}
