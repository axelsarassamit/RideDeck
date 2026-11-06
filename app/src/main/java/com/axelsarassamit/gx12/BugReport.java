package com.axelsarassamit.gx12;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/** Only fixed event codes enter a report. Legacy diagnostics never leave the app through this path. */
public final class BugReport {
    static final String ISSUE_ENDPOINT = "https://github.com/axelsarassamit/RideDeck/issues/new";
    static final int MAX_EVENTS = 32;
    private static final int DRAFT_EVENTS = 8;

    public enum Failure {
        APP_CRASH, CAMERA, DASH_START, HEADSET_MIC, MAP_OPEN, NOTIFICATION_ACCESS,
        REPLY, ROUTE_SEARCH, SHARED_DESTINATION, UPDATE
    }

    static List<Failure> read(String stored) {
        List<Failure> result = new ArrayList<>();
        if (stored == null || stored.length() > MAX_EVENTS * 32) return result;
        for (String value : stored.split("\n")) {
            try { result.add(Failure.valueOf(value)); }
            catch (IllegalArgumentException ignored) { /* Unknown or corrupt entry. */ }
        }
        return new ArrayList<>(result.subList(Math.max(0, result.size() - MAX_EVENTS), result.size()));
    }

    static String append(String stored, Failure failure) {
        List<Failure> events = read(stored);
        events.add(failure);
        StringBuilder result = new StringBuilder();
        for (int i = Math.max(0, events.size() - MAX_EVENTS); i < events.size(); i++) {
            result.append(events.get(i).name()).append('\n');
        }
        return result.toString();
    }

    static String body(String version, int api, List<Failure> events, boolean compact) {
        String safeVersion = version != null && version.matches("[0-9]{1,3}(\\.[0-9]{1,3}){2}") ? version : "unknown";
        int first = Math.max(0, events.size() - (compact ? DRAFT_EVENTS : MAX_EVENTS));
        StringBuilder result = new StringBuilder("### What happened?\n\n### Steps to reproduce\n\n")
            .append("### Expected behavior\n\n### RideDeck diagnostics\n")
            .append("- RideDeck: ").append(safeVersion).append('\n')
            .append("- Android API: ").append(api).append('\n')
            .append("- Recent failure categories (oldest first):\n");
        if (events.isEmpty()) result.append("  - None recorded\n");
        if (first > 0) result.append("  - Earlier entries omitted. Use Copy full report in RideDeck if needed.\n");
        for (int i = first; i < events.size(); i++) result.append("  - ").append(events.get(i).name()).append('\n');
        return result.toString();
    }

    static String issueUrl(String draft) {
        try {
            return ISSUE_ENDPOINT + "?title=RideDeck+bug+report&body=" + URLEncoder.encode(draft, "UTF-8");
        } catch (UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }

    private BugReport() { }
}
