package com.axelsarassamit.gx12
import android.app.Activity
import android.view.View
import android.widget.TextView
object NavigationUi {
    @JvmStatic fun configure(activity: Activity) { }
    @JvmStatic fun search(activity: Activity, text: String?) { }
    @JvmStatic fun panel(activity: Activity): View = TextView(activity)
}
