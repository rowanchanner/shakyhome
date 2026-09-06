package com.sharky.dropbox

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlRulesTest {
    private val link="https://github.com/rowanchanner/shakyhome/releases/download/latest/app-debug.apk"
    @Test fun normalLink() { assertEquals(link,UrlRules.normalize("  $link  ")) }
    @Test fun accidentalDoublePaste() { assertEquals(link,UrlRules.normalize(link+link)) }
    @Test(expected=IllegalArgumentException::class) fun rejectsLocalFiles() { UrlRules.normalize("file:///etc/passwd") }
    @Test(expected=IllegalArgumentException::class) fun rejectsCredentials() { UrlRules.normalize("https://user:secret@example.com/app.apk") }
    @Test(expected=IllegalArgumentException::class) fun rejectsPlainHttp() { UrlRules.normalize("http://example.com/app.apk") }
    @Test(expected=IllegalArgumentException::class) fun rejectsWhitespace() { UrlRules.normalize("https://example.com/app.apk extra") }
}
