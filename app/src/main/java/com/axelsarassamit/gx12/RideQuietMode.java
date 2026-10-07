package com.axelsarassamit.gx12;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.service.notification.Condition;
import android.service.notification.ZenPolicy;

/** Owns only RideDeck's rule; never modifies the user's other quiet modes. */
public final class RideQuietMode {
    private static int visible;
    private static final Uri CONDITION = Uri.parse("condition://com.axelsarassamit.gx12/visible");
    public static synchronized void visibility(Context context, boolean shown) {
        visible = Math.max(0, visible + (shown ? 1 : -1));
        refresh(context);
    }
    public static synchronized void refresh(Context context) {
        if (Build.VERSION.SDK_INT < 29) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (!manager.isNotificationPolicyAccessGranted()) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("quiet_mode", 0);
        String id = prefs.getString("rule", null);
        try {
            AutomaticZenRule rule = id == null ? null : manager.getAutomaticZenRule(id);
            if (rule == null && visible > 0) {
                ZenPolicy policy = new ZenPolicy.Builder().disallowAllSounds()
                    .allowCalls(ZenPolicy.PEOPLE_TYPE_ANYONE).allowAlarms(true).allowMedia(true)
                    .hideAllVisualEffects().build();
                rule = new AutomaticZenRule("RideDeck while open", null,
                    new ComponentName(context, MainActivity.class), CONDITION, policy,
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
                id = manager.addAutomaticZenRule(rule);
                prefs.edit().putString("rule", id).apply();
            }
            if (rule != null) {
                rule.setEnabled(visible > 0);
                manager.updateAutomaticZenRule(id, rule);
                manager.setAutomaticZenRuleState(id, new Condition(CONDITION, "RideDeck visible",
                    visible > 0 ? Condition.STATE_TRUE : Condition.STATE_FALSE));
            }
        } catch (RuntimeException error) {
            android.util.Log.w("RideDeckQuiet", "Quiet mode unavailable", error);
        }
    }
    private RideQuietMode() { }
}
