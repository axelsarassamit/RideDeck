package app.pillion.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent

/** Two-finger gesture targeted only at the authenticated helper's virtual display. */
object DashTouch {
    fun tap(display: Int, x: Float, y: Float) {
        val cls = Class.forName("android.hardware.input.InputManager")
        val manager = cls.getMethod("getInstance").invoke(null)
        val inject = cls.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
        val setter = InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { setter.invoke(event, display); check(inject.invoke(manager, event, 2) == true) { "Map touch injection rejected" } }
            finally { event.recycle() }
        }
    }
    fun zoom(display: Int, width: Int, height: Int, zoomIn: Boolean) {
        val cls = Class.forName("android.hardware.input.InputManager")
        val manager = cls.getMethod("getInstance").invoke(null)
        val inject = cls.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
        val setter = InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        val props = Array(2) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val down = SystemClock.uptimeMillis()
        fun send(action: Int, separation: Float, pointers: Int) {
            val coords = Array(pointers) { i -> MotionEvent.PointerCoords().apply {
                x = width * .75f; y = height * .52f + if (i == 0) -separation else separation
                pressure = 1f; size = .1f
            } }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, pointers, props.copyOf(pointers), coords,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            try { setter.invoke(event, display); check(inject.invoke(manager, event, 2) == true) { "Map touch injection rejected" } }
            finally { event.recycle() }
        }
        val start = height * if (zoomIn) .08f else .22f
        val end = height * if (zoomIn) .22f else .08f
        send(MotionEvent.ACTION_DOWN, start, 1)
        try {
            send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), start, 2)
            for (i in 1..8) { send(MotionEvent.ACTION_MOVE, start + (end - start) * i / 8, 2); Thread.sleep(25) }
            send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), end, 2)
            send(MotionEvent.ACTION_UP, end, 1)
        } catch (error: Throwable) { runCatching { send(MotionEvent.ACTION_CANCEL, start, 2) }; throw error }
    }
}
