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
        assertEquals("Dash\n! Unavailable", DashDockPresentation.label(false, false, false, false, false, false));
    }

    @Test public void showsDashWhenIdleAndStartingWhenCameraLaunches() {
        assertEquals("Dash", DashDockPresentation.label(true, false, false, false, false, false));
        assertEquals("Dash\nConnecting", DashDockPresentation.label(true, false, true, true, true, true));
    }

    @Test public void recordingHasRedRecLabelAndStoppedOrUnavailableStateIsClear() {
        assertEquals("Dash\n● Recording", DashDockPresentation.label(true, true, false, true, true, true));
        assertEquals("Dash\n■ Stopped", DashDockPresentation.label(true, false, false, true, true, true));
        assertEquals("Dash\n! Unavailable", DashDockPresentation.label(true, false, false, true, false, true));
    }

    @Test public void onlyFreshStatusShowsRecording() {
        assertEquals("Dash\nConnecting", DashDockPresentation.label(true, true, false, true, true, false));
        assertEquals("Dash\n● Recording", DashDockPresentation.label(true, true, false, true, true, true));
        assertEquals("Dash\n■ Stopped", DashDockPresentation.label(true, false, false, true, true, true));
    }

    @Test public void dockButtonContractKeepsSharedSizingAndShape() {
        assertEquals(8, DashDockPresentation.DOCK_BUTTON_CORNER_DP);
        assertEquals(16, DashDockPresentation.DOCK_BUTTON_TEXT_SIZE_SP);
        assertEquals(56, DashDockPresentation.DOCK_COMPACT_HEIGHT_DP);
        assertEquals(64, DashDockPresentation.DOCK_HEIGHT_DP);
    }
}
