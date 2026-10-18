package com.axelsarassamit.gx12;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.LinkedHashMap;

/** Parked setup navigation shared by the three RideDeck apps. No preference writes. */
final class SetupMenu {
    private final Activity activity;
    private final int accent;
    private final LinearLayout root, tabs, body;
    private final TextView title;
    private final Button back;
    private final ScrollView scroll;
    private final LinkedHashMap<String, Category> categories = new LinkedHashMap<>();
    private String section = "General", selected = "";
    private int restoredScroll;
    private static final class Category {
        final String id, section, title, summary;
        final LinearLayout content;
        Category(String id, String section, String title, String summary, LinearLayout content) {
            this.id=id; this.section=section; this.title=title; this.summary=summary; this.content=content;
        }
    }
    SetupMenu(Activity activity, String brand, int accent, boolean yamaha, Bundle saved, Runnable close) {
        this.activity=activity; this.accent=accent;
        if(saved!=null) {
            section=saved.getString("setup_section","General");
            selected=saved.getString("setup_category","");
            restoredScroll=saved.getInt("setup_scroll",0);
        }
        root=column(); root.setBackgroundColor(RideStyle.BACKGROUND);
        root.setPadding(dp(16),dp(12),dp(16),dp(12));
        TextView brandView=label(brand,16,accent); brandView.setPadding(dp(4),dp(10),0,dp(12)); root.addView(brandView);
        LinearLayout header=new LinearLayout(activity); header.setGravity(Gravity.CENTER_VERTICAL);
        back=action("Back",()->back()); header.addView(back,new LinearLayout.LayoutParams(dp(76),dp(52)));
        title=label("Setup",28,RideStyle.TEXT); androidx.core.view.ViewCompat.setAccessibilityHeading(title,true);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button done=action("Done",close); header.addView(done,new LinearLayout.LayoutParams(dp(76),dp(52)));
        root.addView(header); tabs=new LinearLayout(activity); tabs.setPadding(0,dp(12),0,dp(16)); root.addView(tabs);
        for(String tab:yamaha?new String[]{"General","Ride","Yamaha"}:new String[]{"General","Ride"}) {
            Button button=action(tab,()->{section=tab;selected="";restoredScroll=0;render();}); button.setTag(tab);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.setMargins(dp(3),0,dp(3),0);tabs.addView(button,lp);
        }
        scroll=new ScrollView(activity);scroll.setFillViewport(true);body=column();scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets b=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(b.left+dp(16),b.top+dp(12),b.right+dp(16),b.bottom+dp(12));return insets;
        });
    }
    LinearLayout add(String id,String section,String title,String summary) {
        if(categories.containsKey(id))return categories.get(id).content;
        LinearLayout content=column();content.setPadding(dp(12),dp(8),dp(12),dp(12));content.setBackground(surface(RideStyle.PANEL));
        categories.put(id,new Category(id,section,title,summary,content));return content;
    }
    View view(){return root;}
    void show(){render();androidx.core.view.ViewCompat.requestApplyInsets(root);}
    void save(Bundle state){state.putString("setup_section",section);state.putString("setup_category",selected);state.putInt("setup_scroll",scroll.getScrollY());}
    boolean back(){if(selected.isEmpty())return false;selected="";restoredScroll=0;render();return true;}
    private void render(){
        for(Category c:categories.values())if(c.content.getParent() instanceof ViewGroup)((ViewGroup)c.content.getParent()).removeView(c.content);
        body.removeAllViews();Category current=categories.get(selected);
        if(current!=null&&!current.section.equals(section)){selected="";current=null;}
        title.setText(current==null?"Setup":current.title);title.setTextSize(current==null?28:23);
        back.setVisibility(current==null?View.GONE:View.VISIBLE);
        for(int i=0;i<tabs.getChildCount();i++){
            Button b=(Button)tabs.getChildAt(i);boolean active=section.equals(b.getTag());
            b.setSelected(active);b.setTextColor(active?accent:RideStyle.MUTED);b.setBackground(surface(active?RideStyle.RAISED:RideStyle.BACKGROUND));
            b.setContentDescription(b.getText()+(active?", selected":""));
        }
        if(current!=null)body.addView(current.content);else{
            for(Category c:categories.values())if(c.section.equals(section)){
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(16),dp(12),dp(16));
                row.setMinimumHeight(dp(84));row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x286c94ff),surface(RideStyle.PANEL),null));
                ImageView icon=new ImageView(activity);icon.setImageDrawable(new MenuIcon(c.id));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(24),dp(24));iconSize.rightMargin=dp(14);row.addView(icon,iconSize);
                LinearLayout words=column();words.addView(label(c.title,19,RideStyle.TEXT));TextView summary=label(c.summary,14,RideStyle.MUTED);summary.setPadding(0,dp(5),dp(8),0);words.addView(summary);
                row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.addView(label("›",26,accent));
                row.setFocusable(true);row.setContentDescription(c.title+". "+c.summary);row.setOnClickListener(v->{selected=c.id;restoredScroll=0;render();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);body.addView(row,lp);
            }
        }
        int position=restoredScroll;restoredScroll=0;scroll.post(()->scroll.scrollTo(0,position));
    }
    private LinearLayout column(){LinearLayout p=new LinearLayout(activity);p.setOrientation(LinearLayout.VERTICAL);return p;}
    private TextView label(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));return t;}
    private GradientDrawable surface(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
    private Button action(String value,Runnable run){Button b=new Button(activity);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(accent);b.setMinHeight(dp(48));b.setPadding(dp(8),dp(6),dp(8),dp(6));b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x286c94ff),surface(RideStyle.BACKGROUND),null));b.setOnClickListener(v->run.run());return b;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private final class MenuIcon extends android.graphics.drawable.Drawable {
        private final String id; private final android.graphics.Paint ink=new android.graphics.Paint(3);
        MenuIcon(String id){this.id=id;ink.setColor(accent);ink.setStyle(android.graphics.Paint.Style.STROKE);ink.setStrokeWidth(1.8f);ink.setStrokeCap(android.graphics.Paint.Cap.ROUND);ink.setStrokeJoin(android.graphics.Paint.Join.ROUND);}
        private void line(android.graphics.Canvas c,float... p){android.graphics.Path path=new android.graphics.Path();path.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)path.lineTo(p[i],p[i+1]);c.drawPath(path,ink);}
        public void draw(android.graphics.Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
            switch(id){
                case "screen": c.drawRoundRect(3,4,21,20,2,2,ink);line(c,3,10,21,10);line(c,10,10,10,20);break;
                case "audio":c.drawArc(4,3,20,20,180,180,false,ink);c.drawRoundRect(3,11,7,21,1,1,ink);c.drawRoundRect(17,11,21,21,1,1,ink);break;
                case "power":c.drawRoundRect(2,6,20,18,2,2,ink);line(c,23,10,23,14);line(c,12,8,9,12,14,12,11,16);break;
                case "help":c.drawCircle(12,12,9,ink);c.drawArc(9,6,15,12,180,270,false,ink);line(c,12,12,12,14);c.drawPoint(12,18,ink);break;
                case "bike":line(c,12,2,18,7,6,17);line(c,6,7,18,17,12,22,12,2);break;
                case "navigation":line(c,3,5,9,2,15,5,21,2,21,19,15,22,9,19,3,22,3,5);line(c,9,2,9,19);line(c,15,5,15,22);break;
                case "calls":c.drawRoundRect(6,2,18,22,2,2,ink);line(c,10,18,14,18);break;
                case "data":line(c,2,12,6,12,9,5,14,20,18,12,22,12);break;
                case "tests":c.drawCircle(12,12,7,ink);line(c,12,2,12,5);line(c,12,19,12,22);line(c,2,12,5,12);line(c,19,12,22,12);break;
                case "library":c.drawRoundRect(3,3,21,21,2,2,ink);line(c,3,16,9,10,15,16,18,13,21,16);break;
                case "overlays":c.drawArc(3,3,21,21,180,180,false,ink);line(c,4,13,4,19,20,19,20,13);line(c,12,15,17,8);break;
                case "companion":c.drawRoundRect(2,7,10,17,3,3,ink);c.drawRoundRect(14,7,22,17,3,3,ink);line(c,8,12,16,12);break;
                default:c.drawRoundRect(2,6,17,20,2,2,ink);line(c,17,10,22,7,22,19,17,16);
            }c.restore();}
        public void setAlpha(int alpha){ink.setAlpha(alpha);}public void setColorFilter(android.graphics.ColorFilter filter){ink.setColorFilter(filter);}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
}
