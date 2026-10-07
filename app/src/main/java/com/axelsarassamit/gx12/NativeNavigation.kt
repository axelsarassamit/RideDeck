package com.axelsarassamit.gx12
import android.content.Context
object NativeNavigation {
    @JvmStatic fun start(context: Context) { }
    @JvmStatic fun frame(mode: String): ByteArray? = null
    @JvmStatic fun routeText(text: String) { }
    @JvmStatic fun zoom(inside: Boolean) { }
    @JvmStatic fun stopRoute() { }
}
