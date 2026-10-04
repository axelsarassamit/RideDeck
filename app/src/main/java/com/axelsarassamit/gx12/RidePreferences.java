package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Telephony;
import java.util.*;

/** Explicit app selection. No account credentials or notification text are saved here. */
public final class RidePreferences {
    public static SharedPreferences prefs(Context context) { return context.getSharedPreferences("ride_config", 0); }
    public static final String[] MESSAGE_NAMES = {"WhatsApp", "WhatsApp Business", "LINE", "Messenger", "TikTok", "SMS"};
    public static String[] messagePackages(Context context) {
        String sms = Telephony.Sms.getDefaultSmsPackage(context);
        String tiktok = "com.zhiliaoapp.musically";
        try { context.getPackageManager().getPackageInfo("com.ss.android.ugc.trill", 0); tiktok = "com.ss.android.ugc.trill"; }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        return new String[]{"com.whatsapp", "com.whatsapp.w4b", "jp.naver.line.android", "com.facebook.orca", tiktok, sms == null ? "" : sms};
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
    public static final String[] MAP_NAMES = {"Google Maps", "Waze", "Grab Driver", "LINE MAN RIDER", "Garmin StreetCross"};
    public static final String[] MAP_PACKAGES = {"com.google.android.apps.maps", "com.waze", "com.grabtaxi.driver2", "com.linecorp.lineman.driver", "com.garmin.android.apps.streetcross"};
    public static String selectedMap(Context context) { return prefs(context).getString("map_app", MAP_PACKAGES[0]); }
    public static String mapName(Context context) {
        String pkg = selectedMap(context);
        for (int i = 0; i < MAP_PACKAGES.length; i++) if (MAP_PACKAGES[i].equals(pkg)) return MAP_NAMES[i];
        return "Google Maps";
    }
}
