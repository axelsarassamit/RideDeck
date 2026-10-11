package com.axelsarassamit.gx12;

final class DashDockPresentation {
    private DashDockPresentation() { }

    static boolean shouldStartRecordingFromControls(boolean rideActive, boolean autoRecord,
            boolean leaseAuthenticated, boolean requestAlreadyMade) {
        return rideActive && autoRecord && leaseAuthenticated && !requestAlreadyMade;
    }

    static String label(boolean installed, boolean recording, boolean starting,
            boolean requestMade, boolean accepted) {
        if (!installed) return "Dash\nUnavailable";
        if (recording) return "● REC";
        if (starting) return "Dash\nStarting";
        if (!requestMade) return "Dash";
        return accepted ? "Dash\nStopped" : "Dash\nUnknown";
    }
}
