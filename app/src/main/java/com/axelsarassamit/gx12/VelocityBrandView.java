package com.axelsarassamit.gx12;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Approved Canva artwork, drawn with transparent margins and a readable light treatment. */
public final class VelocityBrandView extends View {
    private final Bitmap bitmap;
    private final Paint ink = new Paint(3);
    public VelocityBrandView(Context context) {
        super(context);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.velocity_logo);
        setContentDescription("RideDeck logo");
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!RideAppearance.isDark(getContext())) {
            // Keep the coloured accent while mapping the white ink to graphite.
            float[] colour = {-.592f,0,0,0,156, -.789f,0,0,0,209, -1.571f,0,0,0,409, 0,0,0,1,0};
            ink.setColorFilter(new ColorMatrixColorFilter(colour));
        } else ink.setColorFilter(null);
        Rect source = new Rect(17, 36, 184, 168);
        float scale = Math.min(getWidth() / 167f, getHeight() / 132f);
        float w=167*scale,h=132*scale;
        canvas.drawBitmap(bitmap,source,new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),ink);
    }
}
