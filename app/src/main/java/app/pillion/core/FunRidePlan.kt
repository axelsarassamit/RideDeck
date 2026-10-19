package app.pillion.core

import kotlin.math.*
data class FunPoint(val latitude:Double,val longitude:Double) {
    init { require(latitude.isFinite() && longitude.isFinite() && latitude in -85.0..85.0 && longitude in -180.0..180.0) }
}
/** Generates exploration seeds only. Actual roads, access and duration come from routing. */
object FunRidePlan {
    fun offset(start:FunPoint,km:Double,bearing:Double):FunPoint {
        val radius=km/6371.0;val lat=Math.toRadians(start.latitude);val lon=Math.toRadians(start.longitude);val b=Math.toRadians(bearing)
        val a=asin(sin(lat)*cos(radius)+cos(lat)*sin(radius)*cos(b))
        val o=lon+atan2(sin(b)*sin(radius)*cos(lat),cos(radius)-sin(lat)*sin(a))
        return FunPoint(Math.toDegrees(a),((Math.toDegrees(o)+540)%360)-180)
    }
    fun seeds(start:FunPoint,minutes:Int,bearing:Double):List<FunPoint> {
        require(minutes in 30..360)
        // 30 km/h planning assumption is only a seed size, never an ETA promise.
        val radius=(minutes/60.0*30.0/4.5).coerceIn(3.0,40.0)
        return listOf(offset(start,radius,bearing-40),offset(start,radius,bearing+40))
    }
    fun score(actualMinutes:Double,target:Int,uTurns:Int):Double {
        require(actualMinutes.isFinite() && actualMinutes>0 && target in 30..360 && uTurns>=0)
        return abs(actualMinutes-target)/target + uTurns*.04
    }
    /** Approximate occupied road cells, so different seed guesses cannot masquerade as distinct roads. */
    fun sameRoads(a:List<FunPoint>,b:List<FunPoint>):Boolean {
        fun cells(points:List<FunPoint>)=points.map{Pair((it.latitude*500).roundToInt(),(it.longitude*500).roundToInt())}.toSet()
        val x=cells(a);val y=cells(b)
        return x.isNotEmpty() && y.isNotEmpty() && x.intersect(y).size.toDouble()/min(x.size,y.size)>.9
    }
}
