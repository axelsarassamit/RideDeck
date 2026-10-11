package com.axelsarassamit.gx12;

final class DashAutoOpenPolicy {
    private DashAutoOpenPolicy() { }

    static boolean shouldLaunchAfterExplicitStart(boolean explicitStartPending, boolean hostUiResumed,
            boolean rideRunning, boolean leaseServiceRunning, boolean leaseAuthenticated,
            boolean signedEnabledDashAvailable, boolean autoOpenPreferenceEnabled) {
        return explicitStartPending && hostUiResumed && rideRunning && leaseServiceRunning
            && leaseAuthenticated && signedEnabledDashAvailable && autoOpenPreferenceEnabled;
    }
}
