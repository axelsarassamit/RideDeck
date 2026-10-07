package com.axelsarassamit.gx12

import android.app.Activity
import android.app.KeyguardManager
import android.graphics.Point
import android.os.SystemClock
import app.pillion.android.PillionAdb
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Optional shell placement, using existing display access only. No new pairing required. */
object PortraitSplitPlacement {
    private val worker = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private var lastAttempt = 0L
    @JvmStatic fun check(activity: Activity) {
        if (activity is SetupActivity || activity.isFinishing || !activity.isInMultiWindowMode) return
        if ((activity.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) return
        val decor = activity.window.decorView
        val size = Point()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getRealSize(size)
        // Use the physical display, since a landscape phone's half-window can report portrait.
        if (size.y <= size.x || decor.height <= 0) return
        val origin = IntArray(2); decor.getLocationOnScreen(origin)
        if (origin[1] >= size.y / 2 || decor.height >= size.y * 0.8) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastAttempt < 10000) return
        val adb = PillionAdb.getInstance(activity)
        if (!adb.isConnected || !busy.compareAndSet(false, true)) return
        lastAttempt = now
        val task = activity.taskId
        val context = activity.applicationContext
        worker.execute {
            try {
                val help = adb.runShell("dumpsys activity service com.android.systemui/.SystemUIService WMShell help")
                // Only use the command verified on HyperOS. Other implementations need verification.
                if (!help.contains("soScStartTask <taskId> <Position>")) { busy.set(false); return@execute }
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed || !activity.isInMultiWindowMode ||
                        (activity.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) { busy.set(false); return@runOnUiThread }
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay.getRealSize(size)
                    if (size.y <= size.x) { busy.set(false); return@runOnUiThread }
                    val position = IntArray(2); decor.getLocationOnScreen(position)
                    if (position[1] >= size.y / 2 || decor.height >= size.y * 0.8) { busy.set(false); return@runOnUiThread }
                    worker.execute {
                        try {
                            adb.runShell("dumpsys activity service com.android.systemui/.SystemUIService WMShell SoScSplitScreen soScStartTask $task 1")
                            BikeDiagnostics.record(context, "Portrait split bottom placement requested")
                        } catch (_: Exception) {
                            BikeDiagnostics.record(context, "Portrait split placement unavailable")
                        } finally { busy.set(false) }
                    }
                }
            } catch (_: Exception) { busy.set(false) }
        }
    }
}
