package com.axelsarassamit.gx12

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Discover advertised ports, but authenticate only against this phone's loopback endpoint. */
object LocalAdbDiscovery {
    fun ports(context: Context): List<Int> {
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val ports = LinkedBlockingQueue<Int>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) {}
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                runCatching { manager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, code: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        if (info.port in 1..65535) ports.offer(info.port)
                    }
                }) }
            }
        }
        try {
            manager.discoverServices("_adb-tls-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            val result = mutableListOf<Int>()
            while (true) {
                val remaining = deadline - android.os.SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                ports.poll(remaining, TimeUnit.MILLISECONDS)?.let { result.add(it) }
            }
            return result.distinct().take(3)
        } finally { runCatching { manager.stopServiceDiscovery(listener) } }
    }
}
