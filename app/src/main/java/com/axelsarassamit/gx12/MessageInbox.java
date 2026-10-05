package com.axelsarassamit.gx12;

import java.util.*;

/** Pending notification previews, newest first. All data stays in memory. */
public final class MessageInbox<T> {
    private static final class Entry<T> {
        final String app, key, fingerprint; final T value;
        Entry(String app, String key, String fingerprint, T value) {
            this.app = app; this.key = key; this.fingerprint = fingerprint; this.value = value;
        }
    }
    private final LinkedHashMap<String, Entry<T>> entries = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> seen = new LinkedHashMap<>();
    private String id(String app, String key) { return app + "\n" + key; }
    public synchronized void put(String app, String key, T value) {
        put(app, key, String.valueOf(value), value);
    }
    public synchronized void put(String app, String key, String fingerprint, T value) {
        String id = id(app, key);
        if (fingerprint.equals(seen.get(id))) return;
        Entry<T> old = entries.get(id);
        if (old != null && old.fingerprint.equals(fingerprint)) return;
        entries.remove(id); entries.put(id, new Entry<>(app, key, fingerprint, value));
        if (entries.size() > 100) entries.remove(entries.keySet().iterator().next());
    }
    public synchronized void remove(String app, String key) { entries.remove(id(app, key)); }
    public synchronized void acknowledge(String app, String key, T value) {
        String id = id(app, key); Entry<T> entry = entries.get(id);
        if (entry == null || entry.value != value) return;
        seen.put(id, entry.fingerprint); entries.remove(id);
        if (seen.size() > 500) seen.remove(seen.keySet().iterator().next());
    }
    public synchronized List<T> selected(Set<String> apps) {
        entries.entrySet().removeIf(entry -> !apps.contains(entry.getValue().app));
        List<T> result = new ArrayList<>();
        for (Entry<T> entry : entries.values()) result.add(entry.value);
        Collections.reverse(result); return result;
    }
    public synchronized void clear() { entries.clear(); seen.clear(); }
}
