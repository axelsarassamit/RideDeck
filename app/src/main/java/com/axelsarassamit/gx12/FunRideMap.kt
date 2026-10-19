package com.axelsarassamit.gx12

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import app.pillion.core.FunPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.*

/** Native overview: no JavaScript, page navigation or access to application data. */
internal class FunRideMap(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density=resources.displayMetrics.density
    private val worker = Executors.newSingleThreadExecutor()
    private val tiles = object : android.util.LruCache<String, Bitmap>(48) {}
    private val pending = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()
    @Volatile private var visibleKeys=emptySet<String>()
    private var routes = emptyList<FunRoute>()
    private var selected = 0
    private var zoom = 10
    private var cx = .5; private var cy = .5
    private var lastX = 0f; private var lastY = 0f
    private var alive = true
    init { contentDescription = "Route map. Drag to move. Use zoom buttons to inspect roads."; setBackgroundColor(0xffcbd6d3.toInt()) }
    private fun x(p: FunPoint) = (p.longitude + 180) / 360
    private fun y(p: FunPoint) = (1 - ln(tan(Math.PI / 4 + Math.toRadians(p.latitude) / 2)) / Math.PI) / 2
    fun show(rows: List<FunRoute>, index: Int) { routes = rows; selected = index; fit() }
    fun step(delta: Int) { zoom = (zoom + delta).coerceIn(2,18); invalidate() }
    fun fit() {
        val pts = routes.flatMap { it.points }; if (pts.isEmpty() || width == 0) return
        val xs = pts.map(::x); val ys = pts.map(::y)
        cx = (xs.min() + xs.max()) / 2; cy = (ys.min() + ys.max()) / 2
        zoom = (18 downTo 2).firstOrNull { z -> (xs.max()-xs.min()) * (256 shl z)*density < width*.85 && (ys.max()-ys.min())*(256 shl z)*density<height*.8 } ?: 2
        invalidate()
    }
    override fun onSizeChanged(w: Int,h: Int,ow: Int,oh: Int) { fit() }
    override fun onDraw(canvas: Canvas) {
        val tile=256.0*density;val world = tile * (1 shl zoom); val left = cx*world-width/2; val top = cy*world-height/2
        val count = 1 shl zoom
        val cells=mutableListOf<Triple<Int,Int,String>>()
        for(tx in floor(left/tile).toInt()..floor((left+width)/tile).toInt())for(ty in floor(top/tile).toInt()..floor((top+height)/tile).toInt())if(ty in 0 until count){val wrapped=((tx%count)+count)%count;cells.add(Triple(tx,ty,"$zoom/$wrapped/$ty"))}
        visibleKeys=cells.map{it.third}.toSet()
        for ((tx,ty,key) in cells) {
            val bitmap = tiles.get(key)
            if (bitmap != null) canvas.drawBitmap(bitmap,null,RectF((tx*tile-left).toFloat(),(ty*tile-top).toFloat(),((tx+1)*tile-left).toFloat(),((ty+1)*tile-top).toFloat()),paint)
            else fetch(key)
        }
        fun px(p:FunPoint) = (x(p)*world-left).toFloat()
        fun py(p:FunPoint) = (y(p)*world-top).toFloat()
        routes.indices.sortedBy { if(it==selected) 1 else 0 }.forEach { i ->
            val path=Path(); routes[i].points.forEachIndexed { n,p -> if(n==0)path.moveTo(px(p),py(p))else path.lineTo(px(p),py(p)) }
            paint.style=Paint.Style.STROKE;paint.strokeWidth=(if(i==selected)5f else 3f)*density;paint.color=if(i==selected)0xff006ee6.toInt()else 0xff7d8793.toInt();canvas.drawPath(path,paint)
        }
        routes.getOrNull(selected)?.let { r ->
            (listOf(r.start)+r.stops+r.finish).forEachIndexed { i,p ->
                if(i==r.stops.size+1 && r.start==r.finish)return@forEachIndexed
                paint.style=Paint.Style.FILL;paint.color=Color.WHITE;canvas.drawCircle(px(p),py(p),18f*density,paint)
                paint.color=Color.BLACK;paint.textSize=15f*density;paint.textAlign=Paint.Align.CENTER;canvas.drawText(if(i==0 && r.start==r.finish)"S/F"else if(i==0)"S" else if(i==r.stops.size+1)"F" else "$i",px(p),py(p)+5*density,paint)
            }
        }
        paint.style=Paint.Style.FILL;paint.color=0xddffffff.toInt();canvas.drawRect(0f,height-24f*density,width.toFloat(),height.toFloat(),paint)
        paint.color=Color.BLACK;paint.textAlign=Paint.Align.LEFT;paint.textSize=12f*density;canvas.drawText("© OpenStreetMap contributors · Valhalla",5*density,height-6*density,paint)
    }
    private fun fetch(key:String) {
        if(!alive || pending.size>=12 || key in pending || key in failed)return
        pending.add(key)
        worker.execute {
            if(!alive || key !in visibleKeys){post{pending.remove(key);if(alive)invalidate()};return@execute}
            val bitmap=runCatching {
                val file=java.io.File(context.cacheDir,"fun-tile-${key.replace('/','_')}.png")
                val expires=java.io.File(file.path+".expires")
                val until=runCatching{expires.readText().toLong()}.getOrDefault(file.lastModified()+7*86400000L)
                if(file.isFile && System.currentTimeMillis()<until) return@runCatching BitmapFactory.decodeFile(file.path)
                val c=URL("https://tile.openstreetmap.org/$key.png").openConnection() as HttpURLConnection
                try { c.connectTimeout=7000;c.readTimeout=7000;c.setRequestProperty("User-Agent","RideDeck/${BuildConfig.VERSION_NAME} (https://github.com/axelsarassamit/RideDeck)")
                    check(c.responseCode==200);val bytes=c.inputStream.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(true){val n=input.read(buffer);if(n<0)break;check(out.size()+n<=500000);out.write(buffer,0,n)};out.toByteArray()}
                    val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size);check(bitmap!=null)
                    val cacheControl=c.getHeaderField("Cache-Control").orEmpty()
                    if(!cacheControl.contains("no-store",true)){
                        val seconds=Regex("(?:^|,)\\s*max-age=(\\d+)").find(cacheControl)?.groupValues?.get(1)?.toLongOrNull()
                        val expiry=if(cacheControl.contains("no-cache",true))System.currentTimeMillis()else if(seconds!=null)System.currentTimeMillis()+seconds.coerceAtMost(365*86400L)*1000 else c.expiration.takeIf{it>0}?:System.currentTimeMillis()+7*86400000L
                        file.writeBytes(bytes);expires.writeText(expiry.toString())
                    };bitmap
                } finally { c.disconnect() }
            }.getOrNull()
            post { pending.remove(key);if(bitmap!=null)tiles.put(key,bitmap)else failed.add(key);if(alive)invalidate() }
        }
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { parent.requestDisallowInterceptTouchEvent(true);lastX=event.x;lastY=event.y }
            MotionEvent.ACTION_MOVE -> {val world=256.0*density*(1 shl zoom);cx-=((event.x-lastX)/world);cy=(cy-(event.y-lastY)/world).coerceIn(0.0,1.0);lastX=event.x;lastY=event.y;invalidate()}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {parent.requestDisallowInterceptTouchEvent(false);performClick()}
        };return true
    }
    override fun performClick():Boolean {super.performClick();return true}
    fun close(){alive=false;worker.shutdownNow();tiles.evictAll()}
}
