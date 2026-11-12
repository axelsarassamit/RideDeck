package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;

/** Shared RideDeck family surfaces for cockpit and parked settings. */
public final class RideStyle {
    public static int BACKGROUND = 0xff081422;
    public static int PANEL = 0xff101c2e;
    public static int RAISED = 0xff1a2a40;
    public static int LINE = 0xff2d405a;
    public static int TEXT = 0xfff6f8fc;
    public static int MUTED = 0xffa0b0c8;
    public static int DANGER = 0xffff8585;
    static void apply(boolean dark) {
        BACKGROUND=AppearancePalette.background(dark); PANEL=AppearancePalette.panel(dark);
        RAISED=AppearancePalette.raised(dark); LINE=AppearancePalette.line(dark);
        DANGER=AppearancePalette.danger(dark);
        TEXT=AppearancePalette.text(dark); MUTED=AppearancePalette.muted(dark);
    }
    private RideStyle() { }
    /** Mirror the complete action rail together, preserving the same order in both hosts. */
    public static void mirrorActions(LinearLayout rail, boolean mirrored) {
        if (!mirrored) return;
        ArrayList<View> views = new ArrayList<>();
        for(int i=0;i<rail.getChildCount();i++) views.add(rail.getChildAt(i));
        rail.removeAllViews(); Collections.reverse(views);
        for(View view:views) rail.addView(view);
    }
    /** Family action style. Rows and tiles share metrics; callers own their layout and action. */
    public static void action(Button button, boolean row) {
        Context c = button.getContext();
        float density = c.getResources().getDisplayMetrics().density;
        button.setAllCaps(false);
        button.setTextSize(18);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(Math.round(64 * density)); button.setMinimumHeight(Math.round(64 * density));
        button.setGravity(Gravity.CENTER_VERTICAL | (row ? Gravity.START : Gravity.CENTER_HORIZONTAL));
        button.setPadding(Math.round((row ? 16 : 8) * density), Math.round(10 * density), Math.round((row ? 16 : 8) * density), Math.round(10 * density));
        button.setBackgroundTintList(null); button.setStateListAnimator(null); button.setElevation(0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x286c94ff), surface(c, RAISED, 14, true), null));
        button.setTextColor(new ColorStateList(new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}}, new int[] {MUTED, TEXT}));
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
