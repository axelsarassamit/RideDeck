package com.axelsarassamit.gx12

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.*
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.*
import android.speech.tts.TextToSpeech
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.*

/** Own map pixels and GPS. No screenshots, virtual displays or debugging privileges. */
object NativeNavigation {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var context: Context? = null
    @Volatile var running = false
    @Volatile var location: Location? = null
    @Volatile var route: NavigationRoute? = null
    @Volatile var status = "Start navigation to get a GPS position"
    @Volatile var mapBitmap: Bitmap? = null
    @Volatile private var mapJpeg: ByteArray? = null
    @Volatile private var guidance = "Waiting for GPS"
    @Volatile private var maneuver = 0
    private var zoomLevel = 16.0
    private var height = 234
    private var snapshotter: MapSnapshotter? = null
    private var inFlight = false
    private var frameAt = 0L
    private var renderStarted = 0L
    private var routeGeneration = 0
    private var routeBusy = false
    private var pendingDestination: NavigationPlace? = null
    private var progressIndex = 0
    private var remaining = 0.0
    private var lastReroute = 0L
    private var offRouteSamples = 0
    private var lastSpoken = ""
    private var speaker: TextToSpeech? = null
    private var speechReady = false
    private var lastSummary = 0L

    @JvmStatic fun start(c: Context) {
        context = c.applicationContext
        runCatching { NavigationSecrets.importDebugSetup(c) }
        if (c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status = "Allow precise location in Setup to start navigation"; return
        }
        if (!running) {
            try { c.startForegroundService(Intent(c, NativeNavigationService::class.java)) }
            catch (_: Exception) { status = "Open RideDeck and start navigation again" }
        }
    }
    fun attach(c: Context) {
        context = c.applicationContext; running = true
        MapLibre.getInstance(c)
        speaker = TextToSpeech(c) { result -> speechReady = result == TextToSpeech.SUCCESS }
        speaker?.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
        main.removeCallbacks(tick); main.post(tick)
        BikeDiagnostics.record(c, "Native navigation started renderer=MapLibre locationService=true")
    }
    fun detach() {
        running = false; routeGeneration++; routeBusy = false; pendingDestination = null
        main.removeCallbacks(tick); snapshotter?.cancel(); snapshotter = null; inFlight = false
        speaker?.shutdown(); speaker = null; speechReady = false
        location = null; mapJpeg = null; mapBitmap = null
        status = "Navigation stopped"
    }
    fun resize(width: Int, h: Int) {
        require(width == 480 && h in listOf(234, 240))
        main.post { height = h; snapshotter?.cancel(); snapshotter = null; inFlight = false }
    }
    fun update(next: Location) {
        if (!next.hasAccuracy() || next.accuracy > 50f || SystemClock.elapsedRealtimeNanos() - next.elapsedRealtimeNanos > 30_000_000_000L) {
            status = "Waiting for a precise GPS fix"; return
        }
        location = Location(next)
        pendingDestination?.let { target ->
            pendingDestination = null
            context?.let { navigate(it, target) }
            return
        }
        status = if (route == null) "GPS ready. Choose a destination" else "Navigating"
        updateGuidance(next)
    }
    @JvmStatic fun stopRoute() {
        main.post { routeGeneration++; routeBusy = false; pendingDestination = null; route = null; guidance = "Choose a destination"; lastSpoken = ""; progressIndex = 0; speaker?.stop() }
    }
    @JvmStatic fun routeText(text: String) {
        val c = context ?: error("Open RideDeck navigation first")
        val coordinate = NavigationApi.coordinate(text)
        check(coordinate != null) { "Choose this address from search suggestions on the phone first. Save coordinates for bike favorites." }
        main.post { navigate(c, coordinate) }
    }
    fun navigate(c: Context, target: NavigationPlace) {
        context = c.applicationContext
        val origin = location
        if (origin == null || SystemClock.elapsedRealtimeNanos() - origin.elapsedRealtimeNanos > 30_000_000_000L) {
            pendingDestination = target
            status = "Waiting for GPS to calculate the route"; return
        }
        val generation = ++routeGeneration
        routeBusy = true; status = "Calculating route"
        worker.execute {
            val result = runCatching { NavigationApi.route(c, origin, target) }
            main.post {
                if (generation != routeGeneration) return@post
                routeBusy = false
                result.onSuccess {
                    route = it; progressIndex = 0; lastSpoken = ""; offRouteSamples = 0
                    status = "Navigating"; location?.let(::updateGuidance)
                    BikeDiagnostics.record(c, "Native route ready points=${it.points.size} instructions=${it.turns.size}")
                }.onFailure {
                    status = "Route unavailable. Check provider key and routing profile in Setup"
                    BikeDiagnostics.record(c, "Native route failed exception=${it.javaClass.simpleName}")
                }
            }
        }
    }
    @JvmStatic fun zoom(inside: Boolean) { main.post { zoomLevel = (zoomLevel + if (inside) 0.5 else -0.5).coerceIn(12.0, 19.0) } }

