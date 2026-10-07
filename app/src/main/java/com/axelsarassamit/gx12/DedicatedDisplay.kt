package com.axelsarassamit.gx12
import android.content.Context
/** Compatibility facade for shared controls. No ADB or virtual display. */
object DedicatedDisplay {
    @JvmField var ready = true
    @JvmField var status = "Native navigation"
    @JvmStatic fun latestFrame(): ByteArray? = NativeNavigation.frame("map")
    fun receiving() = true
    @JvmStatic fun route(destination: String) { NativeNavigation.routeText(destination) }
    @JvmStatic fun zoom(inside: Boolean) { NativeNavigation.zoom(inside) }
    @JvmStatic fun stopRoute() { NativeNavigation.stopRoute() }
    @JvmStatic fun stop() { }
}
