package com.axelsarassamit.gx12

import android.content.Context
import android.os.SystemClock
import java.io.File

/** Bounded local metadata log. No destination, message, image, pairing code or device address. */
object RideDeckDiagnostics {
    private fun clean(value: String): String = value
        .replace(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"), "[address]")
        .replace(Regex("[0-9a-fA-F]{64}"), "[token]")
        .replace(Regex("(?i)(google.navigation:|geo:|https?://)\\S+"), "[location/url]")
        .take(3500)
    @JvmStatic @Synchronized fun record(context: Context, event: String) {
        runCatching {
            val file = File(context.filesDir, "ridedeck-diagnostics.txt")
            if (file.length() > 200000) {
                val old = File(context.filesDir, "ridedeck-diagnostics.previous.txt")
                old.delete(); file.renameTo(old)
            }
            file.appendText("${System.currentTimeMillis()} elapsed=${SystemClock.elapsedRealtime()} ${clean(event)}\n")
        }
    }
    @JvmStatic @Synchronized fun report(context: Context): String {
        snapshot(context)
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val header = "RideDeck $version; Android ${android.os.Build.VERSION.RELEASE}; map=${RidePreferences.selectedMap(context)}\nMetadata only. No locations, messages or images.\n"
        return header + listOf("ridedeck-diagnostics.previous.txt", "ridedeck-diagnostics.txt").joinToString("\n") {
            runCatching { cleanLog(File(context.filesDir, it).readText()) }.getOrDefault("")
        }
    }
    private fun cleanLog(value: String) = value.lineSequence().map { clean(it) }.joinToString("\n")
    @JvmStatic fun snapshot(context: Context) {
        runCatching {
            val power = context.getSystemService(android.os.PowerManager::class.java)
            val network = context.getSystemService(android.net.ConnectivityManager::class.java)
            val caps = network.getNetworkCapabilities(network.activeNetwork)
            val battery = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val notification = context.getSystemService(android.app.NotificationManager::class.java)
            val audio = context.getSystemService(android.media.AudioManager::class.java)
            val micType = if (android.os.Build.VERSION.SDK_INT >= 31) audio.communicationDevice?.type else null
            val listener = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
            val own = android.content.ComponentName(context, GX12NotificationListener::class.java)
            val listenerAllowed = listener.split(':').any { android.content.ComponentName.unflattenFromString(it) == own }
            record(context, "Snapshot batteryLevel=${battery?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)} batteryScale=${battery?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)} temperatureTenthsC=${battery?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)} interactive=${power.isInteractive} powerSave=${power.isPowerSaveMode} batteryExempt=${power.isIgnoringBatteryOptimizations(context.packageName)}")
            record(context, "Snapshot wifi=${caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true} cellular=${caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true} internetValidated=${caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true} notificationAccess=$listenerAllowed notificationsEnabled=${notification.areNotificationsEnabled()} quietAccess=${notification.isNotificationPolicyAccessGranted} audioMode=${audio.mode} communicationDeviceType=$micType")
            record(context, "Snapshot model=${android.os.Build.MODEL} AndroidSdk=${android.os.Build.VERSION.SDK_INT}")
        }.onFailure { record(context, "Snapshot unavailable exception=${it.javaClass.simpleName}") }
    }
}
