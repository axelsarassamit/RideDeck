package com.axelsarassamit.gx12

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.*
import app.pillion.core.*
import app.pillion.protocol.NaviLiteCodec
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue
import java.io.File

/** Screen frames stay in memory and travel only to the explicitly selected paired CCU. */
class YamahaCastService : Service() {
    companion object {
        @JvmField @Volatile var status = "Ready to connect to a compatible Yamaha navigation dash."
        @JvmField @Volatile var active = false
        @JvmField @Volatile var sessionId = ""
        @JvmField @Volatile var automaticFallbackPending = false
        @JvmField @Volatile var autoReconnectPaused = false
        const val STOP = "ridebridge.STOP_CAST"
    }
    private val main = Handler(Looper.getMainLooper())
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private val lock = Any()
    @Volatile private var running = false
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var deadline = 0L
    private var worker: Thread? = null
    private var dedicated = false
    @Volatile private var automatic = false
    private var waitingForFrameSince = 0L
    @Volatile private var imageRequested = true
    @Volatile private var receiverFailure: String? = null
    private var receiver: Thread? = null
    private val ackQueue = ArrayBlockingQueue<Boolean>(1)

    private fun diagnostic(message: String) {
        BikeDiagnostics.record(this, message)
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { autoReconnectPaused = true; automatic = false; automaticFallbackPending = false; stopSelf(); return START_NOT_STICKY }
        if (running || active) return START_NOT_STICKY
        autoReconnectPaused = false
        sessionId = intent?.getStringExtra("session") ?: "manual"
        automatic = intent?.getBooleanExtra("automatic", false) == true
        automaticFallbackPending = false
        val address = intent?.getStringExtra("device")
        dedicated = true
        if (address == null || (!automatic && !DedicatedDisplay.ready)) {
            status = "Prepare bike-only maps in Setup first. Phone mirroring is disabled."
            automaticFallbackPending = automatic
            stopSelf(); return START_NOT_STICKY
        }
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("yamaha_cast", "Yamaha dash casting", NotificationManager.IMPORTANCE_LOW))
            val stop = PendingIntent.getService(this, 1, Intent(this, YamahaCastService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
            val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val notice = Notification.Builder(this, "yamaha_cast")
                .setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("RideDeck bike-only navigation")
                .setContentText("Yamaha dash • tap Stop to end sharing")
                .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(22, notice,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            else startForeground(22, notice)
            running = true; active = true
            diagnostic("Session started automatic=$automatic map=${RidePreferences.selectedMap(this)} density=${RidePreferences.bikeMapDensity(this)} Android=${Build.VERSION.SDK_INT}")
            status = "Connecting to the selected Yamaha dash…"
            deadline = SystemClock.elapsedRealtime() + 20000
            watchdog.scheduleWithFixedDelay({
                if (running && SystemClock.elapsedRealtime() > deadline) {
                    diagnostic("Watchdog timeout receiving=${DedicatedDisplay.receiving()} imageRequested=$imageRequested")
                    status = "Dash connection timed out. Close StreetCross or another casting app, then try again."
                    automaticFallbackPending = automatic
                    running = false
                    try { socket?.close() } catch (_: Exception) { }
                    main.post { stopSelf() }
                }
            }, 1, 1, TimeUnit.SECONDS)
            worker = Thread({ cast(address) }, "RideDeckBluetooth").also { it.start() }
        } catch (_: Exception) {
            status = "Could not start sharing. Check nearby-device access and try again."
            automaticFallbackPending = automatic
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
                    diagnostic("Bluetooth connect begin")
                    next.connect()
                    diagnostic("Bluetooth socket connected")
                }
                override fun read(buffer: ByteArray) = socket!!.inputStream.read(buffer)
                override fun write(bytes: ByteArray) { synchronized(lock) { socket!!.outputStream.write(bytes) } }
                override fun close() { socket?.close() }
            }
            status = "Checking separate map display before connecting to bike..."
            deadline = SystemClock.elapsedRealtime() + 30000
            if (!DedicatedDisplay.ready) DedicatedDisplay.reconnect(this)
            DedicatedDisplay.preflight(this)
            check(running) { "Stopped" }
            diagnostic("Separate display check passed")
            deadline = SystemClock.elapsedRealtime() + 20000
            link.open()
            val frames = FrameReader(link)
            diagnostic("Handshake begin")
            val size = Handshake(link, frames).perform()
            diagnostic("Authenticated; display ${size.width}x${size.height}")
            check(size.width == 480 && size.height in listOf(234, 240)) { "Unsupported NaviLite display size" }
            status = "Bike connected. Starting selected map on the bike display..."
            deadline = SystemClock.elapsedRealtime() + 30000
            DedicatedDisplay.start(this, size.width, size.height)
            receiver = Thread({
                try {
                    while (running) {
                        val incoming = frames.next()
                        if (incoming.serviceType == 80) { ackQueue.offer(true); continue }
                        val action = DashContentCommand.classify(incoming)
                        if (incoming.serviceType != 65) diagnostic("RX type=${incoming.frameType} service=${incoming.serviceType} dataType=${incoming.payloadDataType} bytes=${incoming.payload.size} action=$action")
                        when (action) {
                            DashContentCommand.Action.START -> {
                                imageRequested = true
                                deadline = SystemClock.elapsedRealtime() + 30000
                                link.write(NaviLiteCodec.build(6, 2, 0, byteArrayOf(1, 0)))
                                link.write(NaviLiteCodec.build(6, 12, 0, byteArrayOf(1, 0)))
                                status = "Bike requested navigation images."
                            }
                            DashContentCommand.Action.STOP -> {
                                imageRequested = false
                                status = "Bike paused navigation images. Open navigation on the bike to resume."
                            }
                            DashContentCommand.Action.IGNORE -> Unit
                        }
                    }
                } catch (e: Exception) {
                    if (running) { diagnostic("Receiver failed exception=${e.javaClass.simpleName} message=${e.message}"); receiverFailure = e.message ?: "Bike Bluetooth receiver disconnected" }
                }
            }, "RideDeckDashCommands").also { it.isDaemon = true; it.start() }
            var lastSummary = SystemClock.elapsedRealtime()
            var sentFrames = 0
            var sequence = 1
            var count = 0
            var started = SystemClock.elapsedRealtime()
            while (running) {
                receiverFailure?.let { error(it) }
                deadline = SystemClock.elapsedRealtime() + 15000
                if (!imageRequested) { Thread.sleep(100); continue }
                val sourceJpeg = DedicatedDisplay.latestFrame()
                if (sourceJpeg == null) {
                    check(DedicatedDisplay.receiving()) { DedicatedDisplay.status }
                    if (waitingForFrameSince == 0L) waitingForFrameSince = SystemClock.elapsedRealtime()
                    check(SystemClock.elapsedRealtime() - waitingForFrameSince < 30000) {
                        "No map frames. Reconnect bike display in Setup. ${if (dedicated) DedicatedDisplay.status else ""}"
                    }
                    Thread.sleep(100); continue
                }
                waitingForFrameSince = 0L
                val jpeg = sourceJpeg
                val payload = byteArrayOf(3, sequence.toByte(), (sequence ushr 8).toByte()) + jpeg
                ackQueue.clear()
                link.write(NaviLiteCodec.build(6, 0, 1, payload))
                while (running && imageRequested && ackQueue.poll(100, TimeUnit.MILLISECONDS) == null) {
                    receiverFailure?.let { error(it) }
                }
                sequence = (sequence + 1) and 65535; count++; sentFrames++
                if (SystemClock.elapsedRealtime() - lastSummary >= 10000) {
                    diagnostic("Stream frames=$sentFrames latestJpegBytes=${jpeg.size} imageRequested=$imageRequested")
                    lastSummary = SystemClock.elapsedRealtime()
                }
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
            diagnostic(status)
            if (running) automaticFallbackPending = automatic
            running = false
            main.post { stopSelf() }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) { automatic = false; automaticFallbackPending = false; stopSelf() }

    override fun onDestroy() {
        running = false; active = false
        if (status.startsWith("Bike-only map") || status.startsWith("Connecting")) status = "Casting stopped."
        watchdog.shutdownNow()
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        worker?.interrupt()
        receiver?.interrupt()
        if (dedicated) DedicatedDisplay.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
