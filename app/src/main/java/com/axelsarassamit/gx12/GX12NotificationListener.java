package com.axelsarassamit.gx12;

import android.service.notification.NotificationListenerService;

/**
 * Android requires an enabled notification listener before an app can access
 * active media sessions belonging to other apps. This service intentionally
 * does not inspect, store, or forward notification contents.
 */
public final class GX12NotificationListener extends NotificationListenerService {
}
