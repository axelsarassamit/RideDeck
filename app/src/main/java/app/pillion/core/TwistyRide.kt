package app.pillion.core

import kotlin.math.*

/** Geometry ranking of routes returned by the provider, not a routing or road-access rule. */
object TwistyRide {
    val names=listOf("Relaxed exploration","Curvy","Very curvy")
    private fun wrap(degrees:Double)=((degrees+540)%360)-180
    private fun heading(a:FunPoint,b:FunPoint):Double {
        val x=wrap(b.longitude-a.longitude)*cos(Math.toRadians((a.latitude+b.latitude)/2))
        return Math.toDegrees(atan2(x,b.latitude-a.latitude))
    }
    fun bonus(style:Int,points:List<FunPoint>):Double {
        if(style !in 1..2 || points.size<3)return 0.0
        var prev=points.first();var anchor=prev;var oldHeading:Double?=null
        var length=0.0;var bends=0.0
        for(p in points.drop(1)) {
            val dx=wrap(p.longitude-prev.longitude)*cos(Math.toRadians((p.latitude+prev.latitude)/2))
            length+=111195*hypot(dx,p.latitude-prev.latitude);prev=p
            val step=111195*hypot(wrap(p.longitude-anchor.longitude)*cos(Math.toRadians((p.latitude+anchor.latitude)/2)),p.latitude-anchor.latitude)
            if(step<55)continue
            val next=heading(anchor,p)
            oldHeading?.let { val bend=abs(wrap(next-it));if(bend in 10.0..65.0)bends+=bend }
            oldHeading=next;anchor=p
        }
        // Route-time error is a fraction of the target. Cap the bonus to keep it comparable.
        return (bends/max(1.0,length/1000)/180.0).coerceAtMost(.15)*style
    }
}
