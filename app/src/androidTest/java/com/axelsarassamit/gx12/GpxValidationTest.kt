package com.axelsarassamit.gx12

import org.junit.Assert.*
import org.junit.Test

class GpxValidationTest {
    private fun read(xml:String)=RideGpx.read(xml.byteInputStream())
    @Test fun acceptsTracksAndRoutesAndPreservesSegments() {
        val track=read("""<gpx><trk><trkseg><trkpt lat="13" lon="100"/><trkpt lat="13.1" lon="100.1"/></trkseg><trkseg><trkpt lat="14" lon="101"/></trkseg></trk></gpx>""")
        assertEquals(3,track.count);assertEquals(2,track.segments.size)
        val output=java.io.ByteArrayOutputStream();RideGpx.write(track,output)
        assertEquals(track,RideGpx.read(output.toByteArray().inputStream()))
        assertEquals(2,read("""<gpx><rte><rtept lat="13" lon="100"/><rtept lat="14" lon="101"/></rte></gpx>""").count)
    }
    @Test fun rejectsEntitiesWrongContainersAndInvalidCoordinates() {
        for(xml in listOf(
            """<!DOCTYPE gpx [<!ENTITY x SYSTEM "file:///private">]><gpx>&x;</gpx>""",
            """<gpx><extensions><trkseg><trkpt lat="13" lon="100"/><trkpt lat="14" lon="101"/></trkseg></extensions></gpx>""",
            """<gpx><rte><trkpt lat="13" lon="100"/><trkpt lat="14" lon="101"/></rte></gpx>""",
            """<gpx><rte><rtept lat="NaN" lon="100"/><rtept lat="14" lon="101"/></rte></gpx>""")) {
            assertTrue(runCatching{read(xml)}.isFailure)
        }
    }
}
