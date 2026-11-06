package com.axelsarassamit.gx12

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.View
import android.widget.*
import app.pillion.core.*
import java.util.Locale
import java.util.concurrent.Executors
import java.util.UUID
import kotlin.math.*

/** Parked preview. A GPX track is never silently converted into active navigation. */
class RideFilesActivity:Activity() {
    private data class Summary(val title:String,val track:RideTrack,val plannedSeconds:Double?=null,val distanceKm:Double?=null,val recordedSeconds:Long?=null,val peakLean:Double?=null)
    companion object {
        private var handoff:Pair<String,Summary>?=null
        @JvmStatic fun open(a:Activity){a.startActivity(Intent(a,RideFilesActivity::class.java))}
        private fun launch(a:Activity,data:Summary) {
            val key=UUID.randomUUID().toString();handoff=key to data
            a.startActivity(Intent(a,RideFilesActivity::class.java).putExtra("handoff",key))
        }
        fun openPlan(a:Activity,points:List<FunPoint>,seconds:Double,km:Double) = launch(a,Summary("Planned ride",RideTrack(listOf(points.map{RideTrackPoint(it.latitude,it.longitude)})),seconds,km))
        fun openRecorded(a:Activity,title:String,segments:List<List<RideTrackPoint>>,km:Double,seconds:Long,lean:Double?) =
            launch(a,Summary(title,RideTrack(segments),distanceKm=km,recordedSeconds=seconds,peakLean=lean))
    }
    private var summary:Summary?=null
    private val io=Executors.newSingleThreadExecutor()
    private var working=false
    private lateinit var status:TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(saved:Bundle?) {
        RideAppearance.prepare(this);super.onCreate(saved)
        summary=lastNonConfigurationInstance as? Summary ?: handoff?.takeIf{it.first==intent.getStringExtra("handoff")}?.let{ handoff=null;it.second }
        showPage()
    }
    override fun onRetainNonConfigurationInstance():Any?=summary
    override fun onDestroy(){io.shutdownNow();super.onDestroy()}
    private fun recap(s:Summary):String {
        val r=s.track.recap();val km=s.distanceKm?:r.meters/1000
        return buildString {
            append(s.title);append(String.format(Locale.US,"\n%.1f km",km))
            s.plannedSeconds?.let{append(" · ${it.toLong()/60} min planned")}
            s.recordedSeconds?.let{append(" · ${it/60} min recording")}
            r.movingMs?.let{append("\n${it/60000} min moving (timed GPS samples only)")}
            r.climb?.let{append("\nEstimated climb ${it.roundToInt()} m")}
            s.peakLean?.let{append(String.format(Locale.US,"\nPeak mounted-phone lean estimate %.1f°",it))}
        }
    }
    private fun showPage(message:String="") {
        val page=LinearLayout(this).apply { orientation=1;setPadding(dp(16),dp(12),dp(16),dp(16));setBackgroundColor(RideStyle.BACKGROUND) }
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(page) { v,insets ->
            val b=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout());v.setPadding(dp(16)+b.left,dp(12)+b.top,dp(16)+b.right,dp(16)+b.bottom);insets
        }
        fun label(t:String,size:Float)=TextView(this).apply { text=t;textSize=size;setTextColor(RideStyle.TEXT);setPadding(0,dp(6),0,dp(6)) }
        fun action(t:String,fn:()->Unit){page.addView(Button(this).apply{text=t;RideStyle.action(this,false);isEnabled=!working;setOnClickListener{fn()}})}
        page.addView(label("GPX & ride recap",26f))
        page.addView(label("Preview a track while parked. A preview does not start navigation.",16f))
        summary?.let { s ->
            val text=recap(s);page.addView(label(text,18f))
            if(s.track.count>=2)page.addView(trackPreview(s.track),LinearLayout.LayoutParams(-1,dp(220)))
            action("Share recap text") { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share ride recap")) }
            if(s.track.count>=2) {
                action("Save GPX track") {
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/gpx+xml").putExtra(Intent.EXTRA_TITLE,"RideDeck-track.gpx"),2)
                }
                action("Share GPX track") { shareTrack(s.track) }
            } else page.addView(label("No GPS track is available for this ride.",16f))
        }
        action("Import GPX file") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),1) }
        action("Back") { finish() }
        status=label(message,16f);page.addView(status)
        setContentView(ScrollView(this).apply{addView(page)})
    }
    private fun shareTrack(track:RideTrack) {
        working=true;showPage("Preparing GPX…")
        io.execute {
            val file=runCatching {
                val dir=java.io.File(cacheDir,"ride-share");check(dir.isDirectory || dir.mkdirs())
                dir.listFiles()?.filter { it.isFile && it.name.startsWith("RideDeck-") && it.extension=="gpx" &&
                    System.currentTimeMillis()-it.lastModified()>86_400_000 }?.forEach { it.delete() }
                java.io.File.createTempFile("RideDeck-", ".gpx", dir).also { f->f.outputStream().use{RideGpx.write(track,it)} }
            }
            runOnUiThread {
                if(isFinishing || isDestroyed)return@runOnUiThread
                working=false
                val shared=file.mapCatching { f ->
                    val uri=androidx.core.content.FileProvider.getUriForFile(this, "$packageName.apkprovider",f)
                    val send=Intent(Intent.ACTION_SEND).setType("application/gpx+xml").putExtra(Intent.EXTRA_STREAM,uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    send.clipData=android.content.ClipData.newUri(contentResolver,"RideDeck GPX",uri)
                    startActivity(Intent.createChooser(send,"Share GPX track"))
                }
                showPage(if(shared.isFailure)"Could not share this GPX track." else "")
            }
        }
    }
    override fun onActivityResult(request:Int,result:Int,data:Intent?) {
        super.onActivityResult(request,result,data)
        if(result!=RESULT_OK || request !in 1..2)return
        val uri=data?.data ?: return
        val track=summary?.track
        working=true;showPage(if(request==1)"Reading track…" else "Saving track…")
        io.execute {
            val resultData=runCatching {
                if(request==1) contentResolver.openInputStream(uri)?.use { RideGpx.read(it) } ?: error("No file")
                else { requireNotNull(track);requireNotNull(contentResolver.openOutputStream(uri,"wt")).use { RideGpx.write(track,it) };null }
            }
            runOnUiThread {
                if(isFinishing || isDestroyed)return@runOnUiThread
                working=false
                resultData.getOrNull()?.let { summary=Summary("Imported GPX ride",it) }
                showPage(if(resultData.isFailure)"Could not ${if(request==1)"read this GPX track" else "save the track"}." else if(request==1)"Track loaded." else "GPX saved.")
            }
        }
    }
    private fun trackPreview(track:RideTrack):View = object:View(this) {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{strokeWidth=dp(3).toFloat();style=Paint.Style.STROKE;color=0xff399afa.toInt()}
        private val all=track.segments.flatten()
        private val ref=all.first().lon
        private fun lon(p:RideTrackPoint)=((p.lon-ref+540)%360)-180
        private val x0=all.minOf(::lon);private val x1=all.maxOf(::lon)
        private val y0=all.minOf{it.lat};private val y1=all.maxOf{it.lat}
        init { contentDescription="Outline of the GPX track" }
        override fun onDraw(c:Canvas) {
            c.drawColor(0xff101724.toInt());val path=Path();val pad=dp(16)
            val correction=cos(Math.toRadians((y0+y1)/2)).coerceAtLeast(.01)
            val dx=(x1-x0)*correction;val dy=y1-y0
            val scale=min((width-2.0*pad)/dx.coerceAtLeast(.000001),(height-2.0*pad)/dy.coerceAtLeast(.000001)).coerceAtLeast(0.0)
            track.segments.forEach { segment -> segment.forEachIndexed { i,p ->
                val x=(width/2.0+(lon(p)-(x0+x1)/2)*correction*scale).toFloat();val y=(height/2.0- (p.lat-(y0+y1)/2)*scale).toFloat()
                if(i==0)path.moveTo(x,y)else path.lineTo(x,y)
            } };c.drawPath(path,paint)
        }
    }
}
