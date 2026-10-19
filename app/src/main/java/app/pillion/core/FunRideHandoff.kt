package app.pillion.core

import java.net.URLEncoder

/** Deliberately bounded to the documented Google Maps URL waypoint contract. */
object FunRideHandoff {
    const val PACKAGE = "com.google.android.apps.maps"
    const val DESCRIPTION = "Google Maps recalculates between every stop. The preview road shape, duration and road preferences are not guaranteed to carry over. Check every stop and road before starting."
    fun url(start: FunPoint, finish: FunPoint, stops: List<FunPoint>, avoid: List<String>): String {
        require(stops.size in 1..2) { "A fun ride must retain its exploration stops" }
        require(avoid.all { it in setOf("tolls", "highways", "ferries") })
        fun point(p: FunPoint) = "${p.latitude},${p.longitude}"
        fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
        return "https://www.google.com/maps/dir/?api=1&travelmode=two-wheeler" +
            "&origin=${encode(point(start))}&destination=${encode(point(finish))}" +
            "&waypoints=${encode(stops.joinToString("|") { point(it) })}" +
            if (avoid.isEmpty()) "" else "&avoid=${encode(avoid.joinToString(","))}"
    }
}
