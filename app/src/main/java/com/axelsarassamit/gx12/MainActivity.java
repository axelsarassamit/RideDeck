package com.axelsarassamit.gx12;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.text.InputType;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends android.app.Activity {
    private static final String REPOSITORY = "axelsarassamit/gearelec-gx12-companion";
    private static final int REQUEST_BLUETOOTH = 12;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView deviceStatus;
    private TextView updateStatus;
    private TextView trackStatus;
    private Button playPauseButton;
    private Button previousButton;
    private Button nextButton;
    private Chronometer rideClock;
    private Button rideButton;
    private File downloadedApk;
    private MediaController mediaController;
    private BluetoothDevice gx12Device;
    private BluetoothProfile a2dpProfile;
    private BluetoothProfile headsetProfile;
    private boolean a2dpRequested;
    private boolean headsetRequested;
    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) { a2dpProfile = proxy; a2dpRequested = true; }
            if (profile == BluetoothProfile.HEADSET) { headsetProfile = proxy; headsetRequested = true; }
            showBluetoothConnection();
        }
        @Override public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) { a2dpProfile = null; a2dpRequested = false; }
            if (profile == BluetoothProfile.HEADSET) { headsetProfile = null; headsetRequested = false; }
            showBluetoothConnection();
        }
    };
    private final Runnable trackRefresh = new Runnable() {
        @Override public void run() {
            refreshMediaSession();
            handler.postDelayed(this, 2500);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        refreshDeviceStatus();
        restoreRide();
    }

    @Override protected void onResume() {
        super.onResume();
        if (deviceStatus != null) refreshDeviceStatus();
        if (downloadedApk != null && canInstallPackages()) openInstaller(downloadedApk);
        handler.removeCallbacks(trackRefresh);
        handler.post(trackRefresh);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(trackRefresh);
        SharedPreferences prefs = getPreferences(0);
        if (prefs.getBoolean("ride_running", false) && rideClock != null) {
            prefs.edit().putLong("ride_elapsed", android.os.SystemClock.elapsedRealtime() - rideClock.getBase()).apply();
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            try { if (a2dpProfile != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile); } catch (Exception ignored) { }
            try { if (headsetProfile != null) adapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetProfile); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    private void buildScreen() {
        int ink = 0xff17212b, muted = 0xff5d6b78, blue = 0xff1666d9;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(24));
        page.setBackgroundColor(0xfff4f7fb);
        page.addView(text("GEARELEC GX12", 13, blue, true));
        TextView title = text("GX12 Ride Companion", 26, ink, true);
        LinearLayout.LayoutParams titleParams = params(); titleParams.topMargin = dp(6); page.addView(title, titleParams);
        page.addView(text("Ride tools, music controls, and app updates", 15, muted, false));

        deviceStatus = text("Checking paired devices…", 15, ink, false);
        addSection(page, "HEADSET", deviceStatus);
        Button bluetoothButton = button("Bluetooth settings");
        bluetoothButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        page.addView(bluetoothButton, buttonParams());

        TextView rideInfo = text("Keep your phone mounted and set navigation before moving.", 14, muted, false);
        addSection(page, "RIDE", rideInfo);
        rideClock = new Chronometer(this);
        rideClock.setTextSize(34); rideClock.setTextColor(ink); rideClock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        rideClock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clockParams = params(); clockParams.topMargin = dp(10); clockParams.bottomMargin = dp(6);
        page.addView(rideClock, clockParams);
        rideButton = button("Start ride");
        rideButton.setOnClickListener(v -> toggleRide());
        page.addView(rideButton, buttonParams());
        Button resetRideButton = button("Reset ride timer");
        resetRideButton.setOnClickListener(v -> resetRide());
        page.addView(resetRideButton, buttonParams());
        LinearLayout mapRow = new LinearLayout(this);
        mapRow.setOrientation(LinearLayout.HORIZONTAL);
        Button mapsButton = button("Open maps");
        mapsButton.setOnClickListener(v -> openMaps());
        Button destinationButton = button("Set destination");
        destinationButton.setOnClickListener(v -> setDestination());
        mapRow.addView(mapsButton, weightedButtonParams());
        mapRow.addView(destinationButton, weightedButtonParams());
        page.addView(mapRow);

        TextView checklistHeading = text("Before you ride", 15, ink, true);
        LinearLayout.LayoutParams checkHeadParams = params(); checkHeadParams.topMargin = dp(18); page.addView(checklistHeading, checkHeadParams);
        String[] reminders = {"Helmet secured", "GX12 fitted and fastened", "Phone mounted safely", "Route ready before moving"};
        for (int i = 0; i < reminders.length; i++) {
            CheckBox check = new CheckBox(this);
            check.setText(reminders[i]); check.setTextColor(ink);
            check.setChecked(getPreferences(0).getBoolean("check_" + i, false));
            final int index = i;
            check.setOnCheckedChangeListener((button, checked) -> getPreferences(0).edit().putBoolean("check_" + index, checked).apply());
            page.addView(check);
        }

        trackStatus = text("Checking Android media sessions…", 14, ink, false);
        addSection(page, "MUSIC", trackStatus);
        Button accessButton = button("Enable music controls");
        accessButton.setOnClickListener(v -> openNotificationAccess());
        page.addView(accessButton, buttonParams());
        LinearLayout mediaRow = new LinearLayout(this); mediaRow.setOrientation(LinearLayout.HORIZONTAL);
        previousButton = button("Previous"); previousButton.setOnClickListener(v -> sendMedia(MediaAction.PREVIOUS));
        playPauseButton = button("Play / pause"); playPauseButton.setOnClickListener(v -> sendMedia(MediaAction.TOGGLE));
        nextButton = button("Next"); nextButton.setOnClickListener(v -> sendMedia(MediaAction.NEXT));
        mediaRow.addView(previousButton, weightedButtonParams()); mediaRow.addView(playPauseButton, weightedButtonParams()); mediaRow.addView(nextButton, weightedButtonParams());
        page.addView(mediaRow);
        page.addView(text("Music buttons use Android’s active media session. Enabling them gives GX12 Companion notification access, which Android says can expose notifications. This app does not read or store notification text.", 12, muted, false));

        updateStatus = text("Current version " + appVersion() + ". Check GitHub for a signed update.", 14, ink, false);
        addSection(page, "APP UPDATES", updateStatus);
        Button updateButton = button("Check for updates"); updateButton.setOnClickListener(v -> checkForUpdate());
        page.addView(updateButton, buttonParams());

        TextView note = text("The GX12 exposes standard audio and hands-free Bluetooth services. No headset battery reading or documented custom control service was found.", 12, muted, false);
        LinearLayout.LayoutParams noteParams = params(); noteParams.topMargin = dp(20); page.addView(note, noteParams);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
    }

    private enum MediaAction { PREVIOUS, TOGGLE, NEXT }

    private void refreshMediaSession() {
        if (trackStatus == null) return;
        ComponentName listener = new ComponentName(this, GX12NotificationListener.class);
        if (!hasNotificationAccess(listener)) {
            mediaController = null;
            trackStatus.setText("Music controls are off. Enable access above to use Android’s current media player.");
            updateMediaButtons(false, false, false);
            return;
        }
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> sessions = manager.getActiveSessions(listener);
            mediaController = sessions.isEmpty() ? null : sessions.get(0);
            if (mediaController == null) {
                trackStatus.setText("No active media player. Start music or audio, then return here.");
                updateMediaButtons(false, false, false);
                return;
            }
            MediaMetadata metadata = mediaController.getMetadata();
            String title = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            PlaybackState state = mediaController.getPlaybackState();
            boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
            String label = title == null || title.isBlank() ? "Active media player" : title;
            if (artist != null && !artist.isBlank()) label += "\n" + artist;
            trackStatus.setText(label + (playing ? "\nPlaying" : "\nNot playing"));
            long actions = state == null ? 0 : state.getActions();
            updateMediaButtons(true, (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
                    (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
            playPauseButton.setText(playing ? "Pause" : "Play");
        } catch (SecurityException | IllegalStateException error) {
            mediaController = null;
            trackStatus.setText("Android has not granted access yet. Enable GX12 Companion under Notification access.");
            updateMediaButtons(false, false, false);
        }
    }

    private void updateMediaButtons(boolean active, boolean previous, boolean next) {
        if (previousButton == null) return;
        previousButton.setEnabled(active && previous);
        playPauseButton.setEnabled(active);
        nextButton.setEnabled(active && next);
    }

    private void sendMedia(MediaAction action) {
        refreshMediaSession();
        if (mediaController == null) return;
        try {
            MediaController.TransportControls controls = mediaController.getTransportControls();
            if (action == MediaAction.PREVIOUS) controls.skipToPrevious();
            else if (action == MediaAction.NEXT) controls.skipToNext();
            else {
                PlaybackState state = mediaController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) controls.pause();
                else controls.play();
            }
            handler.postDelayed(trackRefresh, 500);
        } catch (SecurityException ignored) {
            trackStatus.setText("This media player did not accept that command.");
        }
    }

    private void openNotificationAccess() {
        ComponentName listener = new ComponentName(this, GX12NotificationListener.class);
        if (hasNotificationAccess(listener)) {
            refreshMediaSession();
            return;
        }
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private boolean hasNotificationAccess(ComponentName listener) {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(listener.flattenToString());
    }

    private void toggleRide() {
        SharedPreferences prefs = getPreferences(0);
        boolean running = prefs.getBoolean("ride_running", false);
        if (running) {
            rideClock.stop();
            rideButton.setText("Resume ride");
            prefs.edit().putBoolean("ride_running", false).putLong("ride_elapsed", android.os.SystemClock.elapsedRealtime() - rideClock.getBase()).apply();
        } else {
            long elapsed = prefs.getLong("ride_elapsed", 0);
            rideClock.setBase(android.os.SystemClock.elapsedRealtime() - elapsed);
            rideClock.start();
            rideButton.setText("Pause ride");
            prefs.edit().putBoolean("ride_running", true).apply();
        }
    }

    private void restoreRide() {
        SharedPreferences prefs = getPreferences(0);
        boolean running = prefs.getBoolean("ride_running", false);
        long elapsed = prefs.getLong("ride_elapsed", 0);
        if (running) {
            rideClock.setBase(android.os.SystemClock.elapsedRealtime() - elapsed);
            rideClock.start(); rideButton.setText("Pause ride");
        } else if (elapsed > 0) {
            rideClock.setBase(android.os.SystemClock.elapsedRealtime() - elapsed);
            rideClock.setText(formatElapsed(elapsed));
            rideButton.setText("Resume ride");
        } else {
            rideButton.setText("Start ride");
        }
    }

    private String formatElapsed(long elapsed) {
        long seconds = elapsed / 1000, hours = seconds / 3600, minutes = (seconds % 3600) / 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds % 60);
    }

    private void resetRide() {
        getPreferences(0).edit().putBoolean("ride_running", false).putLong("ride_elapsed", 0).apply();
        rideClock.stop(); rideClock.setBase(android.os.SystemClock.elapsedRealtime()); rideClock.setText("00:00"); rideButton.setText("Start ride");
    }

    private void openMaps() {
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
        try { startActivity(maps); } catch (Exception ignored) { showRideMessage("No maps app is available on this phone."); }
    }

    private void setDestination() {
        EditText destination = new EditText(this);
        destination.setSingleLine(true);
        destination.setHint("Place or address");
        destination.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Find a destination")
                .setView(destination)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open maps", (dialog, which) -> {
                    String query = destination.getText().toString().trim();
                    if (query.isEmpty()) { openMaps(); return; }
                    Intent search = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)));
                    try { startActivity(search); } catch (Exception ignored) { showRideMessage("No maps app is available on this phone."); }
                }).show();
    }

    private void showRideMessage(String message) {
        trackStatus.setText(message);
    }

    private void addSection(LinearLayout parent, String heading, TextView content) {
        TextView label = text(heading, 12, 0xff63768b, true);
        LinearLayout.LayoutParams labelParams = params(); labelParams.topMargin = dp(23); parent.addView(label, labelParams);
        LinearLayout.LayoutParams contentParams = params(); contentParams.topMargin = dp(7);
        content.setPadding(dp(15), dp(14), dp(15), dp(14)); content.setBackgroundColor(0xffffffff); content.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(content, contentParams);
    }

    private void refreshDeviceStatus() {
        if (deviceStatus == null) return;
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
            deviceStatus.setText("Allow nearby-device access to check whether GX12 is paired."); return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) { deviceStatus.setText("This phone does not report Bluetooth support."); return; }
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                String name = device.getName();
                if (name != null && name.toUpperCase(Locale.ROOT).contains("GX12")) {
                    gx12Device = device;
                    deviceStatus.setText("Paired: " + name + "\nChecking audio and call connections…\nHeadset battery is not available to this app.");
                    if (!a2dpRequested) a2dpRequested = adapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP);
                    if (!headsetRequested) headsetRequested = adapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET);
                    showBluetoothConnection();
                    return;
                }
            }
            gx12Device = null;
            deviceStatus.setText("GX12 is not in this phone’s paired-device list. Use Bluetooth settings to pair it.");
        } catch (SecurityException error) { deviceStatus.setText("Bluetooth permission is needed to read the paired-device list."); }
    }

    private void showBluetoothConnection() {
        if (deviceStatus == null || gx12Device == null) return;
        try {
            String name = gx12Device.getName();
            String audio = a2dpProfile == null ? "checking" : connectionLabel(a2dpProfile.getConnectionState(gx12Device));
            String calls = headsetProfile == null ? "checking" : connectionLabel(headsetProfile.getConnectionState(gx12Device));
            deviceStatus.setText("Paired: " + (name == null ? "GX12" : name) + "\nMusic audio: " + audio + "\nCall audio: " + calls + "\nHeadset battery is not available to this app.");
        } catch (SecurityException error) {
            deviceStatus.setText("Bluetooth permission is needed to read connection status.");
        }
    }

    private String connectionLabel(int state) {
        if (state == BluetoothProfile.STATE_CONNECTED) return "connected";
        if (state == BluetoothProfile.STATE_CONNECTING) return "connecting";
        if (state == BluetoothProfile.STATE_DISCONNECTING) return "disconnecting";
        return "not connected";
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_BLUETOOTH) refreshDeviceStatus();
    }

    private void checkForUpdate() {
        updateStatus.setText("Checking the latest public GitHub release…");
        worker.execute(() -> {
            try {
                JSONObject release = getJson("https://api.github.com/repos/" + REPOSITORY + "/releases/latest");
                JSONArray assets = release.getJSONArray("assets"); String apkUrl = null, sumsUrl = null;
                for (int i = 0; i < assets.length(); i++) { JSONObject asset = assets.getJSONObject(i); if ("gx12-companion-release.apk".equals(asset.getString("name"))) apkUrl = asset.getString("browser_download_url"); if ("checksums.txt".equals(asset.getString("name"))) sumsUrl = asset.getString("browser_download_url"); }
                if (apkUrl == null || sumsUrl == null) throw new IllegalStateException("The latest release is missing its APK or checksum file.");
                String expectedHash = findHash(downloadText(sumsUrl), "gx12-companion-release.apk");
                File updateDir = new File(getCacheDir(), "updates"); if (!updateDir.exists() && !updateDir.mkdirs()) throw new IllegalStateException("Could not prepare the update folder.");
                File apk = new File(updateDir, "gx12-companion-release.apk"); downloadFile(apkUrl, apk);
                String actualHash = sha256(apk); if (!actualHash.equalsIgnoreCase(expectedHash)) { apk.delete(); throw new SecurityException("The downloaded APK failed its SHA-256 check."); }
                String tag = release.getString("tag_name"); int localCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; int releaseCode = parseVersionCode(tag);
                if (releaseCode <= localCode) { apk.delete(); showUpdateMessage("You have the latest version (" + appVersion() + ")."); return; }
                downloadedApk = apk; showUpdateMessage("Version " + tag + " is ready. Android will ask you to approve installation.");
                runOnUiThread(() -> { if (canInstallPackages()) openInstaller(apk); else startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); });
            } catch (Exception error) { showUpdateMessage("Update check failed: " + safeMessage(error)); }
        });
    }

    private int parseVersionCode(String tag) {
        try { String numeric = tag.startsWith("v") ? tag.substring(1) : tag; String[] pieces = numeric.split("\\."); int major = Integer.parseInt(pieces[0]); int minor = pieces.length > 1 ? Integer.parseInt(pieces[1]) : 0; int patch = pieces.length > 2 ? Integer.parseInt(pieces[2].replaceAll("[^0-9].*$", "")) : 0; return major * 10000 + minor * 100 + patch; }
        catch (Exception ignored) { throw new IllegalArgumentException("Release tag must use a numeric version such as v0.1.1."); }
    }

    private JSONObject getJson(String address) throws Exception { HttpURLConnection c = openConnection(address); c.setRequestProperty("Accept", "application/vnd.github+json"); try (InputStream in = c.getInputStream()) { return new JSONObject(readText(in)); } finally { c.disconnect(); } }
    private String downloadText(String address) throws Exception { HttpURLConnection c = openConnection(address); try (InputStream in = c.getInputStream()) { return readText(in); } finally { c.disconnect(); } }
    private HttpURLConnection openConnection(String address) throws Exception { HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(true); c.setRequestProperty("User-Agent", "GX12-Companion-Android"); return c; }
    private String readText(InputStream input) throws Exception { StringBuilder b = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) b.append(line).append('\n'); } return b.toString(); }
    private String findHash(String checksums, String filename) { for (String line : checksums.split("\\r?\\n")) { String[] f = line.trim().split("\\s+"); if (f.length >= 2 && f[1].replaceFirst("^\\*", "").equals(filename)) return f[0]; } throw new IllegalStateException("No SHA-256 checksum was published for the APK."); }
    private void downloadFile(String address, File target) throws Exception { HttpURLConnection c = openConnection(address); try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(target)) { byte[] buffer = new byte[8192]; int count; while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count); } finally { c.disconnect(); } }
    private String sha256(File file) throws Exception { MessageDigest d = MessageDigest.getInstance("SHA-256"); try (InputStream in = new java.io.FileInputStream(file)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) d.update(b, 0, n); } StringBuilder s = new StringBuilder(); for (byte v : d.digest()) s.append(String.format(Locale.ROOT, "%02x", v & 0xff)); return s.toString(); }
    private boolean canInstallPackages() { return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls(); }
    private void openInstaller(File apk) { try { Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".apkprovider", apk); Intent i = new Intent(Intent.ACTION_VIEW); i.setDataAndType(uri, "application/vnd.android.package-archive"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); if (downloadedApk != null && downloadedApk.equals(apk)) downloadedApk = null; startActivity(i); } catch (Exception e) { showUpdateMessage("Could not open Android’s installer: " + safeMessage(e)); } }
    private void showUpdateMessage(String message) { runOnUiThread(() -> updateStatus.setText(message)); }
    private String safeMessage(Exception error) { String m = error.getMessage(); return m == null || m.isBlank() ? error.getClass().getSimpleName() : m; }
    private String appVersion() { try { PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0); return info.versionName + " (" + info.versionCode + ")"; } catch (Exception ignored) { return "unknown"; } }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); return b; }
    private LinearLayout.LayoutParams params() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = params(); p.topMargin = dp(8); return p; }
    private LinearLayout.LayoutParams weightedButtonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); p.setMargins(dp(2), dp(4), dp(2), dp(4)); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

}
