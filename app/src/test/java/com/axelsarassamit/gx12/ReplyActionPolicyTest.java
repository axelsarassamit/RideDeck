package com.axelsarassamit.gx12;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplyActionPolicyTest {
    @Test public void explicitReplyWinsOverLegacyFreeFormAction() {
        assertTrue(ReplyActionPolicy.rank(1, true, true) > ReplyActionPolicy.rank(0, true, true));
        assertTrue(ReplyActionPolicy.rank(0, true, true) > 0);
    }
    @Test public void nonReplyAndUnsendableActionsAreExcluded() {
        for (int semantic = 2; semantic <= 12; semantic++) assertEquals(0, ReplyActionPolicy.rank(semantic, true, true));
        assertEquals(0, ReplyActionPolicy.rank(1, false, true));
        assertEquals(0, ReplyActionPolicy.rank(1, true, false));
    }
}
