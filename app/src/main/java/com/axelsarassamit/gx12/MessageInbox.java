package com.axelsarassamit.gx12;

import java.util.*;

/** One current preview per selected app. In-memory only, newest apps first. */
public final class MessageInbox<T> {
    private static final class Entry<T> {
        final String key; final T value;
        Entry(String key, T value) { this.key = key; this.value = value; }
    }
    private final LinkedHashMap<String, Entry<T>> entries = new LinkedHashMap<>();
    public synchronized void put(String app, String key, T value) {
        entries.remove(app); entries.put(app, new Entry<>(key, value));
    }
    public synchronized void remove(String app, String key) {
        Entry<T> entry = entries.get(app);
        if (entry != null && entry.key.equals(key)) entries.remove(app);
    }
    public synchronized List<T> selected(Set<String> apps) {
        entries.entrySet().removeIf(entry -> !apps.contains(entry.getKey()));
        List<T> result = new ArrayList<>();
        for (Entry<T> entry : entries.values()) result.add(entry.value);
        Collections.reverse(result); return result;
    }
    public synchronized void clear() { entries.clear(); }
}
