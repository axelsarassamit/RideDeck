package com.axelsarassamit.gx12;
/** One announcement per ringing notification, including overlapping calls. */
final class CallAnnouncementGate {
 private final java.util.Set<String> queued=new java.util.HashSet<>();
 boolean contains(String key){return queued.contains(key);}
 void queued(String key){queued.add(key);}
 void removed(String key){queued.remove(key);}
 void clear(){queued.clear();}
}
