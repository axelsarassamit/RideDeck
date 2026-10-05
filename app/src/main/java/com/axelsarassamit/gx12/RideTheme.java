package com.axelsarassamit.gx12;
import android.content.Context;
public final class RideTheme {
    private RideTheme() { }
    public static final String[] NAMES = {"Lime", "Ice blue", "Amber", "White"};
    private static final int[] ACCENTS = {0xffc3f77a, 0xff90c8ff, 0xffffc464, 0xffeef2f6};
    public static int accent(Context context) {
        int index = RidePreferences.prefs(context).getInt("color_theme", 0);
        return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, index))];
    }
}
