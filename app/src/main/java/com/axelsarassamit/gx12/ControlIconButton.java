package com.axelsarassamit.gx12;
import android.content.Context;
import android.graphics.*;
import android.widget.Button;
public final class ControlIconButton extends Button {
 private String control; private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG); private final boolean primary;
 public ControlIconButton(Context c,String label,boolean primary){super(c);this.primary=primary;control=label;setText("");setContentDescription(label);}
 public void setControl(String label){control=label;invalidate();}
 private void line(Canvas c,float... xy){Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,ink);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(32*getResources().getDisplayMetrics().density,Math.min(getWidth(),getHeight())*.6f);c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);ink.setColor(isEnabled()?(primary?0xff101510:RideTheme.accent(getContext())):0xff738078);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
 switch(control){
 case "Map":Path p=new Path();p.moveTo(12,22);p.cubicTo(8,17,4,13,4,9);p.cubicTo(4,-1,20,-1,20,9);p.cubicTo(20,13,16,17,12,22);c.drawPath(p,ink);c.drawCircle(12,9,3,ink);break;
 case "Camera":line(c,2,7,7,7,9,4,15,4,17,7,22,7,22,20,2,20,2,7);c.drawCircle(12,13,4,ink);break;
 case "Voice":c.drawRoundRect(9,2,15,15,3,3,ink);Path mic=new Path();mic.moveTo(5,11);mic.cubicTo(5,23,19,23,19,11);c.drawPath(mic,ink);line(c,12,19,12,23);line(c,8,23,16,23);break;
 case "Setup":c.drawCircle(12,12,6,ink);c.drawCircle(12,12,2,ink);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10);}break;
 case "|\u25c0":line(c,5,5,5,19);line(c,18,5,8,12,18,19,18,5);break;
 case "\u25b6|":line(c,19,5,19,19);line(c,6,5,16,12,6,19,6,5);break;
 case "\u2161":line(c,8,5,8,19);line(c,16,5,16,19);break;
 default:line(c,7,4,19,12,7,20,7,4);break;}c.restore();}
}
