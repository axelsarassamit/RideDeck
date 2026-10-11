package com.axelsarassamit.gx12;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import java.lang.ref.WeakReference;

/** Capability-bound host client for Dash recording and the embedded camera preview. */
public final class DashRecordingClient {
    private static final String DASH_PACKAGE = DashPackageVerifier.PACKAGE_NAME;
    private static final String DASH_SERVICE = DASH_PACKAGE + ".CompanionRecordingService";
    private static final int MSG_START = 1;
    private static final int MSG_PREVIEW = 2;
    private static final int MSG_STATUS = 3;
    private static final int REQUEST_PERMISSIONS = 0x5da;
    private static final int PROTOCOL_VERSION = 1;
    private static final int MAX_FRAME_BYTES = 100000;
    private static final int MAX_TOTAL_FRAME_BYTES = 200000;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Context app;
    private static WeakReference<Activity> permissionActivity = new WeakReference<>(null);
    private static Messenger remote;
    private static Messenger reply;
    private static ServiceConnection connection;
    private static boolean bound;
    private static boolean startSent;
    private static boolean accepted;
    private static boolean requestPending;
    private static int pendingWhat;
    private static Listener listener;
    private static WeakReference<StatusListener> statusListener = new WeakReference<>(null);

    public interface Listener {
        boolean previewVisible();
        void onUpdate(boolean accepted, boolean starting, boolean recording, String message, Bitmap rear, Bitmap front);
    }

    public interface StatusListener {
        boolean visible();
        void onStatus(boolean accepted, boolean starting, boolean recording, String message);
    }

    private static final Runnable poll = () -> {
        Listener target = listener;
        if (!accepted || target == null || !target.previewVisible() || remote == null || requestPending) return;
        try {
            Message request = Message.obtain(null, MSG_PREVIEW);
            Bundle data = new Bundle();
            data.putInt("protocolVersion", PROTOCOL_VERSION);
            request.setData(data);
            request.replyTo = reply;
            requestPending = true;
            pendingWhat = MSG_PREVIEW;
            MAIN.postDelayed(requestTimeout, 3000L);
            remote.send(request);
        } catch (RuntimeException | RemoteException error) {
            clearPendingRequest();
            notifyListener(false, false, false, "Dash preview connection was lost.", null, null);
            MAIN.postDelayed(DashRecordingClient::scheduleNextPoll, 1000L);
        }
    };

    private static final Runnable statusPoll = () -> {
        StatusListener target = statusListener.get();
        if (!accepted || target == null || !target.visible() || remote == null || requestPending) return;
        try {
            Message request = Message.obtain(null, MSG_STATUS);
            Bundle data = new Bundle();
            data.putInt("protocolVersion", PROTOCOL_VERSION);
            request.setData(data);
            request.replyTo = reply;
            requestPending = true;
            pendingWhat = MSG_STATUS;
            MAIN.postDelayed(requestTimeout, 3000L);
            remote.send(request);
        } catch (RuntimeException | RemoteException error) {
            clearPendingRequest();
            notifyStatus(false, false, false, "Dash recording status is unavailable.");
            MAIN.postDelayed(DashRecordingClient::scheduleNextPoll, 1000L);
        }
    };

    private static final Runnable requestTimeout = () -> {
        if (!requestPending) return;
        int expiredWhat = pendingWhat;
        requestPending = false;
        pendingWhat = 0;
        if (expiredWhat == MSG_START) {
            accepted = false;
            startSent = false;
            notifyListener(false, false, false, "Dash did not confirm the recording request.", null, null);
            notifyStatus(false, false, false, "Dash did not confirm the recording request.");
        } else {
            notifyStatus(false, false, false, "Dash recording status is unavailable.");
        }
        MAIN.postDelayed(DashRecordingClient::scheduleNextPoll, 1000L);
    };

    private static void clearPendingRequest() {
        requestPending = false;
        pendingWhat = 0;
        MAIN.removeCallbacks(requestTimeout);
    }

    private static void scheduleNextPoll() {
        if (!accepted || remote == null || requestPending) return;
        Listener preview = listener;
        if (preview != null && preview.previewVisible()) {
            MAIN.post(poll);
            return;
        }
        StatusListener status = statusListener.get();
        if (status != null && status.visible()) MAIN.post(statusPoll);
    }

