package com.axelsarassamit.gx12;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DashRecordingProtocolTest {
    @Test public void acceptsOnlyTheInstalledDashUid() {
        assertTrue(DashRecordingProtocol.isTrustedSender(10234, 10234));
        assertFalse(DashRecordingProtocol.isTrustedSender(10235, 10234));
        assertFalse(DashRecordingProtocol.isTrustedSender(10234, -1));
    }

    @Test public void acceptedCaptureStateRequiresAllActualCameraFields() {
        assertTrue(DashRecordingProtocol.hasValidReply(true, true, true, true, true, true, true));
        assertFalse(DashRecordingProtocol.hasValidReply(true, true, true, true, false, true, true));
        assertFalse(DashRecordingProtocol.hasValidReply(false, true, true, true, true, true, true));
        assertFalse(DashRecordingProtocol.hasValidReply(true, true, true, false, true, true, true));
    }

    @Test public void rejectedLeaseMayReturnAnEnvelopeWithoutCameraState() {
        assertTrue(DashRecordingProtocol.hasValidReply(true, true, false, false, false, true, true));
        assertFalse(DashRecordingProtocol.hasValidReply(true, true, false, false, false, false, true));
    }

    @Test public void previewDismissalContinuesStatusPollingAndHiddenUiStopsIt() {
        assertTrue(DashRecordingProtocol.shouldPoll(true, true, false, false, true));
        assertFalse(DashRecordingProtocol.shouldPoll(true, true, false, false, false));
        assertFalse(DashRecordingProtocol.shouldPoll(true, true, true, false, true));
        assertFalse(DashRecordingProtocol.shouldPoll(false, true, false, true, true));
    }

    @Test public void statusRecoveryNeverRestartsRecording() {
        assertTrue(DashRecordingProtocol.shouldSendStart(true, false));
        assertFalse(DashRecordingProtocol.shouldSendStart(false, false));
        assertFalse(DashRecordingProtocol.shouldSendStart(true, true));
    }

    @Test public void timedOutRequestsRejectLateRepliesFromEarlierPolls() {
        assertTrue(DashRecordingProtocol.isCurrentResponse(5L, 5L, 3, 3, true, 3));
        assertFalse(DashRecordingProtocol.isCurrentResponse(4L, 5L, 3, 3, true, 3));
        assertFalse(DashRecordingProtocol.isCurrentResponse(5L, 0L, 3, 0, false, 3));
        assertFalse(DashRecordingProtocol.isCurrentResponse(5L, 5L, 2, 3, true, 2));
    }

    @Test public void activityRecoveryRebindsOnlyForAVisiblePreviouslyRequestedSession() {
        assertTrue(DashRecordingProtocol.shouldReconnect(true, true, false));
        assertFalse(DashRecordingProtocol.shouldReconnect(true, false, false));
        assertFalse(DashRecordingProtocol.shouldReconnect(false, true, false));
        assertFalse(DashRecordingProtocol.shouldReconnect(true, true, true));
    }
}
