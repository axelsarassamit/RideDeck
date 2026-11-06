package app.pillion.core

import org.junit.Assert.*
import org.junit.Test

class RiderFeaturesTest {
    @Test fun curvyRankingRewardsBendsButNotSharpJunctions() {
        val straight=listOf(FunPoint(13.0,100.0),FunPoint(13.001,100.0),FunPoint(13.002,100.0))
        val bend=straight.take(2)+FunPoint(13.002,100.001)
        val rightAngle=straight.take(2)+FunPoint(13.001,100.001)
        assertEquals(0.0,TwistyRide.bonus(2,straight),0.0)
        assertEquals(0.0,TwistyRide.bonus(2,rightAngle),0.0)
        assertEquals(0.0,TwistyRide.bonus(0,bend),0.0)
        assertTrue(TwistyRide.bonus(1,bend)>0)
        assertTrue(TwistyRide.bonus(2,bend)>TwistyRide.bonus(1,bend))
    }
    @Test fun recapDoesNotJoinDisconnectedSegmentsOrInventMovingTime() {
        val a=RideTrackPoint(13.0,100.0)
        val b=RideTrackPoint(13.0001,100.0)
        val c=RideTrackPoint(14.0,101.0)
        val r=RideTrack(listOf(listOf(a,b),listOf(c))).recap()
        assertEquals(11.12,r.meters,.05)
        assertNull(r.movingMs);assertNull(r.climb)
        val timed=RideTrack(listOf(listOf(a.copy(time=1000),b.copy(time=2000),b.copy(time=4000)))).recap()
        assertEquals(1000L,timed.movingMs)
        assertEquals(3000L,timed.timedMs)
    }
}
