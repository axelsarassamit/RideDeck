package com.axelsarassamit.gx12

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

object NavigationUi {
    private val worker=Executors.newFixedThreadPool(2)
    private val main=Handler(Looper.getMainLooper())
    private val names=arrayOf("Map", "Turn arrows", "Music")
    private val modes=arrayOf("map","arrows","music")
    @JvmStatic fun configure(activity: Activity) {
        AlertDialog.Builder(activity).setTitle("Navigation and panels").setItems(arrayOf("Map and routing accounts", "Phone panel", "Bike display", "Precise location", "Voice guidance", "Stop navigation")) { _, choice ->
            when(choice) {
                0 -> credentials(activity)
                1,2 -> {
                    val key=if(choice==1)"phone_panel" else "dash_panel"
                    val selected=modes.indexOf(RidePreferences.prefs(activity).getString(key,"map")).coerceAtLeast(0)
                    AlertDialog.Builder(activity).setTitle(if(choice==1)"Phone panel" else "Bike display")
                        .setSingleChoiceItems(names,selected) { dialog,index ->
                            RidePreferences.prefs(activity).edit().putString(key,modes[index]).apply(); dialog.dismiss()
                            Toast.makeText(activity,if(choice==1)"Saved. Return to the cockpit to see this panel." else "Bike display updated",Toast.LENGTH_LONG).show()
                        }.setNegativeButton("Close",null).show()
                }
                3 -> permission(activity)
                4 -> {
                    val prefs=RidePreferences.prefs(activity)
                    AlertDialog.Builder(activity).setTitle("Voice guidance").setSingleChoiceItems(arrayOf("On","Off"),if(prefs.getBoolean("navigation_voice",true))0 else 1) { d,i -> prefs.edit().putBoolean("navigation_voice",i==0).apply(); d.dismiss() }.show()
                }
                5 -> activity.startService(Intent(activity,NativeNavigationService::class.java).setAction("STOP"))
            }
        }.setNegativeButton("Close",null).show()
    }
    private fun permission(activity: Activity): Boolean {
        if(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) return true
        activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),74)
        Toast.makeText(activity,"Allow precise location, then tap Map again",Toast.LENGTH_LONG).show()
        return false
    }
    private fun credentials(activity: Activity) {
        val fields=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setPadding(24,12,24,0) }
        val map=EditText(activity).apply { hint="MapTiler key (blank keeps existing key)"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val routing=EditText(activity).apply { hint="GraphHopper key (blank keeps existing key)"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val profile=EditText(activity).apply { hint="Routing profile available in your account"; setText(RidePreferences.prefs(activity).getString("routing_profile","scooter")) }
        fields.addView(map); fields.addView(routing); fields.addView(profile)
        AlertDialog.Builder(activity).setTitle("Map and routing accounts")
            .setMessage("MapTiler receives map-area and address searches. GraphHopper receives the route start and destination. Provider quotas and terms apply. Keys are encrypted on this phone.\n\nThe initial profile is scooter. Confirm that it is available on your plan and suitable for your motorcycle. No car-profile fallback is applied.")
            .setView(fields).setNegativeButton("Close",null).setNeutralButton("Provider websites") { _,_ ->
                AlertDialog.Builder(activity).setItems(arrayOf("MapTiler","GraphHopper")) { _,i -> activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(if(i==0)"https://cloud.maptiler.com/" else "https://graphhopper.com/dashboard/"))) }.show()
            }.setPositiveButton("Save",null).create().also { dialog ->
                dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value=profile.text.toString().trim()
                    if(!value.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { profile.error="Enter a valid provider profile"; return@setOnClickListener }
                    try {
                        if(map.text.isNotBlank()) NavigationSecrets.save(activity,"maptiler",map.text.toString().trim())
                        if(routing.text.isNotBlank()) NavigationSecrets.save(activity,"graphhopper",routing.text.toString().trim())
                        RidePreferences.prefs(activity).edit().putString("routing_profile",value).apply()
                        dialog.dismiss(); Toast.makeText(activity,"Saved on this phone",Toast.LENGTH_SHORT).show()
                    } catch(_: Exception) { map.error="Could not store keys on this phone" }
                } }; dialog.show()
            }
    }
    @JvmStatic fun search(activity: Activity, initial: String?) {
        if(!permission(activity)) return
        NativeNavigation.start(activity)
        val page=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setPadding(20,8,20,8) }
        val field=EditText(activity).apply { hint="Search an address or place"; setSingleLine(true) }
        val status=TextView(activity)
        val results=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
        page.addView(field); page.addView(status); page.addView(ScrollView(activity).apply { addView(results) },LinearLayout.LayoutParams(-1,420))
        val dialog=AlertDialog.Builder(activity).setTitle("Destination").setView(page).setNegativeButton("Close",null).setNeutralButton("Stop route") { _,_ -> NativeNavigation.stopRoute() }.create()
        var generation=0
        var pending: Runnable?=null
        field.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int) {}
            override fun onTextChanged(s: CharSequence?,start: Int,before: Int,count: Int) {
                val query=s?.toString()?.trim().orEmpty(); val request=++generation
                pending?.let(main::removeCallbacks); results.removeAllViews()
                if(query.length<3) { status.text="Enter at least three characters"; return }
                status.text="Searching..."
                pending=Runnable {
                    worker.execute {
                        val result=runCatching { NavigationApi.search(activity.applicationContext,query,NativeNavigation.location) }
                        main.post {
                            if(request!=generation || !dialog.isShowing || activity.isDestroyed) return@post
                            result.onSuccess { places ->
                                status.text=if(places.isEmpty())"No matches. Try a full address." else "Choose the destination"
                                places.forEach { place ->
                                    results.addView(Button(activity).apply {
                                        text=place.label; isAllCaps=false
                                        setOnClickListener {
                                            AlertDialog.Builder(activity).setTitle(place.label).setMessage("Start navigation to this destination?")
                                                .setNegativeButton("Cancel",null).setNeutralButton("Save favorite") { _,_ ->
                                                    runCatching { BikePlaces.add(activity,place.label.take(80),"${place.latitude},${place.longitude}") }
                                                        .onSuccess { Toast.makeText(activity,"Favorite saved",Toast.LENGTH_SHORT).show() }
                                                        .onFailure { status.text="Favorite could not be saved" }
                                                }.setPositiveButton("Navigate") { _,_ ->
                                                    NativeNavigation.navigate(activity,place); dialog.dismiss()
                                                }.show()
                                        }
                                    })
                                }
                            }.onFailure { status.text="Search unavailable. Check MapTiler key, internet and account quota in Setup."; BikeDiagnostics.record(activity,"Native search failed exception=${it.javaClass.simpleName}") }
                        }
                    }
                }.also { main.postDelayed(it,600) }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        dialog.setOnDismissListener { generation++; pending?.let(main::removeCallbacks) }
        dialog.show(); initial?.let { field.setText(it) }
    }
    @JvmStatic fun panel(activity: Activity): View {
        val page=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xff151c17.toInt()) }
        val image=ImageView(activity).apply { scaleType=ImageView.ScaleType.FIT_CENTER; contentDescription="Navigation panel"; setOnClickListener { search(activity,null) } }
        val state=TextView(activity).apply {
            setTextColor(0xfff4f6fa.toInt()); gravity=Gravity.CENTER; textSize=14f
            setOnClickListener {
                val credit=TextView(activity).apply {
                    text=android.text.Html.fromHtml("© <a href='https://www.maptiler.com/copyright/'>MapTiler</a> © <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap contributors</a><br>Routing: GraphHopper<br>Search results: MapTiler",android.text.Html.FROM_HTML_MODE_LEGACY)
                    movementMethod=android.text.method.LinkMovementMethod.getInstance(); setPadding(24,20,24,20)
                }
                AlertDialog.Builder(activity).setTitle("Map credits").setView(credit).setPositiveButton("Close",null).show()
            }
        }
        page.addView(image,LinearLayout.LayoutParams(-1,0,1f)); page.addView(state)
        val actions=LinearLayout(activity)
        listOf("Search","+","−","Panels").forEachIndexed { i,label ->
            actions.addView(Button(activity).apply { text=label; isAllCaps=false; setOnClickListener { when(i) { 0 -> search(activity,null); 1 -> NativeNavigation.zoom(true); 2 -> NativeNavigation.zoom(false); 3 -> configure(activity) } } },LinearLayout.LayoutParams(0,-2,1f))
        }
        page.addView(actions)
        val refresh=object: Runnable { override fun run() {
            if(!page.isAttachedToWindow) return
            image.setImageBitmap(NativeNavigation.phoneBitmap(RidePreferences.prefs(activity).getString("phone_panel","map") ?: "map"))
            state.text=NativeNavigation.status
            main.postDelayed(this,750)
        } }
        page.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { main.post(refresh) }
            override fun onViewDetachedFromWindow(v: View) { main.removeCallbacks(refresh) }
        })
        return page
    }
}
