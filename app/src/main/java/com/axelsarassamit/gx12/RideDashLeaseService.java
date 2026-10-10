package com.axelsarassamit.gx12;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public final class RideDashLeaseService extends Service {
    public static final String ACTION_START = "com.axelsarassamit.gx12.action.START_RIDE";
    public static final String ACTION_PAUSE = "com.axelsarassamit.gx12.action.PAUSE_RIDE";
    private static final String EXTRA_STOP_WHEN_DONE = "stop_when_done";
    private static final Uri DASH_PROVIDER_URI = DashPackageVerifier.PROVIDER_URI;
    private static final int PROTOCOL_VERSION = 1;
    private static final long RENEW_INTERVAL_MS = 5_000L;
    private static final int NOTIFICATION_ID = 4812;
    private static final String CHANNEL_ID = "ridedeck_active_ride";
    private static volatile boolean serviceRequested;
    private HandlerThread workerThread;
    private Handler worker;
    private boolean stopping;
    private boolean stopSent;
    private String lastNotificationText;

    private final Runnable renewTask = new Runnable() {
        @Override public void run() {
            if (stopping || !serviceRequested) return;
            SharedPreferences prefs = ridePreferences();
            if (!prefs.getBoolean(RideSessionState.PREF_RUNNING, false)) {
                requestStop();
                return;
            }
            checkpointRide(prefs);
            stopSent = false;
            callProvider("renew", false);
            if (!stopping && serviceRequested) worker.postDelayed(this, RENEW_INTERVAL_MS);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, RideDashLeaseService.class).setAction(ACTION_START);
        serviceRequested = true;
        try { androidx.core.content.ContextCompat.startForegroundService(context, intent); }
        catch (RuntimeException error) { serviceRequested = false; throw error; }
    }

    public static void stop(Context context) {
        boolean wasRunning = serviceRequested;
        serviceRequested = false;
        Intent intent = new Intent(context, RideDashLeaseService.class).setAction(ACTION_PAUSE)
            .putExtra(EXTRA_STOP_WHEN_DONE, !wasRunning);
        try { context.startService(intent); }
        catch (RuntimeException error) { context.stopService(intent); }
    }

    public static boolean isRunning() { return serviceRequested; }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        workerThread = new HandlerThread("RideDeck-Dash-lease");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_PAUSE.equals(action)) {
            serviceRequested = false;
            pausePersistedRide();
            boolean stopWhenDone = intent.getBooleanExtra(EXTRA_STOP_WHEN_DONE, false);
            if (worker != null) {
                worker.removeCallbacks(renewTask);
                worker.post(() -> {
                    sendStop();
                    if (stopWhenDone && !ridePreferences().getBoolean(RideSessionState.PREF_RUNNING, false)) {
                        stopSelf(startId);
                        if (workerThread != null) workerThread.quitSafely();
                    }
                });
            }
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            requestStop();
            return START_NOT_STICKY;
        }
        serviceRequested = true;
        stopping = false;
        stopSent = false;
        try {
            int foregroundType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification("Ride session active."),
                foregroundType);
        } catch (RuntimeException error) {
            serviceRequested = false;
            pausePersistedRide();
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (worker != null) {
            worker.removeCallbacks(renewTask);
            worker.post(renewTask);
        }
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onTaskRemoved(Intent rootIntent) {
        pausePersistedRide();
        requestStop();
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onTimeout(int startId, int foregroundServiceType) {
        pausePersistedRide();
        requestStop();
    }

    @Override public void onDestroy() {
        serviceRequested = false;
        if (worker != null) {
            worker.removeCallbacks(renewTask);
            if (!stopSent) worker.post(() -> {
                sendStop();
                if (workerThread != null) workerThread.quitSafely();
            });
            else if (workerThread != null) workerThread.quitSafely();
        }
        super.onDestroy();
    }

    private void requestStop() {
        serviceRequested = false;
        if (stopping) return;
        stopping = true;
        if (worker == null) { stopSelf(); return; }
        worker.removeCallbacks(renewTask);
        worker.post(() -> {
            sendStop();
            if (ridePreferences().getBoolean(RideSessionState.PREF_RUNNING, false)) {
                // A new explicit ride start arrived while the previous stop was queued.
                stopping = false;
                stopSent = false;
                serviceRequested = true;
                worker.removeCallbacks(renewTask);
                worker.post(renewTask);
                return;
            }
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
            stopSelf();
            if (workerThread != null) workerThread.quitSafely();
        });
    }

    private void sendStop() {
        if (stopSent) return;
        stopSent = true;
        callProvider("stop", true);
    }

    private SharedPreferences ridePreferences() {
        return getSharedPreferences(RideSessionState.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private void checkpointRide(SharedPreferences prefs) {
        long now = android.os.SystemClock.elapsedRealtime();
        long base = prefs.getLong(RideSessionState.PREF_BASE_ELAPSED, prefs.getLong(RideSessionState.PREF_ELAPSED, 0));
        long started = prefs.getLong(RideSessionState.PREF_STARTED_ELAPSED, now);
        long elapsed = RideSessionState.elapsed(base, started, now);
        prefs.edit().putLong(RideSessionState.PREF_ELAPSED, elapsed).apply();
    }

    private void pausePersistedRide() {
        SharedPreferences prefs = ridePreferences();
        if (!prefs.getBoolean(RideSessionState.PREF_RUNNING, false)) return;
        long now = android.os.SystemClock.elapsedRealtime();
        long base = prefs.getLong(RideSessionState.PREF_BASE_ELAPSED, prefs.getLong(RideSessionState.PREF_ELAPSED, 0));
        long started = prefs.getLong(RideSessionState.PREF_STARTED_ELAPSED, now);
        long elapsed = RideSessionState.elapsed(base, started, now);
        prefs.edit().putBoolean(RideSessionState.PREF_RUNNING, false)
            .putLong(RideSessionState.PREF_ELAPSED, elapsed)
            .remove(RideSessionState.PREF_BASE_ELAPSED)
            .remove(RideSessionState.PREF_STARTED_ELAPSED).apply();
    }

    private void callProvider(String method, boolean isStop) {
        if (!isStop && !DashPackageVerifier.isInstalledAndSigned(this)) {
            updateNotification("Ride session active. Signed RideDeck Dash is not installed.");
            return;
        }
        try {
            Bundle request = new Bundle(); request.putInt("protocolVersion", PROTOCOL_VERSION);
            Bundle response = getContentResolver().call(DASH_PROVIDER_URI, method, null, request);
            if (isStop || !serviceRequested || stopping) return;
            String status;
            if (response == null || response.getInt("protocolVersion", -1) != PROTOCOL_VERSION) {
                status = "Dash connection unavailable. Open or update RideDeck Dash.";
            } else if (!response.getBoolean("accepted", false)) {
                status = "Dash is in use by the other RideDeck companion.";
            } else {
                status = "Dash is ready. Start recording in RideDeck Dash.";
            }
            updateNotification(status);
        } catch (RuntimeException error) {
            if (!isStop && serviceRequested && !stopping) updateNotification("Dash connection unavailable. Open or update RideDeck Dash.");
        }
    }

    private void updateNotification(String text) {
        if (text.equals(lastNotificationText)) return;
        lastNotificationText = text;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(new NotificationChannel(
            CHANNEL_ID, "Ride session", NotificationManager.IMPORTANCE_LOW));
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 4812, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent pause = new Intent(this, RideDashLeaseService.class).setAction(ACTION_PAUSE);
        PendingIntent pauseAction = PendingIntent.getService(this, 4813, pause,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RideDeck ride in progress")
            .setContentText(text)
            .setContentIntent(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Pause ride", pauseAction)
            .build();
    }
}
