package com.axelsarassamit.gx12

import app.pillion.core.FunPoint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class FunRoute(val start: FunPoint, val finish: FunPoint, val stops: List<FunPoint>,
    val points: List<FunPoint>, val seconds: Double, val km: Double, val uTurns: Int) {
    init {
        require(stops.size in 1..2 && points.size in 2..100000)
        require(seconds.isFinite() && seconds>0 && seconds<=31536000 && km.isFinite() && km>0 && km<=40075)
        require(uTurns in 0..100000)
    }
    fun json() = JSONObject().put("start", pair(start)).put("finish", pair(finish))
        .put("stops", JSONArray(stops.map(::pair))).put("points", JSONArray(points.map(::pair)))
        .put("seconds", seconds).put("km", km).put("uTurns", uTurns)
    companion object {
        fun pair(p: FunPoint) = JSONArray().put(p.latitude).put(p.longitude)
        fun point(a: JSONArray) = FunPoint(a.getDouble(0), a.getDouble(1))
        fun read(j: JSONObject): FunRoute {
            fun list(key: String) = j.getJSONArray(key).let { a -> require(a.length() in (if(key=="stops")1..2 else 2..100000));(0 until a.length()).map { point(a.getJSONArray(it)) } }
            return FunRoute(point(j.getJSONArray("start")), point(j.getJSONArray("finish")), list("stops"), list("points"), j.getDouble("seconds"), j.getDouble("km"), j.getInt("uTurns"))
        }
    }
}

internal object FunRideRouting {
    fun route(start: FunPoint, finish: FunPoint, stops: List<FunPoint>, motorway: Boolean, toll: Boolean, ferry: Boolean, dirt: Boolean): FunRoute {
        val locations = JSONArray()
        (listOf(start) + stops + finish).forEach { locations.put(JSONObject().put("lat", it.latitude).put("lon", it.longitude).put("type", "break")) }
        val options = JSONObject().put("use_highways", if (motorway) 0 else .5)
            .put("use_tolls", if (toll) 0 else .5).put("use_ferry", if (ferry) 0 else .5).put("exclude_unpaved", dirt)
        val request = JSONObject().put("locations", locations).put("costing", "motorcycle")
            .put("costing_options", JSONObject().put("motorcycle", options)).put("units", "kilometers")
        val c = URL("https://valhalla1.openstreetmap.de/route").openConnection() as HttpURLConnection
        val response = try {
            c.instanceFollowRedirects=false
            c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 12000; c.readTimeout = 20000
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("User-Agent", "RideDeck/${BuildConfig.VERSION_NAME} FunRide")
            c.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            check(c.responseCode == 200) { "Routing unavailable. Try a shorter ride or another direction." }
            c.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val out = StringBuilder(); val buffer = CharArray(4096)
                while (true) { if (Thread.currentThread().isInterrupted) throw InterruptedException(); val n = reader.read(buffer); if (n < 0) break; out.append(buffer,0,n); check(out.length <= 2_000_000) }
                JSONObject(out.toString())
            }
        } finally { c.disconnect() }
        val trip = response.getJSONObject("trip"); val summary = trip.getJSONObject("summary")
        check(trip.optInt("status",-1)==0)
        val points = mutableListOf<FunPoint>(); var turns = 0
        val legs = trip.getJSONArray("legs")
        check(legs.length()==stops.size+1)
        for (i in 0 until legs.length()) {
            val leg = legs.getJSONObject(i);val shape=decode(leg.getString("shape"));check(shape.size>=2);points += shape
            leg.optJSONArray("maneuvers")?.let { a -> for (k in 0 until a.length()) if (a.getJSONObject(k).optInt("type") in 12..13) turns++ }
        }
        check(points.size in 2..100000)
        return FunRoute(start, finish, stops, points, summary.getDouble("time"), summary.getDouble("length"), turns)
    }
    private fun decode(encoded: String): List<FunPoint> {
        var index = 0; var lat = 0L; var lon = 0L; val result = mutableListOf<FunPoint>()
        fun number(): Long {
            var shift = 0; var value = 0L; var b: Int
            do { check(index < encoded.length && shift <= 35); b = encoded[index++].code - 63; check(b in 0..63); value = value or ((b and 31).toLong() shl shift); shift += 5 } while (b >= 32)
            return if (value and 1L != 0L) (value shr 1).inv() else value shr 1
        }
        while (index < encoded.length) { lat += number(); lon += number(); result += FunPoint(lat / 1e6, lon / 1e6); check(result.size <= 100000) }
        return result
    }
}
