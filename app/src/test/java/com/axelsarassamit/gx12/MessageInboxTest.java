package com.axelsarassamit.gx12;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MessageInboxTest {
    @Test public void combinesAppsNewestFirstAndReplacesOnlySameApp() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("line", "a", "old"); inbox.put("whatsapp", "b", "wa"); inbox.put("line", "c", "new");
        assertEquals(Arrays.asList("new", "wa"), inbox.selected(new HashSet<>(Arrays.asList("line", "whatsapp"))));
    }
    @Test public void oldNotificationRemovalDoesNotEraseNewPreview() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("sms", "new", "preview"); inbox.remove("sms", "old");
        assertEquals(Collections.singletonList("preview"), inbox.selected(Collections.singleton("sms")));
        inbox.remove("sms", "new"); assertTrue(inbox.selected(Collections.singleton("sms")).isEmpty());
    }
    @Test public void deselectedAppDataIsDiscarded() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("tiktok", "a", "alert");
        assertTrue(inbox.selected(Collections.emptySet()).isEmpty());
        assertTrue(inbox.selected(Collections.singleton("tiktok")).isEmpty());
    }
    @Test public void clearRemovesAllPreviews() {
        MessageInbox<String> inbox = new MessageInbox<>(); inbox.put("line", "a", "hello"); inbox.clear();
        assertTrue(inbox.selected(Collections.singleton("line")).isEmpty());
    }
}
