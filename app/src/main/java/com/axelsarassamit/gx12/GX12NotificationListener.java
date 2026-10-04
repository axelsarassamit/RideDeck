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
    public static synchronized void clearPreviews() { previews.clear(); latestWhatsAppPreview = null; }

    public static final class NotificationPreview {
        public final String title;
        public final String text;
        public final String key;
        public final String packageName;
        public final String appName;
        public final android.app.PendingIntent open;
        NotificationPreview(String title, String text, String key, android.app.PendingIntent open, String packageName, String appName) {
            this.title = title.substring(0, Math.min(160, title.length())); this.text = text.substring(0, Math.min(1000, text.length())); this.key = key; this.open = open; this.packageName = packageName; this.appName = appName;
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !RidePreferences.selectedMessages(this).contains(sbn.getPackageName())) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;
        Bundle extras = notification.extras;
        CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = extras == null ? null : extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (TextUtils.isEmpty(body) && extras != null) body = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence lines = extras == null ? null : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) == null ? null : TextUtils.join("\n", extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
        if (!TextUtils.isEmpty(lines)) body = lines;
        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(body)) {
            NotificationPreview preview = new NotificationPreview(title == null ? "Message" : title.toString(), body == null ? "" : body.toString(), sbn.getKey(), notification.contentIntent, sbn.getPackageName(), RidePreferences.appName(this, sbn.getPackageName()));
            synchronized (GX12NotificationListener.class) {
                if (!RidePreferences.selectedMessages(this).contains(sbn.getPackageName())) return;
                previews.put(sbn.getPackageName(), sbn.getKey(), preview);
                latestWhatsAppPreview = preview;
            }
        }
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        synchronized (GX12NotificationListener.class) {
            previews.remove(sbn.getPackageName(), sbn.getKey());
            if (latestWhatsAppPreview != null && sbn.getKey().equals(latestWhatsAppPreview.key)) latestWhatsAppPreview = null;
        }
    }
    @Override public void onListenerDisconnected() { clearPreviews(); super.onListenerDisconnected(); }
}
