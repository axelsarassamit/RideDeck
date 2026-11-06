package com.axelsarassamit.gx12

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test

class RainLockTest {
    @Test fun shieldBlocksTouchesUntilUnlockIsHeld() {
        ActivityScenario.launch(RideFilesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var taps=0
                activity.setContentView(Button(activity).apply { setOnClickListener { taps++ } })
                RainLock.engage(activity)
                val root=activity.findViewById<FrameLayout>(android.R.id.content)
                val exact=View.MeasureSpec.EXACTLY
                root.measure(View.MeasureSpec.makeMeasureSpec(600,exact),View.MeasureSpec.makeMeasureSpec(800,exact))
                root.layout(0,0,600,800)
                val shield=root.findViewWithTag<FrameLayout>("ridedeck_rain_lock")
                val unlock=shield.getChildAt(0) as Button
                val now=SystemClock.uptimeMillis()
                for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                    val event=MotionEvent.obtain(now,now,action,300f,650f,0)
                    try { assertTrue(root.dispatchTouchEvent(event)) } finally { event.recycle() }
                }
                assertEquals(0,taps)
                unlock.performClick()
                assertTrue(RainLock.active(activity))
                assertTrue(unlock.performLongClick())
                assertFalse(RainLock.active(activity))
            }
        }
    }
}
