package com.axelsarassamit.gx12

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import app.pillion.core.*
import app.pillion.protocol.NaviLiteCodec
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Screen frames stay in memory and travel only to the explicitly selected paired CCU. */
class YamahaCastService : Service() {
    companion object {
        @JvmField @Volatile var status = "Ready to connect to a compatible Yamaha navigation dash."
        @JvmField @Volatile var active = false
        const val STOP = "ridebridge.STOP_CAST"
    }
    private val main = Handler(Looper.getMainLooper())
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private val lock = Any()
    @Volatile private var running = false
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var deadline = 0L
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var frame: Bitmap? = null
    private var worker: Thread? = null
    private var dedicated = false
    private var waitingForFrameSince = 0L

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (running) return START_NOT_STICKY
        val token = intent?.getParcelableExtra<Intent>("capture")
        val address = intent?.getStringExtra("device")
        dedicated = intent?.getBooleanExtra("dedicated", false) == true
        if (address == null || (!dedicated && (token == null || intent.getIntExtra("result", 0) != Activity.RESULT_OK))) {
            status = "Screen sharing was not approved. Tap Cast to try again."
            stopSelf(); return START_NOT_STICKY
        }
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("yamaha_cast", "Yamaha dash casting", NotificationManager.IMPORTANCE_LOW))
            val stop = PendingIntent.getService(this, 1, Intent(this, YamahaCastService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
            val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val notice = Notification.Builder(this, "yamaha_cast")
                .setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle(if (dedicated) "RideDeck map is on your bike" else "RideDeck is sharing your screen")
                .setContentText("Yamaha dash • tap Stop to end sharing")
                .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(22, notice,
                if (dedicated) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                else android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(22, notice)
            running = true; active = true
            status = "Connecting to the selected Yamaha dash…"
            if (dedicated) {
                DedicatedDisplay.start(this)
            } else {
            captureThread = HandlerThread("RideDeckCapture").also { it.start() }
            val captureHandler = Handler(captureThread!!.looper)
            projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(Activity.RESULT_OK, token!!)
            projection!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    if (running) { status = "Screen sharing ended. Tap Cast to start a new session."; stopSelf() }
                }
            }, main)
            // Android 12L+ fits the shared app without stretching into this fixed dash viewport.
            reader = ImageReader.newInstance(480, 234, PixelFormat.RGBA_8888, 2)
            reader!!.setOnImageAvailableListener({ source ->
                val image = try { source.acquireLatestImage() } catch (_: IllegalStateException) { null }
                if (image != null) {
                    try {
                        val plane = image.planes[0]
                        val strideWidth = plane.rowStride / plane.pixelStride
                        val padded = Bitmap.createBitmap(strideWidth, image.height, Bitmap.Config.ARGB_8888)
                        try {
                            padded.copyPixelsFromBuffer(plane.buffer)
                            val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
                            synchronized(lock) {
                                if (running) { frame?.recycle(); frame = cropped }
                                else cropped.recycle()
                            }
                            if (cropped !== padded) padded.recycle()
                        } catch (e: Exception) { if (!padded.isRecycled) padded.recycle(); throw e }
                    } catch (_: Exception) { /* Skip an incomplete captured image. */ }
                    finally { image.close() }
                }
            }, captureHandler)
            display = projection!!.createVirtualDisplay("RideDeck Yamaha", 480, 234,
                resources.configuration.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface, null, captureHandler)
            }
            deadline = SystemClock.elapsedRealtime() + 20000
            watchdog.scheduleWithFixedDelay({
                if (running && SystemClock.elapsedRealtime() > deadline) {
                    status = "Dash connection timed out. Close StreetCross or another casting app, then try again."
                    running = false
                    try { socket?.close() } catch (_: Exception) { }
                    main.post { stopSelf() }
                }
            }, 1, 1, TimeUnit.SECONDS)
            worker = Thread({ cast(address) }, "RideDeckBluetooth").also { it.start() }
        } catch (_: Exception) {
            status = "Could not start sharing. Check nearby-device access and try again."
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun cast(address: String) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                error("Nearby-device permission was revoked")
            }
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Bluetooth is unavailable")
            check(adapter.isEnabled) { "Turn on Bluetooth before casting" }
            val device = adapter.bondedDevices.firstOrNull { it.address == address }
                ?: error("Selected Yamaha is no longer paired")
            val link = object : ByteChannel {
                override fun open() {
                    check(running)
                    val next = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00007220-0000-1000-8000-00805f9b34fb"))
                    // Assign before blocking connect, so Stop and the timeout can always close it.
                    synchronized(lock) { if (!running) { next.close(); error("Stopped") }; socket = next }
                    next.connect()
                }
                override fun read(buffer: ByteArray) = socket!!.inputStream.read(buffer)
                override fun write(bytes: ByteArray) { socket!!.outputStream.write(bytes) }
                override fun close() { socket?.close() }
            }
            link.open()
            val frames = FrameReader(link)
            val size = Handshake(link, frames).perform()
            check(size.width == 480 && size.height in listOf(234, 240)) { "Unsupported NaviLite display size" }
            var sequence = 1
            var count = 0
            var started = SystemClock.elapsedRealtime()
            while (running) {
                deadline = SystemClock.elapsedRealtime() + 15000
                val sourceJpeg = if (dedicated) DedicatedDisplay.latestFrame() else synchronized(lock) {
                    frame?.let { bitmap -> ByteArrayOutputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 45, output); output.toByteArray()
                    } }
                }
                if (sourceJpeg == null) {
                    if (waitingForFrameSince == 0L) waitingForFrameSince = SystemClock.elapsedRealtime()
                    check(SystemClock.elapsedRealtime() - waitingForFrameSince < 10000) {
                        "No map frames. Reconnect bike display in Setup. ${if (dedicated) DedicatedDisplay.status else ""}"
                    }
                    Thread.sleep(100); continue
                }
                waitingForFrameSince = 0L
                val jpeg = if (size.height == 234) sourceJpeg else {
                    val source = android.graphics.BitmapFactory.decodeByteArray(sourceJpeg, 0, sourceJpeg.size)
                        ?: error("Invalid map image")
                    val resized = Bitmap.createScaledBitmap(source, size.width, size.height, true)
                    try { ByteArrayOutputStream().use { output -> resized.compress(Bitmap.CompressFormat.JPEG, 45, output); output.toByteArray() } }
                    finally { if (resized !== source) resized.recycle(); source.recycle() }
                }
                val payload = byteArrayOf(3, sequence.toByte(), (sequence ushr 8).toByte()) + jpeg
                link.write(NaviLiteCodec.build(6, 0, 1, payload))
                do { val ack = frames.next(); if (ack.serviceType == 80) break } while (running)
                sequence = (sequence + 1) and 65535; count++
                val now = SystemClock.elapsedRealtime()
                if (now - started >= 1000) {
                    status = "Casting • ${size.width} × ${size.height} • $count frames/s"
                    count = 0; started = now
                }
                Thread.sleep(100) // At most 10 frames/s, with dash acknowledgement for every frame.
            }
        } catch (e: SecurityException) {
            if (running) status = "Nearby-device permission was denied. Allow access and tap Cast again."
        } catch (e: Exception) {
            if (running) status = "Casting stopped: " + (e.message ?: "Bluetooth connection lost")
        } finally {
            running = false
            main.post { stopSelf() }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }

    override fun onDestroy() {
        running = false; active = false
        if (status.startsWith("Casting to") || status.startsWith("Connecting")) status = "Casting stopped."
        watchdog.shutdownNow()
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        worker?.interrupt()
        if (dedicated) DedicatedDisplay.stop()
        display?.release(); display = null
        reader?.setOnImageAvailableListener(null, null); reader?.close(); reader = null
        projection?.stop(); projection = null
        captureThread?.quitSafely()
        synchronized(lock) { frame?.recycle(); frame = null }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
