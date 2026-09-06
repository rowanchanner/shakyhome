package com.sharky.dropbox

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Small, bounded HTTP receiver. Only the local UI and authenticated JSON endpoints are exposed. */
class DropServer(private val address: String, private val handle: (String,String,Map<String,String>,ByteArray)->Reply) {
    data class Reply(val code: Int, val type: String, val body: ByteArray)
    private var server: ServerSocket?=null
    private val pool=ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,ArrayBlockingQueue(6),ThreadPoolExecutor.AbortPolicy())
    fun start() {
        val socket=ServerSocket().apply { reuseAddress=true; bind(java.net.InetSocketAddress(InetAddress.getByName(address),8787),8) }
        server=socket
        Thread({
            while(!socket.isClosed) {
                val client=try { socket.accept() } catch(_:Exception) { break }
                try { pool.execute { serve(client) } } catch(_:Exception) { client.close() }
            }
        },"Sharky-receiver").apply { isDaemon=true; start() }
    }
    fun stop() { runCatching { server?.close() }; pool.shutdownNow() }
    private fun serve(client: Socket) {
        client.use { socket ->
            socket.soTimeout=5000
            try {
                val input=BufferedInputStream(socket.getInputStream())
                val header=ByteArrayOutputStream()
                var tail=0
                while(header.size()<16384) {
                    val next=input.read(); require(next>=0)
                    header.write(next); tail=(tail shl 8) or next
                    if(tail==0x0d0a0d0a) break
                }
                require(tail==0x0d0a0d0a)
                val lines=header.toString("ISO-8859-1").split("\r\n")
                val request=lines[0].split(' ')
                require(request.size==3 && request[2]=="HTTP/1.1")
                val headers=mutableMapOf<String,String>()
                lines.drop(1).filter { it.isNotBlank() }.forEach {
                    val split=it.indexOf(':'); require(split>0)
                    val key=it.substring(0,split).lowercase(); require(key !in headers)
                    headers[key]=it.substring(split+1).trim()
                }
                // Numeric Host prevents a public webpage from rebinding its domain to this receiver.
                require(headers["host"]=="$address:8787")
                require("transfer-encoding" !in headers)
                val length=headers["content-length"]?.toInt()?:0
                require(length in 0..16384)
                val body=ByteArray(length)
                var offset=0
                while(offset<length) { val count=input.read(body,offset,length-offset); require(count>0); offset+=count }
                val reply=handle(request[0],request[1],headers,body)
                respond(socket,reply)
            } catch(_:Exception) { runCatching { respond(socket,Reply(400,"text/plain","Invalid request".toByteArray())) } }
        }
    }
    private fun respond(socket: Socket, reply: Reply) {
        val reason=when(reply.code) { 200->"OK";202->"Accepted";401->"Unauthorized";403->"Forbidden";409->"Conflict";429->"Too Many Requests";404->"Not Found";else->"Bad Request" }
        val headers="HTTP/1.1 ${reply.code} $reason\r\nContent-Type: ${reply.type}\r\nContent-Length: ${reply.body.size}\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'\r\n\r\n"
        socket.getOutputStream().apply { write(headers.toByteArray(StandardCharsets.US_ASCII)); write(reply.body); flush() }
    }
}
