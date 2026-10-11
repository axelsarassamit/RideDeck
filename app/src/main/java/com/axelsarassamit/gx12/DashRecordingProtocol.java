package com.axelsarassamit.gx12;

/** Pure decisions for authenticated Dash status polling and lifecycle recovery. */
final class DashRecordingProtocol {
    private DashRecordingProtocol() { }

    static boolean isTrustedSender(int sendingUid, int installedDashUid) {
        return installedDashUid > 0 && sendingUid == installedDashUid;
    }

    static boolean hasValidReply(boolean protocolMatches, boolean acceptedPresent, boolean accepted,
            boolean recordingPresent, boolean startingPresent, boolean messagePresent,
            boolean requireStarting) {
        if (!protocolMatches || !acceptedPresent || !messagePresent) return false;
        return !accepted || (recordingPresent && (!requireStarting || startingPresent));
    }

    static boolean shouldPoll(boolean requestMade, boolean connected, boolean requestPending,
            boolean previewVisible, boolean statusVisible) {
        return requestMade && connected && !requestPending && (previewVisible || statusVisible);
    }

    static boolean shouldReconnect(boolean requestMade, boolean statusVisible, boolean bindingActive) {
        return requestMade && statusVisible && !bindingActive;
    }

    static boolean shouldSendStart(boolean visibleUserGesture, boolean statusOnlyConnection) {
        return visibleUserGesture && !statusOnlyConnection;
    }

    static boolean shouldWaitForStartFocus(boolean gesturePending, boolean hostAlive,
            boolean previewAttached, boolean startSent, long now, long deadline) {
        return gesturePending && hostAlive && previewAttached && !startSent && now < deadline;
    }

    static boolean shouldSendVisibleStart(boolean gesturePending, boolean hostAlive,
            boolean previewAttached, boolean previewVisible, boolean serviceConnected,
            boolean startSent, long now, long deadline) {
        return shouldWaitForStartFocus(gesturePending, hostAlive, previewAttached, startSent, now, deadline)
            && previewVisible && serviceConnected;
    }

    static boolean isCurrentResponse(long requestId, long pendingRequestId, int requestWhat,
            int pendingWhat, boolean requestPending, int responseWhat) {
        return requestPending && requestId != 0L && requestId == pendingRequestId
            && requestWhat == pendingWhat && requestWhat == responseWhat;
    }
}
