package com.axelsarassamit.gx12;

import android.app.*;
import android.content.*;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class StandardLifecycleTest {
    @Test public void rainLockSurvivesRecreationAndStillUnlocks() {
        context();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(RainLock::engage);
            scenario.recreate();
            scenario.onActivity(a->{
                assertTrue(RainLock.active(a));
                android.view.ViewGroup shield=a.findViewById(android.R.id.content).findViewWithTag("ridedeck_rain_lock");
                assertTrue(shield.getChildAt(0).performLongClick());
                assertFalse(RainLock.active(a));
            });
        }
    }
    private Context context() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Run only in the isolated preview", c.getPackageName().endsWith(".preview"));
        return c;
    }
    private Object field(MainActivity a, String name) {
        try { java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(a); }
        catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void editedReplySurvivesRecreationWithoutSending() {
        context();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{try {
                GX12NotificationListener.NotificationPreview target=new GX12NotificationListener.NotificationPreview("Synthetic", "Body", "regression", null, "com.whatsapp", "WhatsApp");
                java.lang.reflect.Method show=MainActivity.class.getDeclaredMethod("confirmVoiceReply",GX12NotificationListener.NotificationPreview.class,String.class);
                show.setAccessible(true);show.invoke(a,target,"Original draft");
                ((EditText)field(a,"reviewDraft")).setText("Edited unsent draft");
            }catch(Exception e){throw new AssertionError(e);}});
            scenario.recreate();InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{assertTrue(((Dialog)field(a,"voiceReplyReview")).isShowing());assertEquals("Edited unsent draft",((EditText)field(a,"reviewDraft")).getText().toString());});
        }
    }
    private boolean leaseExists(Context c) {
        for(ActivityManager.RunningServiceInfo info:c.getSystemService(ActivityManager.class).getRunningServices(100))
            if(info.service.getClassName().equals(RideDashLeaseService.class.getName()))return true;
        return false;
    }
    @Test public void pauseStopsTheLeaseService() throws Exception {
        Context c=context();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).edit().putBoolean(RideSessionState.PREF_RUNNING,true).commit();RideDashLeaseService.start(a);});
            for(int i=0;i<30&&!leaseExists(c);i++)Thread.sleep(100);
            assertTrue(leaseExists(c));
            scenario.onActivity(a->RideDashLeaseService.stop(a));
            for(int i=0;i<30&&leaseExists(c);i++)Thread.sleep(100);
            assertFalse("Pause must release the started service",leaseExists(c));
        }finally{c.stopService(new Intent(c,RideDashLeaseService.class));c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).edit().clear().commit();}
    }
    @Test public void queuedPauseCannotCancelANewerExplicitStart() throws Exception {
        Context c=context();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{
                c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).edit().putBoolean(RideSessionState.PREF_RUNNING,true).commit();
                RideDashLeaseService.start(a);
                RideDashLeaseService.stop(a);
                c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).edit().putBoolean(RideSessionState.PREF_RUNNING,true).commit();
                RideDashLeaseService.start(a);
            });
            Thread.sleep(1200);
            assertTrue("Newest explicit start must remain active",RideDashLeaseService.isRunning());
            assertTrue(leaseExists(c));
            assertTrue(c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).getBoolean(RideSessionState.PREF_RUNNING,false));
        }finally{c.stopService(new Intent(c,RideDashLeaseService.class));c.getSharedPreferences(RideSessionState.PREFERENCES_NAME,0).edit().clear().commit();}
    }
}
