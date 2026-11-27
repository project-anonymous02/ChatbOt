package com.projectanonymous.chatbot.data.aimode

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class AiModeCitation(
    val title: String,
    val url: String,
    val domain: String
)

enum class AiModeCompletion {
    CONFIRMED,
    NOT_CONFIRMED
}

data class AiModeParsedResponse(
    val answer: String,
    val citations: List<AiModeCitation>,
    val completion: AiModeCompletion,
    val answerContainerFound: Boolean
) {
    val hasAnswer: Boolean get() = answer.isNotBlank()
}

/**
 * Parses rendered Google AI Mode HTML snapshots.
 *
 * Selectors are compatibility hints, not a guarantee that Google's current
 * page uses the same DOM. The parser therefore has ordered fallbacks and
 * reports completion separately instead of guessing from a non-empty answer.
 */
class AiModeResponseParser {

    fun parse(html: String, prompt: String = ""): AiModeParsedResponse {
        if (html.isBlank()) {
            return AiModeParsedResponse(
                answer = "",
                citations = emptyList(),
                completion = AiModeCompletion.NOT_CONFIRMED,
                answerContainerFound = false
            )
        }

        val document = Jsoup.parse(html)
        val reply = document.selectFirst("div.mZJni.Dn7Fzd")
        val conversation = reply?.closest("div.tonYlb")
            ?: document.select("div.tonYlb").lastOrNull()

        val answerElement = reply
            ?: conversation?.selectFirst("div.mZJni.Dn7Fzd")
            ?: document.selectFirst("#cnt")
            ?: document.body()

        val citationScope = conversation ?: answerElement
        val completionFound = (conversation ?: answerElement)
            .selectFirst("div.v4bSkd") != null

        val answer = extractAnswer(answerElement, prompt)
        val citations = extractCitations(citationScope)

        return AiModeParsedResponse(
            answer = answer,
            citations = citations,
            completion = if (completionFound) {
                AiModeCompletion.CONFIRMED
            } else {
                AiModeCompletion.NOT_CONFIRMED
            },
            answerContainerFound = answerElement !== document.body()
        )
    }

    private fun extractAnswer(source: Element, prompt: String): String {
        val clean = source.clone()
        clean.select(
            "script, style, noscript, nav, footer, button, input, textarea, " +
                "svg, [aria-hidden=true], .ZFcyjd, .DBd2Wb"
        ).remove()

        val lines = clean.text().split(Regex("\\s*\\n\\s*"))
            .map { it.replace(WHITESPACE, " ").trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        while (lines.firstOrNull()?.lowercase(Locale.ROOT) in LEADING_CHROME) {
            lines.removeAt(0)
        }

        val normalizedPrompt = prompt.trim().replace(WHITESPACE, " ")
        if (normalizedPrompt.isNotEmpty() &&
            lines.firstOrNull()?.equals(normalizedPrompt, ignoreCase = true) == true
        ) {
            lines.removeAt(0)
        }

        val disclaimerIndex = lines.indexOfFirst { line ->
            TRAILING_CHROME.any { marker ->
                line.contains(marker, ignoreCase = true)
            }
        }
        if (disclaimerIndex >= 0) {
            while (lines.size > disclaimerIndex) {
                lines.removeAt(lines.lastIndex)
            }
        }

        while (lines.lastOrNull()?.lowercase(Locale.ROOT) in TRAILING_UI) {
            lines.removeAt(lines.lastIndex)
        }

        return lines.joinToString("\n").trim()
    }

    private fun extractCitations(scope: Element): List<AiModeCitation> {
        val candidates = scope.select("a[href]")
            .filter { anchor ->
                val target = anchor.attr("target")
                val hasGoogleResultStamp = anchor.hasAttr("data-ved")
                target.equals("_blank", ignoreCase = true) && hasGoogleResultStamp
            }
            .ifEmpty {
                scope.select("a[href]")
            }

        val citations = LinkedHashMap<String, AiModeCitation>()
        candidates.take(MAX_CITATIONS).forEach { anchor ->
            val url = normalizeExternalUrl(anchor.attr("href")) ?: return@forEach
            val domain = runCatching { URI(url).host.orEmpty() }
                .getOrDefault("")
                .removePrefix("www.")
            if (domain.isBlank()) return@forEach

            val aria = anchor.attr("aria-label")
                .replace("Opens in new tab.", "", ignoreCase = true)
                .trim()
            val title = (anchor.text().trim().ifBlank { aria })
                .ifBlank { domain }
                .take(MAX_TITLE_LENGTH)

            val existing = citations[url]
            if (existing == null || (title.length > existing.title.length && title != domain)) {
                citations[url] = AiModeCitation(title = title, url = url, domain = domain)
            }
        }
        return citations.values.toList()
    }

    private fun normalizeExternalUrl(href: String): String? {
        if (href.isBlank()) return null

        val unwrapped = runCatching {
            val uri = URI(href)
            if (uri.path == "/url" && uri.host?.contains("google.") == true) {
                val params = uri.rawQuery.orEmpty().split("&")
                    .mapNotNull { part ->
                        val pair = part.split("=", limit = 2)
                        if (pair.size == 2) pair[0] to pair[1] else null
                    }.toMap()
                val target = params["q"] ?: params["url"]
                target?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: href
            } else {
                href
            }
        }.getOrDefault(href)

        val uri = runCatching { URI(unwrapped) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (isGoogleInternalHost(host)) return null
        return uri.toASCIIString()
    }

    private fun isGoogleInternalHost(host: String): Boolean {
        val normalized = host.removePrefix("www.")
        return GOOGLE_INTERNAL_HOSTS.any { normalized == it || normalized.endsWith(".$it") }
    }

    private companion object {
        const val MAX_CITATIONS = 80
        const val MAX_TITLE_LENGTH = 300

        val WHITESPACE = Regex("\\s+")
        val LEADING_CHROME = setOf(
            "ai mode", "all", "images", "videos", "news", "more",
            "search results", "you said:"
        )
        val TRAILING_CHROME = listOf(
            "AI can make mistakes",
            "Generative AI is experimental"
        )
        val TRAILING_UI = setOf("show all", "show more", "show less", "view all", "")

        val GOOGLE_INTERNAL_HOSTS = setOf(
            "google.com", "google.co.id", "google.ca", "google.co.uk",
            "googleusercontent.com", "gstatic.com", "googleapis.com",
            "support.google.com", "policies.google.com", "accounts.google.com"
        )
    }
}
