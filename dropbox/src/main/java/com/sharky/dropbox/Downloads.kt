package com.sharky.dropbox

import android.content.Context
import android.os.StatFs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class Downloads(private val context: Context) {
    val cancelled=AtomicBoolean(false)
    @Volatile private var connection: HttpURLConnection?=null
    fun cancel() { cancelled.set(true); connection?.disconnect() }
    fun fetch(raw: String, progress: (Long,Long)->Unit): File {
        var url=UrlRules.normalize(raw)
        val directory=File(context.filesDir,"downloads").apply { mkdirs() }
        val part=File(directory,"incoming.part")
        val result=File(directory,"received.apk")
        val maxBytes=1024L*1024*1024
        try {
            repeat(8) {
                check(!cancelled.get()) { "Download cancelled." }
                val conn=(URL(url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects=false; connectTimeout=15_000; readTimeout=30_000
                    setRequestProperty("User-Agent","SharkyDropBox/1.0")
                    setRequestProperty("Accept-Encoding","identity")
                }
                connection=conn
                val status=conn.responseCode
                if(status in listOf(301,302,303,307,308)) {
                    val location=conn.getHeaderField("Location") ?: error("Download redirect has no destination.")
                    url=UrlRules.normalize(URL(URL(url),location).toString()); conn.disconnect()
                } else {
                    check(status==200) { "Server returned HTTP $status. Check that this is a direct download link." }
                    val total=conn.contentLengthLong
                    check(total<=maxBytes) { "This file is too large (1 GB maximum)." }
                    val free=StatFs(directory.path).availableBytes-64L*1024*1024
                    check(free>0 && (total<0 || total<free)) { "Not enough free space on the Fire Stick." }
                    var received=0L; var last=0L
                    conn.inputStream.use { input -> part.outputStream().use { output ->
                        val buffer=ByteArray(65536)
                        while(true) {
                            check(!cancelled.get()) { "Download cancelled." }
                            val read=input.read(buffer); if(read<0) break
                            received+=read; check(received<=maxBytes && received<free) { "Download exceeded available storage or the 1 GB limit." }
                            output.write(buffer,0,read)
                            val now=System.currentTimeMillis()
                            if(now-last>250) { progress(received,total); last=now }
                        }
                    } }
                    check(received>0 && (total<0 || received==total)) { "The download was incomplete. Please try again." }
                    check(!cancelled.get()) { "Download cancelled." }
                    // Reject HTML error pages, archives and split APK bundles instead of invoking an installer on them.
                    check(context.packageManager.getPackageArchiveInfo(part.absolutePath,0)!=null) { "This link did not download a valid APK. Use a direct .apk link, not a webpage or .apks bundle." }
                    if(result.exists()) check(result.delete()) { "Couldn't replace the previous download." }
                    check(part.renameTo(result)) { "Couldn't save the APK." }
                    progress(received,received); return result
                }
            }
            error("Too many redirects in this download link.")
        } finally { connection?.disconnect(); connection=null; part.delete() }
    }
}
