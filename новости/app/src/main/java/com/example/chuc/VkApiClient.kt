package com.example.chuc

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class VkApiClient(
    private val apiVersion: String = "5.199",
    private val baseUrl: String = "https://api.vk.com/method/"
) {
    @Volatile private var cachedGroupId: Long? = null

    fun getWallPosts(domain: String, count: Int = 50, offset: Int = 0): List<News> {
        val token = BuildConfig.VK_API_TOKEN
        if (token.isBlank()) return emptyList()

        val groupId = cachedGroupId ?: resolveGroupId(domain, token)?.also { cachedGroupId = it }

        val params = mutableMapOf(
            "count" to count.coerceAtMost(100).toString(),
            "offset" to offset.toString(),
            "filter" to "all",
            "access_token" to token,
            "v" to apiVersion
        )
        if (groupId != null) {
            params["owner_id"] = (-groupId).toString() // owner_id для групп отрицательный
        } else {
            params["domain"] = domain
        }

        val url = buildUrl(method = "wall.get", params = params)

        val body = httpGet(url) ?: return emptyList()
        return parseWallResponse(body)
    }

    private fun resolveGroupId(domain: String, token: String): Long? {
        val url = buildUrl(
            method = "groups.getById",
            params = mapOf(
                "group_ids" to domain,
                "access_token" to token,
                "v" to apiVersion
            )
        )
        val body = httpGet(url) ?: return null
        return try {
            val json = JSONObject(body)
            if (json.has("error")) return null
            val resp = json.getJSONArray("response")
            if (resp.length() > 0) resp.getJSONObject(0).optLong("id") else null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildUrl(method: String, params: Map<String, String>): String {
        val builder = Uri.parse(baseUrl + method).buildUpon()
        params.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        return builder.build().toString()
    }

    private fun httpGet(urlStr: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept-Language", "ru,en;q=0.9")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseWallResponse(jsonStr: String): List<News> {
        return try {
            val json = JSONObject(jsonStr)
            if (json.has("error")) return emptyList()
            val response = json.getJSONObject("response")
            val items = response.getJSONArray("items")
            val list = mutableListOf<News>()
            var idCounter = 1
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                var text = item.optString("text").trim()
                // include repost text if original has none
                if (text.isEmpty() && item.has("copy_history")) {
                    val ch = item.getJSONArray("copy_history")
                    if (ch.length() > 0) text = ch.getJSONObject(0).optString("text").trim()
                }
                val dateUnix = item.optLong("date", 0L)
                val dateStr = if (dateUnix > 0) java.text.SimpleDateFormat("dd.MM.yyyy").format(java.util.Date(dateUnix * 1000)) else ""

                var imageUrl = ""
                if (item.has("attachments")) {
                    val att = item.getJSONArray("attachments")
                    for (j in 0 until att.length()) {
                        val a = att.getJSONObject(j)
                        if (a.optString("type") == "photo") {
                            val photo = a.getJSONObject("photo")
                            if (photo.has("sizes")) {
                                val sizes = photo.getJSONArray("sizes")
                                // pick the largest
                                var bestW = 0
                                var bestUrl = ""
                                for (k in 0 until sizes.length()) {
                                    val s = sizes.getJSONObject(k)
                                    val w = s.optInt("width", 0)
                                    val u = s.optString("url")
                                    if (w > bestW && u.isNotBlank()) {
                                        bestW = w
                                        bestUrl = u
                                    }
                                }
                                if (bestUrl.isNotBlank()) {
                                    imageUrl = bestUrl
                                    // keep scanning in case we want first available only
                                }
                            }
                        } else if (a.optString("type") == "link") {
                            val link = a.getJSONObject("link")
                            val title = link.optString("title").trim()
                            val desc = link.optString("description").trim()
                            if (text.isEmpty()) {
                                text = listOf(title, desc).filter { it.isNotEmpty() }.joinToString("\n").trim()
                            }
                            val photo = link.optJSONObject("photo")
                            if (photo != null && photo.has("sizes") && imageUrl.isBlank()) {
                                val sizes = photo.getJSONArray("sizes")
                                var bestW = 0
                                var bestUrl = ""
                                for (k in 0 until sizes.length()) {
                                    val s = sizes.getJSONObject(k)
                                    val w = s.optInt("width", 0)
                                    val u = s.optString("url")
                                    if (w > bestW && u.isNotBlank()) { bestW = w; bestUrl = u }
                                }
                                if (bestUrl.isNotBlank()) imageUrl = bestUrl
                            }
                        } else if (a.optString("type") == "video") {
                            val video = a.getJSONObject("video")
                            val title = video.optString("title").trim()
                            if (text.isEmpty()) text = title
                        }
                    }
                }
                if (text.isEmpty()) text = "Пост без текста"

                val ownerId = item.optLong("owner_id")
                val postId = item.optLong("id")
                val url = if (ownerId != 0L && postId != 0L) {
                    val owner = if (ownerId < 0) "club${-ownerId}" else "id$ownerId"
                    "https://vk.com/$owner?w=wall${ownerId}_$postId"
                } else ""

                list.add(
                    News(
                        id = idCounter++,
                        title = shortenTitle(text),
                        description = text,
                        imageUrl = imageUrl,
                        date = dateStr,
                        url = url,
                        isImportant = false
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun shortenTitle(text: String, maxLen: Int = 60): String {
        val oneLine = text.replace("\n", " ").trim()
        return if (oneLine.length <= maxLen) oneLine else oneLine.substring(0, maxLen).trimEnd() + "…"
    }
}


