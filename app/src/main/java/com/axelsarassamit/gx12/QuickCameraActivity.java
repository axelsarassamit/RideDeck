package com.axelsarassamit.gx12;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.video.*;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

/** Foreground-only camera. Media is saved locally; recording stops when leaving. */
public final class QuickCameraActivity extends ComponentActivity {
    private boolean front, video, busy;
    private PreviewView preview;
    private TextView status;
    private Button capture, flip, mode;
    private ProcessCameraProvider provider;
    private ImageCapture photo;
    private VideoCapture<Recorder> film;
    private Recording recording;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        front = state == null ? getIntent().getBooleanExtra("front", false) : state.getBoolean("front");
        video = state == null ? getIntent().getBooleanExtra("video", false) : state.getBoolean("video");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff101419); root.setPadding(dp(12), dp(12), dp(12), dp(12));
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets edges = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(edges.left + dp(12), edges.top + dp(12), edges.right + dp(12), edges.bottom + dp(12)); return insets;
        });
        status = new TextView(this); status.setTextColor(0xfff4f6fa); status.setTextSize(18); status.setGravity(Gravity.CENTER);
        root.addView(status);
        preview = new PreviewView(this);
        preview.setContentDescription("Tap camera preview to capture a photo or start or stop recording");
        preview.setOnClickListener(v -> {
            if (capture != null && capture.isEnabled()) {
                preview.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                shoot();
            }
        });
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(this);
        Button done = action("Done"); done.setOnClickListener(v -> finish()); controls.addView(done, weight());
        flip = action("Switch camera"); flip.setOnClickListener(v -> { front = !front; bind(); }); controls.addView(flip, weight());
        mode = action("Photo / video"); mode.setOnClickListener(v -> { video = !video; bind(); }); controls.addView(mode, weight());
        capture = action("Take photo"); capture.setOnClickListener(v -> shoot()); controls.addView(capture, weight());
        root.addView(controls); setContentView(root); ScreenChrome.apply(getWindow(), true); setBusy(true); permissions();
    }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); if (focused) ScreenChrome.apply(getWindow(), true); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private Button action(String label) { Button b = new Button(this); b.setText(label); b.setTextSize(16); b.setAllCaps(false); return b; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, dp(72), 1); }
    private void setBusy(boolean value) { busy = value; capture.setEnabled(!value); flip.setEnabled(!value); mode.setEnabled(!value); }
    private void permissions() {
        ArrayList<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CAMERA);
        if (video && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { missing.add(Manifest.permission.RECORD_AUDIO); getPreferences(0).edit().putBoolean("audio_asked", true).apply(); }
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), 51); else startCamera();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code != 51) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            status.setText("Camera access is required. Allow it in Android app settings."); return;
        } startCamera();
    }
    private void startCamera() {
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> { try { provider = future.get(); bind(); } catch (Exception e) { status.setText("Camera unavailable. Close other camera apps and try again."); } }, ContextCompat.getMainExecutor(this));
    }
    private void bind() {
        if (provider == null || recording != null) return;
        setBusy(true); photo = null; film = null;
        try {
            CameraSelector lens = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
            provider.unbindAll();
            if (!provider.hasCamera(lens)) { status.setText("This phone does not have the selected camera."); flip.setEnabled(true); return; }
            Preview view = new Preview.Builder().build(); view.setSurfaceProvider(preview.getSurfaceProvider());
            int rotation = preview.getDisplay() == null ? android.view.Surface.ROTATION_0 : preview.getDisplay().getRotation();
            if (video) {
                Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))).build();
                film = VideoCapture.withOutput(recorder); film.setTargetRotation(rotation); provider.bindToLifecycle(this, lens, view, film);
            } else {
                photo = new ImageCapture.Builder().setTargetRotation(rotation).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                provider.bindToLifecycle(this, lens, view, photo);
            }
            status.setText((front ? "Front" : "Rear") + (video ? " video - tap picture to record" : " photo - tap picture to capture"));
            capture.setText(video ? "Record" : "Take photo"); setBusy(false);
        } catch (Exception e) { status.setText("Selected camera mode unavailable. Try the other camera or mode."); flip.setEnabled(true); mode.setEnabled(true); }
    }
    private ContentValues values(boolean movie) {
        ContentValues values = new ContentValues(); values.put(MediaStore.MediaColumns.DISPLAY_NAME, "RideDeck_" + System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, movie ? "video/mp4" : "image/jpeg");
        if (Build.VERSION.SDK_INT >= 29) values.put(MediaStore.MediaColumns.RELATIVE_PATH, movie ? "Movies/RideDeck" : "Pictures/RideDeck");
        return values;
    }
    private void shoot() {
        if (recording != null) { capture.setEnabled(false); capture.setText("Saving..."); recording.stop(); return; }
        if (busy) return;
        if (video && film != null) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("audio_asked", false)) {
                getPreferences(0).edit().putBoolean("audio_asked", true).apply(); requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 52); return;
            }
            try {
                MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values(true)).build();
                PendingRecording pending = film.getOutput().prepareRecording(this, options);
                boolean audio = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                if (audio) pending = pending.withAudioEnabled();
                recording = pending.start(ContextCompat.getMainExecutor(this), event -> {
                    if (event instanceof VideoRecordEvent.Status) status.setText("Recording " + (((VideoRecordEvent.Status)event).getRecordingStats().getRecordedDurationNanos() / 1000000000L) + "s - tap picture to stop");
                    if (event instanceof VideoRecordEvent.Finalize) {
                        BikeDiagnostics.record(QuickCameraActivity.this, "Camera recording finished error=" + ((VideoRecordEvent.Finalize)event).getError());
                        recording = null; capture.setText("Record"); setBusy(false);
                        status.setText(((VideoRecordEvent.Finalize)event).hasError() ? "Recording ended with an error. Check your gallery." : "Video saved - tap picture to record again");
                    }
                });
                flip.setEnabled(false); mode.setEnabled(false); capture.setText("Stop recording"); status.setText(audio ? "Recording with sound" : "Recording without sound");
            } catch (Exception e) { BikeDiagnostics.record(this, "Camera recording failed exception=" + e.getClass().getSimpleName()); status.setText("Cannot record. Check permissions and free storage."); }
        } else if (photo != null) {
            setBusy(true);
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values(false)).build();
            photo.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(ImageCapture.OutputFileResults result) { BikeDiagnostics.record(QuickCameraActivity.this, "Camera photo saved"); status.setText("Photo saved - tap picture to take another"); setBusy(false); }
                @Override public void onError(ImageCaptureException error) { BikeDiagnostics.record(QuickCameraActivity.this, "Camera photo failed error=" + error.getImageCaptureError()); status.setText("Photo could not be saved. Check free storage."); setBusy(false); }
            });
        }
    }
    @Override protected void onStart() { super.onStart(); GX12NotificationListener.activityVisible(true); RideQuietMode.visibility(this, true); }
    @Override protected void onStop() { GX12NotificationListener.activityVisible(false); RideQuietMode.visibility(this, false); if (recording != null) recording.stop(); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("front", front); state.putBoolean("video", video); super.onSaveInstanceState(state); }
}
