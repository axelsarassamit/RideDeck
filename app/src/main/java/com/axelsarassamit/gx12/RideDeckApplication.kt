package com.axelsarassamit.gx12

import android.app.Application

class RideDeckApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Exception messages can contain addresses or message text. Store types and code frames only.
            BikeDiagnostics.record(this, "App crash thread=${thread.name} exception=${error.javaClass.simpleName} cause=${error.cause?.javaClass?.simpleName}")
            error.stackTrace.take(16).forEach { frame -> BikeDiagnostics.record(this, "Crash frame $frame") }
            if (previous != null) previous.uncaughtException(thread, error)
            else { android.os.Process.killProcess(android.os.Process.myPid()); kotlin.system.exitProcess(10) }
        }
        BikeDiagnostics.record(this, "App process started version=${packageManager.getPackageInfo(packageName, 0).versionName} Android=${android.os.Build.VERSION.SDK_INT}")
    }
}
