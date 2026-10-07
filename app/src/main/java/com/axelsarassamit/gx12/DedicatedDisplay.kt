package com.axelsarassamit.gx12

import android.content.Context
import android.os.SystemClock
import app.pillion.android.PillionAdb
import app.pillion.server.DashServer
import java.net.Socket
import java.net.InetSocketAddress
import java.io.DataInputStream
import java.security.SecureRandom

/** Only connects to the phone's own debugging endpoint. Never enables legacy TCP debugging. */
object DedicatedDisplay {
    @JvmField @Volatile var status = "Bike-only map needs one-time Wireless debugging setup."
    @JvmField @Volatile var ready = false
    @Volatile private var token: String? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var jpeg: ByteArray? = null
    @Volatile private var receivedAt = 0L
    @Volatile private var reading = false

    @JvmStatic fun savedPort(context: Context): Int =
        context.getSharedPreferences("bike_display", Context.MODE_PRIVATE).getInt("connection_port", 0)
    fun receiving(): Boolean = reading

    @JvmStatic fun reconnect(context: Context) {
        val adb = PillionAdb.getInstance(context)
        var connected = adb.isConnected
        if (!connected) {
            status = "Finding this phone's current display connection port..."
            val discovered = runCatching { LocalAdbDiscovery.ports(context) }.getOrDefault(emptyList())
            BikeDiagnostics.record(context, "ADB discovery candidates=${discovered.size} savedPortPresent=${savedPort(context) != 0}")
            val ports = discovered + savedPort(context)
            adb.setTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            try {
                for (port in ports.distinct().filter { it in 1..65535 }.take(4)) {
                    if (runCatching { adb.connectDevice("127.0.0.1", port) }.getOrDefault(false)) {
                        context.getSharedPreferences("bike_display", Context.MODE_PRIVATE).edit().putInt("connection_port", port).apply()
                        connected = true
                        BikeDiagnostics.record(context, "ADB connected source=${if (port in discovered) "discovered" else "saved"}")
                        break
                    }
                }
            } finally { adb.setTimeout(20, java.util.concurrent.TimeUnit.SECONDS) }
        }
        check(connected) { "Could not find this phone's debugging connection. Keep Wi-Fi and Wireless debugging enabled. A manual connection port is available in Advanced display options." }
        prepareSession(context)
    }

    @JvmStatic fun pair(context: Context, pairingPort: Int, code: String): Boolean {
        require(pairingPort in 1..65535 && code.matches(Regex("[0-9]{6}"))) { "Enter the pairing port and six-digit code" }
        PillionAdb.getInstance(context).pairDevice("127.0.0.1", pairingPort, code)
        status = "Paired. Enter the connection port from the main Wireless debugging screen."
        return true
    }

    @JvmStatic fun prepare(context: Context, port: Int) {
        require(port in 1..65535)
        if (reading) error("Stop casting first")
        val adb = PillionAdb.getInstance(context)
        check(adb.connectDevice("127.0.0.1", port)) { "Connection failed. Keep Wireless debugging enabled and check its connection port." }
        context.getSharedPreferences("bike_display", Context.MODE_PRIVATE).edit().putInt("connection_port", port).apply()
        prepareSession(context)
    }

