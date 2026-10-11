package com.axelsarassamit.gx12;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DashAutoOpenPolicyTest {
    private boolean allowed(boolean explicitStartPending, boolean hostUiResumed, boolean rideRunning,
            boolean leaseServiceRunning, boolean leaseAuthenticated, boolean signedDashAvailable,
            boolean preferenceEnabled) {
        return DashAutoOpenPolicy.shouldLaunchAfterExplicitStart(explicitStartPending, hostUiResumed,
            rideRunning, leaseServiceRunning, leaseAuthenticated, signedDashAvailable, preferenceEnabled);
    }

    @Test public void opensOnlyAfterVisibleExplicitStartAndAuthenticatedLease() {
        assertTrue(allowed(true, true, true, true, true, true, true));
        assertFalse(allowed(false, true, true, true, true, true, true));
        assertFalse(allowed(true, false, true, true, true, true, true));
        assertFalse(allowed(true, true, true, true, false, true, true));
        assertFalse(allowed(true, true, true, true, true, false, true));
        assertFalse(allowed(true, true, true, true, true, true, false));
    }

    @Test public void doesNotOpenWhenRideOrLeaseIsNoLongerActive() {
        assertFalse(allowed(true, true, false, true, true, true, true));
        assertFalse(allowed(true, true, true, false, true, true, true));
    }
}
