package com.axelsarassamit.gx12;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Deliberate artwork fallback; never pretends to be the current album cover. */
public final class MusicArtworkDrawable extends Drawable {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas c) {
        Rect r = getBounds(); float s = Math.min(r.width(), r.height());
        c.save(); c.translate(r.centerX() - s/2, r.centerY() - s/2); c.scale(s/100, s/100);
        ink.setStyle(Paint.Style.FILL); ink.setColor(0xff22334d);
        c.drawRoundRect(0, 0, 100, 100, 8, 8, ink);
        ink.setStyle(Paint.Style.STROKE); ink.setColor(0xff3a5275); ink.setStrokeWidth(1);
        for (int radius : new int[]{25, 34, 43, 52}) c.drawCircle(50, 50, radius, ink);
        ink.setColor(0xffd8e5ff); ink.setStrokeWidth(4); ink.setStrokeCap(Paint.Cap.ROUND);
        Path note = new Path(); note.moveTo(40, 67); note.lineTo(40, 36); note.lineTo(66, 30); note.lineTo(66, 60); c.drawPath(note, ink);
        ink.setStyle(Paint.Style.FILL); c.drawOval(28, 61, 42, 71, ink); c.drawOval(54, 54, 68, 64, ink);
        c.restore();
    }
    @Override public void setAlpha(int alpha) { ink.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { ink.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
