package com.axelsarassamit.gx12

import android.content.Context
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class BikePlace(val name: String, val destination: String, val metres: Float = 0f)
object BikePlaces {
    @Volatile private var stationTime = 0L
    @Volatile private var lastSearch = 0L
    @JvmField @Volatile var stations: List<BikePlace> = emptyList()
    fun freshStations(): List<BikePlace> = if (android.os.SystemClock.elapsedRealtime() - stationTime < 900000) stations else emptyList()
    @JvmStatic fun favorites(context: Context): List<BikePlace> = runCatching {
        val array = JSONArray(RidePreferences.prefs(context).getString("bike_favorites", "[]"))
        (0 until array.length()).map { array.getJSONObject(it) }.map {
            BikePlace(it.getString("name"), it.getString("destination"))
        }.take(20)
    }.getOrDefault(emptyList())
    @JvmStatic fun add(context: Context, name: String, destination: String) {
        require(name.isNotBlank() && destination.isNotBlank()) { "Enter a name and destination" }
        val places = favorites(context)
        require(places.size < 20) { "Up to 20 favorites are supported" }
        save(context, places + BikePlace(name.take(60), destination.take(500)))
    }
    @JvmStatic fun remove(context: Context, index: Int) = save(context, favorites(context).filterIndexed { i, _ -> i != index })
    private fun save(context: Context, places: List<BikePlace>) {
        val array = JSONArray()
        places.forEach { array.put(JSONObject().put("name", it.name).put("destination", it.destination)) }
        RidePreferences.prefs(context).edit().putString("bike_favorites", array.toString()).apply()
    }
    /** Wire layout recovered from CRC-verified StreetCross services 98 and 99. */
    fun listItem(index: Int, place: BikePlace, station: Boolean): ByteArray {
        var label = place.name.take(60)
        while (label.toByteArray(Charsets.UTF_8).size > 180) label = label.dropLast(1)
        val name = label.toByteArray(Charsets.UTF_8)
        val unit = if (station) "km".toByteArray() else ByteArray(0)
        return ByteBuffer.allocate(11 + unit.size + name.size).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(index.toShort()).putShort(0).put(1).put(name.size.toByte()).put(unit.size.toByte())
            .putFloat(if (station) place.metres / 1000 else 0f).put(unit).put(name).array()
    }
    @JvmStatic fun findStations(context: Context, latitude: Double, longitude: Double): Int {
        val now = android.os.SystemClock.elapsedRealtime()
        check(lastSearch == 0L || now - lastSearch >= 60000) { "Wait one minute before searching again" }
        lastSearch = now
        stations = emptyList()
        val query = "[out:json][timeout:15];nwr[amenity=fuel](around:10000,$latitude,$longitude);out center tags;"
        val connection = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 15000; connection.readTimeout = 25000
            connection.setRequestProperty("User-Agent", "RideDeck/Android personal-use fuel search")
            connection.doOutput = true
            connection.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
            check(connection.responseCode == 200) { "Fuel search unavailable. Try again later." }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            check(response.length < 2000000) { "Fuel search response too large" }
            val elements = JSONObject(response).getJSONArray("elements")
            val found = mutableListOf<BikePlace>()
            for (i in 0 until elements.length()) {
                val item = elements.getJSONObject(i); val point = item.optJSONObject("center") ?: item
                if (!point.has("lat") || !point.has("lon")) continue
                val lat = point.getDouble("lat"); val lon = point.getDouble("lon"); val distance = FloatArray(1)
                Location.distanceBetween(latitude, longitude, lat, lon, distance)
                val tags = item.optJSONObject("tags") ?: JSONObject()
                found.add(BikePlace(tags.optString("name", tags.optString("brand", "Fuel station")), "$lat,$lon", distance[0]))
            }
            stations = found.sortedBy { it.metres }.take(20)
            stationTime = android.os.SystemClock.elapsedRealtime()
            BikeDiagnostics.record(context, "Fuel search completed results=${stations.size}")
            return stations.size
        } finally { connection.disconnect() }
    }
}
