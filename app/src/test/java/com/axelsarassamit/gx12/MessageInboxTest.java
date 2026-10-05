package com.axelsarassamit.gx12;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MessageInboxTest {
    @Test public void combinesPendingNotificationsNewestFirst() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("line", "a", "old"); inbox.put("whatsapp", "b", "wa"); inbox.put("line", "c", "new");
        assertEquals(Arrays.asList("new", "wa", "old"), inbox.selected(new HashSet<>(Arrays.asList("line", "whatsapp"))));
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
    @Test public void seenAdvancesAndDoesNotReturnOnRefresh() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("line", "a", "older"); inbox.put("line", "b", "latest");
        inbox.acknowledge("line", "b", "latest");
        inbox.put("line", "b", "latest");
        assertEquals(Collections.singletonList("older"), inbox.selected(Collections.singleton("line")));
        inbox.put("line", "b", "new content");
        assertEquals(Arrays.asList("new content", "older"), inbox.selected(Collections.singleton("line")));
    }
    @Test public void staleReaderDoesNotRemoveUpdatedMessage() {
        MessageInbox<String> inbox = new MessageInbox<>();
        inbox.put("line", "a", "old"); inbox.put("line", "a", "new");
        inbox.acknowledge("line", "a", "old");
        assertEquals(Collections.singletonList("new"), inbox.selected(Collections.singleton("line")));
    }
    @Test public void clearRemovesAllPreviews() {
        MessageInbox<String> inbox = new MessageInbox<>(); inbox.put("line", "a", "hello"); inbox.clear();
        assertTrue(inbox.selected(Collections.singleton("line")).isEmpty());
    }
}