    private fun updateGuidance(fix: Location) {
        val r = route ?: run { guidance = "Choose a destination"; maneuver = 0; return }
        // Project onto nearby segments, then measure along the route rather than direct distance.
        var bestDistance = Double.MAX_VALUE
        var bestIndex = progressIndex
        var bestFraction = 0.0
        val start = (progressIndex - 3).coerceAtLeast(0)
        val end = (progressIndex + 120).coerceAtMost(r.points.size - 2)
        for (i in start..end) {
            val a = r.points[i]; val b = r.points[i+1]
            val scale = cos(Math.toRadians(fix.latitude))
            val ax = (a.longitude - fix.longitude) * scale * 111320
            val ay = (a.latitude - fix.latitude) * 111320
            val dx = (b.longitude - a.longitude) * scale * 111320
            val dy = (b.latitude - a.latitude) * 111320
            val fraction = if (dx*dx + dy*dy < 0.01) 0.0 else (-(ax*dx + ay*dy)/(dx*dx + dy*dy)).coerceIn(0.0,1.0)
            val distance = hypot(ax + dx*fraction, ay + dy*fraction)
            if (distance < bestDistance) { bestDistance = distance; bestIndex = i; bestFraction = fraction }
        }
        if (bestDistance > max(60.0, fix.accuracy * 2.0)) {
            offRouteSamples++
            guidance = "Off route. Recalculating"
            if (!routeBusy && offRouteSamples >= 3 && SystemClock.elapsedRealtime() - lastReroute > 30000) {
                lastReroute = SystemClock.elapsedRealtime(); context?.let { navigate(it, r.destination) }
            }
            return
        }
        offRouteSamples = 0; progressIndex = bestIndex
        val travelled = r.cumulative[bestIndex] + bestFraction*(r.cumulative[bestIndex+1]-r.cumulative[bestIndex])
        remaining = max(0.0, r.cumulative.last() - travelled)
        val next = r.turns.firstOrNull { it.start > bestIndex } ?: r.turns.lastOrNull()
        maneuver = next?.sign ?: 0
        val meters = if (next == null) remaining else max(0.0, r.cumulative[next.start] - travelled)
        guidance = if (remaining < 25 && bestIndex >= r.points.size - 4) "Arriving at destination" else "${distance(meters)}  ${next?.text ?: "Continue"}"
        val speechId = "${routeGeneration}:${next?.start}"
        if (meters < 200 && lastSpoken != speechId && speechReady && context?.let { RidePreferences.prefs(it).getBoolean("navigation_voice", true) } == true) {
            lastSpoken = speechId; speaker?.speak(guidance, TextToSpeech.QUEUE_FLUSH, null, speechId)
        }
    }
    private fun distance(meters: Double) = if (meters >= 1000) String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000) else "${(meters / 10).roundToInt() * 10} m"
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val c = context ?: return
            val fix = location
            val fresh = fix != null && SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos < 30_000_000_000L
            if (!fresh) { status = "GPS signal unavailable"; guidance = "Waiting for GPS" }
            if (inFlight && SystemClock.elapsedRealtime() - renderStarted > 15000) {
                snapshotter?.cancel(); snapshotter = null; inFlight = false
                BikeDiagnostics.record(c, "Native map render timeout")
            }
            if (fresh && !inFlight && SystemClock.elapsedRealtime() - frameAt > 750) render(c, fix!!)
            if (SystemClock.elapsedRealtime() - lastSummary > 10000) {
                lastSummary = SystemClock.elapsedRealtime()
                BikeDiagnostics.record(c, "Native map status gpsFresh=$fresh route=${route != null} renderBusy=$inFlight frameAgeMs=${if(frameAt==0L)-1 else lastSummary-frameAt}")
            }
            main.postDelayed(this, 500)
        }
    }
    private fun render(c: Context, fix: Location) {
        val key = NavigationSecrets.read(c, "maptiler")
        if (key.isBlank()) { status = "Add your map provider key in Setup"; return }
        inFlight = true; renderStarted = SystemClock.elapsedRealtime()
        try {
            val camera = CameraPosition.Builder().target(LatLng(fix.latitude, fix.longitude)).zoom(zoomLevel).bearing(if (fix.hasBearing() && fix.speed > 1f) fix.bearing.toDouble() else 0.0).build()
            val renderer = snapshotter ?: MapSnapshotter(c, MapSnapshotter.Options(480, height).withPixelRatio(1f)
                .withStyleBuilder(Style.Builder().fromUri("https://api.maptiler.com/maps/streets-v4/style.json?key=${android.net.Uri.encode(key)}"))
                .withCameraPosition(camera))
            snapshotter = renderer
            renderer.setCameraPosition(camera)
            renderer.start({ snapshot ->
                if (!running || snapshotter !== renderer) return@start
                val bitmap = snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.save(); canvas.clipRect(0, 34, 480, height - 20)
                route?.let { r ->
                    val path = Path()
                    r.points.drop((progressIndex-1).coerceAtLeast(0)).forEachIndexed { i, point ->
                        val pixel = snapshot.pixelForLatLng(point)
                        if (i==0) path.moveTo(pixel.x,pixel.y) else path.lineTo(pixel.x,pixel.y)
                    }
                    paint.color = Color.WHITE; paint.style=Paint.Style.STROKE; paint.strokeWidth=8f; canvas.drawPath(path,paint)
                    paint.color=0xff087eff.toInt(); paint.strokeWidth=5f; canvas.drawPath(path,paint)
                }
                val pixel = snapshot.pixelForLatLng(LatLng(fix.latitude,fix.longitude))
                paint.style=Paint.Style.FILL; paint.color=Color.WHITE; canvas.drawCircle(pixel.x,pixel.y,10f,paint)
                paint.color=0xff087eff.toInt(); canvas.drawCircle(pixel.x,pixel.y,7f,paint)
                canvas.restore()
                banner(canvas, paint, guidance, 0, 34, 19f)
                // Attribution remains visible on every streamed frame, including the small dashboard.
                paint.color=Color.WHITE; canvas.drawRect(0f,(height-20).toFloat(),480f,height.toFloat(),paint)
                val logo=c.getDrawable(R.drawable.maptiler_logo)!!
                logo.setBounds(3,height-20,70,height); logo.draw(canvas)
                paint.color=Color.BLACK; paint.textSize=11f; paint.typeface=Typeface.DEFAULT
                canvas.drawText("© MapTiler  © OpenStreetMap contributors",83f,height-6f,paint)
                mapBitmap=bitmap; mapJpeg=jpeg(bitmap); frameAt=SystemClock.elapsedRealtime(); inFlight=false
            }) { _ ->
                if (snapshotter === renderer) { inFlight=false; snapshotter=null; status="Map unavailable. Check provider key and internet connection"; BikeDiagnostics.record(c,"Native map render failed") }
            }
        } catch (e: Exception) { inFlight=false; snapshotter=null; status="Map rendering unavailable"; BikeDiagnostics.record(c,"Native map render exception=${e.javaClass.simpleName}") }
    }
    private fun banner(canvas: Canvas, paint: Paint, text: String, top: Int, bottom: Int, size: Float) {
        paint.style=Paint.Style.FILL; paint.color=0xff14251c.toInt(); canvas.drawRect(0f,top.toFloat(),480f,bottom.toFloat(),paint)
        paint.color=Color.WHITE; paint.textSize=size; paint.typeface=Typeface.DEFAULT_BOLD
        val shown=android.text.TextUtils.ellipsize(text,android.text.TextPaint(paint),466f,android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(shown,7f,top+(bottom-top)/2f-(paint.ascent()+paint.descent())/2,paint)
    }
    private fun jpeg(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,85,it); it.toByteArray() }
    @JvmStatic fun frame(mode: String): ByteArray? {
        if (mode == "map" && mapJpeg != null && SystemClock.elapsedRealtime()-frameAt < 10000) return mapJpeg
        val bitmap=Bitmap.createBitmap(480,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap); canvas.drawColor(0xff151c17.toInt())
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val c=context
        if (mode=="music" && c!=null) {
            val sessions=runCatching { c.getSystemService(MediaSessionManager::class.java).getActiveSessions(android.content.ComponentName(c,GX12NotificationListener::class.java)) }.getOrDefault(emptyList())
            val controller=sessions.firstOrNull { it.packageName==RidePreferences.selectedMusic(c) }
            banner(canvas,paint,"MUSIC",0,38,22f)
            paint.color=Color.WHITE; paint.textSize=25f; paint.typeface=Typeface.DEFAULT_BOLD
            val title=controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Open ${RidePreferences.musicName(c)} on phone"
            canvas.drawText(android.text.TextUtils.ellipsize(title,android.text.TextPaint(paint),450f,android.text.TextUtils.TruncateAt.END).toString(),15f,105f,paint)
            paint.textSize=19f
            val artist=controller?.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            canvas.drawText(android.text.TextUtils.ellipsize(artist,android.text.TextPaint(paint),450f,android.text.TextUtils.TruncateAt.END).toString(),15f,145f,paint)
        } else if(mode=="arrows" && route!=null) {
            banner(canvas,paint,guidance,0,40,20f)
            paint.color=0xffb5ff76.toInt(); paint.textSize=112f; paint.typeface=Typeface.DEFAULT_BOLD
            val arrow=when { maneuver==6 -> "↻"; maneuver==5 -> "●"; maneuver<0 -> "←"; maneuver in 1..3 || maneuver==7 -> "→"; else -> "↑" }
            canvas.drawText(arrow,205f,167f,paint)
            banner(canvas,paint,"${distance(remaining)} remaining",height-34,height,18f)
        } else {
            banner(canvas,paint,if(mode=="map") status else guidance,0,48,22f)
            paint.color=Color.WHITE; paint.textSize=20f
            canvas.drawText(if(running) "Choose a destination on your phone" else "Open navigation on your phone",18f,125f,paint)
        }
        return jpeg(bitmap)
    }
    fun phoneBitmap(mode: String): Bitmap? = if(mode=="map") {
        if(SystemClock.elapsedRealtime()-frameAt<10000) mapBitmap else frame("map")?.let { BitmapFactory.decodeByteArray(it,0,it.size) }
    } else frame(mode)?.let { BitmapFactory.decodeByteArray(it,0,it.size) }
}

class NativeNavigationService : Service(), LocationListener {
    private var locations: LocationManager? = null
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action=="STOP") { stopSelf(); return START_NOT_STICKY }
        if(NativeNavigation.running) return START_NOT_STICKY
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("navigation","RideDeck navigation",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,NativeNavigationService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"navigation").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("RideDeck navigation")
            .setContentText("GPS navigation active").setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
        if(Build.VERSION.SDK_INT>=29) startForeground(23,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(23,notification)
        try {
            locations=getSystemService(LocationManager::class.java)
            NativeNavigation.attach(this)
            locations!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1f,this,Looper.getMainLooper())
            locations!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { NativeNavigation.update(it) }
        } catch(e: Exception) { BikeDiagnostics.record(this,"Native GPS start failed exception=${e.javaClass.simpleName}"); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location: Location) { NativeNavigation.update(location) }
    override fun onProviderDisabled(provider: String) { NativeNavigation.status="Enable phone location to continue navigation" }
    override fun onDestroy() { locations?.removeUpdates(this); NativeNavigation.detach(); super.onDestroy() }
}
