package com.axelsarassamit.gx12;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import android.content.pm.ActivityInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class SharedSetupTest {
 static List<View> views(View v){List<View> all=new ArrayList<>();all.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)all.addAll(views(((ViewGroup)v).getChildAt(i)));return all;}
 static List<TextView> controls(Activity a){List<TextView> all=new ArrayList<>();for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews())for(View v:views(root))if(v instanceof TextView&&v.isShown())all.add((TextView)v);return all;}
 static void click(Activity a,String label){TextView match=null;for(TextView v:controls(a))if(label.contentEquals(v.getText()))match=v;assertNotNull("Missing: "+label,match);View action=match;while(!action.isClickable()&&action.getParent() instanceof View)action=(View)action.getParent();assertTrue("No action: "+label,action.performClick());}
 static boolean has(Activity a,String label){for(TextView v:controls(a))if(label.contentEquals(v.getText()))return true;return false;}
 static void shot(Activity a,String label){try{View v=a.getWindow().getDecorView();android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));java.io.File folder=new java.io.File(a.getExternalFilesDir("tests"),"shared-setup");folder.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,label+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new AssertionError(e);}}
 @Test public void categoryNavigationAndRecreationKeepPlace(){
  try(ActivityScenario<SetupActivity> scenario=ActivityScenario.launch(SetupActivity.class)){
   scenario.onActivity(a->{;assertTrue(has(a,"General"));assertTrue(has(a,"Ride"));assertTrue(has(a,"Screen & layout"));assertTrue(has(a,"Headset & sound"));assertTrue(has(a,"Background & power"));assertTrue(has(a,"Help & app"));assertFalse(has(a,"CUSTOMIZATION"));click(a,"Screen & layout");assertTrue(has(a,"Appearance and colour theme"));});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.recreate();InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{assertTrue(has(a,"Appearance and colour theme"));click(a,"Back");click(a,"Ride");assertTrue(has(a,"Calls & messages"));click(a,"Calls & messages");assertTrue(has(a,"Announce incoming caller names"));click(a,"Back");;click(a,"General");click(a,"Headset & sound");});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{shot(a,"portrait-audio");a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);});
   boolean landscape=false;for(int i=0;i<60;i++){InstrumentationRegistry.getInstrumentation().waitForIdleSync();final boolean[] result={false};scenario.onActivity(a->result[0]=a.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE);if(result[0]){landscape=true;break;}try{Thread.sleep(100);}catch(InterruptedException e){throw new AssertionError(e);}}
   assertTrue("Landscape supported",landscape);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{assertTrue(has(a,"Headset & sound"));shot(a,"landscape-audio");click(a,"Back");assertFalse(has(a,"Yamaha"));;});
  }
 }
}
