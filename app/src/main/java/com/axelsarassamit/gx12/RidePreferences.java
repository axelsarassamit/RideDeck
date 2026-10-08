package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Telephony;
import java.util.*;

/** Explicit app selection. No account credentials or notification text are saved here. */
public final class RidePreferences {
    public static SharedPreferences prefs(Context context) { return context.getSharedPreferences("ride_config", 0); }
    public static final String[] MUSIC_NAMES = {"Spotify", "YouTube Music", "Apple Music", "Amazon Music", "Deezer", "TIDAL", "VLC", "Poweramp", "Samsung Music", "Musicolet"};
    public static final String[] MUSIC_PACKAGES = {"com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music", "com.amazon.mp3", "deezer.android.app", "com.aspiro.tidal", "org.videolan.vlc", "com.maxmpz.audioplayer", "com.sec.android.app.music", "in.krosbits.musicolet"};
    public static String selectedMusic(Context context) { return prefs(context).getString("music_app", MUSIC_PACKAGES[0]); }
    public static String musicName(Context context) {
        String pkg = selectedMusic(context);
        for (int i = 0; i < MUSIC_PACKAGES.length; i++) if (pkg.equals(MUSIC_PACKAGES[i])) return MUSIC_NAMES[i];
        try { return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception ignored) { return "Music player"; }
    }
    public static final String[] MESSAGE_NAMES = {"WhatsApp", "WhatsApp Business", "LINE", "Messenger", "TikTok", "SMS", "Telegram", "Signal", "Instagram", "Viber", "Discord", "WeChat"};
    public static String[] messagePackages(Context context) {
        String sms = Telephony.Sms.getDefaultSmsPackage(context);
        String tiktok = "com.zhiliaoapp.musically";
        try { context.getPackageManager().getPackageInfo("com.ss.android.ugc.trill", 0); tiktok = "com.ss.android.ugc.trill"; }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        return new String[]{"com.whatsapp", "com.whatsapp.w4b", "jp.naver.line.android", "com.facebook.orca", tiktok, sms == null ? "" : sms, "org.telegram.messenger", "org.thoughtcrime.securesms", "com.instagram.android", "com.viber.voip", "com.discord", "com.tencent.mm"};
    }
    public static Set<String> selectedMessages(Context context) {
        Set<String> selected = new HashSet<>(prefs(context).getStringSet("message_apps", new HashSet<>(Arrays.asList("com.whatsapp", "com.whatsapp.w4b"))));
        if (selected.contains("com.zhiliaoapp.musically") || selected.contains("com.ss.android.ugc.trill")) {
            selected.add("com.zhiliaoapp.musically"); selected.add("com.ss.android.ugc.trill");
        }
        return selected;
    }
    public static String appName(Context context, String pkg) {
        if (pkg.equals("com.zhiliaoapp.musically") || pkg.equals("com.ss.android.ugc.trill")) return "TikTok";
        String[] packages = messagePackages(context);
        for (int i = 0; i < packages.length; i++) if (packages[i].equals(pkg)) return MESSAGE_NAMES[i];
        return "Messages";
    }
    public static final String[] MAP_NAMES = {"Google Maps", "Waze", "Grab Driver", "LINE MAN RIDER", "HERE WeGo", "Sygic", "MAPS.ME", "OsmAnd", "OsmAnd+", "Organic Maps"};
    public static final String[] MAP_PACKAGES = {"com.google.android.apps.maps", "com.waze", "com.grabtaxi.driver2", "com.linecorp.lineman.driver", "com.here.app.maps", "com.sygic.aura", "com.mapswithme.maps.pro", "net.osmand", "net.osmand.plus", "app.organicmaps"};
    public static String selectedMap(Context context) {
        String selected = prefs(context).getString("map_app", MAP_PACKAGES[0]);
        return Arrays.asList(MAP_PACKAGES).contains(selected) ? selected : MAP_PACKAGES[0];
    }
    public static String mapName(Context context) {
        String pkg = selectedMap(context);
        for (int i = 0; i < MAP_PACKAGES.length; i++) if (MAP_PACKAGES[i].equals(pkg)) return MAP_NAMES[i];
        return "Google Maps";
    }
}
