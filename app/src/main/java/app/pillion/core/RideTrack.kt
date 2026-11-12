package app.pillion.core

import kotlin.math.*

data class RideTrackPoint(val lat:Double,val lon:Double,val time:Long?=null,val elevation:Double?=null) {
    init { require(lat in -90.0..90.0 && lon in -180.0..180.0) }
}

data class RideTrack(val segments:List<List<RideTrackPoint>>) {
    init { require(segments.size<=10000 && segments.sumOf { it.size }<=100000) }
    val count get()=segments.sumOf { it.size }
    fun recap():TrackRecap {
        var meters=0.0;var moving=0L;var covered=0L;var elevation=0.0;var hasElevation=false
        for(segment in segments)for(i in 1 until segment.size) {
            val a=segment[i-1];val b=segment[i]
            val dlat=Math.toRadians(b.lat-a.lat);val dlon=Math.toRadians(b.lon-a.lon)
            val h=(sin(dlat/2).pow(2)+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*sin(dlon/2).pow(2)).coerceIn(0.0,1.0)
            val distance=12_742_000*asin(sqrt(h));meters+=distance
            if(a.time!=null && b.time!=null) {
                val dt=b.time-a.time
                if(b.time>a.time && dt in 1..30_000 && distance*1000/dt<=80) { covered+=dt;if(distance*1000/dt>=1.4)moving+=dt }
            }
            if(a.elevation?.isFinite()==true && b.elevation?.isFinite()==true) {
                hasElevation=true; elevation+=max(0.0,b.elevation-a.elevation)
            }
        }
        return TrackRecap(meters,if(covered>0)moving else null,covered,if(hasElevation)elevation else null)
    }
}
data class TrackRecap(val meters:Double,val movingMs:Long?,val timedMs:Long,val climb:Double?)
