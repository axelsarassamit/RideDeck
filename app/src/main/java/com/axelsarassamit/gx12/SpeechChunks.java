package com.axelsarassamit.gx12;

import java.util.ArrayList;
import java.util.List;

/** Keep each speech request below the engine limit without splitting a surrogate pair. */
public final class SpeechChunks {
    private SpeechChunks() { }
    public static List<String> split(String text, int limit) {
        if (limit < 2) throw new IllegalArgumentException("Speech limit is too small");
        List<String> chunks = new ArrayList<>();
        for (int offset = 0; offset < text.length();) {
            int end = Math.min(text.length(), offset + limit);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            chunks.add(text.substring(offset, end)); offset = end;
        }
        return chunks;
    }
}
