package com.sharky.dropbox

import java.net.URI

object UrlRules {
    fun normalize(input: String): String {
        var text=input.trim()
        require(text.length in 1..8192) { "Paste a download link (maximum 8192 characters)." }
        // Correct precisely the accidental double-paste shown in the original request.
        val second=text.indexOf("https://",8)
        if(second>0 && text.substring(0,second)==text.substring(second)) text=text.substring(0,second)
        require(!text.any { it.isWhitespace() || it.code<32 }) { "The link contains spaces or extra text." }
        val uri=try { URI(text) } catch(_:Exception) { throw IllegalArgumentException("That link isn't a valid URL.") }
        require(uri.scheme.equals("https",true) && !uri.host.isNullOrBlank() && uri.userInfo==null) { "Use a full https:// download link with no username or password." }
        require(uri.port==-1 || uri.port in 1..65535) { "Invalid URL port." }
        require(uri.fragment==null) { "Remove the # fragment from the download link." }
        return uri.toASCIIString()
    }
}
