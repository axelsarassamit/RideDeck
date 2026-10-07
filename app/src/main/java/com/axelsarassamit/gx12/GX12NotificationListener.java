package com.axelsarassamit.gx12;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Android requires an enabled notification listener before an app can access
 * active media sessions belonging to other apps. Selected-app notification previews
 * are kept only in memory and are never persisted or transmitted.
 */
public final class GX12NotificationListener extends NotificationListenerService {
    public static volatile NotificationPreview latestWhatsAppPreview;
    private static final MessageInbox<NotificationPreview> previews = new MessageInbox<>();
    public static synchronized java.util.List<NotificationPreview> selectedPreviews(android.content.Context context) {
        return previews.selected(RidePreferences.selectedMessages(context));
    }
    public static synchronized void acknowledge(NotificationPreview item) {
        item.acknowledged = true;
        previews.acknowledge(item.packageName, item.key, item);
        if (latestWhatsAppPreview == item) latestWhatsAppPreview = null;
    }
    public static synchronized void clearPreviews() { previews.clear(); latestWhatsAppPreview = null; }

    public static final class NotificationPreview {
        public final String title;
        public final String text;
        public final String key;
        public final String packageName;
        public final String appName;
        public final android.app.PendingIntent open;
        public volatile boolean acknowledged;
        public android.app.PendingIntent markRead;
        public android.app.PendingIntent reply;
        public android.app.RemoteInput replyInput;
        NotificationPreview(String title, String text, String key, android.app.PendingIntent open, String packageName, String appName) {
            this.title = title.substring(0, Math.min(160, title.length())); this.text = text.substring(0, Math.min(20000, text.length())); this.key = key; this.open = open; this.packageName = packageName; this.appName = appName;
        }
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected(); connected = this;
        android.util.Log.i("RideDeckListener", "Notification listener connected"); refreshActive();
    }
    public void refreshActive() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                java.util.Arrays.sort(active, java.util.Comparator.comparingLong(StatusBarNotification::getPostTime));
                for (StatusBarNotification item : active) onNotificationPosted(item);
            }
        } catch (SecurityException ignored) { }
    }
    private static volatile GX12NotificationListener connected;
    private static long lastRebind = -30000;
    public static synchronized void recover(android.content.Context context) {
        if (connected != null) return;
        android.content.ComponentName component = new android.content.ComponentName(context, GX12NotificationListener.class);
        String enabled = android.provider.Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        boolean granted = false;
        if (enabled != null) for (String entry : enabled.split(":")) {
            if (component.equals(android.content.ComponentName.unflattenFromString(entry))) { granted = true; break; }
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if (!granted || now - lastRebind < 30000) return;
        lastRebind = now;
        try {
            android.content.SharedPreferences state = context.getSharedPreferences("listener_recovery", 0);
            int version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            if (state.getInt("component_refresh_version", -1) != version) {
                android.content.pm.PackageManager manager = context.getPackageManager();
                int previous = manager.getComponentEnabledSetting(component);
                try {
                    manager.setComponentEnabledSetting(component, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP);
                } finally {
                    manager.setComponentEnabledSetting(component, previous,
                        android.content.pm.PackageManager.DONT_KILL_APP);
                }
                state.edit().putInt("component_refresh_version", version).apply();
                android.util.Log.i("RideDeckListener", "Refreshed listener component after update");
            }
            requestRebind(component);
            android.util.Log.i("RideDeckListener", "Requested notification listener rebind");
        } catch (Exception error) { android.util.Log.w("RideDeckListener", "Listener recovery failed", error); }
    }
    public static void reloadSelected() { if (connected != null) connected.refreshActive(); }
    @Override public void onDestroy() { if (connected == this) connected = null; super.onDestroy(); }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !RidePreferences.selectedMessages(this).contains(sbn.getPackageName())) return;
        Notification notification = sbn.getNotification();
        if (notification == null || (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Bundle extras = notification.extras;
        CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = extras == null ? null : extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (TextUtils.isEmpty(body) && extras != null) body = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence lines = extras == null ? null : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) == null ? null : TextUtils.join("\n", extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
        if (TextUtils.isEmpty(body) && !TextUtils.isEmpty(lines)) body = lines;
        androidx.core.app.NotificationCompat.MessagingStyle style = androidx.core.app.NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        if (style != null && !style.getMessages().isEmpty()) {
            androidx.core.app.NotificationCompat.MessagingStyle.Message last = style.getMessages().get(style.getMessages().size() - 1);
            body = last.getText();
            if (last.getPerson() != null && !TextUtils.isEmpty(last.getPerson().getName())) title = last.getPerson().getName();
        }
        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(body)) {
            NotificationPreview preview = new NotificationPreview(title == null ? "Message" : title.toString(), body == null ? "" : body.toString(), sbn.getKey(), notification.contentIntent, sbn.getPackageName(), RidePreferences.appName(this, sbn.getPackageName()));
            if (notification.actions != null) for (Notification.Action action : notification.actions) {
                if (android.os.Build.VERSION.SDK_INT >= 28 && action.getSemanticAction() == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ) preview.markRead = action.actionIntent;
                android.app.RemoteInput[] inputs = action.getRemoteInputs();
                if (inputs != null && action.actionIntent != null) for (android.app.RemoteInput input : inputs) {
                    if (input.getAllowFreeFormInput()) { preview.reply = action.actionIntent; preview.replyInput = input; break; }
                }

            }
            synchronized (GX12NotificationListener.class) {
                if (!RidePreferences.selectedMessages(this).contains(sbn.getPackageName())) return;
                previews.put(sbn.getPackageName(), sbn.getKey(), preview.title + "\n" + preview.text, preview);
                latestWhatsAppPreview = preview;
            }
        }
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        synchronized (GX12NotificationListener.class) {
            for (NotificationPreview item : previews.selected(RidePreferences.selectedMessages(this))) {
                if (sbn.getKey().equals(item.key)) { item.reply = null; item.replyInput = null; item.markRead = null; }
            }
        }
    }
    @Override public void onListenerDisconnected() {
        if (connected == this) connected = null;
        clearPreviews(); super.onListenerDisconnected(); recover(this);
    }
}
