package com.axelsarassamit.gx12;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RideSessionStateTest {
    @Test public void persistedFlagDoesNotRestartLeaseAfterServiceDies() {
        assertTrue(RideSessionState.shouldResume(true, true));
        assertFalse(RideSessionState.shouldResume(true, false));
        assertFalse(RideSessionState.shouldResume(false, true));
    }

    @Test public void elapsedRideTimeUsesOnlyTheActiveMonotonicSegment() {
        assertEquals(12_000L, RideSessionState.elapsed(10_000L, 100L, 2_100L));
        assertEquals(10_000L, RideSessionState.elapsed(10_000L, 2_100L, 100L));
    }
}