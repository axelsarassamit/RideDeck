package com.axelsarassamit.gx12;

/** Ask Android to restore an already-authorized listener after replacing this APK. */
public final class UpdateRecoveryReceiver extends android.content.BroadcastReceiver {
    @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
        if (android.content.Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            GX12NotificationListener.recover(context);
        }
    }
}
