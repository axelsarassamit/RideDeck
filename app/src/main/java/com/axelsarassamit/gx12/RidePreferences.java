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
    public static final String[] BIKE_NAMES = {"Automatic / other Yamaha with StreetCross", "XMAX 2024 Tech MAX", "XMAX / NMAX (compatible navigation dash)", "MT-07 (2025)", "R9 (2026)", "MT-09 (2024 / 2025)", "MT-09 SP (2026)", "XSR900 (2025)", "XSR900 GP", "Tracer 9 GT+", "Niken GT", "TMAX (compatible navigation dash)", "Other bike / no compatible display"};
    public static String bikeName(Context context) {
        int choice = prefs(context).getInt("bike_profile", 1);
        return BIKE_NAMES[Math.max(0, Math.min(choice, BIKE_NAMES.length - 1))];
    }
    public static final String[] BIKE_MAP_SIZE_NAMES = {"Balanced - more map space", "More map - smaller labels", "Larger labels - less map space"};
    public static final int[] BIKE_MAP_DENSITIES = {192, 160, 240};
    public static int bikeMapDensity(Context context) {
        int choice = Math.max(0, Math.min(2, prefs(context).getInt("bike_map_size", 0)));
        return BIKE_MAP_DENSITIES[choice];
    }
    public static final String[] MAP_NAMES = {"Google Maps", "Waze", "Grab Driver", "LINE MAN RIDER", "Garmin StreetCross", "HERE WeGo", "Sygic", "MAPS.ME", "OsmAnd", "OsmAnd+", "Organic Maps"};
    public static final String[] MAP_PACKAGES = {"com.google.android.apps.maps", "com.waze", "com.grabtaxi.driver2", "com.linecorp.lineman.driver", "com.garmin.android.apps.streetcross", "com.here.app.maps", "com.sygic.aura", "com.mapswithme.maps.pro", "net.osmand", "net.osmand.plus", "app.organicmaps"};
    public static String selectedMap(Context context) { return BuildConfig.YAMAHA ? "maplibre" : prefs(context).getString("map_app", MAP_PACKAGES[0]); }
    public static boolean automaticMap(Context context) { return BuildConfig.YAMAHA && prefs(context).getBoolean("map_auto", true); }
    public static boolean manualPhoneMap(Context context) { return prefs(context).getBoolean("map_manual_phone", true); }
    public static String mapName(Context context) {
        if (BuildConfig.YAMAHA) return "MapLibre";
        String pkg = selectedMap(context);
        for (int i = 0; i < MAP_PACKAGES.length; i++) if (MAP_PACKAGES[i].equals(pkg)) return MAP_NAMES[i];
        return "Google Maps";
    }
}
