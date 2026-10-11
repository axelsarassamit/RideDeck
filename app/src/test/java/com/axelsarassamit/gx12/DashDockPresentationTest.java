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
        assertEquals("Dash\nUnavailable", DashDockPresentation.label(false, false, false, false, false));
    }

    @Test public void showsDashWhenIdleAndStartingWhenCameraLaunches() {
        assertEquals("Dash", DashDockPresentation.label(true, false, false, false, false));
        assertEquals("Dash\nStarting", DashDockPresentation.label(true, false, true, true, true));
    }

    @Test public void recordingHasRedRecLabelAndStoppedOrUnknownStateIsClear() {
        assertEquals("● REC", DashDockPresentation.label(true, true, false, true, true));
        assertEquals("Dash\nStopped", DashDockPresentation.label(true, false, false, true, true));
        assertEquals("Dash\nUnknown", DashDockPresentation.label(true, false, false, true, false));
    }
}