    private DashRecordingClient() { }

    public static synchronized void start(Activity activity, Listener target) {
        if (activity == null || target == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!DashPackageVerifier.isInstalledAndSigned(activity)) {
            target.onUpdate(false, false, false, "Signed RideDeck Dash is not available.", null, null);
            return;
        }
        app = activity.getApplicationContext();
        listener = target;
        accepted = false;
        startSent = false;
        requestPending = false;
        ensureReplyMessenger();
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (activity.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            missing.add(android.Manifest.permission.CAMERA);
        if (activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            missing.add(android.Manifest.permission.RECORD_AUDIO);
        if (!missing.isEmpty()) {
            permissionActivity = new WeakReference<>(activity);
            try { activity.requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS); }
            catch (RuntimeException error) {
                notifyListener(false, false, false, "Grant Camera and Microphone permissions to RideDeck, then try again.", null, null);
            }
            return;
        }
        bindFromVisibleHost(activity);
    }

    public static synchronized boolean onRequestPermissionsResult(Activity activity, int requestCode, int[] results) {
        if (requestCode != REQUEST_PERMISSIONS) return false;
        Activity pending = permissionActivity.get();
        permissionActivity.clear();
        if (pending != activity || activity.isFinishing() || activity.isDestroyed()) return true;
        Listener target = listener;
        if (target == null || !target.previewVisible()) return true;
        boolean granted = results != null && results.length > 0;
        if (granted) for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            notifyListener(false, false, false, "Camera and Microphone access are required for Dash recording. Enable them for RideDeck and try again.", null, null);
            return true;
        }
        bindFromVisibleHost(activity);
        return true;
    }

