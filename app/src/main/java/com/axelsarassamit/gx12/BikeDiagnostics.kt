package com.axelsarassamit.gx12

import android.content.Context
import android.os.SystemClock
import java.io.File

/** Bounded local metadata log. No destination, message, image, pairing code or device address. */
object BikeDiagnostics {
    private fun clean(value: String): String = value
        .replace(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"), "[address]")
        .replace(Regex("[0-9a-fA-F]{64}"), "[token]")
        .replace(Regex("(?i)(google.navigation:|geo:|https?://)\\S+"), "[location/url]")
        .take(3500)
    @JvmStatic @Synchronized fun record(context: Context, event: String) {
        runCatching {
            val file = File(context.filesDir, "bike-diagnostics.txt")
            if (file.length() > 200000) {
                val old = File(context.filesDir, "bike-diagnostics.previous.txt")
                old.delete(); file.renameTo(old)
            }
            file.appendText("${System.currentTimeMillis()} elapsed=${SystemClock.elapsedRealtime()} ${clean(event)}\n")
        }
    }
    @JvmStatic @Synchronized fun report(context: Context): String {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val header = "RideDeck $version; Android ${android.os.Build.VERSION.RELEASE}; map=${RidePreferences.selectedMap(context)}; density=${RidePreferences.bikeMapDensity(context)}\nMetadata only. No locations, messages or images.\n"
        return header + listOf("bike-diagnostics.previous.txt", "bike-diagnostics.txt").joinToString("\n") {
            runCatching { cleanLog(File(context.filesDir, it).readText()) }.getOrDefault("")
        }
    }
    private fun cleanLog(value: String) = value.lineSequence().map { clean(it) }.joinToString("\n")
}
