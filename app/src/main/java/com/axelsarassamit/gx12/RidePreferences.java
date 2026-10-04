package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Telephony;
import java.util.*;

/** Explicit app selection. No account credentials or notification text are saved here. */
public final class RidePreferences {
    public static SharedPreferences prefs(Context context) { return context.getSharedPreferences("ride_config", 0); }
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
    public static final String[] BIKE_NAMES = {"Automatic / other Yamaha with StreetCross", "XMAX 2024 Tech MAX", "XMAX / NMAX (compatible navigation dash)", "MT-07 (2025)", "R9 (2026)", "MT-09 (2024 / 2025)", "MT-09 SP (2026)", "XSR900 (2025)", "XSR900 GP", "Tracer 9 GT+", "Niken GT", "TMAX (compatible navigation dash)"};
    public static String bikeName(Context context) {
        int choice = prefs(context).getInt("bike_profile", 1);
        return BIKE_NAMES[Math.max(0, Math.min(choice, BIKE_NAMES.length - 1))];
    }
    public static final String[] MAP_NAMES = {"Google Maps", "Waze", "Grab Driver", "LINE MAN RIDER", "Garmin StreetCross", "HERE WeGo", "Sygic", "MAPS.ME", "OsmAnd", "OsmAnd+", "Organic Maps"};
    public static final String[] MAP_PACKAGES = {"com.google.android.apps.maps", "com.waze", "com.grabtaxi.driver2", "com.linecorp.lineman.driver", "com.garmin.android.apps.streetcross", "com.here.app.maps", "com.sygic.aura", "com.mapswithme.maps.pro", "net.osmand", "net.osmand.plus", "app.organicmaps"};
    public static String selectedMap(Context context) { return prefs(context).getString("map_app", MAP_PACKAGES[0]); }
    public static String mapName(Context context) {
        String pkg = selectedMap(context);
        for (int i = 0; i < MAP_PACKAGES.length; i++) if (MAP_PACKAGES[i].equals(pkg)) return MAP_NAMES[i];
        return "Google Maps";
    }
}
