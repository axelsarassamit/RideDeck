package com.axelsarassamit.gx12;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.function.Consumer;

/** Connect the Bluetooth call-audio route before asking Android to listen. */
public final class HeadsetMicRoute {
    private final Context context;
    private final AudioManager audio;
    private boolean scoConnected, receiverRegistered;
    private final android.content.BroadcastReceiver scoState = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, android.content.Intent intent) {
            scoConnected = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED) == AudioManager.SCO_AUDIO_STATE_CONNECTED;
        }
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int previousMode;
    private AudioDeviceInfo previousDevice;
    private boolean owned, active;
    private long deadline;
    private Runnable ready;
    private Consumer<String> failed;
    public HeadsetMicRoute(Context context) { this.context = context; audio = context.getSystemService(AudioManager.class); }
    public void start(Runnable ready, Consumer<String> failed) {
        release(); this.ready = ready; this.failed = failed;
        try {
            previousMode = audio.getMode();
            if (previousMode != AudioManager.MODE_NORMAL) { fail("Audio is busy with a call or another voice app. End it and try again."); return; }
            if (Build.VERSION.SDK_INT >= 31) {
                AudioDeviceInfo headset = null;
                for (AudioDeviceInfo device : audio.getAvailableCommunicationDevices()) {
                    if (bluetooth(device.getType(), Build.VERSION.SDK_INT)) { headset = device; break; }
                }
                if (headset == null) { fail("No headset microphone connection. Connect your headset and enable Calls in its Bluetooth settings."); return; }
                previousDevice = audio.getCommunicationDevice(); owned = true;
                audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
                if (!audio.setCommunicationDevice(headset)) { fail("Android could not select the headset microphone."); return; }
            } else {
                if (!audio.isBluetoothScoAvailableOffCall()) { fail("This phone does not support headset microphone audio outside calls."); return; }
                owned = true; audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
                scoConnected = false;
                context.registerReceiver(scoState, new android.content.IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                receiverRegistered = true;
                audio.startBluetoothSco(); audio.setBluetoothScoOn(true);
            }
            active = true; deadline = SystemClock.elapsedRealtime() + 10000;
            handler.post(check);
        } catch (RuntimeException error) { fail("Could not connect headset microphone. Check Bluetooth call audio and permissions."); }
    }
    static boolean bluetooth(int type, int sdk) {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (sdk >= 31 && type == AudioDeviceInfo.TYPE_BLE_HEADSET);
    }
    private boolean connected() {
        if (Build.VERSION.SDK_INT >= 31) {
            AudioDeviceInfo current = audio.getCommunicationDevice();
            return current != null && bluetooth(current.getType(), Build.VERSION.SDK_INT);
        }
        return scoConnected;
    }
    private final Runnable check = new Runnable() {
        @Override public void run() {
            if (!active) return;
            try {
                if (connected()) {
                    if (ready != null) { Runnable next = ready; ready = null; next.run(); }
                } else if (ready == null || SystemClock.elapsedRealtime() >= deadline) {
                    fail("Headset microphone disconnected or did not connect. Phone microphone was not requested."); return;
                }
                if (active) handler.postDelayed(this, 300);
            } catch (RuntimeException error) { fail("Headset audio connection failed."); }
        }
    };
    private void fail(String message) {
        Consumer<String> callback = failed; release(); if (callback != null) callback.accept(message);
    }
    public boolean listening() { return active && ready == null; }
    public void cancelPending() { if (active && ready != null) release(); }
    public void release() {
        active = false; handler.removeCallbacks(check); ready = null; failed = null;
        if (receiverRegistered) { try { context.unregisterReceiver(scoState); } catch (RuntimeException ignored) { } receiverRegistered = false; }
        scoConnected = false;
        if (!owned) return; owned = false;
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                audio.clearCommunicationDevice();
                if (previousDevice != null) audio.setCommunicationDevice(previousDevice);
            } else { audio.setBluetoothScoOn(false); audio.stopBluetoothSco(); }
            if (audio.getMode() == AudioManager.MODE_IN_COMMUNICATION) audio.setMode(previousMode);
        } catch (RuntimeException ignored) { }
        previousDevice = null;
    }
}
