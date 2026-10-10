package com.axelsarassamit.gx12;

final class RideSessionState {
    static final String PREFERENCES_NAME = "MainActivity";
    static final String PREF_RUNNING = "ride_running";
    static final String PREF_ELAPSED = "ride_elapsed";
    static final String PREF_BASE_ELAPSED = "ride_base_elapsed";
    static final String PREF_STARTED_ELAPSED = "ride_started_elapsed";

    private RideSessionState() { }

    static boolean shouldResume(boolean persistedRunning, boolean leaseServiceAlive) {
        return persistedRunning && leaseServiceAlive;
    }

    static long elapsed(long accumulatedMillis, long segmentStartedAt, long now) {
        if (now < segmentStartedAt) return accumulatedMillis;
        return accumulatedMillis + (now - segmentStartedAt);
    }
}