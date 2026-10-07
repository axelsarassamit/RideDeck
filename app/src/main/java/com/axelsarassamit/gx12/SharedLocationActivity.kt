package com.axelsarassamit.gx12

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Shared content is untrusted. Resolve only recognized Google Maps HTTPS links. */
class SharedLocationActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        BikeDiagnostics.record(this, "Shared destination received")
        val text = if (intent.action == Intent.ACTION_SEND) intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() else null
        if (text.isNullOrBlank() || text.length > 12000) { fail("Share a place from Google Maps to RideDeck."); return }
        val progress = AlertDialog.Builder(this).setTitle("Shared destination").setMessage("Finding the place...").setNegativeButton("Cancel") { _, _ -> finish() }.create()
        progress.setCancelable(false); progress.show()
        worker.execute {
            val result = runCatching { resolve(text) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.dismiss()
                result.onSuccess { BikeDiagnostics.record(this, "Shared destination resolved"); confirm(it) }.onFailure { BikeDiagnostics.record(this, "Shared destination resolution failed exception=${it.javaClass.simpleName}"); fail("This shared link could not be read. Share the place again, or enter its full address in RideDeck.") }
            }
        }
    }

    private fun allowed(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && (uri.port == -1 || uri.port == 443) &&
            (host == "maps.app.goo.gl" || (host == "goo.gl" && uri.path.orEmpty().startsWith("/maps")) ||
             host == "maps.google.com" || ((host == "google.com" || host == "www.google.com") && uri.path.orEmpty().startsWith("/maps")))
    }

    private fun resolve(text: String): String {
        val link = Regex("https://[^\\s<>]+").find(text)?.value?.trimEnd('.', ',', ')')
        if (link == null) {
            require(!text.contains("://"))
            return text.trim().take(500)
        }
        var uri = Uri.parse(link)
        require(allowed(uri))
        repeat(6) {
            coordinates(uri.toString())?.let { return it }
            for (key in listOf("destination", "query", "q")) {
                uri.getQueryParameter(key)?.takeIf { it.isNotBlank() && !it.startsWith("http") }?.let { return it.take(500) }
            }
            val connection = URL(uri.toString()).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8000; connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "RideDeck-Android")
                val code = connection.responseCode
                if (code in 300..399) {
                    val next = Uri.parse(URL(URL(uri.toString()), connection.getHeaderField("Location") ?: error("Missing redirect")).toString())
                    require(allowed(next)); uri = next
                } else {
                    require(code == 200)
                    // Do not substitute the map's @camera center for the destination.
                    val place = Regex("/maps/place/([^/]+)").find(uri.path.orEmpty())?.groupValues?.get(1)
                    require(!place.isNullOrBlank())
                    return place.replace('+', ' ').take(500)
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }

    private fun coordinates(link: String): String? {
        val match = Regex("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)").find(link) ?: return null
        val lat = match.groupValues[1].toDouble(); val lon = match.groupValues[2].toDouble()
        require(lat in -90.0..90.0 && lon in -180.0..180.0)
        return "$lat,$lon"
    }

    private fun confirm(destination: String) {
        val field = EditText(this).apply { setText(destination); hint = "Confirm the place or full address" }
        val dialog = AlertDialog.Builder(this).setTitle("Shared destination").setMessage("Check the destination while parked.")
            .setView(field).setNegativeButton("Cancel") { _, _ -> finish() }
            .setPositiveButton("Navigate", null).create()
        dialog.setOnCancelListener { finish() }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val target = field.text.toString().trim()
                if (target.isEmpty()) { field.error = "Enter a destination"; return@setOnClickListener }
                if (BuildConfig.YAMAHA) {
                    startActivity(Intent(this, MainActivity::class.java).putExtra("shared_destination", target))
                    finish()
                    return@setOnClickListener
                }
                val coordinate = Regex("^-?\\d+(?:\\.\\d+)?,\\s*-?\\d+(?:\\.\\d+)?$").matches(target)
                val destinationUri = if (RidePreferences.selectedMap(this) == "com.waze" && coordinate) Uri.parse("https://waze.com/ul").buildUpon().appendQueryParameter("ll", target).appendQueryParameter("navigate", "yes").build()
                    else Uri.parse("geo:0,0").buildUpon().appendQueryParameter("q", target).build()
                try { startActivity(Intent(Intent.ACTION_VIEW, destinationUri).setPackage(RidePreferences.selectedMap(this))); finish() }
                catch (_: Exception) { field.error = "The selected map app cannot open this destination." }
            }
        }
        dialog.show()
    }
    private fun fail(message: String) {
        AlertDialog.Builder(this).setTitle("Shared destination").setMessage(message).setPositiveButton("Close") { _, _ -> finish() }.setOnCancelListener { finish() }.show()
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
