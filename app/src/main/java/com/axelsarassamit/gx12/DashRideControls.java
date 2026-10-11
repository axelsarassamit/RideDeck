package com.axelsarassamit.gx12;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Native RideDeck controls for the optional Dash camera companion. */
final class DashRideControls {
    interface Host {
        State state();
        void onRideToggle();
        void onRideEnd();
        void onRecordWithDashChanged(boolean enabled);
        void onDashSettings();
        void onDismiss();
    }

    static final class State {
        boolean dashAvailable;
        boolean rideActive;
        boolean rideHasElapsed;
        boolean recordingRequested;
        boolean recording;
        boolean starting;
        boolean accepted;
        boolean autoRecord;
        long elapsedMillis;
        String detail = "";
    }

    private final Activity activity;
    private final Host host;
    private final Dialog dialog;
    private final HandlerRefresh refresh = new HandlerRefresh();
    private final TextView title;
    private final TextView rideState;
    private final TextView detail;
    private final TextView previewState;
    private final LinearLayout previewArea;
    private final ImageView rear;
    private final ImageView front;
    private final Switch autoRecord;
    private final Button rideToggle;
    private final Button rideEnd;
    private final Button settings;
    private final Button done;
    private final TextView explanation;
    private boolean applyingState;

    DashRideControls(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        page.setPadding(pad, pad, pad, pad);
        page.setBackgroundColor(0xff101722);
        ViewCompat.setOnApplyWindowInsetsListener(page, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left + dp(16), bars.top + dp(16),
                bars.right + dp(16), bars.bottom + dp(16));
            return insets;
        });

        title = text(28, true);
        page.addView(title);
        rideState = text(24, true);
        page.addView(rideState);
        detail = text(18, false);
        page.addView(detail);

        previewArea = new LinearLayout(activity);
        previewArea.setOrientation(LinearLayout.VERTICAL);
        previewArea.setVisibility(View.GONE);
        TextView angle = text(20, true);
        angle.setText("Check both camera angles before riding.");
        previewArea.addView(angle);
        previewState = text(18, false);
        previewArea.addView(previewState);
        LinearLayout cameras = new LinearLayout(activity);
        cameras.setOrientation(LinearLayout.HORIZONTAL);
        rear = camera(cameras, "Rear camera");
        front = camera(cameras, "Front camera");
        previewArea.addView(cameras);
        page.addView(previewArea);

        explanation = text(18, false);
        explanation.setText("Start or resume a ride to record with Dash and check the camera angles here. Pause or End stops the background session.");
        page.addView(explanation);

        autoRecord = new Switch(activity);
        autoRecord.setText("Record with Dash");
        autoRecord.setTextSize(22);
        autoRecord.setTextColor(Color.WHITE);
        autoRecord.setMinHeight(dp(80));
        page.addView(autoRecord, new LinearLayout.LayoutParams(-1, dp(80)));
        autoRecord.setOnCheckedChangeListener((button, enabled) -> {
            if (!applyingState) host.onRecordWithDashChanged(enabled);
        });

        LinearLayout controls = new LinearLayout(activity);
        rideToggle = button("Start ride", true);
        rideEnd = button("End ride", false);
        controls.addView(rideToggle, new LinearLayout.LayoutParams(0, dp(80), 1f));
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, dp(80), 1f);
        endParams.leftMargin = dp(8);
        controls.addView(rideEnd, endParams);
        page.addView(controls);
        rideToggle.setOnClickListener(v -> host.onRideToggle());
        rideEnd.setOnClickListener(v -> host.onRideEnd());

        settings = button("Dash settings", false);
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-1, dp(80));
        settingsParams.topMargin = dp(12);
        page.addView(settings, settingsParams);
        settings.setOnClickListener(v -> host.onDashSettings());

        done = button("Close", false);
        page.addView(done, 1, new LinearLayout.LayoutParams(-1, dp(80)));
        done.setOnClickListener(v -> dialog.dismiss());

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff101722);
        scroll.addView(page);
        dialog.setContentView(scroll);
        dialog.setOnDismissListener(d -> {
            refresh.stop();
            clearFrames();
            host.onDismiss();
        });
    }

    private final class HandlerRefresh implements Runnable {
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private boolean running;

        void start() {
            if (running) return;
            running = true;
            handler.post(this);
        }

        void stop() {
            running = false;
            handler.removeCallbacks(this);
        }

        @Override public void run() {
            if (!running || !dialog.isShowing()) {
                stop();
                return;
            }
            if (!dialogFocused()) {
                handler.postDelayed(this, 250L);
                return;
            }
            update(host.state());
            handler.postDelayed(this, 500L);
        }
    }

    private TextView text(int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setTypeface(android.graphics.Typeface.DEFAULT,
            bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        view.setPadding(0, 0, 0, dp(12));
        return view;
    }

    private Button button(String label, boolean primary) {
        Button view = new Button(activity);
        view.setText(label);
        view.setAllCaps(false);
        view.setTextSize(22);
        view.setMinHeight(dp(80));
        view.setTextColor(Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? 0xff087eff : 0xff242f40);
        background.setCornerRadius(dp(12));
        view.setBackground(background);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        return view;
    }

    private ImageView camera(LinearLayout cameras, String label) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView cameraLabel = text(18, true);
        cameraLabel.setText(label);
        column.addView(cameraLabel);
        ImageView frame = new ImageView(activity);
        frame.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.setBackgroundColor(Color.BLACK);
        frame.setContentDescription(label + " live angle");
        column.addView(frame, new LinearLayout.LayoutParams(-1, dp(180)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        cameras.addView(column, params);
        return frame;
    }

    void show() {
        if (!dialog.isShowing()) {
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0xff101722));
                dialog.getWindow().setLayout(-1, -1);
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                ViewCompat.requestApplyInsets(dialog.getWindow().getDecorView());
            }
        }
        update(host.state());
        refresh.start();
    }

    boolean isShowing() { return dialog.isShowing(); }

    void pause() {
        refresh.stop();
        clearFrames();
    }

    void refresh() {
        if (!dialog.isShowing()) return;
        update(host.state());
        refresh.start();
    }

    boolean previewVisible() {
        return dialog.isShowing() && dialogFocused()
            && previewArea.getVisibility() == View.VISIBLE;
    }

    void setPreviewStatus(String message) {
        previewState.setText(message == null ? "" : message);
    }

    void setPreviewFrames(Bitmap rearFrame, Bitmap frontFrame) {
        rear.setImageBitmap(rearFrame);
        front.setImageBitmap(frontFrame);
    }

    void clearFrames() {
        rear.setImageDrawable(null);
        front.setImageDrawable(null);
    }

    private boolean dialogFocused() {
        return dialog.getWindow() != null && dialog.getWindow().getDecorView().hasWindowFocus();
    }

    private void update(State state) {
        boolean available = state.dashAvailable;
        boolean recording = available && state.rideActive && state.recording;
        title.setText(recording ? "● REC · RideDeck Dash" : available ? "RideDeck Dash" : "Ride session");
        title.setTextColor(recording ? 0xffff4c4c : Color.WHITE);
        long seconds = Math.max(0, state.elapsedMillis / 1000L);
        String time = String.format(java.util.Locale.US, "%02d:%02d:%02d",
            seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L);
        rideState.setText(state.rideActive ? "Ride active · " + time
            : state.rideHasElapsed ? "Ride paused · " + time : "Ready to ride");
        detail.setText(state.detail == null ? "" : state.detail);
        applyingState = true;
        if (autoRecord.isChecked() != state.autoRecord) autoRecord.setChecked(state.autoRecord);
        applyingState = false;
        boolean showPreview = available && state.rideActive && state.recordingRequested;
        detail.setVisibility(available && !showPreview ? View.VISIBLE : View.GONE);
        autoRecord.setVisibility(available && !showPreview ? View.VISIBLE : View.GONE);
        explanation.setVisibility(available && !showPreview ? View.VISIBLE : View.GONE);
        settings.setVisibility(available ? View.VISIBLE : View.GONE);
        done.setText(showPreview ? "Done" : "Close");
        rideToggle.setText(state.rideActive ? "Pause ride"
            : state.rideHasElapsed ? "Resume ride" : "Start ride");
        rideEnd.setEnabled(state.rideActive || state.rideHasElapsed);
        previewArea.setVisibility(showPreview ? View.VISIBLE : View.GONE);
        if (!showPreview) clearFrames();
        if (recording) previewState.setText("Recording · check the camera angles");
        else if (state.starting) previewState.setText("Dash is starting. Waiting for live images.");
        else if (!state.accepted && state.recordingRequested)
            previewState.setText("Dash is unavailable. Check settings and try again.");
        else previewState.setText("Waiting for live camera preview.");
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
