package com.axelsarassamit.gx12;

final class DashDockPresentation {
    // Shared dock-button contract for Standard and Yamaha hosts.
    static final int DOCK_BUTTON_CORNER_DP = 8;
    static final int DOCK_BUTTON_TEXT_SIZE_SP = 16;
    static final int DOCK_HEIGHT_DP = 64;
    static final int DOCK_COMPACT_HEIGHT_DP = 56;

    private DashDockPresentation() { }

    static boolean shouldStartRecordingFromControls(boolean rideActive, boolean autoRecord,
            boolean leaseAuthenticated, boolean requestAlreadyMade) {
        return rideActive && autoRecord && leaseAuthenticated && !requestAlreadyMade;
    }

    static String label(boolean installed, boolean recording, boolean starting,
            boolean requestMade, boolean accepted, boolean freshStatus) {
        if (!installed) return "Dash\n! Unavailable";
        if (requestMade && !freshStatus) return "Dash\nConnecting";
        if (recording) return "Dash\n● Recording";
        if (starting) return "Dash\nConnecting";
        if (!requestMade) return "Dash";
        return accepted ? "Dash\n■ Stopped" : "Dash\n! Unavailable";
    }
}
