package com.axelsarassamit.gx12

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import app.pillion.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

/** Parked planning only. External navigation remains responsible for every actual road. */
class FunRideActivity : Activity() {
    private val worker=Executors.newSingleThreadExecutor()
    @Volatile private var generation=0
    private var start:FunPoint?=null
    private var finishPoint:FunPoint?=null
    private val stops=mutableListOf<FunPoint>()
    private val placeLabels=mutableMapOf<String,String>()
    private var minutes=60;private var bearing=0;private var fewer=false
    private var motorway=true;private var toll=true;private var ferry=false;private var dirt=true
    private var routes=emptyList<FunRoute>();private var selected=0
    private var map:FunRideMap?=null
    private var status:TextView?=null
    private var busy=false
    private var mapDialog:Dialog?=null
    private val draft by lazy{getSharedPreferences("fun_ride",MODE_PRIVATE)}
    private val routeFile get()=File(filesDir,"fun-ride-options.json")
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    override fun onCreate(state:Bundle?) {
        RideAppearance.prepare(this);super.onCreate(state)
        runCatching { restore(JSONObject(draft.getString("draft","{}")!!)) }
        if(state?.getBoolean("overview")==true) runCatching {
            routes=loadCached();selected=state.getInt("selected").coerceIn(0,(routes.size-1).coerceAtLeast(0))
        }
        if(routes.isNotEmpty())overview()else form()
    }
    private fun text(value:String,size:Float=17f)=TextView(this).apply{text=value;textSize=size;setTextColor(RideStyle.TEXT);setPadding(0,dp(5),0,dp(5))}
    private fun button(value:String,action:()->Unit)=Button(this).apply{text=value;textSize=18f;minHeight=dp(64);setOnClickListener{action()}}
    private fun page():LinearLayout=LinearLayout(this).apply {
        orientation=1;setBackgroundColor(RideStyle.BACKGROUND);setPadding(dp(12),dp(8),dp(12),dp(8))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this){v,insets->
            val b=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout());v.setPadding(dp(12)+b.left,dp(8)+b.top,dp(12)+b.right,dp(8)+b.bottom);insets}
    }
    private fun form() {
        map?.close();map=null
        val page=page();page.addView(text("Go for a fun ride",27f))
        page.addView(text("Plan while parked. Up to three real road routes, no AI. Review roads and motorcycle access before riding."))
        page.addView(text("Planning sends your chosen locations to the public Valhalla routing service. Map tiles come from OpenStreetMap. Free shared services have availability and fair-use limits."))
        page.addView(button("Start: ${start?.let(::coordinate) ?: "current location"}") {
            AlertDialog.Builder(this).setTitle("Start ride").setItems(arrayOf("Use current location","Enter a place or coordinates")){_,i->if(i==0){start=null;save();form()}else place("Starting point"){start=it;save();form()}}.show()
        })
        page.addView(text("Riding time · stops are extra"))
        val times=listOf(30,60,90,120,180,240,360)
        page.addView(Spinner(this).apply{minimumHeight=dp(64);adapter=ArrayAdapter(this@FunRideActivity,android.R.layout.simple_spinner_dropdown_item,times.map{"$it minutes"});setSelection(times.indexOf(minutes).coerceAtLeast(0));onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,i:Int,id:Long){minutes=times[i];save()}}})
        page.addView(button("Finish: ${finishPoint?.let(::coordinate) ?: "return to start"}") {
            AlertDialog.Builder(this).setTitle("Finish ride").setItems(arrayOf("Return to start","Enter a place or coordinates")){_,i->if(i==0){finishPoint=null;save();form()}else place("Finish"){finishPoint=it;save();form()}}.show()
        })
        page.addView(button(if(fewer) "Ride style: fewer U-turns" else "Ride style: relaxed exploration") {
            AlertDialog.Builder(this).setTitle("Ride style").setSingleChoiceItems(arrayOf("Relaxed exploration", "Fewer U-turns · rank route choices"),if(fewer)1 else 0){dialog,i->fewer=i==1;save();dialog.dismiss();form()}.setNegativeButton("Cancel",null).show()
        }.apply{tag="fun_ride_style"})
        page.addView(text("Fewer U-turns ranks the returned roads; it cannot guarantee no U-turns. Winding-road routing and live traffic are unavailable with this provider."))
        page.addView(text("Explore towards"))
        page.addView(Spinner(this).apply{minimumHeight=dp(64);adapter=ArrayAdapter(this@FunRideActivity,android.R.layout.simple_spinner_dropdown_item,listOf("North","East","South","West"));setSelection(bearing/90);onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,i:Int,id:Long){bearing=i*90;save()}}})
        page.addView(button("Stops: ${stops.size} of 2") {
            val choices=stops.mapIndexed{i,p->"Remove stop ${i+1}: ${coordinate(p)}"}.toMutableList();if(stops.size<2)choices.add("Add a place · coffee, fuel, viewpoint or address")
            AlertDialog.Builder(this).setTitle("Your stops, in order").setItems(choices.toTypedArray()){_,i->if(i<stops.size){stops.removeAt(i);save();form()}else place("Add stop"){stops.add(it);save();form()}}.show()
        })
        page.addView(text("With no chosen stops, we try exploration points. They are not verified attractions, parking or safe stopping places."))
        fun check(label:String,value:Boolean,changed:(Boolean)->Unit){page.addView(CheckBox(this).apply{text=label;textSize=18f;setTextColor(RideStyle.TEXT);minHeight=dp(64);isChecked=value;setOnCheckedChangeListener{_,v->changed(v);save()}})}
        check("Prefer avoiding motorways",motorway){motorway=it};check("Prefer avoiding tolls",toll){toll=it};check("Prefer avoiding ferries",ferry){ferry=it};check("Exclude mapped unpaved roads",dirt){dirt=it}
        page.addView(text("Road preferences can be incomplete. Google Maps recalculates between stops and may choose different roads. No live traffic or closure check in this preview."))
        status=text(if(busy)"Finding rides…"else"Duration is a target, not a promise.");page.addView(status)
        if(!busy && draft.getString("route_draft",null)==snapshot().toString() && routeFile.isFile && System.currentTimeMillis()-routeFile.lastModified() in 0..86400000L)
            page.addView(button("Review last generated rides"){
                val cached=runCatching{loadCached()}.getOrDefault(emptyList())
                if(cached.isEmpty())status?.text="Saved choices could not be read. Find rides again."else{routes=cached;selected=0;overview()}
            })
        page.addView(button(if(busy)"Cancel search"else"Find rides"){if(busy){cancel();form()}else plan()})
        page.addView(button("Back"){finish()});setContentView(ScrollView(this).apply{addView(page)})
        if(busy) for(i in 0 until page.childCount){val child=page.getChildAt(i);if(child is Spinner || child is CheckBox || (child is Button && child.text!="Cancel search" && child.text!="Back"))child.isEnabled=false}
    }
    private fun loadCached():List<FunRoute> {
        require(routeFile.length() in 1..16_000_000)
        val rows=JSONArray(routeFile.readText());require(rows.length() in 1..3)
        return (0 until rows.length()).map{FunRoute.read(rows.getJSONObject(it))}
    }
    private fun coordinate(p:FunPoint):String {
        val key=String.format(Locale.US,"%.5f, %.5f",p.latitude,p.longitude)
        return placeLabels[key]?.let{"$it ($key)"}?:key
    }
    private fun place(title:String,chosen:(FunPoint)->Unit) {
        val field=EditText(this).apply{hint="Place / full address / latitude, longitude";textSize=20f;minHeight=dp(64)}
        val d=AlertDialog.Builder(this).setTitle(title).setView(field).setNegativeButton("Cancel",null).setPositiveButton("Find",null).create()
        d.setOnShowListener{d.getButton(-1).setOnClickListener {
            val query=field.text.toString().trim();if(query.length !in 3..300){field.error="Enter the full place or coordinates";return@setOnClickListener}
            val parts=query.split(',').map{it.trim().toDoubleOrNull()}
            if(parts.size==2 && parts.all{it!=null}) {
                val p=runCatching{FunPoint(parts[0]!!,parts[1]!!)}.getOrNull();if(p==null)field.error="Invalid coordinates"else{d.dismiss();chosen(p)};return@setOnClickListener
            }
            d.getButton(-1).isEnabled=false;val token=generation
            worker.execute {
                @Suppress("DEPRECATION") val found=runCatching{Geocoder(this,Locale.ENGLISH).getFromLocationName(query,6).orEmpty()}.getOrDefault(emptyList())
                runOnUiThread{if(isFinishing || isDestroyed || !d.isShowing || token!=generation)return@runOnUiThread;d.getButton(-1).isEnabled=true
                    if(found.isEmpty())field.error="Place not found. Try its full address or Google Maps coordinates."
                    else{d.dismiss();AlertDialog.Builder(this).setTitle("Choose the exact place").setItems(found.map{it.getAddressLine(0)?:"${it.latitude}, ${it.longitude}"}.toTypedArray()){_,i->
                        val point=runCatching{FunPoint(found[i].latitude,found[i].longitude)}.getOrNull()
                        if(point==null){Toast.makeText(this,"Choose a place between 85 degrees south and north.",Toast.LENGTH_LONG).show();return@setItems}
                        val key=String.format(Locale.US,"%.5f, %.5f",point.latitude,point.longitude)
                        placeLabels[key]=(found[i].getAddressLine(0)?:query).take(100);chosen(point)}.show()}}
            }
        }};d.show()
    }
    private fun current():FunPoint? {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return null
        val manager=getSystemService(LocationManager::class.java)
        return manager.getProviders(true).mapNotNull{runCatching{manager.getLastKnownLocation(it)}.getOrNull()}
            .filter{it.latitude in -85.0..85.0 && it.hasAccuracy() && it.accuracy<=100 && SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos in 0..60_000_000_000L}
            .minByOrNull{it.accuracy}?.let{FunPoint(it.latitude,it.longitude)}
    }
    private var listener:LocationListener?=null
    private fun plan() {
        if(start==null && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),41);return}
        val origin=start?:current()
        if(origin==null){
            status?.text="Waiting for a fresh location. Move outdoors or choose a starting place."
            if(listener==null){val l=object:LocationListener{override fun onLocationChanged(location:Location){if(current()!=null){stopLocation();status?.text="Location ready. Tap Find rides."}}};listener=l
                runCatching{getSystemService(LocationManager::class.java).requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0f,l)}
                Handler(mainLooper).postDelayed({stopLocation()},20000)};return
        }
        busy=true;val token=++generation;save();form()
        // Snapshot every control before work begins; editing invalidates the whole result.
        val target=minutes;val heading=bearing;val destination=finishPoint?:origin;val explicit=stops.toList()
        val avoidMotorway=motorway;val avoidToll=toll;val avoidFerry=ferry;val avoidDirt=dirt;val rankUTurn=fewer
        worker.execute {
            val rows=mutableListOf<FunRoute>();var lastStart=0L
            for(offset in if(explicit.isEmpty())listOf(0.0,65.0,-65.0)else listOf(0.0)) {
                if(token!=generation || Thread.currentThread().isInterrupted)break
                val wait=1200-(SystemClock.elapsedRealtime()-lastStart);if(wait>0)try{Thread.sleep(wait)}catch(_:InterruptedException){break}
                if(token!=generation)break
                lastStart=SystemClock.elapsedRealtime()
                val waypoints=if(explicit.isNotEmpty())explicit else runCatching{FunRidePlan.seeds(origin,target,heading+offset)}.getOrNull()?:continue
                val route=runCatching{FunRideRouting.route(origin,destination,waypoints,avoidMotorway,avoidToll,avoidFerry,avoidDirt)}.getOrNull()
                if(route!=null && route.km>=1 && route.seconds/60 in 1.0..720.0 && rows.none{FunRidePlan.sameRoads(it.points,route.points)})rows.add(route)
            }
            runOnUiThread{if(isFinishing || isDestroyed || token!=generation)return@runOnUiThread;busy=false
                routes=rows.sortedBy{FunRidePlan.score(it.seconds/60,target,if(rankUTurn)it.uTurns else 0)}
                if(routes.isEmpty()){form();status?.text="No suitable route found. Try another direction, shorter duration or different stops."}
                else{selected=0;runCatching{routeFile.writeText(JSONArray(routes.map{it.json()}).toString());draft.edit().putString("route_draft",snapshot().toString()).apply()};overview()}}
        }
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grants:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grants);if(requestCode==41){if(grants.any{it==PackageManager.PERMISSION_GRANTED})plan()else status?.text="Location permission declined. Choose a starting place instead."}}
    private fun overview() {
        map?.close();val page=page();page.addView(text("Choose your ride",25f))
        val info=text("");page.addView(info)
        val picker=Spinner(this).apply{minimumHeight=dp(56);adapter=ArrayAdapter(this@FunRideActivity,android.R.layout.simple_spinner_dropdown_item,routes.mapIndexed{i,r->"Route ${i+1} · ${r.seconds.toInt()/60} min · ${String.format(Locale.US,"%.1f",r.km)} km"});setSelection(selected)};page.addView(picker)
        val view=FunRideMap(this);map=view;page.addView(view,LinearLayout.LayoutParams(-1,0,1f))
        fun update(){val r=routes[selected];info.text="${r.stops.size} stops · ${r.uTurns} U-turns · no live traffic";view.show(routes,selected)}
        picker.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,i:Int,id:Long){selected=i;update()}}
        val tools=LinearLayout(this);listOf("−" to {view.step(-1)},"Fit" to {view.fit()},"+" to {view.step(1)},"Expand" to {expandedMap()}).forEach{(label,action)->tools.addView(button(label,action),LinearLayout.LayoutParams(0,dp(56),1f))};page.addView(tools)
        page.addView(button("Open in Google Maps · recalculates"){handoff()}.apply{isEnabled=mapsAvailable()})
        if(!mapsAvailable())page.addView(text("No supported navigation app installed. Install Google Maps to carry all stops. Other apps are not offered."))
        page.addView(button("Change plan"){routes=emptyList();cancel();form()});setContentView(page);update()
    }
    private fun expandedMap() {
        val dialog=Dialog(this,android.R.style.Theme_Material_NoActionBar_Fullscreen);mapDialog=dialog
        val full=page();val view=FunRideMap(this);full.addView(view,LinearLayout.LayoutParams(-1,0,1f))
        val tools=LinearLayout(this);listOf("−" to {view.step(-1)},"Fit" to {view.fit()},"+" to {view.step(1)},"Done" to {dialog.dismiss()}).forEach{(label,action)->tools.addView(button(label,action),LinearLayout.LayoutParams(0,dp(64),1f))};full.addView(tools)
        dialog.setContentView(full);dialog.setOnDismissListener{view.close();mapDialog=null};dialog.show();view.show(routes,selected)
    }
    private fun mapsIntent():Intent {
        val r=routes[selected];val avoid=mutableListOf<String>();if(motorway)avoid.add("highways");if(toll)avoid.add("tolls");if(ferry)avoid.add("ferries")
        return Intent(Intent.ACTION_VIEW,Uri.parse(FunRideHandoff.url(r.start,r.finish,r.stops,avoid))).setPackage(FunRideHandoff.PACKAGE)
    }
    private fun mapsAvailable()=runCatching{mapsIntent().resolveActivity(packageManager)!=null}.getOrDefault(false)
    private fun handoff() {
        val r=routes[selected]
        AlertDialog.Builder(this).setTitle("Review every stop before riding")
            .setMessage(FunRideHandoff.DESCRIPTION+"\n\n"+r.stops.mapIndexed{i,p->"Stop ${i+1}: ${coordinate(p)}"}.joinToString("\n")+"\nFinish: ${coordinate(r.finish)}\n\nUnpaved-road exclusion cannot be passed to Google Maps. Motorcycle mode depends on coverage. No other app or browser fallback will be used.")
            .setNegativeButton("Cancel",null).setPositiveButton("Open route preview"){_,_->runCatching{startActivity(mapsIntent())}.onFailure{Toast.makeText(this,"Google Maps could not open this ride. No fallback used.",Toast.LENGTH_LONG).show()}}.show()
    }
    private fun snapshot()=JSONObject().put("start",start?.let(FunRoute::pair)).put("finish",finishPoint?.let(FunRoute::pair))
        .put("stops",JSONArray(stops.map(FunRoute::pair))).put("minutes",minutes).put("bearing",bearing)
        .put("motorway",motorway).put("toll",toll).put("ferry",ferry).put("dirt",dirt).put("fewer",fewer).put("labels",JSONObject(placeLabels.toMap()))
    private fun save(){draft.edit().putString("draft",snapshot().toString()).apply()}
    private fun restore(j:JSONObject){start=j.optJSONArray("start")?.let(FunRoute::point);finishPoint=j.optJSONArray("finish")?.let(FunRoute::point)
        j.optJSONArray("stops")?.let{a->for(i in 0 until minOf(2,a.length()))stops.add(FunRoute.point(a.getJSONArray(i)))}
        minutes=j.optInt("minutes",60).takeIf{it in listOf(30,60,90,120,180,240,360)}?:60;bearing=j.optInt("bearing",0).takeIf{it in listOf(0,90,180,270)}?:0
        motorway=j.optBoolean("motorway",true);toll=j.optBoolean("toll",true);ferry=j.optBoolean("ferry",false);dirt=j.optBoolean("dirt",true);fewer=j.optBoolean("fewer",false)
        j.optJSONObject("labels")?.let{labels->labels.keys().forEach{key->placeLabels[key]=labels.optString(key).take(100)}}}
    private fun stopLocation(){listener?.let{runCatching{getSystemService(LocationManager::class.java).removeUpdates(it)}};listener=null}
    private fun cancel(){generation++;busy=false;stopLocation()}
    override fun onSaveInstanceState(out:Bundle){save();out.putBoolean("overview",routes.isNotEmpty());out.putInt("selected",selected);super.onSaveInstanceState(out)}
    override fun onDestroy(){cancel();mapDialog?.dismiss();map?.close();worker.shutdownNow();super.onDestroy()}
}
