package app.pillion.core

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder

class FunRideTest {
    @Test fun orderedStopsAndLoopFinishSurviveHandoff() {
        val start=FunPoint(13.5,100.5);val stops=listOf(FunPoint(13.6,100.4),FunPoint(13.7,100.6))
        val url=URLDecoder.decode(FunRideHandoff.url(start,start,stops,listOf("tolls","ferries")),"UTF-8")
        assertTrue(url.contains("origin=13.5,100.5&destination=13.5,100.5"))
        assertTrue(url.contains("waypoints=13.6,100.4|13.7,100.6"))
        assertTrue(url.contains("travelmode=two-wheeler"));assertFalse(url.contains("dir_action=navigate"))
        assertTrue(url.contains("avoid=tolls,ferries"))
    }
    @Test fun unsupportedHandoffIsRejectedWithoutTruncation() {
        val p=FunPoint(13.5,100.5)
        assertThrows(IllegalArgumentException::class.java){FunRideHandoff.url(p,p,listOf(p,p,p),emptyList())}
        assertThrows(IllegalArgumentException::class.java){FunRideHandoff.url(p,p,listOf(p),listOf("dirt"))}
        assertThrows(IllegalArgumentException::class.java){FunRideHandoff.url(p,p,emptyList(),emptyList())}
    }
    @Test fun explorationSeedsStayValidAcrossDateline() {
        val seeds=FunRidePlan.seeds(FunPoint(0.0,179.99),360,90.0)
        assertEquals(2,seeds.size);assertTrue(seeds.all{it.longitude in -180.0..180.0 && it.latitude in -85.0..85.0})
        assertThrows(IllegalArgumentException::class.java){FunRidePlan.seeds(FunPoint(13.5,100.5),10,0.0)}
    }
    @Test fun fewerUTurnsCanOutrankSlightlyCloserDuration() {
        assertTrue(FunRidePlan.score(63.0,60,0)<FunRidePlan.score(60.0,60,3))
    }
    @Test fun sameRoadsDoNotBecomeThreeAlternatives() {
        val a=listOf(FunPoint(13.5,100.5),FunPoint(13.51,100.51),FunPoint(13.52,100.52))
        assertTrue(FunRidePlan.sameRoads(a,a.reversed()))
        assertFalse(FunRidePlan.sameRoads(a,a.map{FunPoint(it.latitude+.05,it.longitude)}))
    }
}
