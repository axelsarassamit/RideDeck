package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;

/** Shared RideDeck family surfaces for cockpit and parked settings. */
public final class RideStyle {
    public static final int BACKGROUND = 0xff0c111b;
    public static final int PANEL = 0xff141d2b;
    public static final int RAISED = 0xff1c293b;
    public static final int LINE = 0xff2b3a50;
    public static final int TEXT = 0xfff4f6fb;
    public static final int MUTED = 0xffa3b2c9;
    private RideStyle() { }
    public static GradientDrawable surface(Context c, int color, int radius, boolean border) {
        float density = c.getResources().getDisplayMetrics().density;
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(radius * density);
        if (border) shape.setStroke(Math.max(1, (int) density), LINE);
        return shape;
    }
    public static RippleDrawable row(Context c) {
        return new RippleDrawable(ColorStateList.valueOf(0x286c94ff), surface(c, PANEL, 8, true), null);
    }
}
