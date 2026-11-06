package com.axelsarassamit.gx12

import android.util.Xml
import app.pillion.core.RideTrack
import app.pillion.core.RideTrackPoint
import org.xmlpull.v1.XmlPullParser as X
import java.io.*
import java.time.Instant

internal object RideGpx {
    private const val MAX_BYTES=8_000_000
    fun read(input:InputStream):RideTrack {
        val bytes=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) { val n=input.read(buffer);if(n<0)break;require(bytes.size()+n<=MAX_BYTES);bytes.write(buffer,0,n) }
        val parser=Xml.newPullParser().apply {
            setFeature(X.FEATURE_PROCESS_NAMESPACES,true)
            setFeature(X.FEATURE_PROCESS_DOCDECL,false)
            setInput(ByteArrayInputStream(bytes.toByteArray()),null)
        }
        val segments=mutableListOf<MutableList<RideTrackPoint>>()
        var count=0;var container=0;var pointDepth=0;var field="";var value=""
        var lat=0.0;var lon=0.0;var time:Long?=null;var elevation:Double?=null
        var foundRoot=false
        while(true) {
            when(parser.nextToken()) {
                X.END_DOCUMENT -> break
                X.DOCDECL -> error("Unsupported GPX XML")
                X.START_TAG -> {
                    require(parser.depth<=32)
                    if(parser.depth==1) { require(parser.name=="gpx");foundRoot=true }
                    if(pointDepth==0 && ((parser.depth==3 && parser.name=="trkseg")||(parser.depth==2 && parser.name=="rte"))) {
                        require(segments.size<10000);segments.add(mutableListOf());container=parser.depth
                    } else if(container>0 && parser.depth==container+1 && parser.name in setOf("trkpt","rtept")) {
                        require(++count<=100000);pointDepth=parser.depth
                        lat=parser.getAttributeValue(null,"lat").toDouble();lon=parser.getAttributeValue(null,"lon").toDouble();time=null;elevation=null
                    } else if(pointDepth>0 && parser.depth==pointDepth+1 && parser.name in setOf("time","ele")) { field=parser.name;value="" }
                }
                X.TEXT, X.CDSECT -> if(field.isNotEmpty() && value.length<80) { value+=parser.text.orEmpty().take(80) }
                X.END_TAG -> {
                    if(field.isNotEmpty() && parser.depth==pointDepth+1) {
                        if(field=="time") time=runCatching { Instant.parse(value.trim()).toEpochMilli() }.getOrNull()
                        else elevation=value.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in -15000.0..20000.0 }
                        field=""
                    }
                    if(pointDepth>0 && parser.depth==pointDepth) {
                        segments.last().add(RideTrackPoint(lat,lon,time,elevation));pointDepth=0
                    }
                    if(parser.depth==container)container=0
                }
            }
        }
        require(foundRoot && count>=2)
        return RideTrack(segments.filter{it.isNotEmpty()})
    }
    fun write(track:RideTrack,output:OutputStream) {
        val xml=Xml.newSerializer().apply { setOutput(output,"UTF-8");startDocument("UTF-8",true) }
        val ns="http://www.topografix.com/GPX/1/1"
        xml.setPrefix("",ns);xml.startTag(ns,"gpx").attribute(null,"version","1.1").attribute(null,"creator","RideDeck")
        xml.startTag(ns,"trk")
        for(s in track.segments) {
            xml.startTag(ns,"trkseg")
            for(p in s) {
                xml.startTag(ns,"trkpt").attribute(null,"lat",p.lat.toString()).attribute(null,"lon",p.lon.toString())
                p.elevation?.takeIf { it.isFinite() }?.let { xml.startTag(ns,"ele").text(it.toString()).endTag(ns,"ele") }
                p.time?.let { xml.startTag(ns,"time").text(Instant.ofEpochMilli(it).toString()).endTag(ns,"time") }
                xml.endTag(ns,"trkpt")
            }
            xml.endTag(ns,"trkseg")
        }
        xml.endTag(ns,"trk").endTag(ns,"gpx");xml.endDocument();xml.flush()
    }
}
