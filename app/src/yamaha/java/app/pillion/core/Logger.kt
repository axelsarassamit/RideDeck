package app.pillion.core

/** Local adapter for Pillion's protocol logger. No device identifiers are logged. */
object Logger {
    fun d(message: String) { /* Protocol details stay local and are not logged. */ }
}
