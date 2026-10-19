package com.axelsarassamit.gx12;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/** Layout treatments taken from the approved MAHYcdpoPwg reference. */
final class RoadlineUi {
    static Button mapButton(Context c){
        Button b=new Button(c){
            final android.graphics.Paint ink=new android.graphics.Paint(3);
            @Override protected void onDraw(android.graphics.Canvas canvas){
                super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,y=getHeight()/2f;
                ink.setColor(RideTheme.accent(c));ink.setStyle(android.graphics.Paint.Style.STROKE);ink.setStrokeWidth(2*d);
                android.graphics.Path book=new android.graphics.Path();book.moveTo(16*d,y-8*d);book.lineTo(23*d,y-10*d);book.lineTo(30*d,y-8*d);book.lineTo(37*d,y-10*d);book.lineTo(37*d,y+8*d);book.lineTo(30*d,y+10*d);book.lineTo(23*d,y+8*d);book.lineTo(16*d,y+10*d);book.close();canvas.drawPath(book,ink);
                canvas.drawLine(23*d,y-10*d,23*d,y+8*d,ink);canvas.drawLine(30*d,y-8*d,30*d,y+10*d,ink);
                float x=getWidth()-22*d;android.graphics.Path arrow=new android.graphics.Path();arrow.moveTo(x-3*d,y-5*d);arrow.lineTo(x+2*d,y);arrow.lineTo(x-3*d,y+5*d);canvas.drawPath(arrow,ink);
            }
        };
        b.setText("Maps");b.setAllCaps(false);b.setTextSize(17);b.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);b.setPadding(dp(c,48),0,dp(c,40),0);return b;
    }
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static void setup(Button b){
        b.setBackgroundColor(Color.TRANSPARENT); b.setTextColor(RideTheme.accent(b.getContext()));
        b.setMinHeight(dp(b.getContext(),48)); b.setMinimumHeight(dp(b.getContext(),48));
    }
    static void transport(LinearLayout row, Button previous,Button play,Button next){
        Context c=row.getContext(); row.removeAllViews();row.setGravity(android.view.Gravity.CENTER);
        for(Button b:new Button[]{previous,play,next}){
            int size=b==play?64:56;
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(c,size),dp(c,size));
            p.setMargins(dp(c,8),0,dp(c,8),0); row.addView(b,p);
            b.setBackground(RideStyle.surface(c,b==play?(RideAppearance.accentIndex(c)==4?0xff0665db:RideTheme.accent(c)):RideStyle.RAISED,100,false));
        }
    }
    static void messageAction(Button b){
        Context c=b.getContext(); b.setTextSize(14);b.setMaxLines(1);
        int foreground=RideAppearance.accentIndex(c)==4?Color.WHITE:AppearancePalette.onAccent(RideAppearance.isDark(c));
        b.setTextColor(foreground);
        for(android.graphics.drawable.Drawable icon:b.getCompoundDrawables())if(icon!=null)icon.setTint(foreground);
        b.setBackground(RideStyle.surface(c,RideAppearance.accentIndex(c)==4?0xff0665db:RideTheme.accent(c),8,false));
    }
    static void dock(View view){
        view.setBackgroundColor(Color.TRANSPARENT);
        if(view instanceof TextView){TextView t=(TextView)view;t.setTextSize(12);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)dock(group.getChildAt(i));}
    }
}
