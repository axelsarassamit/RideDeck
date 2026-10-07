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
        val port = savedPort(context)
        val connected = adb.isConnected ||
            (port in 1..65535 && runCatching { adb.connectDevice("127.0.0.1", port) }.getOrDefault(false)) ||
            runCatching { adb.autoConnectDevice(context, 5000) }.getOrDefault(false)
        check(connected) { "Could not reconnect. Enable Wireless debugging and enter its current connection port." }
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
        val output = PillionAdb.getInstance(context).runShell("CLASSPATH='$apk' app_process / app.pillion.server.DashServer PROBE 2>&1")
        check(output.lineSequence().any { it.trim() == "RIDEDECK_DISPLAY_READY" }) {
            "Separate display check failed: " + output.takeLast(3500).ifBlank { "No helper output. Reconnect Wireless debugging in Setup." }
        }
    }

    fun start(context: Context, width: Int, height: Int) {
        check(!reading) { "A map display session is already running" }
        check(ready) { "Prepare the bike display in Setup first" }
        val session = token ?: error("No display session")
        val spec = BikeMapRenderSpec(width, height, RidePreferences.bikeMapDensity(context))
        val apk = context.applicationInfo.sourceDir.replace("'", "'\\''")
        val component = context.packageManager.getLaunchIntentForPackage(RidePreferences.selectedMap(context))?.component?.flattenToString()
            ?: error("Install the selected navigation/rider app first")
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
                    synchronized(this) {
                        if (token == session && reading) { jpeg = next; receivedAt = SystemClock.elapsedRealtime() }
                    }
                    status = "Separate map frames received."
                }
            } catch (e: Exception) {
                if (reading && token == session) status = "Bike display stopped: ${e.message ?: "connection lost"}"
            } finally {
                synchronized(this) {
                    if (token == session) { reading = false; ready = false; jpeg = null }
                }
                runCatching { ownedSocket?.close() }
            }
        }, "RideDeckDisplay").apply { isDaemon = true; start() }
    }

    fun latestFrame(): ByteArray? = if (reading && SystemClock.elapsedRealtime() - receivedAt < 1500) jpeg else null

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
