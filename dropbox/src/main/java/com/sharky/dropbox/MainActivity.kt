package com.sharky.dropbox

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity: AppCompatActivity() {
    private val handler=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private var server: DropServer?=null
    private var host=""
    @Volatile private var code=newCode()
    @Volatile private var message="Ready for a link from your PC"
    @Volatile private var fraction=-1
    private val busy=AtomicBoolean(false)
    private var download: Downloads?=null
    private var ready: File?=null
    private var resumed=false
    private var pendingInstaller=false
    private var permissionPending=false
    private var failedAttempts=0
    private var attemptWindow=0L
    private lateinit var addressLabel: TextView
    private lateinit var codeLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var bar: ProgressBar
    private val accent=Color.rgb(76,188,237)
    private val refresh=object:Runnable { override fun run() { updateUi(); handler.postDelayed(this,500) } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility=5894
        val saved=File(filesDir,"downloads/received.apk"); if(saved.exists()) ready=saved
        buildUi()
    }
    override fun onStart() { super.onStart(); startReceiver(); handler.post(refresh) }
    override fun onResume() {
        super.onResume(); resumed=true
        if(permissionPending) { permissionPending=false; if(packageManager.canRequestPackageInstalls()) pendingInstaller=true }
        if(pendingInstaller && ready!=null) { pendingInstaller=false; handler.post { openInstaller() } }
    }
    override fun onPause() { resumed=false; super.onPause() }
    override fun onStop() { server?.stop(); server=null; handler.removeCallbacks(refresh); super.onStop() }
    override fun onDestroy() { download?.cancel(); worker.shutdownNow(); handler.removeCallbacksAndMessages(null); super.onDestroy() }
    private fun newCode()=String.format(Locale.ROOT,"%06d",SecureRandom().nextInt(1_000_000))
    private fun startReceiver() {
        val cm=getSystemService<ConnectivityManager>()
        host=cm?.getLinkProperties(cm.activeNetwork)?.linkAddresses?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }?.address?.hostAddress.orEmpty()
        if(host.isEmpty()) { message="Connect your Fire Stick to Wi-Fi, then reopen DropBox."; return }
        try {
            server=DropServer(host,::handle).also { it.start() }
        } catch(_:Exception) { message="Couldn't open the receiver. Close and reopen DropBox."; server=null }
        updateUi()
    }
    private fun json(status: Int,text: String)=DropServer.Reply(status,"application/json; charset=utf-8",JSONObject().put("message",text).toString().toByteArray())
    private fun handle(method: String,path: String,headers: Map<String,String>,body: ByteArray): DropServer.Reply {
        if(method=="GET") {
            val asset=when(path) { "/"->"index.html"; "/app.js"->"app.js"; "/style.css"->"style.css";else->null }
            if(asset!=null) return DropServer.Reply(200,when(asset.substringAfterLast('.')) { "js"->"text/javascript";"css"->"text/css";else->"text/html; charset=utf-8" },assets.open(asset).use { it.readBytes() })
            if(path=="/logo.png") return DropServer.Reply(200,"image/png",resources.openRawResource(R.drawable.sharky_logo).use { it.readBytes() })
        }
        if(path !in listOf("/api/status","/api/drop")) return json(404,"Not found")
        val origin=headers["origin"]
        if(origin!=null && origin!="http://$host:8787") return json(403,"Open the address displayed on your TV.")
        synchronized(this) {
            val now=System.currentTimeMillis()
            if(now-attemptWindow>60_000) { attemptWindow=now; failedAttempts=0 }
            if(failedAttempts>=8) return json(429,"Too many incorrect codes. Wait one minute.")
            if(!MessageDigest.isEqual(headers["x-sharky-code"].orEmpty().toByteArray(),code.toByteArray())) { failedAttempts++; return json(401,"Enter the six-digit code shown on your TV.") }
        }
        if(method=="GET" && path=="/api/status") return DropServer.Reply(200,"application/json",JSONObject().put("message",message).put("progress",fraction).put("busy",busy.get()).toString().toByteArray())
        if(method!="POST" || path!="/api/drop" || !headers["content-type"].orEmpty().startsWith("application/json")) return json(400,"Send a JSON download request.")
        val url=try { UrlRules.normalize(JSONObject(String(body,Charsets.UTF_8)).getString("url")) } catch(e:Exception) { return json(400,e.message?:"Invalid URL") }
        if(!busy.compareAndSet(false,true)) return json(409,"A download is already running. Wait for it or cancel on the TV.")
        message="Starting download…"; fraction=-1
        handler.post { beginDownload(url) }
        return json(202,"Link sent. Watch your TV for the download and installation prompt.")
    }
    private fun beginDownload(url: String) {
        if(isDestroyed) { busy.set(false); return }
        ready=null; pendingInstaller=false
        val task=Downloads(this); download=task
        worker.execute {
            try {
                val file=task.fetch(url) { bytes,total ->
                    fraction=if(total>0) (bytes*100/total).toInt() else -1
                    message=if(total>0) "Downloading · $fraction%" else String.format(Locale.getDefault(),"Downloading · %.1f MB",bytes/1048576.0)
                }
                handler.post { if(!isDestroyed) {
                    ready=file; message="Download complete · confirm Install on your TV"; busy.set(false); updateUi()
                    if(resumed) openInstaller() else pendingInstaller=true
                } }
            } catch(e:Exception) {
                handler.post { if(!isDestroyed) { message=if(task.cancelled.get()) "Download cancelled" else e.message?:"Download failed. Try again."; fraction=-1; busy.set(false); updateUi() } }
            }
        }
    }
    private fun openInstaller() {
        val file=ready ?: return
        if(!packageManager.canRequestPackageInstalls()) {
            message="Allow Sharky DropBox to install apps, then return here."
            permissionPending=true
            try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName"))) }
            catch(_:Exception) { permissionPending=false; message="On Fire TV: Settings → My Fire TV → Developer Options → Install unknown apps → Sharky DropBox ON. Then select Install downloaded APK." }
            return
        }
        try {
            val uri=FileProvider.getUriForFile(this,"$packageName.files",file)
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            message="APK ready. Confirm Install on the TV, or reopen the prompt below."
        } catch(_:Exception) { message="Couldn't open the installer. Select Install downloaded APK to retry." }
    }
    private fun buildUi() {
        val root=LinearLayout(this).apply { orientation=1; setBackgroundColor(Color.rgb(11,15,24)); setPadding(dp(48),dp(26),dp(48),dp(20)) }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.sharky_logo) },LinearLayout.LayoutParams(dp(66),dp(58)))
        header.addView(label("Sharky DropBox",28,true),LinearLayout.LayoutParams(0,dp(58),1f)); root.addView(header)
        root.addView(label("A link on your PC. An app on your TV.",16),LinearLayout.LayoutParams(-1,dp(40)))
        val card=LinearLayout(this).apply { orientation=1; setPadding(dp(24),dp(14),dp(24),dp(14)); background=surface(false) }
        card.addView(label("OPEN ON YOUR PC · SAME WI-FI",12))
        addressLabel=label("Connecting…",26,true).apply { setTextColor(accent) }; card.addView(addressLabel,LinearLayout.LayoutParams(-1,dp(48)))
        codeLabel=label("",25,true); card.addView(codeLabel,LinearLayout.LayoutParams(-1,dp(46)))
        root.addView(card,LinearLayout.LayoutParams(-1,dp(150)))
        statusLabel=label("Ready for a link from your PC",17).apply { maxLines=3 }
        root.addView(statusLabel,LinearLayout.LayoutParams(-1,dp(80)))
        bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100 }
        root.addView(bar,LinearLayout.LayoutParams(-1,dp(10)))
        val buttons=LinearLayout(this)
        listOf("Install downloaded APK" to { if(ready==null) message="Send an APK link from your PC first." else openInstaller() },
            "Cancel download" to { download?.cancel(); Unit },
            "New pairing code" to { if(!busy.get()) { code=newCode(); message="New code ready. Enter it on your PC."; updateUi() }; Unit }
        ).forEach { (title,action) -> buttons.addView(button(title,action),LinearLayout.LayoutParams(0,dp(46),1f).apply { marginEnd=dp(10) }) }
        root.addView(buttons,LinearLayout.LayoutParams(-1,dp(62)).apply { topMargin=dp(16) })
        root.addView(label("Keep DropBox open to receive links. APK installation always needs your confirmation.\nPrivate Wi-Fi only · No cloud account · Not affiliated with Dropbox, Inc.",11))
        setContentView(root); updateUi()
    }
    private fun updateUi() {
        if(!::statusLabel.isInitialized) return
        addressLabel.text=if(server!=null) "http://$host:8787" else "Receiver offline"
        codeLabel.text="Pairing code   $code"
        statusLabel.text=message
        bar.isIndeterminate=busy.get() && fraction<0; bar.progress=fraction.coerceAtLeast(0)
    }
    private fun label(text: String,size: Int,bold: Boolean=false)=TextView(this).apply {
        this.text=text; textSize=size.toFloat(); setTextColor(Color.rgb(231,238,248)); gravity=Gravity.CENTER_VERTICAL
        if(bold) typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
    }
    private fun button(text: String,action: ()->Unit)=label(text,14,true).apply {
        isFocusable=true; isClickable=true; gravity=Gravity.CENTER; background=surface(false)
        setOnFocusChangeListener { v,f -> v.background=surface(f) }; setOnClickListener { action(); updateUi() }
    }
    private fun surface(focused: Boolean)=GradientDrawable().apply { setColor(Color.rgb(26,41,58)); cornerRadius=dp(12).toFloat(); setStroke(dp(2),if(focused) accent else Color.TRANSPARENT) }
    private fun dp(n: Int)=(n*resources.displayMetrics.density).toInt()
}
