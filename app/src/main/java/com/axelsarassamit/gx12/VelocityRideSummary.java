package com.axelsarassamit.gx12;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
import java.util.Locale;

/** Native preview cockpit. Unknown or stale speed is never displayed as a measurement. */
public final class VelocityRideSummary extends LinearLayout implements LocationListener {
    private final TextView speed, quality;
    private final Activity activity;
    private LocationManager locations;
    private Location fix;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();timer.postDelayed(this,1000);}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    public VelocityRideSummary(Activity a, String action, Runnable open) {
        super(a);activity=a;setGravity(Gravity.CENTER_VERTICAL);setPadding(dp(16),dp(8),dp(8),dp(8));
        setBackground(RideStyle.surface(a,RideStyle.PANEL,16,false));
        LinearLayout readings=new LinearLayout(a);readings.setOrientation(VERTICAL);
        speed=new TextView(a);speed.setTextSize(42);speed.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));speed.setTextColor(RideStyle.TEXT);speed.setIncludeFontPadding(false);
        quality=new TextView(a);quality.setTextSize(12);quality.setTextColor(RideStyle.MUTED);
        readings.addView(speed);readings.addView(quality);readings.setContentDescription("GPS speed; tap to allow location");readings.setOnClickListener(v->{if(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},74);else startGps();});addView(readings,new LayoutParams(0,-2,1));
        Button launch=new Button(a);launch.setText(action);launch.setAllCaps(false);launch.setTextSize(17);launch.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        launch.setBackgroundTintList(null);launch.setBackground(RideStyle.surface(a,RideTheme.accent(a),14,false));launch.setTextColor(AppearancePalette.onAccent(RideAppearance.isDark(a)));launch.setOnClickListener(v->open.run());
        addView(launch,new LayoutParams(dp(156),dp(72)));render();
    }
    private void render(){boolean fresh=fix!=null&&SystemClock.elapsedRealtimeNanos()-fix.getElapsedRealtimeNanos()<6000000000L&&fix.hasSpeed();speed.setText(fresh?String.format(Locale.ROOT,"%.0f",fix.getSpeed()*3.6):"?");quality.setText(fresh?"km/h · GPS":"GPS speed · Waiting");}
    private void startGps(){if(locations!=null||activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;try{locations=activity.getSystemService(LocationManager.class);if(locations!=null)locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());}catch(RuntimeException ignored){locations=null;}}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)startGps();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();timer.post(refresh);startGps();}
    @Override protected void onDetachedFromWindow(){timer.removeCallbacksAndMessages(null);if(locations!=null)try{locations.removeUpdates(this);}catch(RuntimeException ignored){}locations=null;super.onDetachedFromWindow();}
    @Override public void onLocationChanged(Location value){fix=new Location(value);render();}
    @Override public void onProviderDisabled(String provider){fix=null;render();}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
}
