package com.axelsarassamit.gx12;

import android.content.Context;
import java.util.Locale;

public final class VoiceLanguage {
    private VoiceLanguage() { }
    public static String tag(Context context) {
        return RidePreferences.prefs(context).getString("voice_language", "en-US");
    }
    public static Locale locale(Context context) { return Locale.forLanguageTag(tag(context)); }
}
