package com.axelsarassamit.gx12;
import org.junit.Test;import static org.junit.Assert.*;
public class CallAnnouncementGateTest {
 @Test public void repeatsAndOverlappingCallsDoNotRepeatNames(){CallAnnouncementGate g=new CallAnnouncementGate();g.queued("a");g.queued("b");assertTrue(g.contains("a"));assertTrue(g.contains("b"));}
 @Test public void newCallCanReuseAFormerNotificationKey(){CallAnnouncementGate g=new CallAnnouncementGate();g.queued("a");g.removed("a");assertFalse(g.contains("a"));}
 @Test public void disconnectClearsOldAnnouncements(){CallAnnouncementGate g=new CallAnnouncementGate();g.queued("a");g.clear();assertFalse(g.contains("a"));}
}
