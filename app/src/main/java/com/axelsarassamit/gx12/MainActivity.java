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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends android.app.Activity {
    private static final String REPOSITORY = "axelsarassamit/gearelec-gx12-companion";
    private static final String YAMAHA_Y_CONNECT_PACKAGE = "jp.co.yamahamotor.yamahamotorcycleconnect.sccu";
    private static final String GARMIN_STREETCROSS_PACKAGE = "com.garmin.android.apps.streetcross";
    private static final int REQUEST_BLUETOOTH = 12;
    private static final int REQUEST_CAST = 23;
    private String castDeviceAddress;
    private TextView castStatus;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView deviceStatus;
    private TextView updateStatus;
    private TextView trackStatus;
    private TextView messagePreview;
    private TextView messageSource;
    private TextView dockMessage;
    private Button playPauseButton;
    private Button previousButton;
    private Button nextButton;
    private android.widget.ImageView albumArt;
    private android.speech.tts.TextToSpeech speech;
    private boolean speechReady;
    private String messageApp;
    private GX12NotificationListener.NotificationPreview voiceReplyTarget;
    private GX12NotificationListener.NotificationPreview newestSeen;
    private boolean setupVisible;
    private boolean cockpitVisible;
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
            refreshWhatsAppPreview();
            if (castStatus != null) castStatus.setText(YamahaCastService.status);
            handler.postDelayed(this, 2500);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) castDeviceAddress = state.getString("cast_device");
        buildScreen();
        refreshDeviceStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (deviceStatus != null) refreshDeviceStatus();
        refreshWhatsAppPreview();
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
        if (speech != null) { speech.stop(); speech.shutdown(); }
        worker.shutdownNow();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            try { if (a2dpProfile != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile); } catch (Exception ignored) { }
            try { if (headsetProfile != null) adapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetProfile); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) ScreenChrome.apply(getWindow(), !setupVisible);
    }

    private void buildSetupScreen() {
        setupVisible = true; cockpitVisible = false;
        albumArt = null; messagePreview = null; messageSource = null; dockMessage = null;
        trackStatus = null; previousButton = null; playPauseButton = null; nextButton = null;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff101419); root.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text("Setup - use while parked", 24, 0xfff4f6fa, true);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        Button back = rideAction("Done", false); back.setOnClickListener(v -> buildScreen());
        header.addView(back, new LinearLayout.LayoutParams(dp(120), dp(56))); root.addView(header);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        Button personalize = button("LAYOUT + APP CHOICES"); personalize.setOnClickListener(v -> showPersonalization());
        addSection(page, "YOUR COCKPIT", personalize);
        android.widget.Spinner bikes = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> bikeOptions = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, RidePreferences.BIKE_NAMES);
        bikes.setAdapter(bikeOptions); bikes.setBackgroundColor(0xffdce5ee); bikes.setMinimumHeight(dp(64));
        bikes.setSelection(Math.max(0, Math.min(RidePreferences.prefs(this).getInt("bike_profile", 1), RidePreferences.BIKE_NAMES.length - 1)));
        bikes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                RidePreferences.prefs(MainActivity.this).edit().putInt("bike_profile", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        page.addView(text("YOUR BIKE", 16, 0xfff4f6fa, true)); page.addView(bikes, new LinearLayout.LayoutParams(-1, dp(64)));
        page.addView(text("Requires a Yamaha navigation dash compatible with Garmin StreetCross. Display size is detected from the connected CCU. Listed models are based on Pillion reports, not RideBridge hardware tests.", 14, 0xffaab4c0, false));
        Button access = button("MUSIC + MESSAGE ACCESS"); access.setOnClickListener(v -> openNotificationAccess());
        page.addView(access, buttonParams());
        deviceStatus = text("Checking headset-", 14, 0xfff4f6fa, false);
        addCockpitCard(page, "CONNECTIONS", deviceStatus);
        Button bluetooth = button("BLUETOOTH SETTINGS"); bluetooth.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        page.addView(bluetooth, buttonParams());
        castStatus = text(YamahaCastService.status, 14, 0xfff4f6fa, false);
        addCockpitCard(page, "BIKE DISPLAY - EXPERIMENTAL", castStatus);
        Button cast = button(YamahaCastService.active ? "STOP BIKE DISPLAY" : "START BIKE DISPLAY");
        cast.setOnClickListener(v -> {
            if (YamahaCastService.active) { stopService(new Intent(this, YamahaCastService.class)); handler.postDelayed(this::buildSetupScreen, 400); }
            else chooseDash();
        }); page.addView(cast, buttonParams());
        Button display = button("HOW TO CAST TO THE BIKE"); display.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Bluetooth map sharing")
            .setMessage("Pair your phone with the Yamaha in Bluetooth settings. On the bike, open its navigation screen. Tap Start bike display in Setup, select the Yamaha and approve screen sharing. Then open your map app. Mobile data can provide the map internet connection. No Wi-Fi configuration is needed for ordinary screen sharing. The shared map also remains visible on the phone.")
            .setPositiveButton("Got it", null).show());
        page.addView(display, buttonParams());
        Button notifications = button("CASTING NOTIFICATION ACCESS"); notifications.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 24);
            else android.widget.Toast.makeText(this, "Available when casting starts", android.widget.Toast.LENGTH_SHORT).show();
        }); page.addView(notifications, buttonParams());
        updateStatus = text("Installed " + appVersion(), 14, 0xfff4f6fa, false);
        addCockpitCard(page, "APP UPDATES", updateStatus);
        Button update = button("CHECK FOR UPDATES"); update.setOnClickListener(v -> checkForUpdate()); page.addView(update, buttonParams());
        page.addView(text("Install updates while parked. Android may show an installation confirmation and a Google Play Protect scan. These screens are controlled by Android.", 14, 0xffaab4c0, false));
        Button advanced = button("ADVANCED DISPLAY OPTIONS"); advanced.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Separate map screen - experimental")
            .setMessage("Only this optional mode uses Wireless debugging to put the map on a separate display while keeping phone controls visible. The current implementation may stop when Wi-Fi disconnects. Use normal Cast for mobile-data-only rides.")
            .setNegativeButton("Close", null).setPositiveButton("Open advanced setup", (dialog, which) -> showDisplaySetup()).show());
        page.addView(advanced, buttonParams());
        Button about = button("ABOUT + DISPLAY HELP"); about.setOnClickListener(v -> showAbout()); page.addView(about, buttonParams());
        ScrollView scroll = new ScrollView(this); scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left + dp(16), bars.top + dp(12), bars.right + dp(16), bars.bottom + dp(12)); return insets;
        });
        setContentView(root); ScreenChrome.apply(getWindow(), false);
        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }

    private void addCastControls(LinearLayout page) {
        castStatus = text(YamahaCastService.status, 15, 0xfff4f6fa, false);
        addCockpitCard(page, "YAMAHA DASH • EXPERIMENTAL", castStatus);
        Button start = button("CAST MAP TO YAMAHA DASH"); start.setMinHeight(dp(80));
        start.setOnClickListener(v -> chooseDash()); page.addView(start, buttonParams());
        Button stop = button("STOP CASTING"); stop.setMinHeight(dp(72));
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, YamahaCastService.class));
            handler.postDelayed(() -> castStatus.setText(YamahaCastService.status), 300);
        }); page.addView(stop, buttonParams());
        Button about = button("CAST SETUP + ABOUT"); about.setOnClickListener(v -> showAbout());
        page.addView(about, buttonParams());
    }

    private void chooseDash() {
        if (YamahaCastService.active) { castStatus.setText("Already sharing. Stop before starting another session."); return; }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
            castStatus.setText("Allow nearby devices, then tap Cast again."); return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) { castStatus.setText("Turn on Bluetooth before casting."); return; }
        java.util.ArrayList<BluetoothDevice> devices = new java.util.ArrayList<>();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            String name = device.getName();
            if (name != null && (name.toUpperCase(Locale.ROOT).contains("CCU") || name.toUpperCase(Locale.ROOT).contains("YAMAHA"))) devices.add(device);
        }
        if (devices.isEmpty()) { castStatus.setText("No paired Yamaha CCU found. Pair your dash using the bike's normal setup first. A GX12 headset is not the dash."); return; }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) names[i] = devices.get(i).getName() + "\n" + devices.get(i).getAddress();
        new android.app.AlertDialog.Builder(this).setTitle("Choose your Yamaha dash")
            .setItems(names, (dialog, which) -> {
                castDeviceAddress = devices.get(which).getAddress();
                if (DedicatedDisplay.ready) {
                    Intent dedicated = new Intent(this, YamahaCastService.class)
                        .putExtra("device", castDeviceAddress).putExtra("dedicated", true);
                    startForegroundService(dedicated); castDeviceAddress = null; buildScreen(); return;
                }
                new android.app.AlertDialog.Builder(this).setTitle("Share " + RidePreferences.mapName(this) + " with your bike")
                    .setMessage("Test while parked. Close StreetCross or other dash casting apps. On the next Android screen, choose your selected navigation/rider app if single-app sharing is offered. Whole-screen sharing also shows messages and other visible content. Rotate the phone landscape for a larger map. Open the dash navigation view using its normal controls.")
                    .setPositiveButton("Choose screen", (d, w) -> {
                        android.media.projection.MediaProjectionManager manager = getSystemService(android.media.projection.MediaProjectionManager.class);
                        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAST);
                    }).setNegativeButton("Cancel", null).show();
            }).setNegativeButton("Cancel", null).show();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("cast_device", castDeviceAddress); super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 82) {
            GX12NotificationListener.NotificationPreview target = voiceReplyTarget; voiceReplyTarget = null;
            java.util.ArrayList<String> words = data == null ? null : data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (result == RESULT_OK && target != null && words != null && !words.isEmpty() && !words.get(0).trim().isEmpty()) confirmVoiceReply(target, words.get(0));
            return;
        }
        if (request == REQUEST_CAST && result == RESULT_OK && data != null && castDeviceAddress != null) {
            try {
                Intent service = new Intent(this, YamahaCastService.class).putExtra("capture", data)
                    .putExtra("result", result).putExtra("device", castDeviceAddress);
                startForegroundService(service);
                castStatus.setText("Starting screen sharing… Open Maps when ready.");
            } catch (Exception e) { castStatus.setText("Could not start casting. Return to RideBridge and try again."); }
        } else if (request == REQUEST_CAST) castStatus.setText("Sharing cancelled. Nothing is being cast.");
        castDeviceAddress = null;
    }

    private void showAbout() {
        String license;
        try (InputStream stream = getAssets().open("PILLION_LICENSE.md")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) content.append(line).append('\n');
            license = content.toString();
        } catch (Exception e) { license = "https://polyformproject.org/licenses/noncommercial/1.0.0/"; }
        new android.app.AlertDialog.Builder(this).setTitle("RideBridge - " + RidePreferences.bikeName(this))
            .setMessage("Dash casting is experimental. Select your paired Yamaha CCU, approve screen sharing, open Google Maps and use the dash navigation view. Compatible NaviLite CCUs use their detected display size. Model selection alone does not establish compatibility.\n\nRequired Notice: Copyright 2026 the Pillion authors\nProtocol adapted from github.com/alexandrevega/pillion, revision 29497f4. Noncommercial personal and hobby use. Independent of Yamaha and Pillion.\n\n" + license)
            .setPositiveButton("Close", null).show();
    }

    private void showDisplaySetup() {
        if (Build.VERSION.SDK_INT < 30) {
            new android.app.AlertDialog.Builder(this).setMessage("Bike-only Maps requires Android 11 or newer. Ordinary screen sharing is available.")
                .setPositiveButton("Close", null).show(); return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Bike display • " + RidePreferences.mapName(this))
            .setMessage("Park the bike. Connect the phone to Wi-Fi for setup. In Developer options, enable Wireless debugging. This grants RideBridge debugging access to this phone so it can create a separate map display. No computer or paid Maps API is needed.\n\n1. Pair RideBridge using the port and six-digit code from Pair device with pairing code.\n2. Connect using the different port on the main Wireless debugging screen.\n3. Tap Start bike display in Setup and select the Yamaha dash.\n\nRepeat Connect after ending a session or restarting the phone. You can revoke RideBridge in Wireless debugging > Paired devices. Phone brands may block the separate display. We do not enable legacy TCP debugging or change phone power settings.")
            .setNeutralButton("Developer options", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)))
            .setNegativeButton("Pair", (dialog, which) -> displayPairDialog())
            .setPositiveButton("Connect", (dialog, which) -> displayConnectDialog()).show();
    }

    private EditText numericField(String hint) {
        EditText field = new EditText(this); field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER); return field;
    }

    private void displayPairDialog() {
        LinearLayout inputs = new LinearLayout(this); inputs.setOrientation(LinearLayout.VERTICAL);
        inputs.setPadding(dp(24), dp(12), dp(24), 0);
        EditText port = numericField("Pairing port"); EditText code = numericField("6-digit pairing code");
        code.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        inputs.addView(port); inputs.addView(code);
        new android.app.AlertDialog.Builder(this).setTitle("Pair with this phone")
            .setMessage("Use the values in Wireless debugging > Pair device with pairing code. Keep its code dialog open in split-screen if your phone closes pairing when switching apps. We connect locally to this phone only.")
            .setView(inputs).setNegativeButton("Cancel", null).setPositiveButton("Pair", (dialog, which) -> {
                String pairingPort = port.getText().toString().trim(), pairingCode = code.getText().toString().trim();
                worker.execute(() -> {
                    try {
                        DedicatedDisplay.pair(this, Integer.parseInt(pairingPort), pairingCode);
                        runOnUiThread(() -> new android.app.AlertDialog.Builder(this).setMessage(DedicatedDisplay.status)
                            .setPositiveButton("Connect", (d, w) -> displayConnectDialog()).show());
                    } catch (Exception e) { runOnUiThread(() -> displayError("Pairing failed. " + safeMessage(e))); }
                });
            }).show();
    }

    private void displayConnectDialog() {
        EditText port = numericField("Connection port");
        new android.app.AlertDialog.Builder(this).setTitle("Prepare bike display")
            .setMessage("Enter the port after the colon in IP address & port on the main Wireless debugging screen. This is different from the pairing port. Keep Wi-Fi and Wireless debugging on for this step.")
            .setView(port).setNegativeButton("Cancel", null).setPositiveButton("Connect", (dialog, which) -> {
                String connectionPort = port.getText().toString().trim();
                worker.execute(() -> {
                    try {
                        DedicatedDisplay.prepare(this, Integer.parseInt(connectionPort));
                        runOnUiThread(() -> { buildSetupScreen(); chooseDash(); });
                    } catch (Exception e) { runOnUiThread(() -> displayError("Display setup failed. " + safeMessage(e))); }
                });
            }).show();
    }

    private void displayError(String message) {
        new android.app.AlertDialog.Builder(this).setTitle("Bike display setup")
            .setMessage(message).setPositiveButton("Close", null).show();
    }

    private void rideMapAction() {
        if (YamahaCastService.active && DedicatedDisplay.ready) {
            if (!RidePreferences.selectedMap(this).equals("com.google.android.apps.maps")) {
                displayError(RidePreferences.mapName(this) + " is selected for the bike display. Set its route/job in that app before casting. RideBridge cannot accept delivery jobs or set routes inside it."); return;
            }
            EditText destination = new EditText(this); destination.setHint("Address or place name");
            new android.app.AlertDialog.Builder(this).setTitle("Destination on bike display").setView(destination)
                .setNegativeButton("Cancel", null).setPositiveButton("Navigate", (dialog, which) -> {
                    String query = destination.getText().toString().trim();
                    if (query.isEmpty()) return;
                    worker.execute(() -> {
                        try { DedicatedDisplay.route(query); }
                        catch (Exception e) { runOnUiThread(() -> displayError(safeMessage(e))); }
                    });
                }).show();
        } else openMapsAdjacent();
    }

    private void buildAppDock() { buildScreen(); }

    private void buildScreen() {
        setupVisible = false; cockpitVisible = true;
        messagePreview = null; messageSource = null; dockMessage = null; albumArt = null;
        rideClock = null; rideButton = null;
        deviceStatus = text("", 12, 0xffaab4c0, false);
        updateStatus = text("", 12, 0xffaab4c0, false);
        boolean portrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        boolean compact = getResources().getConfiguration().screenWidthDp < 580
            || getResources().getConfiguration().screenHeightDp < 480;
        boolean controlsRight = controlsOnRight();
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff0b1017); root.setPadding(dp(12), dp(8), dp(12), dp(8));
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left + dp(12), bars.top + dp(compact ? 4 : 8), bars.right + dp(12), bars.bottom + dp(compact ? 4 : 8));
            return insets;
        });
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("RideBridge", 20, 0xfff4f6fa, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        castStatus = text(YamahaCastService.status, 11, 0xff92a9be, false);
        castStatus.setMaxLines(1); castStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView clock = new android.widget.TextClock(this); ((android.widget.TextClock) clock).setFormat24Hour("HH:mm");
        ((android.widget.TextClock) clock).setFormat12Hour("h:mm"); clock.setTextColor(0xffaab4c0); clock.setTextSize(17);
        header.addView(clock); root.addView(header, new LinearLayout.LayoutParams(-1, dp(compact ? 40 : 56)));

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout music = rideCard("NOW PLAYING");
        if (compact) {
            music.removeAllViews(); music.setOrientation(LinearLayout.HORIZONTAL);
            music.setGravity(Gravity.CENTER_VERTICAL); music.setPadding(dp(8), dp(4), dp(8), dp(4));
        }
        LinearLayout details = new LinearLayout(this); details.setGravity(Gravity.CENTER_VERTICAL);
        if (!compact) {
            albumArt = new android.widget.ImageView(this); albumArt.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            albumArt.setImageResource(android.R.drawable.ic_media_play);
            LinearLayout.LayoutParams art = new LinearLayout.LayoutParams(dp(58), dp(58)); art.rightMargin = dp(12);
            details.addView(albumArt, art);
        }
        trackStatus = text("Open Spotify to start listening", compact ? 16 : 19, 0xfff4f6fa, true);
        trackStatus.setMaxLines(compact ? 2 : 3); trackStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        details.addView(trackStatus, new LinearLayout.LayoutParams(0, -1, 1));
        music.addView(details, compact ? new LinearLayout.LayoutParams(0, -1, 1) : new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout transport = new LinearLayout(this);
        previousButton = rideAction("|◀", false); previousButton.setContentDescription("Previous track");
        previousButton.setOnClickListener(v -> sendMedia(MediaAction.PREVIOUS));
        playPauseButton = rideAction("▶", true); playPauseButton.setContentDescription("Play or pause music");
        playPauseButton.setOnClickListener(v -> sendMedia(MediaAction.TOGGLE));
        nextButton = rideAction("▶|", false); nextButton.setContentDescription("Next track");
        nextButton.setOnClickListener(v -> sendMedia(MediaAction.NEXT));
        transport.addView(previousButton, rideWeight(compact ? 56 : 72)); transport.addView(playPauseButton, rideWeight(compact ? 56 : 72));
        transport.addView(nextButton, rideWeight(compact ? 56 : 72)); music.addView(transport, compact ? new LinearLayout.LayoutParams(dp(180), -2) : new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams musicParams = compact
            ? new LinearLayout.LayoutParams(-1, dp(72))
            : new LinearLayout.LayoutParams(0, -1, 0.8f);
        if (compact) musicParams.bottomMargin = dp(8); else musicParams.rightMargin = dp(10);
        controls.addView(music, musicParams);

        LinearLayout messages = rideCard("MESSAGES");
        messageSource = text("LATEST MESSAGE", compact ? 13 : 20, 0xff83b5ff, true);
        messageSource.setMaxLines(1); messageSource.setEllipsize(android.text.TextUtils.TruncateAt.END);
        messages.addView(messageSource);
        messagePreview = text("No new messages", compact ? 14 : 16, 0xffc8d3df, false);
        messagePreview.setTextSize(compact ? 16 : portrait ? 20 : 18);
        messagePreview.setMaxLines(portrait ? 7 : 4); messagePreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        messagePreview.setOnClickListener(v -> showFullMessage());
        messageSource.setOnClickListener(v -> showFullMessage());
        messages.setOnClickListener(v -> showFullMessage());
        messages.addView(messagePreview, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout messageActions = new LinearLayout(this);
        Button listen = rideAction("Read aloud", false); listen.setOnClickListener(v -> readMessageAloud());
        messageActions.addView(listen, rideWeight(56));
        Button seenNext = rideAction("Seen / next", false);
        seenNext.setOnClickListener(v -> acknowledgeMessage(displayedMessage()));
        messageActions.addView(seenNext, rideWeight(56));
        messages.addView(messageActions);
        controls.addView(messages, compact ? new LinearLayout.LayoutParams(-1, 0, 1)
            : new LinearLayout.LayoutParams(0, -1, 1));
        if (!compact && controlsRight) {
            controls.removeView(music); controls.addView(music);
            musicParams.rightMargin = 0; musicParams.leftMargin = dp(10); music.setLayoutParams(musicParams);
        }
        workspace.addView(controls, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, 0, 1); wp.topMargin = dp(8);
        root.addView(workspace, wp);

        LinearLayout dock = new LinearLayout(this);
        Button settings = rideAction("Setup", false);
        settings.setOnClickListener(v -> buildSetupScreen());
        header.addView(settings, new LinearLayout.LayoutParams(dp(80), dp(compact ? 40 : 56)));
        String[] labels = new String[]{"Map", "Camera", "Voice"};
        if (controlsRight) java.util.Collections.reverse(java.util.Arrays.asList(labels));
        for (String label : labels) {
            Button action = rideAction(label, false);
            action.setOnClickListener(v -> {
                switch (label) {
                    case "Map": rideMapAction(); break;
                    case "Camera": showQuickCamera(); break;
                    case "Voice": startGoogleVoice(); break;
                }
            }); dock.addView(action, rideWeight(compact ? 56 : 64));
        }
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2); dp.topMargin = dp(8);
        root.addView(dock, dp);
        setContentView(root);
        ScreenChrome.apply(getWindow(), true);
        androidx.core.view.ViewCompat.requestApplyInsets(root);
        refreshMediaSession(); refreshWhatsAppPreview();
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private LinearLayout rideCard(String label) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(8)); card.setBackground(rideBackground(0xff18212d, 18));
        TextView title = text(label, 11, 0xff92a9be, true); title.setLetterSpacing(0.12f);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(18))); return card;
    }

    private android.graphics.drawable.GradientDrawable rideBackground(int color, int radius) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius)); return shape;
    }

    private Button rideAction(String label, boolean primary) {
        Button action = button(label); action.setAllCaps(false); action.setTextSize(16);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD); action.setMinWidth(0); action.setMinimumWidth(0);
        action.setMinHeight(dp(56)); action.setMinimumHeight(dp(56));
        action.setPadding(dp(4), dp(4), dp(4), dp(4)); action.setMaxLines(1);
        action.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{0xff8191a1, primary ? 0xff0b1017 : 0xfff4f6fa}));
        action.setBackgroundTintList(null);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, rideBackground(0xff24303d, 14));
        states.addState(new int[]{android.R.attr.state_pressed}, rideBackground(0xff526c87, 14));
        states.addState(new int[]{}, rideBackground(primary ? 0xff90c8ff : 0xff263548, 14));
        action.setBackground(states); return action;
    }

    private LinearLayout.LayoutParams rideWeight(int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(height), 1);
        p.setMargins(dp(3), dp(4), dp(3), 0); return p;
    }

    private LinearLayout.LayoutParams rideParams(int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(height)); p.topMargin = dp(8); return p;
    }

    private void castOrStop() {
        if (YamahaCastService.active) {
            stopService(new Intent(this, YamahaCastService.class));
            android.widget.Toast.makeText(this, "Casting stopped", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            if (castStatus == null) castStatus = text("", 14, 0xfff4f6fa, false);
            chooseDash();
        }
    }

    private GX12NotificationListener.NotificationPreview displayedMessage() {
        List<GX12NotificationListener.NotificationPreview> messages = GX12NotificationListener.selectedPreviews(this);
        if (messages.isEmpty()) return null;
        if (messages.get(0) != newestSeen) { newestSeen = messages.get(0); messageApp = null; }
        return messages.get(0);
    }

    private void chooseMessageSource() {
        String[] packages = RidePreferences.messagePackages(this);
        java.util.List<String> names = new java.util.ArrayList<>(), choices = new java.util.ArrayList<>();
        names.add("Latest from all apps"); choices.add(null);
        java.util.Set<String> selected = RidePreferences.selectedMessages(this);
        for (int i = 0; i < packages.length; i++) if (!packages[i].isEmpty() && selected.contains(packages[i])) {
            names.add(RidePreferences.MESSAGE_NAMES[i]); choices.add(packages[i]);
        }
        new android.app.AlertDialog.Builder(this).setTitle("Message source")
            .setItems(names.toArray(new String[0]), (dialog, which) -> { messageApp = choices.get(which); refreshWhatsAppPreview(); })
            .setNegativeButton("Close", null).show();
    }

    private void acknowledgeMessage(GX12NotificationListener.NotificationPreview item) {
        if (item == null) return;
        GX12NotificationListener.acknowledge(item);
        if (item.markRead != null) try { item.markRead.send(); }
        catch (android.app.PendingIntent.CanceledException ignored) { }
        refreshWhatsAppPreview();
    }

    private void showFullMessage() {
        GX12NotificationListener.NotificationPreview item = displayedMessage();
        android.app.Dialog reader = new android.app.Dialog(this);
        reader.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xff101419);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(page, (view, insets) -> {
            androidx.core.graphics.Insets edges = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(edges.left + dp(16), edges.top + dp(16), edges.right + dp(16), edges.bottom + dp(16)); return insets;
        });
        TextView source = text(item == null ? "Messages" : item.appName + " - " + item.title, 24, 0xff83b5ff, true);
        page.addView(source);
        TextView body = text(item == null ? "No message available yet. Enable music + message access in Setup and check notification previews in the selected messaging apps." : item.text, 26, 0xfff4f6fa, false);
        body.setPadding(0, dp(16), 0, dp(16));
        ScrollView scroll = new ScrollView(this); scroll.addView(body); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = new LinearLayout(this);
        Button close = rideAction("Close", false); close.setOnClickListener(v -> reader.dismiss()); actions.addView(close, rideWeight(72));
        Button read = rideAction("Seen / next", false);
        read.setEnabled(item != null);
        read.setOnClickListener(v -> {
            acknowledgeMessage(item); reader.dismiss();
            if (displayedMessage() != null) showFullMessage();
        });
        Button reply = rideAction("Reply", true); reply.setEnabled(item != null);
        reply.setOnClickListener(v -> { reader.dismiss(); replyByVoice(item); }); actions.addView(reply, rideWeight(72));
        Button aloud = rideAction("Read aloud", false); aloud.setEnabled(item != null);
        aloud.setOnClickListener(v -> readMessageAloud(item)); actions.addView(aloud, rideWeight(72)); actions.addView(read, rideWeight(72));
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            LinearLayout rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
            for (int row = 0; row < 2; row++) {
                LinearLayout pair = new LinearLayout(this);
                for (int column = 0; column < 2; column++) {
                    android.view.View action = actions.getChildAt(0); actions.removeViewAt(0); pair.addView(action, rideWeight(72));
                } rows.addView(pair);
            } page.addView(rows);
        } else page.addView(actions);
        body.setOnClickListener(v -> reader.dismiss());
        reader.setContentView(page); reader.show();
        if (reader.getWindow() != null) {
            reader.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff101419));
            reader.getWindow().setLayout(-1, -1);
            ScreenChrome.apply(reader.getWindow(), true);
            androidx.core.view.ViewCompat.requestApplyInsets(page);
        }
    }

    private void replyByVoice(GX12NotificationListener.NotificationPreview target) {
        if (RidePreferences.prefs(this).getInt("reply_mode", 0) == 1 || target.reply == null || target.replyInput == null) {
            new android.app.AlertDialog.Builder(this).setTitle("Reply in " + target.appName)
                .setMessage("Voice messages are recorded in the original app. If this notification has no text reply action, reply there too.")
                .setNegativeButton("Cancel", null).setPositiveButton("Open conversation", (d, w) -> {
                    try { if (target.open != null) target.open.send(); else launchChosenApp(target.packageName); }
                    catch (android.app.PendingIntent.CanceledException e) { launchChosenApp(target.packageName); }
                }).show(); return;
        }
        Intent speechIntent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Reply to " + target.title + " in " + target.appName);
        speechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        voiceReplyTarget = target;
        try { startActivityForResult(speechIntent, 82); }
        catch (android.content.ActivityNotFoundException e) { voiceReplyTarget = null; android.widget.Toast.makeText(this, "No speech recognition app is available", android.widget.Toast.LENGTH_LONG).show(); }
    }

    private void confirmVoiceReply(GX12NotificationListener.NotificationPreview target, String words) {
        new android.app.AlertDialog.Builder(this).setTitle("Send to " + target.title + " - " + target.appName)
            .setMessage(words).setNegativeButton("Cancel", null).setNeutralButton("Speak again", (d, w) -> replyByVoice(target))
            .setPositiveButton("Send", (d, w) -> {
                if (!RidePreferences.selectedMessages(this).contains(target.packageName) || target.reply == null || target.replyInput == null) return;
                android.os.Bundle results = new android.os.Bundle(); results.putCharSequence(target.replyInput.getResultKey(), words);
                Intent response = new Intent();
                android.app.RemoteInput.addResultsToIntent(new android.app.RemoteInput[]{target.replyInput}, response, results);
                try { target.reply.send(this, 0, response); android.widget.Toast.makeText(this, "Reply handed to " + target.appName, android.widget.Toast.LENGTH_SHORT).show(); }
                catch (android.app.PendingIntent.CanceledException e) { android.widget.Toast.makeText(this, "Reply expired. Open the conversation to reply.", android.widget.Toast.LENGTH_LONG).show(); }
            }).show();
    }

    private void showQuickCamera() {
        LinearLayout choices = new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(dp(16), dp(8), dp(16), dp(8));
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setTitle("Quick camera")
            .setView(choices).setNegativeButton("Cancel", null).create();
        for (boolean video : new boolean[]{false, true}) {
            LinearLayout row = new LinearLayout(this);
            for (boolean front : new boolean[]{false, true}) {
                Button action = rideAction((front ? "Front" : "Rear") + (video ? " video" : " photo"), false);
                action.setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, QuickCameraActivity.class).putExtra("front", front).putExtra("video", video));
                }); row.addView(action, rideWeight(72));
            } choices.addView(row);
        } dialog.show();
    }

    private void showPersonalization() {
        new android.app.AlertDialog.Builder(this).setTitle("Your cockpit")
            .setItems(new String[]{"Phone mount: Left / Centre / Right", "Messaging apps (choose several)", "Navigation / rider app", "Reply method", "Controls / map placement"}, (dialog, which) -> {
                if (which == 0) new android.app.AlertDialog.Builder(this).setTitle("Phone mount position")
                    .setSingleChoiceItems(new String[]{"Left: music controls on left", "Centre: controls on left", "Right: music controls on right"}, RidePreferences.prefs(this).getInt("mount", 1), (d, selected) -> {
                        RidePreferences.prefs(this).edit().putInt("mount", selected).apply(); d.dismiss(); buildScreen();
                    }).setNegativeButton("Cancel", null).show();
                else if (which == 1) chooseMessageApps(); else if (which == 2) chooseMapApp();
                else if (which == 4) showLayoutChoice();
                else new android.app.AlertDialog.Builder(this).setTitle("Reply method")
                    .setSingleChoiceItems(new String[]{"Voice to text - confirm before sending", "Voice message - open original app"}, RidePreferences.prefs(this).getInt("reply_mode", 0), (d, choice) -> {
                        RidePreferences.prefs(this).edit().putInt("reply_mode", choice).apply(); d.dismiss();
                    }).setNegativeButton("Close", null).show();
            }).setNegativeButton("Close", null).show();
    }

    private boolean controlsOnRight() {
        int layout = RidePreferences.prefs(this).getInt("controls_side", 0);
        return layout == 2 || (layout == 0 && RidePreferences.prefs(this).getInt("mount", 1) == 2);
    }

    private void showLayoutChoice() {
        new android.app.AlertDialog.Builder(this).setTitle("Controls / map placement")
            .setSingleChoiceItems(new String[]{"Automatic: left for left/centre mount", "Controls left / map right", "Controls right / map left"},
                RidePreferences.prefs(this).getInt("controls_side", 0), (dialog, choice) -> {
                    RidePreferences.prefs(this).edit().putInt("controls_side", choice).apply();
                    dialog.dismiss(); buildScreen(); showSplitPlacementGuide();
                }).setNegativeButton("Close", null).show();
    }

    private void showSplitPlacementGuide() {
        String arrangement = controlsOnRight() ? "Maps on the left and RideBridge on the right" : "RideBridge on the left and Maps on the right";
        new android.app.AlertDialog.Builder(this).setTitle("Arrange split screen while parked")
            .setMessage("Preferred landscape layout: " + arrangement + ".\n\nAndroid controls the positions of separate apps. Open split screen from Recent apps, then use your phone's swap control or choose the first app to arrange them. Rotate to landscape for side-by-side panels. In portrait Android normally stacks the apps.\n\nThis preference arranges RideBridge's own controls; it cannot move another app's window automatically.")
            .setPositiveButton("Open map", (dialog, which) -> openMapsAdjacent())
            .setNegativeButton("Done", null).show();
    }

    private void chooseMessageApps() {
        String[] packages = RidePreferences.messagePackages(this);
        java.util.Set<String> selected = RidePreferences.selectedMessages(this);
        boolean[] checked = new boolean[packages.length];
        for (int i = 0; i < packages.length; i++) checked[i] = selected.contains(packages[i]);
        new android.app.AlertDialog.Builder(this).setTitle("Choose message previews")
            .setMultiChoiceItems(RidePreferences.MESSAGE_NAMES, checked, (dialog, which, enabled) -> checked[which] = enabled)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", (dialog, which) -> {
                java.util.Set<String> enabled = new java.util.HashSet<>();
                for (int i = 0; i < packages.length; i++) if (checked[i] && !packages[i].isEmpty()) enabled.add(packages[i]);
                RidePreferences.prefs(this).edit().putStringSet("message_apps", enabled).commit();
                GX12NotificationListener.reloadSelected(); messageApp = null; buildScreen();
                android.widget.Toast.makeText(this, "Saved. New notifications from selected apps will appear after access is enabled.", android.widget.Toast.LENGTH_LONG).show();
            }).show();
    }

    private void chooseMapApp() {
        if (YamahaCastService.active || DedicatedDisplay.ready) { displayError("Stop casting and end the prepared display before changing its app."); return; }
        int current = java.util.Arrays.asList(RidePreferences.MAP_PACKAGES).indexOf(RidePreferences.selectedMap(this));
        new android.app.AlertDialog.Builder(this).setTitle("Navigation / rider app")
            .setSingleChoiceItems(RidePreferences.MAP_NAMES, current, (dialog, which) -> {
                RidePreferences.prefs(this).edit().putString("map_app", RidePreferences.MAP_PACKAGES[which]).apply();
                dialog.dismiss(); buildScreen();
                android.widget.Toast.makeText(this, "Selected " + RidePreferences.MAP_NAMES[which] + ". Bike display support is experimental.", android.widget.Toast.LENGTH_LONG).show();
            }).setNegativeButton("Cancel", null).show();
    }

    private void launchChosenApp(String pkg) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) { displayError("This app is not installed or does not expose a launch screen."); return; }
        try { startActivity(launch); } catch (Exception e) { displayError("Could not open the selected app."); }
    }

    private void showRideApps() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        java.util.ArrayList<String> packages = new java.util.ArrayList<>();
        labels.add("Spotify"); packages.add("com.spotify.music");
        labels.add(RidePreferences.mapName(this)); packages.add(RidePreferences.selectedMap(this));
        String[] chatPackages = RidePreferences.messagePackages(this);
        for (int i = 0; i < chatPackages.length; i++) if (RidePreferences.selectedMessages(this).contains(chatPackages[i])) {
            labels.add(RidePreferences.MESSAGE_NAMES[i]); packages.add(chatPackages[i]);
        }
        labels.add("Yamaha Y-Connect"); packages.add(YAMAHA_Y_CONNECT_PACKAGE);
        labels.add("Layout + apps"); packages.add("");
        new android.app.AlertDialog.Builder(this).setTitle("Ride apps")
            .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                if (packages.get(which).isEmpty()) showPersonalization(); else launchChosenApp(packages.get(which));
            }).setNegativeButton("Close", null).show();
    }

    private void readMessageAloud() {
        if (!hasNotificationAccess(new ComponentName(this, GX12NotificationListener.class))) { openNotificationAccess(); return; }
        readMessageAloud(displayedMessage());
    }

    private void readMessageAloud(GX12NotificationListener.NotificationPreview preview) {
        if (preview == null) { android.widget.Toast.makeText(this, "No new message to read", android.widget.Toast.LENGTH_SHORT).show(); return; }
        if (speech == null) {
            speech = new android.speech.tts.TextToSpeech(this, result -> {
                speechReady = result == android.speech.tts.TextToSpeech.SUCCESS;
                if (speechReady) readMessageAloud(preview);
                else android.widget.Toast.makeText(this, "Speech is unavailable on this phone", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else if (speechReady) speech.speak(preview.appName + ". " + preview.title + ". " + preview.text,
            android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "message-preview");
    }

    private Button dockButton(String title) {
        Button b = button(title); b.setTextSize(15); b.setMinHeight(dp(76)); b.setPadding(dp(6), dp(6), dp(6), dp(6));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff252e39)); b.setTextColor(0xfff4f6fa); return b;
    }

    private LinearLayout.LayoutParams dockButtonParams() {
        LinearLayout.LayoutParams p = params(); p.topMargin = dp(10); return p;
    }

    private Button cockpitButton(String eyebrow, String label) {
        Button b = button(eyebrow + "\n" + label);
        b.setTextSize(17); b.setAllCaps(false); b.setMinHeight(dp(100)); b.setPadding(dp(8), dp(8), dp(8), dp(8));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff252e39)); b.setTextColor(0xfff4f6fa);
        return b;
    }

    private void addCockpitCard(LinearLayout parent, String heading, View content) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0xff1a212a);
        TextView label = text(heading, 11, 0xff83b5ff, true); card.addView(label);
        LinearLayout.LayoutParams cp = params(); cp.topMargin = dp(5); card.addView(content, cp);
        LinearLayout.LayoutParams p = params(); p.topMargin = dp(10); parent.addView(card, p);
    }

    private enum MediaAction { PREVIOUS, TOGGLE, NEXT }

    private void refreshMediaSession() {
        if (trackStatus == null) return;
        ComponentName listener = new ComponentName(this, GX12NotificationListener.class);
        if (!hasNotificationAccess(listener)) {
            mediaController = null;
            trackStatus.setText("Connect music in Setup");
            updateMediaButtons(false, false, false);
            return;
        }
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> sessions = manager.getActiveSessions(listener);
            mediaController = null;
            for (MediaController session : sessions) {
                if ("com.spotify.music".equals(session.getPackageName())) {
                    mediaController = session;
                    break;
                }
            }
            if (mediaController == null) {
                for (MediaController session : sessions) {
                    PlaybackState playback = session.getPlaybackState();
                    if (playback != null && playback.getState() == PlaybackState.STATE_PLAYING) { mediaController = session; break; }
                }
            }
            if (mediaController == null && !sessions.isEmpty()) mediaController = sessions.get(0);
            if (mediaController == null) {
                trackStatus.setText("Open Spotify and start a track");
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
            trackStatus.setText(label + (cockpitVisible ? "" : (playing ? "\nPlaying" : "\nNot playing")));
            if (albumArt != null) {
                android.graphics.Bitmap image = metadata == null ? null : metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (image == null && metadata != null) image = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (image != null) albumArt.setImageBitmap(image);
                else albumArt.setImageResource(android.R.drawable.ic_media_play);
            }
            long actions = state == null ? 0 : state.getActions();
            updateMediaButtons(true, (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
                    (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
            playPauseButton.setText(cockpitVisible ? (playing ? "Ⅱ" : "▶") : (playing ? "Pause" : "Play"));
        } catch (SecurityException | IllegalStateException error) {
            mediaController = null;
            trackStatus.setText("Android has not granted access yet. Enable RideBridge under Notification access.");
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
            handler.removeCallbacks(trackRefresh); handler.postDelayed(trackRefresh, 500);
        } catch (SecurityException ignored) {
            trackStatus.setText("This media player did not accept that command.");
        }
    }

    private void openNotificationAccess() {
        ComponentName listener = new ComponentName(this, GX12NotificationListener.class);
        if (hasNotificationAccess(listener)) {
            android.service.notification.NotificationListenerService.requestRebind(listener);
            GX12NotificationListener.reloadSelected();
            openNotificationSettings(listener); return;
        }
        new android.app.AlertDialog.Builder(this)
        .setTitle("Music controls and selected messages")
                .setMessage("Android Notification access is a broad, sensitive permission. If enabled, this app reads Spotify playback details and new notifications from your selected messaging apps only. Notifications may include alerts beyond chats. It ignores unselected apps' notification text, keeps the preview temporarily on this phone, and never uploads it. You can revoke access in Android Settings. On some phones, first open App info, tap ⋮, and choose Allow restricted settings.")
                .setNegativeButton("Not now", null)
                .setNeutralButton("App info", (dialog, which) -> {
                    Intent appInfo = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivity(appInfo);
                })
                .setPositiveButton("Notification access", (dialog, which) -> openNotificationSettings(listener))
                .show();
    }

    private void openNotificationSettings(ComponentName listener) {
        if (Build.VERSION.SDK_INT >= 30) {
            Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            detail.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString());
            try { startActivity(detail); return; } catch (android.content.ActivityNotFoundException | SecurityException ignored) { }
        }
        try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
        catch (android.content.ActivityNotFoundException | SecurityException e) {
            new android.app.AlertDialog.Builder(this).setTitle("Open notification access manually")
                .setMessage("In Android Settings, search for Notification access and enable RideBridge. If access is restricted, open RideBridge App info and choose Allow restricted settings from its menu when available.")
                .setNegativeButton("Close", null).setPositiveButton("Open Settings", (d, w) -> {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                    catch (android.content.ActivityNotFoundException ignored) { android.widget.Toast.makeText(this, "Open Android Settings manually", android.widget.Toast.LENGTH_LONG).show(); }
                }).show();
        }
    }

    private boolean hasNotificationAccess(ComponentName listener) {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        for (String entry : enabled.split(":")) if (listener.equals(ComponentName.unflattenFromString(entry))) return true;
        return false;
    }

    private void refreshWhatsAppPreview() {
        if (messagePreview == null && dockMessage == null) return;
        if (!hasNotificationAccess(new ComponentName(this, GX12NotificationListener.class))) {
            if (messageSource != null) messageSource.setText("ACCESS IS OFF");
            if (messagePreview != null) messagePreview.setText("Enable message access while parked");
            if (dockMessage != null) dockMessage.setText("Messages\nChoose apps and enable access in Setup.");
            return;
        }
        GX12NotificationListener.NotificationPreview preview = displayedMessage();
        if (messageSource != null) messageSource.setText(preview == null
            ? (messageApp == null ? "LATEST MESSAGE" : RidePreferences.appName(this, messageApp).toUpperCase(java.util.Locale.ROOT))
            : preview.appName.toUpperCase(java.util.Locale.ROOT) + (preview.acknowledged ? " - SEEN" : ""));
        String label = preview == null ? "No message received yet." : preview.appName + " • " + preview.title + (preview.text.isEmpty() ? "" : "\n" + preview.text);
        if (messagePreview != null) messagePreview.setText(label);
        if (dockMessage != null) dockMessage.setText("Messages\n" + (preview == null ? "No new preview" : preview.appName + " - " + preview.title + (preview.text.isEmpty() ? "" : "\n" + preview.text)));
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
        if (rideClock == null || rideButton == null) return;
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
        maps.setPackage("com.google.android.apps.maps");
        try { startActivity(maps); } catch (Exception ignored) { showRideMessage("No maps app is available on this phone."); }
    }

    private void openMapsAdjacent() {
        if (!RidePreferences.selectedMap(this).equals("com.google.android.apps.maps")) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(RidePreferences.selectedMap(this));
            if (launch == null) { displayError("Install " + RidePreferences.mapName(this) + " first."); return; }
            try { startAdjacent(launch); } catch (Exception e) { launchChosenApp(RidePreferences.selectedMap(this)); } return;
        }
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
        maps.setPackage("com.google.android.apps.maps");
        try { startAdjacent(maps); }
        catch (Exception ignored) { openMaps(); }
    }

    private void startAdjacent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        startActivity(intent);
    }

    private void openSpotifyAdjacent() {
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify == null) { openSpotify(); return; }
        try { startAdjacent(spotify); } catch (Exception ignored) { openSpotify(); }
    }

    private void openSpotify() {
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify != null) {
            try { startActivity(spotify); return; } catch (Exception ignored) { }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")));
        } catch (Exception ignored) {
            showRideMessage("Spotify is not installed and no browser is available.");
        }
    }

    private void openYamahaApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(YAMAHA_Y_CONNECT_PACKAGE);
        if (launch == null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Yamaha Y-Connect not found")
                    .setMessage("Install Yamaha Motorcycle Connect from Google Play, then try again.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Google Play", (dialog, which) -> {
                        Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + YAMAHA_Y_CONNECT_PACKAGE));
                        try { startActivity(store); } catch (Exception ignored) { showRideMessage("Could not open Google Play."); }
                    }).show();
            return;
        }
        try { startActivity(launch); }
        catch (Exception ignored) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Could not open Y-Connect")
                    .setMessage("Check that Yamaha Motorcycle Connect is installed and try again.")
                    .setPositiveButton("OK", null).show();
        }
    }

    private void openYamahaAppAdjacent() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(YAMAHA_Y_CONNECT_PACKAGE);
        if (launch == null) { openYamahaApp(); return; }
        try { startAdjacent(launch); } catch (Exception ignored) { openYamahaApp(); }
    }

    private void openStreetCross() {
        openStreetCross(false);
    }

    private void openStreetCross(boolean adjacent) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(GARMIN_STREETCROSS_PACKAGE);
        if (launch != null) {
            try { if (adjacent) startAdjacent(launch); else startActivity(launch); return; } catch (Exception ignored) { }
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("StreetCross not found")
                .setMessage("Install Garmin StreetCross from Google Play if it is available for your motorcycle and region. StreetCross and Google Maps remain separate navigation apps.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Google Play", (dialog, which) -> {
                    Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=Garmin%20StreetCross&c=apps"));
                    try { startActivity(store); } catch (Exception ignored) { showRideMessage("Could not open Google Play."); }
                }).show();
    }

    private void openWhatsApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) launch = getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
        if (launch != null) { try { startActivity(launch); return; } catch (Exception ignored) { } }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))); }
        catch (Exception ignored) { showRideMessage("WhatsApp is not available on this phone."); }
    }

    private void openWhatsAppAdjacent() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) launch = getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
        if (launch == null) { openWhatsApp(); return; }
        try { startAdjacent(launch); } catch (Exception ignored) { openWhatsApp(); }
    }

    private void startGoogleVoice() {
        Intent voice = new Intent("android.intent.action.VOICE_COMMAND");
        voice.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(voice); }
        catch (Exception first) {
            Intent assist = new Intent("android.intent.action.ASSIST");
            try { startActivity(assist); }
            catch (Exception ignored) { showRideMessage("No voice assistant is configured. Set Google as the phone's digital assistant in Android Settings."); }
        }
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
                    search.setPackage("com.google.android.apps.maps");
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
    private HttpURLConnection openConnection(String address) throws Exception { HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(true); c.setRequestProperty("User-Agent", "RideBridge-Android"); return c; }
    private String readText(InputStream input) throws Exception { StringBuilder b = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) b.append(line).append('\n'); } return b.toString(); }
    private String findHash(String checksums, String filename) { for (String line : checksums.split("\\r?\\n")) { String[] f = line.trim().split("\\s+"); if (f.length >= 2 && f[1].replaceFirst("^\\*", "").equals(filename)) return f[0]; } throw new IllegalStateException("No SHA-256 checksum was published for the APK."); }
    private void downloadFile(String address, File target) throws Exception { HttpURLConnection c = openConnection(address); try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(target)) { byte[] buffer = new byte[8192]; int count; while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count); } finally { c.disconnect(); } }
    private String sha256(File file) throws Exception { MessageDigest d = MessageDigest.getInstance("SHA-256"); try (InputStream in = new java.io.FileInputStream(file)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) d.update(b, 0, n); } StringBuilder s = new StringBuilder(); for (byte v : d.digest()) s.append(String.format(Locale.ROOT, "%02x", v & 0xff)); return s.toString(); }
    private boolean canInstallPackages() { return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls(); }
    private void openInstaller(File apk) { try { Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".apkprovider", apk); Intent i = new Intent(Intent.ACTION_INSTALL_PACKAGE); i.setDataAndType(uri, "application/vnd.android.package-archive"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); if (downloadedApk != null && downloadedApk.equals(apk)) downloadedApk = null; startActivity(i); } catch (Exception e) { showUpdateMessage("Could not open Android’s installer: " + safeMessage(e)); } }
    private void showUpdateMessage(String message) { runOnUiThread(() -> { if (updateStatus != null) updateStatus.setText(message); }); }
    private String safeMessage(Exception error) { String m = error.getMessage(); return m == null || m.isBlank() ? error.getClass().getSimpleName() : m; }
    private String appVersion() { try { PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0); return info.versionName + " (" + info.versionCode + ")"; } catch (Exception ignored) { return "unknown"; } }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); return b; }
    private LinearLayout.LayoutParams params() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = params(); p.topMargin = dp(8); return p; }
    private LinearLayout.LayoutParams weightedButtonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); p.setMargins(dp(2), dp(4), dp(2), dp(4)); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

}
