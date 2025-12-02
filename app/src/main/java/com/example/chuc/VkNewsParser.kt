package com.example.chuc

import com.example.chuc.data.model.News
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class VkNewsParser(
    private val groupUrl: String = "https://vk.com/chuc_che?from=groups"
) {
    fun fetchLatestNews(limit: Int = 50): List<News> {
        val document = fetchDocument(groupUrl)
        if (document == null) return emptyList()

        val posts = mutableListOf<News>()

        // Try several common VK post selectors (desktop/mobile)
        val candidates = document.select("div._post, div.post, div.Post, div.feed_row, article, div.post--with-likes, div[itemtype='http://schema.org/Article']")
        var idCounter = 1
        for (postEl in candidates) {
            val text = extractText(postEl)
            if (text.isBlank()) continue
            val imageUrl = extractImage(postEl)
            val dateText = extractDate(postEl)

            posts.add(
                News(
                    id = idCounter++,
                    title = shortenTitle(text),
                    description = text,
                    content = text,
                    imageUrl = imageUrl,
                    date = dateText,
                    isImportant = false,
                    category = "VK",
                    author = "VK Group"
                )
            )

            if (posts.size >= limit) break
        }

        return posts
    }

    private fun fetchDocument(url: String): Document? {
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                .referrer("https://www.google.com")
                .timeout(15_000)
                .get()
        } catch (_: Exception) {
            // Try mobile site as fallback
            val fallbacks = listOf(
                url.replace("https://vk.com", "https://m.vk.com"),
                // Reader proxy to bypass JS/anti-bot. Returns simplified HTML.
                url.replace("https://vk.com", "https://r.jina.ai/http://vk.com"),
                url.replace("https://vk.com", "https://r.jina.ai/http://m.vk.com")
            )
            for (fb in fallbacks) {
                try {
                    return Jsoup.connect(fb)
                        .userAgent("Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                        .header("Accept-Language", "ru,en;q=0.9")
                        .referrer("https://www.google.com")
                        .timeout(15_000)
                        .get()
                } catch (_: Exception) {
                    // try next fallback
                }
            }
            null
        }
    }

    private fun extractText(postEl: Element): String {
        // Common content containers
        val textSelectors = listOf(
            ".wall_post_text",
            ".post_content",
            "div[data-post-content]",
            ".pi_text",
            ".copy_quote_text",
            "article p",
            "div.post__text",
            "div.section p"
        )
        for (selector in textSelectors) {
            val textEl = postEl.selectFirst(selector)
            if (textEl != null) {
                val text = textEl.text().trim()
                if (text.isNotBlank()) return text
            }
        }
        // Fallback: take any paragraph text inside the post
        val fallback = postEl.select("p").joinToString("\n") { it.text() }.trim()
        return fallback
    }

    private fun extractImage(postEl: Element): String {
        // Prefer high-res images if present
        val img = postEl.select("img[src]").firstOrNull()
        val src = img?.attr("abs:src") ?: img?.attr("src") ?: ""
        return src
    }

    private fun extractDate(postEl: Element): String {
        // VK often uses time or a_link elements
        val timeEl = postEl.selectFirst("time")
        if (timeEl != null) {
            val datetime = timeEl.attr("datetime").ifBlank { timeEl.attr("title") }
            if (datetime.isNotBlank()) return datetime
        }
        val linkTime = postEl.selectFirst("a.time, a.rel_date, span.rel_date, .post_date, .wi_date")
        if (linkTime != null) return linkTime.text().trim()
        return ""
    }

    private fun shortenTitle(text: String, maxLen: Int = 60): String {
        val oneLine = text.replace("\n", " ").trim()
        return if (oneLine.length <= maxLen) oneLine else oneLine.substring(0, maxLen).trimEnd() + "…"
    }
}
