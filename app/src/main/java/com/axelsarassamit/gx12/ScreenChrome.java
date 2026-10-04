package com.axelsarassamit.gx12;

import android.os.Build;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Dark system bars; ride surfaces use transient bars revealed by an edge swipe. */
public final class ScreenChrome {
    private ScreenChrome() { }
    public static void apply(Window window, boolean immersive) {
        window.setStatusBarColor(0xff101419);
        window.setNavigationBarColor(0xff101419);
        if (Build.VERSION.SDK_INT >= 28) window.setNavigationBarDividerColor(0xff101419);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(window, window.getDecorView());
        bars.setAppearanceLightStatusBars(false); bars.setAppearanceLightNavigationBars(false);
        bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        if (immersive) bars.hide(WindowInsetsCompat.Type.systemBars());
        else bars.show(WindowInsetsCompat.Type.systemBars());
    }
}
