package com.axelsarassamit.gx12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.Locale;

/** Display-only playback timeline. Unknown metadata never becomes fake progress. */
public final class PlaybackTimelineView extends View {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long position, duration;
    public PlaybackTimelineView(Context c) { super(c); setVisibility(INVISIBLE); }
    public void update(long position, long duration) {
        this.duration = Math.max(0, duration);
        this.position = Math.max(0, Math.min(position, this.duration));
        setVisibility(this.duration > 0 ? VISIBLE : INVISIBLE);
        setContentDescription(this.duration > 0 ? "Playback " + time(this.position) + " of " + time(this.duration) : "Playback progress unavailable");
        invalidate();
    }
    private String time(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.ROOT, "%d:%02d", seconds/60, seconds%60);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); if (duration <= 0) return;
        float d = getResources().getDisplayMetrics().density;
        ink.setStyle(Paint.Style.STROKE); ink.setStrokeCap(Paint.Cap.ROUND); ink.setStrokeWidth(2*d);
        ink.setColor(RideStyle.LINE); c.drawLine(2*d, 4*d, getWidth()-2*d, 4*d, ink);
        ink.setColor(RideTheme.accent(getContext()));
        c.drawLine(2*d, 4*d, 2*d+(getWidth()-4*d)*position/duration, 4*d, ink);
        ink.setStyle(Paint.Style.FILL); ink.setTypeface(android.graphics.Typeface.create("sans-serif",0));
        ink.setTextSize(12*getResources().getDisplayMetrics().scaledDensity); ink.setColor(RideStyle.MUTED);
        ink.setTextAlign(Paint.Align.LEFT); c.drawText(time(position), 0, getHeight()-3*d, ink);
        ink.setTextAlign(Paint.Align.RIGHT); c.drawText(time(duration), getWidth(), getHeight()-3*d, ink);
    }
}
