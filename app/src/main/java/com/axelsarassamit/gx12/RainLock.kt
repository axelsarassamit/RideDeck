package com.axelsarassamit.gx12

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

/** Shields this activity's controls without pausing navigation, media or recording. */
object RainLock {
    private const val TAG = "ridedeck_rain_lock"
    private fun content(activity: Activity): FrameLayout = activity.findViewById(android.R.id.content)
    @JvmStatic fun active(activity: Activity) = content(activity).findViewWithTag<View>(TAG) != null

    @JvmStatic fun engage(activity: Activity) {
        val root=content(activity)
        if(active(activity))return
        fun dp(n:Int)=(n*activity.resources.displayMetrics.density).toInt()
        val children=(0 until root.childCount).map { root.getChildAt(it) }.associateWith { it.importantForAccessibility }
        children.keys.forEach { it.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS }
        val shield=FrameLayout(activity).apply { tag=TAG; isClickable=true;isFocusable=true }
        val unlock=Button(activity).apply {
            text="Rain lock on · Hold to unlock"; isAllCaps=false
            RideStyle.action(this,true)
            setOnLongClickListener {
                root.removeView(shield)
                children.forEach { (view,value)->view.importantForAccessibility=value }
                true
            }
        }
        shield.addView(unlock,FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            leftMargin=dp(12);rightMargin=dp(12);topMargin=dp(12)
        })
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(shield) { _,insets ->
            val safe=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            shield.setPadding(safe.left,safe.top,safe.right,safe.bottom);insets
        }
        root.addView(shield,FrameLayout.LayoutParams(-1,-1))
        androidx.core.view.ViewCompat.requestApplyInsets(shield)
        shield.announceForAccessibility("Rain lock on. Hold the unlock button to restore controls.")
    }
}
