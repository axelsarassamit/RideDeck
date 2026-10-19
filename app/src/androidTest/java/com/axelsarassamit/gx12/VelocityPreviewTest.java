package com.axelsarassamit.gx12;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class VelocityPreviewTest {
    private static void waitUi(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();try{Thread.sleep(350);}catch(InterruptedException e){throw new AssertionError(e);}}
    private static void check(View v,int screenHeight){
        if(!v.isShown())return;
        if(v instanceof Button && v.getWidth()>0){int[] xy=new int[2];v.getLocationOnScreen(xy);assertTrue("Button outside screen: "+((Button)v).getText(),xy[1]+v.getHeight()<=screenHeight+2);}
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)check(((ViewGroup)v).getChildAt(i),screenHeight);
    }
    private static void shot(Activity a,String label){
        View v=a.getWindow().getDecorView();assertTrue(v.getWidth()>0&&v.getHeight()>0);check(v,a.getResources().getDisplayMetrics().heightPixels);
        View box=v.findViewWithTag("ride_message_box"); assertNotNull("Message box must remain present",box);
        if(box.isShown()){ LinearLayout card=(LinearLayout)box; TextView body=(TextView)card.getChildAt(2); assertTrue("Message body must have room for readable text",body.getHeight() >= body.getLineHeight()+body.getCompoundPaddingTop()+body.getCompoundPaddingBottom()); }
        Bitmap image=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(image));
        java.io.File folder=new java.io.File(a.getExternalFilesDir("tests"),"velocity");folder.mkdirs();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,label+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}catch(Exception e){throw new AssertionError(e);}finally{image.recycle();}
    }
    @Test public void realCockpitSupportsLightDarkAndRotation(){
        android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        int mode=RideAppearance.mode(c),accent=RideAppearance.accentIndex(c);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            for(int theme:new int[]{2,1}){
                RideAppearance.prefs(c).edit().putInt("appearance_mode",theme).putInt("color_theme",4).commit();scenario.recreate();waitUi();
                for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
                    scenario.onActivity(a->a.setRequestedOrientation(orientation));waitUi();
                    int expected=orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?Configuration.ORIENTATION_PORTRAIT:Configuration.ORIENTATION_LANDSCAPE;
                    for(int i=0;i<30;i++){final boolean[] ready={false};scenario.onActivity(a->ready[0]=a.getResources().getConfiguration().orientation==expected);if(ready[0])break;waitUi();}
                    scenario.onActivity(a->{assertEquals(expected,a.getResources().getConfiguration().orientation);shot(a,(theme==2?"dark":"light")+(expected==1?"-portrait":"-landscape"));});
                }
            }
        }finally{RideAppearance.prefs(c).edit().putInt("appearance_mode",mode).putInt("color_theme",accent).commit();}
    }
}
