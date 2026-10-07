package com.axelsarassamit.gx12;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class SpeechChunksTest {
    @Test public void longMessageIsCompleteAndBounded() {
        String text = "message ".repeat(2500);
        List<String> chunks = SpeechChunks.split(text, 3999);
        assertEquals(text, String.join("", chunks));
        for (String chunk : chunks) assertTrue(chunk.length() <= 3999);
    }
    @Test public void emojiIsNotSplitAtBoundary() {
        List<String> chunks = SpeechChunks.split("abc\uD83D\uDE00xyz", 4);
        assertEquals("abc", chunks.get(0));
        assertEquals("abc\uD83D\uDE00xyz", String.join("", chunks));
    }
    @Test public void emptyTextHasNoChunks() { assertTrue(SpeechChunks.split("", 4).isEmpty()); }
}
