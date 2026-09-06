package com.sharky.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("sharky", MODE_PRIVATE) }
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable { override fun run() { updateStatus(); handler.postDelayed(this, 30_000) } }
    private val ink = Color.rgb(11,15,24)
    private val panel = Color.rgb(26,33,47)
    private val accent = Color.rgb(76,188,237)
    private var apps = emptyList<TvApp>()
    private var page = "Home"
    private var lastFocus = "nav:Home"
    private var active = false
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var scroll: ScrollView

    override fun onCreate(state: Bundle?) { super.onCreate(state); page=state?.getString("page")?:"Home"; lastFocus=state?.getString("focus")?:"nav:Home"; buildUi(); renderPage() }
    override fun onResume() {
        super.onResume(); active=true; handler.removeCallbacks(ticker); ticker.run()
        worker.execute {
            val found=runCatching { AppRepository.installed(this) }.getOrDefault(emptyList())
            handler.post { if (!isDestroyed && active) { apps=found; renderPage() } }
        }
    }
    override fun onPause() { active=false; handler.removeCallbacks(ticker); super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy() }
    override fun onSaveInstanceState(state: Bundle) { state.putString("page",page); state.putString("focus",lastFocus); super.onSaveInstanceState(state) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); home() }
    @Deprecated("TV back navigation")
    override fun onBackPressed() { home() }
    private fun home() { page="Home"; lastFocus="nav:Home"; buildUi(); renderPage() }

    private fun buildUi() {
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root=LinearLayout(this).apply { orientation=1; setBackgroundColor(ink); setPadding(dp(38),dp(16),dp(38),dp(8)) }
        val nav=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        nav.addView(ImageView(this).apply { setImageResource(R.drawable.sharky_logo); contentDescription="Sharky Home" }, LinearLayout.LayoutParams(dp(58),dp(48)))
        nav.addView(label("SHARKY",17,Color.WHITE,true),LinearLayout.LayoutParams(dp(106),dp(48)))
        listOf("Home","Apps","Settings").forEach { name -> nav.addView(button(name,"nav:$name",name==page) {
            page=name; lastFocus="nav:$name"; buildUi(); renderPage()
        },LinearLayout.LayoutParams(dp(88),dp(42)).apply { marginEnd=dp(6) }) }
        status=label("",12,Color.LTGRAY).apply { gravity=Gravity.END or Gravity.CENTER_VERTICAL }
        nav.addView(status,LinearLayout.LayoutParams(0,dp(48),1f)); root.addView(nav)
        content=LinearLayout(this).apply { orientation=1; clipChildren=false }
        scroll=ScrollView(this).apply { isVerticalScrollBarEnabled=false; isFillViewport=true; clipToPadding=false; setPadding(0,dp(12),0,dp(12)); addView(content) }
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(label("SELECT  Open     •     MENU / HOLD SELECT  App options     •     BACK  Home",11,Color.rgb(141,154,175)),LinearLayout.LayoutParams(-1,dp(25)))
        setContentView(root); updateStatus()
    }
    private fun renderPage() {
        val focus=currentFocus?.tag as? String ?: lastFocus
        val y=scroll.scrollY
        content.removeAllViews()
        when(page) {
            "Apps" -> {
                heading("Your apps","Every installed app, including TV-only apps")
                if(apps.isEmpty()) empty("No apps found. Reopen Sharky after installing an app.")
                apps.chunked(5).forEach { group ->
                    val row=LinearLayout(this).apply { clipChildren=false; setPadding(dp(6),dp(8),dp(6),dp(8)) }
                    group.forEach { row.addView(appCard(it),LinearLayout.LayoutParams(0,dp(118),1f).apply { marginEnd=dp(12) }) }
                    repeat(5-group.size) { row.addView(View(this),LinearLayout.LayoutParams(0,1,1f).apply { marginEnd=dp(12) }) }
                    content.addView(row)
                }
            }
            "Settings" -> settingsPage()
            else -> homePage()
        }
        content.post { if(!isDestroyed) {
            (window.decorView.findViewWithTag<View>(focus) ?: window.decorView.findViewWithTag<View>("nav:$page"))?.requestFocus()
            scroll.scrollTo(0,y)
        } }
    }
    private fun homePage() {
        val sharky = apps.filter { isSharky(it) }.sortedBy { if(it.packageName=="uk.co.sharkmovie.tv") 0 else 1 }
        section("Sharky apps")
        if(sharky.isEmpty()) empty("Your Sharky apps will appear here when installed.")
        else {
            val row=horizontalRow()
            sharky.forEach { row.addView(appCard(it,"sharky:${it.packageName}"),LinearLayout.LayoutParams(dp(210),dp(138)).apply { marginEnd=dp(16) }) }
        }
        val preferred=listOf("com.amazon.firetv.youtube","com.netflix.ninja","com.amazon.firebat","com.spotify.tv.android")
        val normal=apps.filterNot { isSharky(it) }.sortedWith(compareByDescending<TvApp> { it.packageName in favourites() }
            .thenBy { preferred.indexOf(it.packageName).let { index -> if(index<0) 100 else index } }.thenBy { it.label.lowercase() })
        shelf("Your apps",normal)
        shelf("Recently opened",recent().mapNotNull { pkg -> apps.find { it.packageName==pkg } })
    }
    private fun isSharky(app: TvApp): Boolean = app.packageName.startsWith("com.sharky.") ||
        app.packageName.startsWith("uk.co.sharkmovie.") || app.label.equals("Sharky",true) || app.label.startsWith("Sharky ",true)
    private fun horizontalRow(): LinearLayout {
        val row=LinearLayout(this).apply { clipChildren=false; setPadding(dp(6),dp(8),dp(6),dp(8)) }
        content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; clipToPadding=false; addView(row) }); return row
    }
    private fun shelf(title: String, items: List<TvApp>) {
        if(items.isEmpty()) return
        section(title); val row=horizontalRow()
        items.forEach { row.addView(appCard(it,"$title:${it.packageName}"),LinearLayout.LayoutParams(dp(155),dp(116)).apply { marginEnd=dp(14) }) }
    }
    private fun appCard(app: TvApp,key: String="app:${app.packageName}"): View {
        val tile=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setPadding(dp(12),dp(10),dp(12),dp(8)) }
        tile.addView(ImageView(this).apply { setImageDrawable(app.icon); scaleType=ImageView.ScaleType.FIT_CENTER },LinearLayout.LayoutParams(-1,0,1f))
        tile.addView(label((if(app.packageName in favourites()) "★ " else "")+app.label,13,Color.WHITE).apply { gravity=Gravity.CENTER; maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END },LinearLayout.LayoutParams(-1,dp(28)))
        styleFocus(tile,key); tile.contentDescription=app.label; tile.setOnClickListener { launch(app) }; tile.setOnLongClickListener { appOptions(app); true }
        tile.setOnKeyListener { _,code,event -> if(code==KeyEvent.KEYCODE_MENU) { if(event.action==KeyEvent.ACTION_UP) appOptions(app); true } else false }
        return tile
    }
    private fun launch(app: TvApp) {
        if(AppRepository.launch(this,app.packageName)) prefs.edit().putString("recent",(listOf(app.packageName)+recent()).distinct().take(12).joinToString("|")).apply()
        else toast("${app.label} couldn't open. It may have been removed or disabled.")
    }
    private fun favourites()=prefs.getStringSet("favourites",emptySet())?.toSet()?:emptySet()
    private fun recent()=prefs.getString("recent","").orEmpty().split('|').filter { it.isNotBlank() }
    private fun movieApp(): TvApp? {
        val pkg=prefs.getString("movie_package","").orEmpty()
        return if(pkg.isNotEmpty()) apps.find { it.packageName==pkg } else apps.filter { it.packageName.contains("shark",true)||it.label.contains("shark",true) }.singleOrNull()
    }
    private fun appOptions(app: TvApp) {
        val pinned=app.packageName in favourites()
        AlertDialog.Builder(this).setTitle(app.label).setItems(arrayOf(if(pinned) "Remove favourite" else "Add favourite","Use as Movies app","App details")) { _,which -> when(which) {
            0 -> { val next=favourites().toMutableSet(); if(pinned) next.remove(app.packageName) else next.add(app.packageName); prefs.edit().putStringSet("favourites",next).apply(); renderPage() }
            1 -> { prefs.edit().putString("movie_package",app.packageName).apply(); renderPage(); toast("Movies now opens ${app.label}") }
            2 -> open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${app.packageName}")))
        } }.setNegativeButton("Close",null).show()
    }
    private fun chooseMovies() {
        AlertDialog.Builder(this).setTitle("Choose your Movies app").setItems(apps.map { it.label }.toTypedArray()) { _,which -> prefs.edit().putString("movie_package",apps[which].packageName).apply(); renderPage() }
            .setNeutralButton("Enter package") { _,_ -> manualMovie() }.setNegativeButton("Cancel",null).show()
    }
    private fun manualMovie() {
        val input=EditText(this).apply { setText(prefs.getString("movie_package","")); hint="com.example.movies"; setSingleLine() }
        val dialog=AlertDialog.Builder(this).setTitle("Movies package").setView(input).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value=input.text.toString().trim()
            if(value.isNotEmpty()&&!Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+").matches(value)) input.error="Enter a valid package name"
            else { prefs.edit().putString("movie_package",value).apply(); dialog.dismiss(); renderPage() }
        } }; dialog.show()
    }
    private fun settingsPage() {
        heading("Make it yours","Sharky Home ${BuildConfig.VERSION_NAME}")
        setting("Movies app",movieApp()?.label?:"Choose an installed app","movies") { chooseMovies() }
        setting("Home button & startup","Configure Home on Fire","redirect") { redirectSetup() }
        setting("Fire TV settings","Network, Bluetooth, display and device","system") { open(Intent(Settings.ACTION_SETTINGS)) }
        setting("Accessibility settings","Enable Home on Fire","accessibility") { open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        setting("Clear recent apps","Only clears Sharky's recently opened list","recent") {
            AlertDialog.Builder(this).setTitle("Clear recent apps?").setPositiveButton("Clear") { _,_ -> prefs.edit().remove("recent").apply(); toast("Recent apps cleared") }.setNegativeButton("Cancel",null).show()
        }
        setting("Launch at boot (best effort)",if(prefs.getBoolean("boot_attempt",false)) "On · Home on Fire is recommended" else "Off · Home on Fire is recommended","boot") {
            prefs.edit().putBoolean("boot_attempt",!prefs.getBoolean("boot_attempt",false)).apply(); renderPage()
        }
        empty("Home and power-on redirection need the companion's accessibility permission. Installing Sharky alone cannot change Fire OS's Home button.")
    }
    private fun redirectSetup() {
        AlertDialog.Builder(this).setTitle("Make Sharky your home")
            .setMessage("1. Install Home on Fire from its official GitHub release using Downloader.\n\n2. Enable its accessibility service, choose Sharky Home as the target, and enable Launch on boot.\n\nIf Fire OS hides the accessibility switch, a one-time permission grant from a computer is required. Run setup-home-redirect.bat in the Sharky folder for that step.\n\nHome on Fire is a separate open-source app. Amazon's home may briefly appear before redirecting.")
            .setPositiveButton("Open Home on Fire") { _,_ -> if(!AppRepository.launch(this,"io.github.toolicious.homeonfire")) open(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/toolicious/home-on-fire/releases/latest"))) }
            .setNeutralButton("Accessibility") { _,_ -> open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.setNegativeButton("Close",null).show()
    }
    private fun open(intent: Intent) { if(!AppRepository.open(this,intent)) toast("This page isn't available. Use Fire TV Settings or Downloader.") }
    private fun setting(title: String,subtitle: String,key: String,action: () -> Unit) {
        content.addView(button("$title   ›\n$subtitle","setting:$key",action=action).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(18),0,dp(18),0) },LinearLayout.LayoutParams(-1,dp(64)).apply { bottomMargin=dp(10) })
    }
    private fun button(text: String,key: String,selected: Boolean=false,action: () -> Unit): TextView=label(text,14,Color.WHITE,selected).apply {
        gravity=Gravity.CENTER; setPadding(dp(8),dp(4),dp(8),dp(4)); styleFocus(this,key,selected); setOnClickListener { action() }
    }
    private fun styleFocus(view: View,key: String,selected: Boolean=false) {
        view.tag=key; view.id=View.generateViewId(); view.isFocusable=true; view.isClickable=true
        view.background=surface(if(selected) Color.rgb(31,75,98) else panel,false)
        view.setOnFocusChangeListener { v,focus ->
            if(focus) lastFocus=key
            v.background=surface(if(focus) Color.rgb(37,72,95) else if(selected) Color.rgb(31,75,98) else panel,focus)
            v.animate().scaleX(if(focus) 1.025f else 1f).scaleY(if(focus) 1.025f else 1f).setDuration(120).start()
        }
    }
    private fun surface(color: Int,focused: Boolean)=GradientDrawable().apply { setColor(color); cornerRadius=dp(10).toFloat(); setStroke(dp(2),if(focused) accent else Color.TRANSPARENT) }
    private fun label(text: String,size: Int,color: Int,bold: Boolean=false)=TextView(this).apply { this.text=text; textSize=size.toFloat(); setTextColor(color); gravity=Gravity.CENTER_VERTICAL; if(bold) typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL) }
    private fun section(title: String) { content.addView(label(title,18,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(42)).apply { topMargin=dp(6) }) }
    private fun heading(title: String,subtitle: String) { content.addView(label(title,28,Color.WHITE,true)); content.addView(label(subtitle,14,Color.LTGRAY),LinearLayout.LayoutParams(-1,dp(42))) }
    private fun empty(text: String) { content.addView(label(text,14,Color.LTGRAY).apply { setPadding(dp(6),dp(8),dp(6),dp(12)) }) }
    private fun updateStatus() {
        if(!::status.isInitialized) return
        val time=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())
        val network=runCatching {
            val cm=getSystemService<ConnectivityManager>(); val caps=cm?.getNetworkCapabilities(cm.activeNetwork)
            when { caps==null -> "Offline"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"; else -> "Connected" }
        }.getOrDefault("Offline")
        val free=runCatching { String.format(Locale.getDefault(),"%.1f GB",StatFs(filesDir.path).availableBytes/1073741824.0) }.getOrDefault("—")
        status.text="$time  •  $network\n$free free"
    }
    private fun toast(text: String)=Toast.makeText(this,text,Toast.LENGTH_LONG).show()
    private fun dp(n: Int)=(n*resources.displayMetrics.density).toInt()
}
