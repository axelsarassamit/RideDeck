package com.axelsarassamit.gx12;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.WeakHashMap;

/** One appearance choice for every screen, independent of recording and map settings. */
public final class RideAppearance implements Application.ActivityLifecycleCallbacks {
    public static final String[] MODES = {"System", "Light", "Dark"};
    private static final WeakHashMap<Activity,String> rendered = new WeakHashMap<>();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
    private RideAppearance() { }
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ride_config",Context.MODE_PRIVATE); }
    public static int mode(Context c) { return prefs(c).getInt("appearance_mode",0); }
    public static int accentIndex(Context c) { return prefs(c).getInt("color_theme",4); }
    public static boolean isDark(Context c) {
        boolean system = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return AppearancePalette.dark(mode(c),system);
    }
    private static String key(Context c) { return mode(c)+":"+isDark(c)+":"+accentIndex(c); }
    public static void prepare(Activity a) {
        update(a); int index=Math.max(0,Math.min(4,accentIndex(a)));
        int[] themes=isDark(a) ? new int[]{R.style.RideDark0,R.style.RideDark1,R.style.RideDark2,R.style.RideDark3,R.style.RideDark4} : new int[]{R.style.RideLight0,R.style.RideLight1,R.style.RideLight2,R.style.RideLight3,R.style.RideLight4};
        a.setTheme(themes[index]);
        rendered.put(a,key(a));
    }
    public static void update(Context c) { RideStyle.apply(isDark(c)); }
    public static void install(Application app) { update(app); app.registerActivityLifecycleCallbacks(new RideAppearance()); }
    public static void refresh() { MAIN.post(() -> {
        for(Activity a : new java.util.ArrayList<>(rendered.keySet())) {
            if(a != null && !a.isFinishing() && !a.isDestroyed() && !key(a).equals(rendered.get(a))) {
                // Mark before recreating so concurrent provider updates do not queue duplicate recreations.
                rendered.put(a,key(a)); a.recreate();
            }
        }
    }); }
    public static void show(Activity a) { a.startActivity(new android.content.Intent(a,AppearanceActivity.class)); }
    public static void showOptions(Activity a) {
        new AlertDialog.Builder(a).setTitle("Appearance")
            .setSingleChoiceItems(MODES,Math.max(0,Math.min(2,mode(a))),(dialog,which) -> {
                prefs(a).edit().putInt("appearance_mode",which).apply();
                dialog.dismiss(); AppearanceSync.send(a); refresh();
            }).setNegativeButton("Cancel",null).show();
    }
    public static String description(Context c) { return MODES[Math.max(0,Math.min(2,mode(c)))]; }
    @Override public void onActivityResumed(Activity a) {
        if(!key(a).equals(rendered.get(a))) { refresh(); return; }
        update(a); ScreenChrome.apply(a.getWindow(),a instanceof MainActivity && !(a instanceof SetupActivity)); AppearanceSync.send(a);
    }
    @Override public void onActivityCreated(Activity a,Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { rendered.remove(a); }
    
}
