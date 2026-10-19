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
        animation.addListener(new android.animation.AnimatorListenerAdapter(){public void onAnimationEnd(android.animation.Animator animator){decor.post(()->{if(view.getParent()==decor)decor.removeView(view);});}});
        animation.start();
    }
    private static final class BrandView extends View {
        final String edition;final int background,accent,text;final Paint paint=new Paint(3);float progress;
        final Bitmap logo;
        BrandView(Activity a,String edition,int background,int accent,int text){super(a);this.edition=edition;this.background=background;this.accent=accent;this.text=text;logo=BitmapFactory.decodeResource(getResources(),R.drawable.velocity_logo);}
        protected void onDraw(Canvas canvas){
            canvas.drawColor(background);float d=getResources().getDisplayMetrics().density;
            float alpha=progress>.82f?(1-progress)/.18f:Math.min(1,progress/.2f);paint.setAlpha((int)(255*alpha));
            float size=Math.min(getWidth()*.7f,280*d);RectF box=new RectF((getWidth()-size)/2,(getHeight()-size)/2,(getWidth()+size)/2,(getHeight()+size)/2);
            if(!RideAppearance.isDark(getContext()))paint.setColorFilter(new android.graphics.PorterDuffColorFilter(text,android.graphics.PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(logo,null,box,paint);
        }
    }
}
