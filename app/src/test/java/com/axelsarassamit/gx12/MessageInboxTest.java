package com.axelsarassamit.gx12;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MessageInboxTest {
    private static final class Metadata { String reply; Metadata(String reply) { this.reply = reply; } }
    @Test public void delayedReplyActionRefreshKeepsReaderIdentityAndMessageOrder() {
        MessageInbox<Metadata> inbox = new MessageInbox<>();
        Metadata original = new Metadata(null), newer = new Metadata("newer-action");
        inbox.put("line", "a", "same text", original);
        inbox.put("line", "b", "newer text", newer);
        inbox.put("line", "a", "same text", new Metadata("reply-action"), (old, updated) -> old.reply = updated.reply);
        assertEquals("reply-action", original.reply);
        assertEquals(Arrays.asList(newer, original), inbox.selected(Collections.singleton("line")));
        inbox.acknowledge("line", "a", original);
        assertEquals(Collections.singletonList(newer), inbox.selected(Collections.singleton("line")));
    }
    @Test public void metadataRefreshCannotResurrectSeenMessageOrRefreshAnotherConversation() {
        MessageInbox<Metadata> inbox = new MessageInbox<>();
        Metadata first = new Metadata(null), other = new Metadata("other-action");
        inbox.put("line", "a", "same text", first);
        inbox.put("line", "b", "same text", other);
        inbox.acknowledge("line", "a", first);
        inbox.put("line", "a", "same text", new Metadata("late-action"), (old, updated) -> old.reply = updated.reply);
        assertNull(first.reply);
        assertEquals("other-action", other.reply);
        assertEquals(Collections.singletonList(other), inbox.selected(Collections.singleton("line")));
    }
    @Test public void newMessageDoesNotCopyActionsIntoStaleReader() {
        MessageInbox<Metadata> inbox = new MessageInbox<>();
        Metadata original = new Metadata("old-action"), latest = new Metadata("latest-action");
        inbox.put("line", "a", "old text", original);
        inbox.put("line", "a", "new text", latest, (old, updated) -> old.reply = updated.reply);
        assertEquals("old-action", original.reply);
        inbox.acknowledge("line", "a", original);
        assertEquals(Collections.singletonList(latest), inbox.selected(Collections.singleton("line")));
    }
    @Test public void indicatorFollowsAllSelectedPendingMessagesUntilLastAcknowledgement() {
        MessageInbox<Object> inbox = new MessageInbox<>();
        Set<String> selected = new HashSet<>(Arrays.asList("line", "whatsapp"));
        Object first = new Object(), second = new Object();
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "first", first);
        inbox.put("whatsapp", "b", "second", second);
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
        inbox.acknowledge("whatsapp", "b", second);
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
        inbox.acknowledge("line", "a", first);
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "first", new Object());
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "new message", new Object());
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
    }
    @Test public void indicatorTracksSelectionRemovalAndClearUsingPhoneQueue() {
        MessageInbox<String> inbox = new MessageInbox<>();
        Set<String> selected = Collections.singleton("line");
        inbox.put("whatsapp", "b", "not selected");
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "pending");
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(Collections.emptySet(), true));
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "pending");
        inbox.remove("line", "a");
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "a", "new"); inbox.clear();
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
    }
    @Test public void unavailableListenerDoesNotClaimEmptyOrResurrectStaleMessages() {
        MessageInbox<String> inbox = new MessageInbox<>();
        Set<String> selected = Collections.singleton("line");
        assertEquals(MessageInbox.Indicator.UNAVAILABLE, inbox.indicator(selected, false));
        inbox.put("line", "a", "pending");
        assertEquals(MessageInbox.Indicator.UNAVAILABLE, inbox.indicator(selected, false));
        inbox.clear();
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
        inbox.put("line", "b", "fresh after reconnect");
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
    }
    @Test public void staleReaderCannotClearIndicatorForUpdatedMessage() {
        MessageInbox<Object> inbox = new MessageInbox<>();
        Set<String> selected = Collections.singleton("line");
        Object old = new Object(), latest = new Object();
        inbox.put("line", "a", "old", old);
        inbox.put("line", "a", "latest", latest);
        inbox.acknowledge("line", "a", old);
        assertEquals(Collections.singletonList(latest), inbox.selected(selected));
        assertEquals(MessageInbox.Indicator.PENDING, inbox.indicator(selected, true));
        inbox.acknowledge("line", "a", latest);
        assertEquals(MessageInbox.Indicator.CLEAR, inbox.indicator(selected, true));
    }
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
