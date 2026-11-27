package com.projectanonymous.chatbot.data.aimode

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AiModeWebResult(
    val response: AiModeParsedResponse,
    val finalUrl: String,
    val elapsedMillis: Long
)

class AiModeWebException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Drives a caller-owned, attached WebView. Authentication remains in Android
 * WebView's own session. This class never reads or exports credentials/cookies.
 * Google can change its DOM at any time; this is an unofficial UI integration.
 */
class GoogleAiModeWebViewAdapter(
    private val parser: AiModeResponseParser = AiModeResponseParser(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS
) {
    suspend fun ask(
        webView: WebView,
        prompt: String,
        onProgress: (String) -> Unit = {}
    ): AiModeWebResult = withContext(Dispatchers.Main.immediate) {
        require(prompt.isNotBlank()) { "Prompt tidak boleh kosong." }
        require(timeoutMillis in 10_000L..180_000L) { "Timeout di luar rentang yang didukung." }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = false

        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val startedAt = System.currentTimeMillis()
            val finished = AtomicBoolean(false)
            var lastHtml = ""
            var lastUrl = ""
            var pollTask: Runnable? = null
            var timeoutTask: Runnable? = null
            var poll: (() -> Unit)? = null
            var schedulePoll: ((Long) -> Unit)? = null

            fun finish(result: AiModeWebResult) {
                if (finished.compareAndSet(false, true)) {
                    pollTask?.let(handler::removeCallbacks)
                    timeoutTask?.let(handler::removeCallbacks)
                    if (continuation.isActive) continuation.resume(result)
                }
            }

            fun fail(error: Throwable) {
                if (finished.compareAndSet(false, true)) {
                    pollTask?.let(handler::removeCallbacks)
                    timeoutTask?.let(handler::removeCallbacks)
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }

            schedulePoll = { delay ->
                if (!finished.get()) {
                    val task = Runnable { poll?.invoke() }
                    pollTask = task
                    handler.postDelayed(task, delay.coerceAtLeast(0L))
                }
            }

            poll = {
                if (!finished.get()) {
                    val script = """
                        (function() {
                          const root = document.querySelector('#cnt');
                          const body = (document.body && document.body.innerText || '').toLowerCase();
                          return JSON.stringify({
                            html: root ? root.innerHTML : '',
                            url: location.href,
                            blocked: body.includes('unusual traffic') || location.pathname.includes('/sorry/'),
                            marker: !!(root && root.querySelector('.v4bSkd'))
                          });
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(script) { raw ->
                        if (finished.get()) return@evaluateJavascript
                        try {
                            val decoded = JSONTokener(raw ?: "null").nextValue() as? String
                                ?: throw AiModeWebException("Google belum memberikan dokumen yang dapat dibaca.")
                            val snapshot = JSONObject(decoded)
                            lastUrl = snapshot.optString("url", webView.url.orEmpty())
                            if (snapshot.optBoolean("blocked")) {
                                fail(AiModeWebException("Google menampilkan halaman pembatasan atau verifikasi. Selesaikan langsung di browser Google, lalu coba lagi."))
                                return@evaluateJavascript
                            }

                            lastHtml = snapshot.optString("html", "")
                            val parsed = parser.parse(lastHtml, prompt)
                            if (snapshot.optBoolean("marker") && parsed.hasAnswer &&
                                parsed.completion == AiModeCompletion.CONFIRMED
                            ) {
                                finish(AiModeWebResult(parsed, lastUrl, System.currentTimeMillis() - startedAt))
                            } else {
                                onProgress(if (parsed.hasAnswer) "Menerima jawaban…" else "Menunggu jawaban Google AI Mode…")
                                schedulePoll?.invoke(pollIntervalMillis)
                            }
                        } catch (error: Throwable) {
                            fail(error)
                        }
                    }
                }
            }

            timeoutTask = Runnable {
                val partial = parser.parse(lastHtml, prompt)
                finish(AiModeWebResult(partial, lastUrl, System.currentTimeMillis() - startedAt))
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    lastUrl = url
                    if (url.startsWith("https://www.google.", true) ||
                        url.startsWith("https://google.", true) ||
                        url.contains("accounts.google.com", true)
                    ) {
                        onProgress("Membaca halaman Google…")
                        schedulePoll?.invoke(900L)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame) {
                        fail(AiModeWebException("Halaman Google gagal dimuat: ${error.description}"))
                    }
                }
            }

            continuation.invokeOnCancellation {
                handler.post {
                    if (finished.compareAndSet(false, true)) {
                        pollTask?.let(handler::removeCallbacks)
                        timeoutTask?.let(handler::removeCallbacks)
                        webView.stopLoading()
                    }
                }
            }

            handler.postDelayed(timeoutTask!!, timeoutMillis)
            onProgress("Mengirim pertanyaan ke Google AI Mode…")
            webView.loadUrl(buildSearchUrl(prompt))
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 90_000L
        const val DEFAULT_POLL_INTERVAL_MILLIS = 1_000L

        fun buildSearchUrl(prompt: String): String =
            Uri.parse("https://www.google.com/search").buildUpon()
                .appendQueryParameter("udm", "50")
                .appendQueryParameter("q", prompt.trim())
                .build()
                .toString()
    }
}
