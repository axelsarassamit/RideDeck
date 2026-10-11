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
    private static boolean previewPending;
    private static Listener listener;

    public interface Listener {
        boolean previewVisible();
        void onUpdate(boolean accepted, boolean recording, String message, Bitmap rear, Bitmap front);
    }

    private static final Runnable poll = () -> {
        Listener target = listener;
        if (!accepted || target == null || !target.previewVisible() || remote == null || previewPending) return;
        try {
            Message request = Message.obtain(null, MSG_PREVIEW);
            Bundle data = new Bundle();
            data.putInt("protocolVersion", PROTOCOL_VERSION);
            request.setData(data);
            request.replyTo = reply;
            previewPending = true;
            remote.send(request);
        } catch (RuntimeException | RemoteException error) {
            previewPending = false;
            notifyListener(false, false, "Dash preview connection was lost.", null, null);
        }
    };

    private DashRecordingClient() { }

    public static synchronized void start(Activity activity, Listener target) {
        if (activity == null || target == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!DashPackageVerifier.isInstalledAndSigned(activity)) {
            target.onUpdate(false, false, "Signed RideDeck Dash is not available.", null, null);
            return;
        }
        app = activity.getApplicationContext();
        listener = target;
        accepted = false;
        startSent = false;
        previewPending = false;
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
                notifyListener(false, false, "Grant Camera and Microphone permissions to RideDeck, then try again.", null, null);
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
            notifyListener(false, false, "Camera and Microphone access are required for Dash recording. Enable them for RideDeck and try again.", null, null);
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
                    remote = null; startSent = false; accepted = false; previewPending = false;
                    MAIN.removeCallbacks(poll);
                    notifyListener(false, false, "Dash recording connection ended.", null, null);
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
            if (!bound) notifyListener(false, false, Build.VERSION.SDK_INT < 30
                    ? "Dash background camera access is unavailable on this Android version."
                    : "Dash recording service is unavailable. Update Dash and try again.", null, null);
        } catch (RuntimeException error) {
            bound = false;
            notifyListener(false, false, Build.VERSION.SDK_INT < 30
                    ? "Dash background camera access is unavailable on this Android version."
                    : "Dash recording service could not be reached.", null, null);
        }
    }

    private static void ensureReplyMessenger() {
        if (reply != null) return;
        reply = new Messenger(new Handler(Looper.getMainLooper()) {
            @Override public void handleMessage(Message message) {
                Bundle data = message.getData();
                if (data == null || data.getInt("protocolVersion", 0) != PROTOCOL_VERSION) {
                    accepted = false;
                    notifyListener(false, false, "Dash needs a compatible update.", null, null);
                    return;
                }
                boolean ok = data.getBoolean("accepted", false);
                boolean recording = data.getBoolean("recording", false);
                String text = data.getString("message", "");
                if (message.what == MSG_START) {
                    accepted = ok;
                    notifyListener(ok, recording, text, null, null);
                    if (ok) MAIN.post(poll);
                } else if (message.what == MSG_PREVIEW) {
                    previewPending = false;
                    accepted = ok;
                    byte[] rearBytes = data.getByteArray("rearJpeg");
                    byte[] frontBytes = data.getByteArray("frontJpeg");
                    int total = (rearBytes == null ? 0 : rearBytes.length) + (frontBytes == null ? 0 : frontBytes.length);
                    Bitmap rear = total <= MAX_TOTAL_FRAME_BYTES ? decode(rearBytes) : null;
                    Bitmap front = total <= MAX_TOTAL_FRAME_BYTES ? decode(frontBytes) : null;
                    notifyListener(ok, recording, text, rear, front);
                    Listener target = listener;
                    if (accepted && target != null && target.previewVisible()) MAIN.postDelayed(poll, 1000L);
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
            remote.send(request);
        } catch (RuntimeException | RemoteException error) {
            startSent = false;
            notifyListener(false, false, "Dash could not accept the recording request.", null, null);
        }
    }

    private static void notifyListener(boolean ok, boolean recording, String text, Bitmap rear, Bitmap front) {
        Listener target = listener;
        if (target == null) return;
        MAIN.post(() -> {
            Listener current = listener;
            if (current != null && current.previewVisible()) current.onUpdate(ok, recording, text, rear, front);
        });
    }

    public static synchronized void refreshPreview() {
        Listener target = listener;
        if (accepted && target != null && target.previewVisible()) MAIN.post(poll);
    }

    public static synchronized void finishPreview() {
        listener = null;
        previewPending = false;
        MAIN.removeCallbacks(poll);
    }

    public static synchronized void end(Context context) {
        finishPreview();
        accepted = false;
        startSent = false;
        previewPending = false;
        remote = null;
        if (bound && connection != null) {
            try { (app == null ? context.getApplicationContext() : app).unbindService(connection); }
            catch (RuntimeException ignored) { }
        }
        bound = false;
        connection = null;
        permissionActivity.clear();
    }
}
