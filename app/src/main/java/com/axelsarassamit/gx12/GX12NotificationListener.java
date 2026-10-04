package com.axelsarassamit.gx12;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Android requires an enabled notification listener before an app can access
 * active media sessions belonging to other apps. WhatsApp notification previews
 * are kept only in memory and are never persisted or transmitted.
 */
public final class GX12NotificationListener extends NotificationListenerService {
    public static volatile NotificationPreview latestWhatsAppPreview;

    public static final class NotificationPreview {
        public final String title;
        public final String text;
        public final String key;
        public final android.app.PendingIntent open;
        NotificationPreview(String title, String text, String key, android.app.PendingIntent open) {
            this.title = title; this.text = text; this.key = key; this.open = open;
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || (!"com.whatsapp".equals(sbn.getPackageName()) && !"com.whatsapp.w4b".equals(sbn.getPackageName()))) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;
        Bundle extras = notification.extras;
        CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = extras == null ? null : extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (TextUtils.isEmpty(body) && extras != null) body = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence lines = extras == null ? null : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) == null ? null : TextUtils.join("\n", extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
        if (!TextUtils.isEmpty(lines)) body = lines;
        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(body))
            latestWhatsAppPreview = new NotificationPreview(title == null ? "WhatsApp" : title.toString(), body == null ? "" : body.toString(), sbn.getKey(), notification.contentIntent);
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        NotificationPreview current = latestWhatsAppPreview;
        if (sbn != null && current != null && sbn.getKey().equals(current.key))
            latestWhatsAppPreview = null;
    }
}