    @Synchronized private fun prepareSession(context: Context) {
        check(!reading) { "Stop casting first" }
        val pkg = RidePreferences.selectedMap(context)
        require(pkg in RidePreferences.MAP_PACKAGES) { "Unsupported display app" }
        val session = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it.toInt() and 255) }
        token = session; ready = true
        status = "Bike-only access prepared. Connection settings saved. Select your Yamaha to start."
    }

    fun preflight(context: Context) {
        val apk = context.applicationInfo.sourceDir.replace("'", "'\\''")
        val output = PillionAdb.getInstance(context).runShell("CLASSPATH='$apk' app_process / app.pillion.server.DashServer PROBE ${RidePreferences.bikeMapDensity(context)} 2>&1")
        check(output.lineSequence().any { it.trim() == "RIDEDECK_DISPLAY_READY" }) {
            "Separate display check failed: " + output.takeLast(3500).ifBlank { "No helper output. Reconnect Wireless debugging in Setup." }
        }
    }

    fun start(context: Context, width: Int, height: Int) {
        check(!reading) { "A map display session is already running" }
        check(ready) { "Prepare the bike display in Setup first" }
        val session = token ?: error("No display session")
        val spec = BikeMapRenderSpec(width, height, RidePreferences.bikeMapDensity(context))
        BikeDiagnostics.record(context, "Helper launch render=${spec.renderWidth}x${spec.renderHeight} density=${spec.densityDpi} output=${width}x${height}")
        val apk = context.applicationInfo.sourceDir.replace("'", "'\\''")
        val component = context.packageManager.getLaunchIntentForPackage(RidePreferences.selectedMap(context))?.component?.flattenToString()
            ?: error("Install the selected navigation/rider app first")
        // A stopped helper can still be closing Maps. Wait for its process to release the
        // listening socket before starting a replacement with a different session token.
        val helperDeadline = SystemClock.elapsedRealtime() + 6000
        while (true) {
            val occupied = runCatching {
                Socket().use { probe -> probe.connect(InetSocketAddress("127.0.0.1", DashServer.PORT), 200) }
                true
            }.getOrDefault(false)
            if (!occupied) break
            check(SystemClock.elapsedRealtime() < helperDeadline) { "Previous map helper is still stopping. Wait a moment and retry." }
            Thread.sleep(200)
        }
        val launchOutput = PillionAdb.getInstance(context).runShell("CLASSPATH='$apk' nohup app_process / app.pillion.server.DashServer ${spec.renderWidth} ${spec.renderHeight} ${spec.densityDpi} 45 $width $height $session ${RidePreferences.selectedMap(context)} >/data/local/tmp/ridebridge-dash.log 2>&1 </dev/null &")
        synchronized(this) {
            check(ready && token == session) { "Map display startup was cancelled" }
            jpeg = null; receivedAt = 0L; reading = true
        }
        Thread({
            var ownedSocket: Socket? = null
            try {
                var connected: Socket? = null
                repeat(20) {
                    if (connected == null && reading && token == session) {
                        val candidate = Socket()
                        ownedSocket = candidate
                        synchronized(this) {
                            check(token == session && reading) { "Map display startup was cancelled" }
                            socket = candidate
                        }
                        try { candidate.connect(InetSocketAddress("127.0.0.1", DashServer.PORT), 500); connected = candidate }
                        catch (_: Exception) { candidate.close(); Thread.sleep(250) }
                    }
                }
                val link = connected ?: run {
                    val log = runCatching { PillionAdb.getInstance(context).runShell("tail -c 3500 /data/local/tmp/ridebridge-dash.log 2>&1") }
                        .getOrDefault("Could not read helper startup log. Reconnect debugging access.")
                    error("Display helper startup failed. $launchOutput $log")
                }
                BikeDiagnostics.record(context, "Helper frame socket connected")
                link.soTimeout = 30000
                link.getOutputStream().apply { write("$session\nPROMOTE $component\n".toByteArray()); flush() }
                val input = DataInputStream(link.getInputStream().buffered())
                while (reading && token == session) {
                    val size = input.readInt()
                    if (size in -4096..-1) {
                        val message = ByteArray(-size); input.readFully(message)
                        error(String(message, Charsets.UTF_8))
                    }
                    check(size in 4..1048576) { "Invalid display image" }
                    val next = ByteArray(size); input.readFully(next)
                    if (receivedAt == 0L) BikeDiagnostics.record(context, "Helper first frame bytes=$size")
                    synchronized(this) {
                        if (token == session && reading) { jpeg = next; receivedAt = SystemClock.elapsedRealtime() }
                    }
                    status = "Separate map frames received."
                }
            } catch (e: Exception) {
                if (reading && token == session) BikeDiagnostics.record(context, "Helper failure exception=${e.javaClass.simpleName} message=${e.message}")
                if (reading && token == session) status = "Bike display stopped: ${e.message ?: "connection lost"}"
            } finally {
                synchronized(this) {
                    if (token == session) { reading = false; ready = false; jpeg = null }
                }
                runCatching { ownedSocket?.close() }
            }
        }, "RideDeckDisplay").apply { isDaemon = true; start() }
    }

    @JvmStatic fun latestFrame(): ByteArray? = if (reading && SystemClock.elapsedRealtime() - receivedAt < 1500) jpeg else null

    @JvmStatic fun tap(x: Float, y: Float) {
        require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f)
        check(reading && socket?.isConnected == true) { "Map display is not ready" }
        synchronized(this) { socket!!.getOutputStream().apply { write("TAP $x $y\n".toByteArray()); flush() } }
    }
    fun stopRoute() {
        check(reading && socket?.isConnected == true) { "Map display is not ready" }
        synchronized(this) { socket!!.getOutputStream().apply { write("STOP_ROUTE\n".toByteArray()); flush() } }
    }
    fun zoom(zoomIn: Boolean) {
        check(reading && socket?.isConnected == true) { "Map display is not ready" }
        synchronized(this) { socket!!.getOutputStream().apply { write((if (zoomIn) "ZOOM_IN\n" else "ZOOM_OUT\n").toByteArray()); flush() } }
    }
    @JvmStatic fun route(destination: String) {
        check(reading) { "Start bike-only casting first" }
        val uri = "google.navigation:q=" + android.net.Uri.encode(destination) + "&mode=d"
        synchronized(this) { socket!!.getOutputStream().apply { write("ROUTE $uri\n".toByteArray()); flush() } }
    }

    @JvmStatic @Synchronized fun stop() {
        token = null
        reading = false; ready = false; jpeg = null
        runCatching { socket?.getOutputStream()?.apply { write("QUIT\n".toByteArray()); flush() } }
        runCatching { socket?.close() }; socket = null
    }
}
