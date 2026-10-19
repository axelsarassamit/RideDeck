package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;

/** Shared RideDeck family surfaces for cockpit and parked settings. */
public final class RideStyle {
    public static int BACKGROUND = 0xff0c111b;
    public static int PANEL = 0xff141d2b;
    public static int RAISED = 0xff1c293b;
    public static int LINE = 0xff2b3a50;
    public static int TEXT = 0xfff4f6fb;
    public static int MUTED = 0xffa3b2c9;
    public static int DANGER = 0xffff8585;
    static void apply(boolean dark) {
        BACKGROUND=AppearancePalette.background(dark); PANEL=AppearancePalette.panel(dark);
        RAISED=AppearancePalette.raised(dark); LINE=AppearancePalette.line(dark);
        DANGER=AppearancePalette.danger(dark);
        TEXT=AppearancePalette.text(dark); MUTED=AppearancePalette.muted(dark);
    }
    private RideStyle() { }
    /** Mirror the complete action rail together, preserving the same order in both hosts. */
    public static void mirrorActions(android.widget.LinearLayout rail, boolean mirrored) {
        if (!mirrored) return;
        java.util.ArrayList<android.view.View> views = new java.util.ArrayList<>();
        for(int i=0;i<rail.getChildCount();i++) views.add(rail.getChildAt(i));
        rail.removeAllViews(); java.util.Collections.reverse(views);
        for(android.view.View view:views) rail.addView(view);
    }
    public static GradientDrawable surface(Context c, int color, int radius, boolean border) {
        float density = c.getResources().getDisplayMetrics().density;
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius((radius == 8 ? 14 : radius) * density);
        if (border) shape.setStroke(Math.max(1, (int) density), LINE);
        return shape;
    }
    public static RippleDrawable row(Context c) {
        return new RippleDrawable(ColorStateList.valueOf(0x286c94ff), surface(c, PANEL, 8, true), null);
    }
}
