package com.projectanonymous.chatbot.data.aimode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModeResponseParserTest {

    private val parser = AiModeResponseParser()

    @Test
    fun extractsAnswerAndDeduplicatesCitations() {
        val html = """
            <html><body>
              <div id="cnt">
                <div class="tonYlb">
                  <div class="mZJni Dn7Fzd">
                    <p>Fotosintesis adalah proses tumbuhan mengubah cahaya menjadi energi kimia.</p>
                    <a href="https://example.org/article" target="_blank" data-ved="abc">Artikel ilmiah</a>
                    <a href="https://example.org/article" target="_blank" data-ved="def">Artikel ilmiah lengkap</a>
                    <p>AI can make mistakes, so double-check.</p>
                  </div>
                  <div class="v4bSkd"></div>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, "Jelaskan fotosintesis")

        assertTrue(result.hasAnswer)
        assertTrue(result.answer.contains("Fotosintesis adalah proses"))
        assertFalse(result.answer.contains("AI can make mistakes"))
        assertEquals(1, result.citations.size)
        assertEquals("https://example.org/article", result.citations.single().url)
        assertEquals(AiModeCompletion.CONFIRMED, result.completion)
    }

    @Test
    fun unwrapsGoogleRedirectAndRejectsGoogleChromeLinks() {
        val html = """
            <div id="cnt">
              <p>Jawaban yang cukup panjang agar dapat dikenali sebagai isi.</p>
              <a href="https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fsource"
                 target="_blank" data-ved="abc">Sumber</a>
              <a href="https://support.google.com/websearch?p=aimode"
                 target="_blank" data-ved="def">Bantuan Google</a>
            </div>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals(1, result.citations.size)
        assertEquals("https://example.com/source", result.citations.single().url)
    }

    @Test
    fun doesNotPretendCompletionWhenMarkerIsMissing() {
        val result = parser.parse(
            """<div id="cnt"><p>Jawaban sementara.</p></div>"""
        )

        assertEquals(AiModeCompletion.NOT_CONFIRMED, result.completion)
    }

    @Test
    fun emptyHtmlReturnsAnEmptyResult() {
        val result = parser.parse(" ")

        assertFalse(result.hasAnswer)
        assertFalse(result.answerContainerFound)
        assertTrue(result.citations.isEmpty())
    }

    @Test
    fun rejectsJavascriptAndRelativeLinks() {
        val result = parser.parse(
            """
            <div id="cnt">
              <p>Jawaban parser.</p>
              <a href="javascript:alert(1)">Script</a>
              <a href="/relative/path">Relative</a>
            </div>
            """.trimIndent()
        )

        assertTrue(result.citations.isEmpty())
    }
}
