package com.sharky.home

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.StatFs
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("sharky", MODE_PRIVATE) }
    private lateinit var appRow: LinearLayout
    private lateinit var status: TextView
    private var apps = emptyList<TvApp>()
    private val blue = Color.rgb(25, 185, 232)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); refresh() }

    private fun buildUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(52), dp(34), dp(52), dp(26)); setBackgroundColor(Color.rgb(7,18,30)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "SHARKY"; textSize = 32f; setTextColor(blue); typeface = Typeface.DEFAULT_BOLD; letterSpacing = .12f }, LinearLayout.LayoutParams(0, dp(52), 1f))
        status = TextView(this).apply { textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        header.addView(status, LinearLayout.LayoutParams(dp(430), dp(52)))
        root.addView(header)
        root.addView(TextView(this).apply { text = "Your TV. Your way."; textSize = 20f; setTextColor(Color.WHITE); alpha = .88f }, LinearLayout.LayoutParams(-1, dp(42)))
        root.addView(section("QUICK LAUNCH"))
        val quick = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val tiles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val moviePkg = prefs.getString("movie_package", "")!!.ifBlank { "com.sharky.movies" }
        val featured = listOf(
            Triple("Movies", moviePkg, "Your movie app"), Triple("Apps", "__apps", "All installed apps"),
            Triple("Settings", "__settings", "Fire TV settings"), Triple("YouTube", "com.google.android.youtube.tv", "Watch YouTube"),
            Triple("Netflix", "com.netflix.ninja", "Netflix"), Triple("Prime Video", "com.amazon.amazonvideo.livingroom", "Prime Video"),
            Triple("Spotify", "com.spotify.tv.android", "Spotify")
        )
        featured.forEachIndexed { i, item -> tiles.addView(featureTile(item.first,item.second,item.third), LinearLayout.LayoutParams(dp(if(i==0) 270 else 190), dp(142)).apply { marginEnd=dp(14) }) }
        quick.addView(tiles); root.addView(quick, LinearLayout.LayoutParams(-1, dp(166)))
        root.addView(section("FAVOURITES & RECENT APPS"))
        appRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; addView(appRow) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply { text="Press Menu on an app to add/remove it from favourites   •   Long-press Select for app details"; textSize=14f; setTextColor(Color.rgb(150,175,190)); gravity=Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-1,dp(34)))
        setContentView(root)
    }
    private fun section(text:String) = TextView(this).apply { this.text=text; textSize=14f; letterSpacing=.1f; setTextColor(Color.rgb(140,188,205)); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.BOTTOM }.also { it.layoutParams=LinearLayout.LayoutParams(-1,dp(34)) }
    private fun featureTile(title:String, pkg:String, sub:String): TextView = TextView(this).apply {
        text="$title\n\n$sub"; textSize=18f; setTextColor(Color.WHITE); setPadding(dp(20),dp(18),dp(18),dp(12)); gravity=Gravity.CENTER_VERTICAL
        setBackgroundColor(if(title=="Movies") Color.rgb(0,108,151) else Color.rgb(17,38,58)); isFocusable=true; isClickable=true
        setOnFocusChangeListener { v, focus -> v.scaleX=if(focus)1.06f else 1f; v.scaleY=if(focus)1.06f else 1f; v.setBackgroundColor(if(focus) Color.rgb(26,91,120) else if(title=="Movies") Color.rgb(0,108,151) else Color.rgb(17,38,58)) }
        setOnClickListener { when(pkg) { "__apps" -> showApps(); "__settings" -> AppRepository.systemSettings(this@MainActivity); else -> if(!AppRepository.launch(this@MainActivity,pkg)) toast("$title is not installed. Add it in Fire TV settings, or set Movies package in Sharky settings.") } }
    }
    private fun refresh() { apps=AppRepository.installed(this); updateStatus(); renderApps() }
    private fun updateStatus() { val time=SimpleDateFormat("EEE d MMM  •  HH:mm", Locale.getDefault()).format(Date()); val online=getSystemService<ConnectivityManager>()?.activeNetwork != null; val storage=StatFs(filesDir.path).availableBytes/1024/1024/1024; status.text="$time   ${if(online) "● Online" else "○ Offline"}   ${storage} GB free" }
    private fun renderApps() { appRow.removeAllViews(); val favs=prefs.getStringSet("favourites", emptySet()) ?: emptySet(); val ordered=apps.sortedWith(compareByDescending<TvApp>{it.packageName in favs}.thenBy{it.label}); if(ordered.isEmpty()) appRow.addView(TextView(this).apply { text="No launchable apps found yet."; setTextColor(Color.LTGRAY); textSize=18f }); ordered.take(12).forEach { appRow.addView(appTile(it),LinearLayout.LayoutParams(dp(148),dp(155)).apply { marginEnd=dp(14) }) } }
    private fun appTile(app:TvApp): LinearLayout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(10),dp(10),dp(10),dp(8)); setBackgroundColor(Color.rgb(17,38,58)); isFocusable=true; isClickable=true
        addView(ImageView(this@MainActivity).apply { setImageDrawable(app.icon); scaleType=ImageView.ScaleType.FIT_CENTER },LinearLayout.LayoutParams(dp(72),dp(72)))
        addView(TextView(this@MainActivity).apply { text=app.label; textSize=14f; maxLines=2; gravity=Gravity.CENTER; setTextColor(Color.WHITE) },LinearLayout.LayoutParams(-1,dp(50)))
        setOnFocusChangeListener { v,f -> v.scaleX=if(f)1.07f else 1f; v.scaleY=if(f)1.07f else 1f; v.setBackgroundColor(if(f)Color.rgb(26,91,120) else Color.rgb(17,38,58)) }
        setOnClickListener { AppRepository.launch(this@MainActivity,app.packageName) }
        setOnLongClickListener { appOptions(app); true }
    }
    private fun appOptions(app:TvApp) { val favs=(prefs.getStringSet("favourites",emptySet())?:emptySet()).toMutableSet(); val label=if(favs.contains(app.packageName))"Remove from favourites" else "Add to favourites"; AlertDialog.Builder(this).setTitle(app.label).setItems(arrayOf(label,"App info")){_,which-> if(which==0){ if(!favs.add(app.packageName)) favs.remove(app.packageName); prefs.edit().putStringSet("favourites",favs).apply(); renderApps() } else try { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${app.packageName}"))) } catch(_:Exception){} }.show() }
    private fun showApps(){ val labels=apps.map{it.label}.toTypedArray(); AlertDialog.Builder(this).setTitle("All apps").setItems(labels){_,which->AppRepository.launch(this,apps[which].packageName)}.setNeutralButton("Sharky settings"){_,_->settings()}.show() }
    private fun settings(){ val input=EditText(this).apply { setText(prefs.getString("movie_package","")); hint="Movie app package, e.g. com.example.movies" }; AlertDialog.Builder(this).setTitle("Sharky settings").setMessage("Movies tile package name. Use Home on Fire for reliable boot/Home redirect on Fire OS.").setView(input).setMultiChoiceItems(arrayOf("Attempt launch on system boot"),booleanArrayOf(prefs.getBoolean("boot_attempt",false))){_,_,checked->prefs.edit().putBoolean("boot_attempt",checked).apply()}.setPositiveButton("Save"){_,_->prefs.edit().putString("movie_package",input.text.toString().trim()).apply(); buildUi();refresh()}.setNegativeButton("Cancel",null).show() }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
}
