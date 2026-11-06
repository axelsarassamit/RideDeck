package com.axelsarassamit.gx12

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pillion.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class RideGpxTest {
    @Test fun exportThenImportPreservesSegmentsAndSampleMetadata() {
        val point=RideTrackPoint(13.0,100.0,123456L,30.0)
        val track=RideTrack(listOf(listOf(point,point.copy(lat=13.01)),listOf(point.copy(lon=100.01))))
        val output=ByteArrayOutputStream()
        RideGpx.write(track,output)
        assertEquals(track,RideGpx.read(output.toByteArray().inputStream()))
    }
    @Test fun invalidCoordinatesAndDtdAreRejected() {
        fun read(xml:String)=runCatching { RideGpx.read(xml.byteInputStream()) }
        val bad="<gpx><rte><rtept lat='91' lon='1'/><rtept lat='2' lon='2'/></rte></gpx>"
        val dtd="<!DOCTYPE gpx [<!ENTITY x '1'>]><gpx><rte><rtept lat='&x;' lon='1'/><rtept lat='2' lon='2'/></rte></gpx>"
        assertTrue(read(bad).isFailure);assertTrue(read(dtd).isFailure)
    }
}
