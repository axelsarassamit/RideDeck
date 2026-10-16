package com.axelsarassamit.gx12;
import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import java.util.function.BooleanSupplier;
/** A brief, interruptible first-launch reveal. The working screen is already underneath. */
public final class StartupReveal {
    private static boolean shown;
    private StartupReveal() { }
    public static void show(Activity a, Bundle saved, String edition, int background, int accent, int text, BooleanSupplier interrupt) {
        Intent intent=a.getIntent();
        android.view.accessibility.AccessibilityManager access=a.getSystemService(android.view.accessibility.AccessibilityManager.class);
        if(shown || saved!=null || !Intent.ACTION_MAIN.equals(intent.getAction()) || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return;
        shown=true;
        if(!ValueAnimator.areAnimatorsEnabled() || (access!=null && access.isTouchExplorationEnabled()) || interrupt.getAsBoolean()) return;
        ViewGroup decor=(ViewGroup)a.getWindow().getDecorView();
        BrandView view=new BrandView(a,edition,background,accent,text);view.setContentDescription("RideDeck "+edition);view.setClickable(true);
        ValueAnimator animation=ValueAnimator.ofFloat(0,1);animation.setDuration(720);
        Runnable remove=()->{animation.cancel();if(view.getParent()==decor)decor.removeView(view);};
        view.setOnClickListener(v->remove.run());
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){animation.cancel();}});
        decor.addView(view,new ViewGroup.LayoutParams(-1,-1));
        animation.addUpdateListener(frame->{if(a.isFinishing() || a.isDestroyed() || interrupt.getAsBoolean()){remove.run();return;}view.progress=(float)frame.getAnimatedValue();view.invalidate();});
        animation.addListener(new android.animation.AnimatorListenerAdapter(){public void onAnimationEnd(android.animation.Animator animator){if(view.getParent()==decor)decor.removeView(view);}});
        animation.start();
    }
    private static final class BrandView extends View {
        final String edition;final int background,accent,text;final Paint paint=new Paint(3);float progress;
        final Path[] roads={new Path(),new Path(),new Path()};
        BrandView(Activity a,String edition,int background,int accent,int text){super(a);this.edition=edition;this.background=background;this.accent=accent;this.text=text;
            roads[0].moveTo(23,28);roads[0].lineTo(64,28);roads[0].cubicTo(90,28,90,60,64,60);roads[0].lineTo(54,60);roads[0].lineTo(35,85);
            roads[1].moveTo(23,40);roads[1].lineTo(62,40);roads[1].cubicTo(76,40,76,49,62,49);roads[1].lineTo(49,49);roads[1].lineTo(22,85);
            roads[2].moveTo(61,60);roads[2].lineTo(85,85);
        }
        protected void onDraw(Canvas c){
            float d=getResources().getDisplayMetrics().density;
            c.drawColor(background);float reveal=Math.min(1,progress/0.66f);float opacity=progress>0.82f?(1-progress)/0.18f:1;
            c.save();c.translate(getWidth()/2f-54*1.7f*d,getHeight()/2f-112*d);c.scale(1.7f*d,1.7f*d);
            paint.setColor(accent);paint.setAlpha((int)(255*opacity));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(8);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            for(Path road:roads){PathMeasure measure=new PathMeasure(road,false);Path stroke=new Path();measure.getSegment(0,measure.getLength()*reveal,stroke,true);c.drawPath(stroke,paint);}c.restore();
            paint.setStyle(Paint.Style.FILL);paint.setColor(text);paint.setAlpha((int)(255*Math.min(1,progress/0.3f)*opacity));paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(30*d);
            c.drawText("RideDeck",getWidth()/2f,getHeight()/2f+80*d,paint);
            paint.setColor(accent);paint.setAlpha((int)(255*opacity));paint.setTextSize(15*d);c.drawText(edition,getWidth()/2f,getHeight()/2f+110*d,paint);
        }
    }
}
