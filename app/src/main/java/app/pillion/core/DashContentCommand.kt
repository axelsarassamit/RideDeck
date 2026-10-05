package app.pillion.core

/** Only accepts the documented navigation-image request. Unknown commands have no side effects. */
object DashContentCommand {
    enum class Action { START, STOP, IGNORE }
    fun classify(frame: NaviFrameView): Action {
        if (frame.frameType != 1 || frame.payloadDataType != 0) return Action.IGNORE
        if (frame.payload.size !in 1..2 || frame.payload[0].toInt() != 1) return Action.IGNORE
        if (frame.payload.size == 2 && frame.payload[1].toInt() != 0) return Action.IGNORE
        return when (frame.serviceType) {
            55 -> Action.START
            56 -> Action.STOP
            else -> Action.IGNORE
        }
    }
}