    private static void bindFromVisibleHost(Activity activity) {
        Listener target = listener;
        if (target == null || !target.previewVisible()) return;
        if (remote != null) {
            sendStart();
            return;
        }
        if (bound) return;
        Intent intent = new Intent().setComponent(new ComponentName(DASH_PACKAGE, DASH_SERVICE));
        int flags = Context.BIND_AUTO_CREATE;
        if (Build.VERSION.SDK_INT >= 30) flags |= Context.BIND_INCLUDE_CAPABILITIES;
        connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
                synchronized (DashRecordingClient.class) {
                    remote = new Messenger(binder);
                    sendStart();
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                synchronized (DashRecordingClient.class) {
                    remote = null; startSent = false; accepted = false; clearPendingRequest();
                    MAIN.removeCallbacks(poll);
                    MAIN.removeCallbacks(statusPoll);
                    notifyListener(false, false, false, "Dash recording connection ended.", null, null);
                }
            }
            @Override public void onBindingDied(ComponentName name) {
                onServiceDisconnected(name);
                end(app);
            }
            @Override public void onNullBinding(ComponentName name) {
                onServiceDisconnected(name);
                end(app);
            }
        };
        try {
            // Called only from the visible host Activity after an explicit recording choice.
            bound = app.bindService(intent, connection, flags);
            if (!bound) notifyListener(false, false, false, Build.VERSION.SDK_INT < 30
                    ? "Dash background camera access is unavailable on this Android version."
                    : "Dash recording service is unavailable. Update Dash and try again.", null, null);
        } catch (RuntimeException error) {
            bound = false;
            notifyListener(false, false, false, Build.VERSION.SDK_INT < 30
                    ? "Dash background camera access is unavailable on this Android version."
                    : "Dash recording service could not be reached.", null, null);
        }
    }

    private static void ensureReplyMessenger() {
        if (reply != null) return;
        reply = new Messenger(new Handler(Looper.getMainLooper()) {
            @Override public void handleMessage(Message message) {
                Bundle data = message.getData();
                boolean ok = data != null && data.getInt("protocolVersion", 0) == PROTOCOL_VERSION
                    && data.getBoolean("accepted", false);
                boolean starting = data != null && data.getBoolean("starting", false);
                boolean recording = data != null && data.getBoolean("recording", false);
                String text = data == null ? "Dash status is unavailable." : data.getString("message", "");
                if (message.what == MSG_STATUS) {
                    if (pendingWhat != MSG_STATUS) return;
                    clearPendingRequest();
                    notifyStatus(ok, starting, recording, text);
                    MAIN.postDelayed(DashRecordingClient::scheduleNextPoll, 1000L);
                    return;
                }
                if (message.what == MSG_START && pendingWhat != MSG_START) return;
                if (message.what == MSG_PREVIEW && pendingWhat != MSG_PREVIEW) return;
                if (!ok && (data == null || data.getInt("protocolVersion", 0) != PROTOCOL_VERSION)) {
                    clearPendingRequest();
                    accepted = false;
                    notifyListener(false, false, false, "Dash needs a compatible update.", null, null);
                    notifyStatus(false, false, false, "Dash needs a compatible update.");
                    return;
                }
                if (message.what == MSG_START) {
                    clearPendingRequest();
                    accepted = ok;
                    notifyListener(ok, starting, recording, text, null, null);
                    notifyStatus(ok, starting, recording, text);
                    scheduleNextPoll();
                } else if (message.what == MSG_PREVIEW) {
                    clearPendingRequest();
                    accepted = ok;
                    byte[] rearBytes = data.getByteArray("rearJpeg");
                    byte[] frontBytes = data.getByteArray("frontJpeg");
                    int total = (rearBytes == null ? 0 : rearBytes.length) + (frontBytes == null ? 0 : frontBytes.length);
                    Bitmap rear = total <= MAX_TOTAL_FRAME_BYTES ? decode(rearBytes) : null;
                    Bitmap front = total <= MAX_TOTAL_FRAME_BYTES ? decode(frontBytes) : null;
                    notifyListener(ok, starting, recording, text, rear, front);
                    notifyStatus(ok, starting, recording, text);
                    MAIN.postDelayed(DashRecordingClient::scheduleNextPoll, 1000L);
                }
            }
        });
    }

    private static Bitmap decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_FRAME_BYTES) return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static void sendStart() {
        if (remote == null || startSent) return;
        try {
            Message request = Message.obtain(null, MSG_START);
            Bundle data = new Bundle();
            data.putInt("protocolVersion", PROTOCOL_VERSION);
            request.setData(data);
            request.replyTo = reply;
            startSent = true;
            requestPending = true;
            pendingWhat = MSG_START;
            MAIN.postDelayed(requestTimeout, 5000L);
            remote.send(request);
        } catch (RuntimeException | RemoteException error) {
            startSent = false;
            clearPendingRequest();
            notifyListener(false, false, false, "Dash could not accept the recording request.", null, null);
        }
    }

    private static void notifyListener(boolean ok, boolean starting, boolean recording, String text, Bitmap rear, Bitmap front) {
        Listener target = listener;
        if (target == null) return;
        MAIN.post(() -> {
            Listener current = listener;
            if (current != null && current.previewVisible()) current.onUpdate(ok, starting, recording, text, rear, front);
        });
        notifyStatus(ok, starting, recording, text);
    }

    private static void notifyStatus(boolean ok, boolean starting, boolean recording, String text) {
        StatusListener target = statusListener.get();
        if (target == null) return;
        MAIN.post(() -> {
            StatusListener current = statusListener.get();
            if (current != null && current.visible()) current.onStatus(ok, starting, recording, text);
        });
    }

    public static synchronized void setStatusListener(StatusListener target) {
        statusListener = new WeakReference<>(target);
        scheduleNextPoll();
    }

    public static synchronized void clearStatusListener() {
        statusListener.clear();
        MAIN.removeCallbacks(statusPoll);
    }

    public static synchronized void refreshPreview() {
        scheduleNextPoll();
    }

    public static synchronized void finishPreview() {
        listener = null;
        MAIN.removeCallbacks(poll);
        scheduleNextPoll();
    }

    public static synchronized void end(Context context) {
        finishPreview();
        accepted = false;
        startSent = false;
        clearPendingRequest();
        remote = null;
        if (bound && connection != null) {
            try { (app == null ? context.getApplicationContext() : app).unbindService(connection); }
            catch (RuntimeException ignored) { }
        }
        bound = false;
        connection = null;
        permissionActivity.clear();
        statusListener.clear();
        MAIN.removeCallbacks(statusPoll);
    }
}
