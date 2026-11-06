package com.axelsarassamit.gx12;

import org.junit.Test;
import java.net.URI;
import java.net.URLDecoder;
import static org.junit.Assert.*;

public class BugReportTest {
    @Test public void rejectsUnstructuredEntriesAndKeepsNewest32() {
        String history = "secret token and location\nAPP_CRASH\n";
        for (int i = 0; i < 32; i++) history = BugReport.append(history, BugReport.Failure.CAMERA);
        assertEquals(32, BugReport.read(history).size());
        assertFalse(history.contains("APP_CRASH"));
        assertFalse(history.contains("secret"));
        assertTrue(BugReport.read("X".repeat(1025)).isEmpty());
    }

    @Test public void compactDraftTargetsOnlyStandardAndEncodesTheReviewedBody() throws Exception {
        String history = "APP_CRASH\n";
        for (int i = 0; i < 8; i++) history = BugReport.append(history, BugReport.Failure.SHARED_DESTINATION);
        String full = BugReport.body("0.12.32", 35, BugReport.read(history), false);
        String draft = BugReport.body("0.12.32", 35, BugReport.read(history), true);
        assertTrue(full.contains("APP_CRASH"));
        assertFalse(draft.contains("APP_CRASH"));
        assertTrue(draft.contains("Earlier entries omitted"));
        String link = BugReport.issueUrl(draft);
        URI uri = new URI(link);
        assertEquals("https", uri.getScheme());
        assertEquals("github.com", uri.getHost());
        assertEquals("/axelsarassamit/RideDeck/issues/new", uri.getPath());
        assertEquals(draft, URLDecoder.decode(uri.getRawQuery().split("&body=", 2)[1], "UTF-8"));
        assertTrue(link.length() < 1500);
        assertFalse(BugReport.body("private version label", 35, BugReport.read(null), false).contains("private"));
    }
}
