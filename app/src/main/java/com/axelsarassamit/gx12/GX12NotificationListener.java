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
    public static volatile NotificationPreview activeCall;
    private static int visibleActivities;
    private static final java.util.LinkedHashMap<String,NotificationPreview> calls = new java.util.LinkedHashMap<>();
    private static long lastCallRefresh;
    public static void refreshCallsIfDue() {
        GX12NotificationListener listener=connected; long now=android.os.SystemClock.elapsedRealtime();
        if(listener!=null && now-lastCallRefresh>=1500){lastCallRefresh=now;listener.refreshActive();}
    }
    static void removeCall(android.content.Context context,String key) { synchronized(GX12NotificationListener.class){calls.remove(key);CallerAnnouncement.removed(key);} updateCallState(context); }
    static void reconcileCalls(android.content.Context context,java.util.Set<String> live) { synchronized(GX12NotificationListener.class){for(String key:new java.util.ArrayList<>(calls.keySet())) if(!live.contains(key)) {calls.remove(key);CallerAnnouncement.removed(key);}} updateCallState(context); }
    private static boolean isCall(Notification n) {
        return n!=null && (Notification.CATEGORY_CALL.equals(n.category) ||
            (n.extras!=null && "android.app.Notification$CallStyle".equals(n.extras.getString("android.template"))));
    }
    private static void updateCallState(android.content.Context context) {
        NotificationPreview next=null;
        synchronized(GX12NotificationListener.class) {
            for(NotificationPreview item:calls.values()) if(next==null || item.incoming || !next.incoming) next=item;
            boolean changed=(activeCall==null)!=(next==null);
            activeCall=next;
            if(connected!=null) connected.applyQuietMode();
            if(changed) RideQuietMode.refresh(context);
            CallerAnnouncement.update(context,next,visibleActivities>0);
        }
    }
    public static synchronized void activityVisible(boolean visible) {
        visibleActivities = Math.max(0, visibleActivities + (visible ? 1 : -1));
        if (connected != null) { connected.applyQuietMode(); CallerAnnouncement.update(connected,activeCall,visibleActivities>0); }
    }
    private void applyQuietMode() {
        requestListenerHints(visibleActivities > 0 && activeCall == null ? HINT_HOST_DISABLE_NOTIFICATION_EFFECTS : 0);
    }
    private static final MessageInbox<NotificationPreview> previews = new MessageInbox<>();
    public static synchronized java.util.List<NotificationPreview> selectedPreviews(android.content.Context context) {
        return previews.selected(RidePreferences.selectedMessages(context));
    }
    /**
     * Yamaha's future accessory writer must use this state, not a second inbox
     * or Android's raw notification count. It exposes no message contents.
     * UNAVAILABLE is distinct from an empty inbox when the listener is lost.
     */
    public static synchronized MessageInbox.Indicator messageIndicator(android.content.Context context) {
        return previews.indicator(RidePreferences.selectedMessages(context), connected != null);
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
        public volatile android.app.PendingIntent open;
        public volatile boolean acknowledged;
        public Notification.Action[] callActions;
        public boolean incoming;
        public int callType;
        public String callerName="";
        public android.app.PendingIntent markRead;
        public volatile ReplyAction reply;
        void refreshActions(NotificationPreview updated) {
            if (updated.open != null) open = updated.open;
            if (updated.reply != null) reply = updated.reply;
            if (updated.markRead != null) markRead = updated.markRead;
        }
        NotificationPreview(String title, String text, String key, android.app.PendingIntent open, String packageName, String appName) {
            this.title = title.substring(0, Math.min(160, title.length())); this.text = text.substring(0, Math.min(20000, text.length())); this.key = key; this.open = open; this.packageName = packageName; this.appName = appName;
        }
    }

    public static final class ReplyAction {
        public final android.app.PendingIntent intent;
        public final android.app.RemoteInput input;
        public final android.app.RemoteInput[] inputs;
        final int rank;
        ReplyAction(android.app.PendingIntent intent, android.app.RemoteInput input,
                    android.app.RemoteInput[] inputs, int rank) {
            this.intent = intent; this.input = input; this.inputs = inputs; this.rank = rank;
        }
    }
    private static void inspectAction(NotificationPreview preview, Notification.Action action) {
        if (action == null || RidePreferences.readOnlyEmail(preview.packageName)) return;
        int semantic = android.os.Build.VERSION.SDK_INT >= 28 ? action.getSemanticAction() : 0;
        if (semantic == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ) preview.markRead = action.actionIntent;
        android.app.RemoteInput[] inputs = action.getRemoteInputs();
        if (inputs == null) return;
        for (android.app.RemoteInput input : inputs) {
            int rank = ReplyActionPolicy.rank(semantic, input.getAllowFreeFormInput(), action.actionIntent != null);
            if (rank > 0 && (preview.reply == null || rank > preview.reply.rank)) {
                preview.reply = new ReplyAction(action.actionIntent, input, inputs, rank);
            }
        }
    }

    private static void inspectInvisibleAction(NotificationPreview preview, androidx.core.app.NotificationCompat.Action action) {
        if (RidePreferences.readOnlyEmail(preview.packageName)) return;
        androidx.core.app.RemoteInput[] compatInputs = action.getRemoteInputs();
        if (compatInputs == null || action.actionIntent == null) return;
        android.app.RemoteInput[] inputs = new android.app.RemoteInput[compatInputs.length];
        for (int i = 0; i < inputs.length; i++) {
            androidx.core.app.RemoteInput input = compatInputs[i];
            android.app.RemoteInput.Builder builder = new android.app.RemoteInput.Builder(input.getResultKey())
                .setLabel(input.getLabel()).setChoices(input.getChoices()).setAllowFreeFormInput(input.getAllowFreeFormInput())
                .addExtras(input.getExtras());
            if (input.getAllowedDataTypes() != null) for (String type : input.getAllowedDataTypes()) builder.setAllowDataType(type, true);
            if (android.os.Build.VERSION.SDK_INT >= 29) builder.setEditChoicesBeforeSending(input.getEditChoicesBeforeSending());
            inputs[i] = builder.build();
        }
        for (android.app.RemoteInput input : inputs) {
            int rank = ReplyActionPolicy.rank(action.getSemanticAction(), input.getAllowFreeFormInput(), true);
            if (rank > 0 && (preview.reply == null || rank > preview.reply.rank)) {
                preview.reply = new ReplyAction(action.actionIntent, input, inputs, rank);
            }
        }
    }

    @Override public void onListenerConnected() {
        RideDeckDiagnostics.record(this, "Notification listener connected");
        super.onListenerConnected(); connected = this; RideQuietMode.refresh(this);
        android.util.Log.i("RideDeckListener", "Notification listener connected"); applyQuietMode(); refreshActive();
    }
    public void refreshActive() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                java.util.Arrays.sort(active, java.util.Comparator.comparingLong(StatusBarNotification::getPostTime));
                java.util.Set<String> live=new java.util.HashSet<>();
                for (StatusBarNotification item : active) { if(isCall(item.getNotification())) live.add(item.getKey()); onNotificationPosted(item); }
                reconcileCalls(this,live);
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
    @Override public void onDestroy() { if (connected == this) connected = null; synchronized(GX12NotificationListener.class){calls.clear();activeCall=null;} CallerAnnouncement.close(); RideQuietMode.refresh(this); super.onDestroy(); }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try { consume(this,sbn); } catch (RuntimeException error) { android.util.Log.w("RideDeckCalls","Unsupported notification payload",error); }
    }
    static void consume(android.content.Context context, StatusBarNotification sbn) {
        if (sbn == null) return;
        boolean call = isCall(sbn.getNotification());
        if (!call && !RidePreferences.selectedMessages(context).contains(sbn.getPackageName())) return;
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
        if (call || !TextUtils.isEmpty(title) || !TextUtils.isEmpty(body)) {
            NotificationPreview preview = new NotificationPreview(title == null ? (call ? "Caller unavailable" : "Message") : title.toString(), body == null ? "" : body.toString(), sbn.getKey(), notification.contentIntent, sbn.getPackageName(), RidePreferences.appName(context, sbn.getPackageName()));
            if (call) {
                if (activeCall == null || !activeCall.key.equals(sbn.getKey())) RideDeckDiagnostics.record(context, "Call notification received actions=" + (notification.actions == null ? 0 : notification.actions.length));
                preview.callActions = notification.actions;
                preview.callType=extras==null?0:extras.getInt("android.callType",0);
                boolean answer=false;
                if(notification.actions!=null) for(Notification.Action action:notification.actions) {
                    if(action==null) continue;
                    String label=action.title==null?"":action.title.toString().toLowerCase(java.util.Locale.ROOT);
                    if(label.matches(".*(answer|accept|svara|annehmen|รับสาย|ตอบรับ).*")) answer=true;
                }
                preview.incoming=IncomingCallPolicy.incoming(preview.callType,answer,notification.fullScreenIntent!=null,extras!=null && extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER,false));
                android.app.Person person=android.os.Build.VERSION.SDK_INT>=28 && extras!=null?extras.getParcelable("android.callPerson"):null;
                preview.callerName=IncomingCallPolicy.callerName(person!=null && person.getName()!=null?person.getName().toString():preview.title,preview.appName);
                synchronized(GX12NotificationListener.class){calls.remove(preview.key);calls.put(preview.key,preview);}
                updateCallState(context);
                return;
            }
            if (notification.actions != null) for (Notification.Action action : notification.actions) inspectAction(preview, action);
            for (Notification.Action action : new Notification.WearableExtender(notification).getActions()) inspectAction(preview, action);
            for (androidx.core.app.NotificationCompat.Action action : androidx.core.app.NotificationCompat.getInvisibleActions(notification)) inspectInvisibleAction(preview, action);
            synchronized (GX12NotificationListener.class) {
                if (!RidePreferences.selectedMessages(context).contains(sbn.getPackageName())) return;
                previews.put(sbn.getPackageName(), sbn.getKey(), preview.title + "\n" + preview.text, preview, NotificationPreview::refreshActions);
                latestWhatsAppPreview = preview;
            }
        }
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        removeCall(this,sbn.getKey());
        synchronized (GX12NotificationListener.class) {
            // Retain the pending RideDeck message until Seen / next. The bike
            // indicator uses this same queue and must not clear independently.
            for (NotificationPreview item : previews.selected(RidePreferences.selectedMessages(this))) {
                // Dismissing a notification does not cancel its PendingIntent. Keep the
                // explicit reply handle while this message remains in RideDeck.
                // A cancelled handle is detected on Send, with the draft retained.
                if (sbn.getKey().equals(item.key)) item.markRead = null;
            }
        }
    }
    @Override public void onListenerDisconnected() {
        RideDeckDiagnostics.record(this, "Notification listener disconnected");
        if (connected == this) connected = null;
        synchronized(GX12NotificationListener.class){calls.clear();activeCall=null;} CallerAnnouncement.close(); RideQuietMode.refresh(this); clearPreviews(); super.onListenerDisconnected(); recover(this);
    }
}
