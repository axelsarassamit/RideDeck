package com.axelsarassamit.gx12;

final class DashDockPresentation {
    // Shared dock-button contract for Standard and Yamaha hosts.
    static final int DOCK_BUTTON_COLOR = 0xff2c322c;
    static final int DOCK_BUTTON_CORNER_DP = 12;
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
        if (!installed) return "Dash\nUnavailable";
        if (requestMade && !freshStatus) return "Dash\nChecking";
        if (recording) return "● REC";
        if (starting) return "Dash\nStarting";
        if (!requestMade) return "Dash";
        return accepted ? "Dash\nStopped" : "Dash\nUnavailable";
    }
}
