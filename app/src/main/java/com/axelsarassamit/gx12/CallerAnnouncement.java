package com.axelsarassamit.gx12;
import android.content.Context;
import android.speech.tts.TextToSpeech;
/** Reads a supplied caller name once while RideDeck is visible; never reads a phone number. */
final class CallerAnnouncement {
    private static TextToSpeech speech;
    private static boolean ready;
    private static String pending;
    private static final CallAnnouncementGate gate=new CallAnnouncementGate();
    private CallerAnnouncement() { }
    static synchronized void update(Context c, GX12NotificationListener.NotificationPreview call, boolean visible) {
        if (!visible || call==null || !call.incoming || !RidePreferences.prefs(c).getBoolean("announce_callers",true)) {
            pending=null; if(speech!=null) speech.stop();
            if(call==null) gate.clear();
            return;
        }
        if(gate.contains(call.key)) return;
        String name=IncomingCallPolicy.callerName(call.callerName,call.appName);
        if(name.isEmpty()) return;
        pending=call.key;
        if(speech==null) {
            Context app=c.getApplicationContext();
            speech=new TextToSpeech(app,status->{synchronized(CallerAnnouncement.class){
                ready=status==TextToSpeech.SUCCESS && speech!=null && speech.setLanguage(VoiceLanguage.locale(app))>=0;
                if(ready) { speech.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build()); say(app); }
            }});
        } else if(ready) say(c);
    }
    private static void say(Context c) {
        GX12NotificationListener.NotificationPreview call=GX12NotificationListener.activeCall;
        if(call==null || !call.incoming || pending==null || !pending.equals(call.key) || gate.contains(pending)) return;
        String name=IncomingCallPolicy.callerName(call.callerName,call.appName);
        if(name.isEmpty()) return;
        if(speech.speak(name+" is calling",TextToSpeech.QUEUE_FLUSH,null,"incoming-caller")==TextToSpeech.SUCCESS) { gate.queued(pending); android.util.Log.i("RideDeckCalls","Caller announcement queued"); }
    }
    static synchronized void removed(String key){gate.removed(key);}
    static synchronized void close(){if(speech!=null){speech.stop();speech.shutdown();}speech=null;ready=false;pending=null;gate.clear();}
}
