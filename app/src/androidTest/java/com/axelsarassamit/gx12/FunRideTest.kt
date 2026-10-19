package com.axelsarassamit.gx12

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class FunRideTest {
    @Test fun questionnaireRetainsChosenStartAndStopsAcrossRecreation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("fun_ride",Context.MODE_PRIVATE)
        val before=prefs.getString("draft",null)
        try {
            val draft="""{"start":[13.5,100.5],"finish":[13.6,100.6],"stops":[[13.51,100.51],[13.52,100.52]],"minutes":90,"bearing":90,"motorway":true,"toll":false,"ferry":true,"dirt":true,"fewer":true}"""
            prefs.edit().putString("draft",draft).commit()
            ActivityScenario.launch(FunRideActivity::class.java).use { scenario ->
                scenario.recreate()
                scenario.onActivity { activity ->
                    val method=FunRideActivity::class.java.getDeclaredMethod("snapshot").apply{isAccessible=true}
                    val saved=method.invoke(activity) as org.json.JSONObject
                    assertEquals(90,saved.getInt("minutes"));assertEquals(2,saved.getJSONArray("stops").length())
                    assertEquals(13.5,saved.getJSONArray("start").getDouble(0),.00001)
                    assertFalse(saved.getBoolean("toll"));assertTrue(saved.getBoolean("ferry"))
                }
            }
        } finally {if(before==null)prefs.edit().remove("draft").commit()else prefs.edit().putString("draft",before).commit()}
    }
    @Test fun routeSnapshotKeepsFullGeometryAndOrderedWaypoints() {
        val p=app.pillion.core.FunPoint(13.5,100.5);val a=app.pillion.core.FunPoint(13.6,100.6);val b=app.pillion.core.FunPoint(13.7,100.7)
        val route=FunRoute(p,p,listOf(a,b),listOf(p,a,b,p),3600.0,30.0,2)
        assertEquals(route,FunRoute.read(route.json()))
        assertTrue(runCatching{FunRoute.read(route.json().put("stops",org.json.JSONArray()))}.isFailure)
        assertTrue(runCatching{FunRoute.read(route.json().put("points",org.json.JSONArray()))}.isFailure)
        assertTrue(runCatching{FunRoute.read(route.json().put("seconds","NaN"))}.isFailure)
        assertTrue(runCatching{FunRoute.read(route.json().put("km",-1))}.isFailure)
    }
    @Test fun selectedRouteAndFullMapSurviveRecreation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=java.io.File(context.filesDir,"fun-ride-options.json");val previous=if(file.exists())file.readBytes()else null
        val p=app.pillion.core.FunPoint(13.75,100.5);val a=app.pillion.core.FunPoint(13.77,100.52);val b=app.pillion.core.FunPoint(13.79,100.49)
        val route=FunRoute(p,p,listOf(a,b),listOf(p,a,b,p),3600.0,30.0,2)
        try {
            file.writeText(org.json.JSONArray(listOf(route.json())).toString())
            ActivityScenario.launch(FunRideActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    FunRideActivity::class.java.getDeclaredField("routes").apply{isAccessible=true}.set(activity,listOf(route))
                    FunRideActivity::class.java.getDeclaredMethod("overview").apply{isAccessible=true}.invoke(activity)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val restored=FunRideActivity::class.java.getDeclaredField("routes").apply{isAccessible=true}.get(activity) as List<*>
                    assertEquals(listOf(route),restored)
                    FunRideActivity::class.java.getDeclaredMethod("expandedMap").apply{isAccessible=true}.invoke(activity)
                    val dialog=FunRideActivity::class.java.getDeclaredField("mapDialog").apply{isAccessible=true}.get(activity) as android.app.Dialog
                    assertTrue(dialog.isShowing)
                }
                scenario.recreate() // The expanded map must dismiss without leaking its activity window.
                scenario.onActivity { activity ->assertFalse(activity.isFinishing)}
            }
        } finally {if(previous==null)file.delete()else file.writeBytes(previous)}
    }
}
