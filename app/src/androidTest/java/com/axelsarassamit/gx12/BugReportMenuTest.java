package com.axelsarassamit.gx12;

import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import static org.junit.Assert.*;

public class BugReportMenuTest {
    @Test public void helpShowsReviewedReportWithoutOpeningGitHub() {
        try (ActivityScenario<SetupActivity> scenario = ActivityScenario.launch(SetupActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.getPackageName().endsWith(".preview"));
                SharedSetupTest.click(activity, "Help & app");
                assertFalse(SharedSetupTest.has(activity, "View and share diagnostic log"));
                SharedSetupTest.click(activity, "Review bug report");
                assertTrue(SharedSetupTest.has(activity, "Report on GitHub"));
                assertTrue(SharedSetupTest.has(activity, "Copy full report"));
                SharedSetupTest.click(activity, "Close");
            });
        }
    }
}
