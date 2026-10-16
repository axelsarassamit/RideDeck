package com.axelsarassamit.gx12;
import android.content.Context;
public final class RideTheme {
    private RideTheme() { }
    public static final String[] NAMES = {"Lime", "Ice blue", "Amber", "White", "RideDeck blue"};
    public static int accent(Context context) {
        return AppearancePalette.accent(RideAppearance.accentIndex(context),RideAppearance.isDark(context));
    }
}
