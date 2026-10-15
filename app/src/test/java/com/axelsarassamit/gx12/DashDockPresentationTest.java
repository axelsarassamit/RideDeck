package com.axelsarassamit.gx12;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DashDockPresentationTest {
    @Test public void startsOnlyFromActiveAuthenticatedRideControls() {
        assertEquals(true, DashDockPresentation.shouldStartRecordingFromControls(true, true, true, false));
        assertEquals(false, DashDockPresentation.shouldStartRecordingFromControls(false, true, true, false));
        assertEquals(false, DashDockPresentation.shouldStartRecordingFromControls(true, false, true, false));
        assertEquals(false, DashDockPresentation.shouldStartRecordingFromControls(true, true, false, false));
        assertEquals(false, DashDockPresentation.shouldStartRecordingFromControls(true, true, true, true));
    }

    @Test public void showsUnavailableWhenDashIsNotInstalled() {
        assertEquals("Dash\nUnavailable", DashDockPresentation.label(false, false, false, false, false, false));
    }

    @Test public void showsDashWhenIdleAndStartingWhenCameraLaunches() {
        assertEquals("Dash", DashDockPresentation.label(true, false, false, false, false, false));
        assertEquals("Dash\nStarting", DashDockPresentation.label(true, false, true, true, true, true));
    }

    @Test public void recordingHasRedRecLabelAndStoppedOrUnavailableStateIsClear() {
        assertEquals("● REC", DashDockPresentation.label(true, true, false, true, true, true));
        assertEquals("Dash\nStopped", DashDockPresentation.label(true, false, false, true, true, true));
        assertEquals("Dash\nUnavailable", DashDockPresentation.label(true, false, false, true, false, true));
    }

    @Test public void onlyFreshStatusShowsRecording() {
        assertEquals("Dash\nChecking", DashDockPresentation.label(true, true, false, true, true, false));
        assertEquals("● REC", DashDockPresentation.label(true, true, false, true, true, true));
        assertEquals("Dash\nStopped", DashDockPresentation.label(true, false, false, true, true, true));
    }

    @Test public void dockButtonContractKeepsSharedSizingAndShape() {
        assertEquals(0xff1c293b, DashDockPresentation.DOCK_BUTTON_COLOR);
        assertEquals(8, DashDockPresentation.DOCK_BUTTON_CORNER_DP);
        assertEquals(16, DashDockPresentation.DOCK_BUTTON_TEXT_SIZE_SP);
        assertEquals(56, DashDockPresentation.DOCK_COMPACT_HEIGHT_DP);
        assertEquals(64, DashDockPresentation.DOCK_HEIGHT_DP);
    }
}
