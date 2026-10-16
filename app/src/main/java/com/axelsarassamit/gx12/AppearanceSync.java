package com.axelsarassamit.gx12;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Appearance is sent only to the authenticated Dash installation, never via launch extras. */
public final class AppearanceSync {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private AppearanceSync() { }
    public static void send(Context context) {
        Context app=context.getApplicationContext();
        WORKER.execute(()->{
            try {
                if(!DashPackageVerifier.isInstalledAndSigned(app)) return;
                Bundle extras=new Bundle();extras.putInt("protocolVersion",1);
                extras.putInt("appearanceMode",RideAppearance.mode(app));
                extras.putInt("appearanceAccent",RideAppearance.accentIndex(app));
                app.getContentResolver().call(Uri.parse("content://com.axelsarassamit.ridedeck.dash.bridge"),"appearance",null,extras);
            } catch(RuntimeException ignored) { /* Older or absent Dash keeps working without appearance sync. */ }
        });
    }
}
