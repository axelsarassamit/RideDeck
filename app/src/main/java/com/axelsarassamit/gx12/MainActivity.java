package com.axelsarassamit.gx12;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends android.app.Activity {
    private static final String REPOSITORY = "axelsarassamit/gearelec-gx12-companion";
    private static final int REQUEST_BLUETOOTH = 12;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView deviceStatus;
    private TextView updateStatus;
    private File downloadedApk;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        refreshDeviceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (deviceStatus != null) refreshDeviceStatus();
        if (downloadedApk != null && canInstallPackages()) openInstaller(downloadedApk);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void buildScreen() {
        int ink = 0xff17212b;
        int muted = 0xff5d6b78;
        int blue = 0xff1666d9;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(28), dp(24), dp(24));
        page.setBackgroundColor(0xfff4f7fb);

        TextView eyebrow = text("GEARELEC GX12", 13, blue, true);
        page.addView(eyebrow);
        TextView title = text("GX12 Companion", 28, ink, true);
        LinearLayout.LayoutParams titleParams = params();
        titleParams.topMargin = dp(6);
        page.addView(title, titleParams);
        TextView intro = text("Device status and app updates", 15, muted, false);
        page.addView(intro);

        deviceStatus = text("Checking paired devices…", 16, ink, false);
        addSection(page, "DEVICE", deviceStatus);
        Button bluetoothButton = button("Open Bluetooth settings");
        bluetoothButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        page.addView(bluetoothButton, buttonParams());

        updateStatus = text("Current version " + appVersion() + ". Check GitHub for a signed update.", 15, ink, false);
        addSection(page, "APP UPDATES", updateStatus);
        Button updateButton = button("Check for updates");
        updateButton.setOnClickListener(v -> checkForUpdate());
        page.addView(updateButton, buttonParams());

        TextView note = text("This app reports the pairing Android can see. The GX12 currently exposes standard audio and hands-free Bluetooth services; no documented custom control service was found.", 14, muted, false);
        LinearLayout.LayoutParams noteParams = params();
        noteParams.topMargin = dp(24);
        page.addView(note, noteParams);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        setContentView(scroll);
    }

    private void addSection(LinearLayout parent, String heading, TextView content) {
        TextView label = text(heading, 12, 0xff63768b, true);
        LinearLayout.LayoutParams labelParams = params();
        labelParams.topMargin = dp(28);
        parent.addView(label, labelParams);
        LinearLayout.LayoutParams contentParams = params();
        contentParams.topMargin = dp(8);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackgroundColor(0xffffffff);
        content.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(content, contentParams);
    }

    private void refreshDeviceStatus() {
        if (deviceStatus == null) return;
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
            deviceStatus.setText("Allow nearby-device access to check whether GX12 is paired.");
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            deviceStatus.setText("This phone does not report Bluetooth support.");
            return;
        }
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                String name = device.getName();
                if (name != null && name.toUpperCase(Locale.ROOT).contains("GX12")) {
                    deviceStatus.setText("GX12 is paired with this phone.\n" + name);
                    return;
                }
            }
            deviceStatus.setText("GX12 is not in this phone’s paired-device list. Use Bluetooth settings to pair it.");
        } catch (SecurityException error) {
            deviceStatus.setText("Bluetooth permission is needed to read the paired-device list.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_BLUETOOTH) refreshDeviceStatus();
    }

    private void checkForUpdate() {
        updateStatus.setText("Checking the latest public GitHub release…");
        worker.execute(() -> {
            try {
                JSONObject release = getJson("https://api.github.com/repos/" + REPOSITORY + "/releases/latest");
                JSONArray assets = release.getJSONArray("assets");
                String apkUrl = null;
                String sumsUrl = null;
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if ("gx12-companion-release.apk".equals(asset.getString("name"))) apkUrl = asset.getString("browser_download_url");
                    if ("checksums.txt".equals(asset.getString("name"))) sumsUrl = asset.getString("browser_download_url");
                }
                if (apkUrl == null || sumsUrl == null) throw new IllegalStateException("The latest release is missing its APK or checksum file.");
                String expectedHash = findHash(downloadText(sumsUrl), "gx12-companion-release.apk");
                File updateDir = new File(getCacheDir(), "updates");
                if (!updateDir.exists() && !updateDir.mkdirs()) throw new IllegalStateException("Could not prepare the update folder.");
                File apk = new File(updateDir, "gx12-companion-release.apk");
                downloadFile(apkUrl, apk);
                String actualHash = sha256(apk);
                if (!actualHash.equalsIgnoreCase(expectedHash)) {
                    apk.delete();
                    throw new SecurityException("The downloaded APK failed its SHA-256 check.");
                }
                String tag = release.getString("tag_name");
                int localCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                int releaseCode = parseVersionCode(tag);
                if (releaseCode <= localCode) {
                    apk.delete();
                    showUpdateMessage("You have the latest version (" + appVersion() + ").");
                    return;
                }
                downloadedApk = apk;
                showUpdateMessage("Version " + tag + " is ready. Android will ask you to approve installation.");
                runOnUiThread(() -> {
                    if (canInstallPackages()) openInstaller(apk);
                    else startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                });
            } catch (Exception error) {
                showUpdateMessage("Update check failed: " + safeMessage(error));
            }
        });
    }

    private int parseVersionCode(String tag) {
        try {
            String numeric = tag.startsWith("v") ? tag.substring(1) : tag;
            String[] pieces = numeric.split("\\.");
            int major = Integer.parseInt(pieces[0]);
            int minor = pieces.length > 1 ? Integer.parseInt(pieces[1]) : 0;
            int patch = pieces.length > 2 ? Integer.parseInt(pieces[2].replaceAll("[^0-9].*$", "")) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Release tag must use a numeric version such as v0.1.1.");
        }
    }

    private JSONObject getJson(String address) throws Exception {
        HttpURLConnection connection = openConnection(address);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try (InputStream input = connection.getInputStream()) {
            return new JSONObject(readText(input));
        } finally {
            connection.disconnect();
        }
    }

    private String downloadText(String address) throws Exception {
        HttpURLConnection connection = openConnection(address);
        try (InputStream input = connection.getInputStream()) {
            return readText(input);
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "GX12-Companion-Android");
        return connection;
    }

    private String readText(InputStream input) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }

    private String findHash(String checksums, String filename) {
        for (String line : checksums.split("\\r?\\n")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length >= 2 && fields[1].replaceFirst("^\\*", "").equals(filename)) return fields[0];
        }
        throw new IllegalStateException("No SHA-256 checksum was published for the APK.");
    }

    private void downloadFile(String address, File target) throws Exception {
        HttpURLConnection connection = openConnection(address);
        try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } finally {
            connection.disconnect();
        }
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private boolean canInstallPackages() {
        return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls();
    }

    private void openInstaller(File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".apkprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (downloadedApk != null && downloadedApk.equals(apk)) downloadedApk = null;
            startActivity(intent);
        } catch (Exception error) {
            showUpdateMessage("Could not open Android’s installer: " + safeMessage(error));
        }
    }

    private void showUpdateMessage(String message) {
        runOnUiThread(() -> updateStatus.setText(message));
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private String appVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName + " (" + info.versionCode + ")";
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams result = params();
        result.topMargin = dp(10);
        return result;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
