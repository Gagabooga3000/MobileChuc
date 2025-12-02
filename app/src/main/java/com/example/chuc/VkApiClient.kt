package com.example.chuc

import android.net.Uri
import com.example.chuc.data.model.News
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class VkApiClient(
    private val apiVersion: String = "5.199",
    private val baseUrl: String = "https://api.vk.com/method/"
) {
    @Volatile
    private var cachedGroupId: Long? = null

    fun getWallPosts(domain: String, count: Int = 50, offset: Int = 0): List<News> {
        val token = BuildConfig.VK_API_TOKEN
        if (token.isBlank()) return emptyList()

        val groupId = cachedGroupId ?: resolveGroupId(domain, token)?.also { cachedGroupId = it }

        val params = mutableMapOf(
            "count" to count.coerceAtMost(100).toString(),
            "offset" to offset.toString(),
            "filter" to "all",
            "extended" to "1", // Получаем расширенную информацию, включая видео
            "fields" to "id,title,duration,description,date,views,comments,reposts,photo_320,photo_640,photo_800,photo_1280,files,player", // Запрашиваем поля для видео
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
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                android.util.Log.e(
                    "VkApiClient",
                    "VK API ошибка: ${error.optString("error_msg")} (код: ${error.optInt("error_code")})"
                )
                return emptyList()
            }
            val response = json.getJSONObject("response")
            val items = response.getJSONArray("items")
            android.util.Log.d("VkApiClient", "VK API вернул ${items.length()} постов")
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
                val dateStr =
                    if (dateUnix > 0) java.text.SimpleDateFormat("dd.MM.yyyy").format(java.util.Date(dateUnix * 1000)) else ""

                var imageUrl = ""
                var videoUrl = ""
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
                                text = listOf(title, desc).filter { it.isNotEmpty() }
                                    .joinToString("\n").trim()
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
                                    if (w > bestW && u.isNotBlank()) {
                                        bestW = w; bestUrl = u
                                    }
                                }
                                if (bestUrl.isNotBlank()) imageUrl = bestUrl
                            }
                        } else if (a.optString("type") == "video") {
                            val video = a.getJSONObject("video")
                            val title = video.optString("title").trim()
                            if (text.isEmpty()) text = title

                            // Пытаемся получить URL видео
                            if (video.has("files")) {
                                val files = video.getJSONObject("files")
                                val directUrl = files.optString("mp4_1080", "")
                                    .takeIf { it.isNotBlank() }
                                    ?: files.optString("mp4_720", "")
                                        .takeIf { it.isNotBlank() }
                                    ?: files.optString("mp4_480", "")
                                        .takeIf { it.isNotBlank() }
                                    ?: files.optString("mp4_360", "")
                                        .takeIf { it.isNotBlank() }
                                    ?: files.optString("mp4_240", "")
                                        .takeIf { it.isNotBlank() }
                                if (directUrl != null) {
                                    videoUrl = directUrl
                                }
                            }

                            // Если не получили прямую ссылку, пробуем player URL
                            if (videoUrl.isBlank() && video.has("player")) {
                                videoUrl = video.optString("player", "")
                            }

                            // Превью видео
                            if (video.has("image") && imageUrl.isBlank()) {
                                val imageArray = video.getJSONArray("image")
                                if (imageArray.length() > 0) {
                                    val lastImage =
                                        imageArray.getJSONObject(imageArray.length() - 1)
                                    imageUrl = lastImage.optString("url", "")
                                }
                            } else if (video.has("photo_800") && imageUrl.isBlank()) {
                                imageUrl = video.optString("photo_800", "")
                            } else if (video.has("photo_640") && imageUrl.isBlank()) {
                                imageUrl = video.optString("photo_640", "")
                            } else if (video.has("photo_320") && imageUrl.isBlank()) {
                                imageUrl = video.optString("photo_320", "")
                            }

                            // Если нет прямого URL, создаем ссылку на видео в VK
                            if (videoUrl.isBlank()) {
                                val ownerId = video.optLong("owner_id", 0)
                                val vid = video.optLong("id", 0)
                                val accessKey = video.optString("access_key", "")
                                if (ownerId != 0L && vid != 0L) {
                                    val owner =
                                        if (ownerId < 0) "club${-ownerId}" else "id$ownerId"
                                    videoUrl = if (accessKey.isNotBlank()) {
                                        "https://vk.com/$owner?z=video${ownerId}_${vid}_$accessKey"
                                    } else {
                                        "https://vk.com/$owner?z=video${ownerId}_${vid}"
                                    }
                                }
                            }
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
                        content = text,
                        imageUrl = imageUrl.takeIf { it.isNotBlank() },
                        videoUrl = videoUrl.takeIf { it.isNotBlank() },
                        date = dateStr,
                        isImportant = false,
                        category = "VK",
                        author = "VK Group"
                    )
                )
            }
            android.util.Log.d(
                "VkApiClient",
                "Обработано ${list.size} новостей из ${items.length()} постов"
            )
            list
        } catch (e: Exception) {
            android.util.Log.e("VkApiClient", "Ошибка парсинга ответа VK API: ${e.message}", e)
            emptyList()
        }
    }

    private fun shortenTitle(text: String, maxLen: Int = 60): String {
        val oneLine = text.replace("\n", " ").trim()
        return if (oneLine.length <= maxLen) oneLine
        else oneLine.substring(0, maxLen).trimEnd() + "…"
    }

    // =============================================================================================
    //   НОВЫЕ МЕТОДЫ ДЛЯ iframe VK ВИДЕО
    // =============================================================================================

    /**
     * Получает iframe URL для VK видео через API.
     * Формат: https://vk.com/video_ext.php?oid=-123&id=456&hash=...&__ref=vk.api
     */
    fun getVideoIframeUrl(
        ownerId: Long,
        videoId: Long,
        accessKey: String? = null
    ): String? {
        val token = BuildConfig.VK_API_TOKEN
        if (token.isBlank()) {
            android.util.Log.w(
                "VkApiClient",
                "VK API токен не установлен, создаем iframe URL из параметров"
            )
            return createIframeUrl(ownerId, videoId, accessKey)
        }

        val params = mutableMapOf(
            "videos" to "${ownerId}_${videoId}${if (accessKey != null) "_$accessKey" else ""}",
            "access_token" to token,
            "v" to apiVersion
        )

        val url = buildUrl(method = "video.get", params = params)
        android.util.Log.d("VkApiClient", "Запрос iframe URL для видео: $url")
        val body = httpGet(url) ?: return createIframeUrl(ownerId, videoId, accessKey)

        return try {
            val json = JSONObject(body)
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                android.util.Log.e(
                    "VkApiClient",
                    "VK API ошибка при получении видео: " +
                            "${error.optString("error_msg")} (код: ${error.optInt("error_code")})"
                )
                return createIframeUrl(ownerId, videoId, accessKey)
            }
            val response = json.getJSONObject("response")
            val items = response.getJSONArray("items")
            if (items.length() > 0) {
                val video = items.getJSONObject(0)

                val oid = video.optLong("owner_id", ownerId)
                val id = video.optLong("id", videoId)
                val hash = video.optString("access_key", accessKey ?: "")

                android.util.Log.d(
                    "VkApiClient",
                    "Извлечены данные для iframe: oid=$oid, id=$id, hash=$hash"
                )

                createIframeUrl(oid, id, hash.takeIf { it.isNotBlank() })
            } else {
                android.util.Log.w(
                    "VkApiClient",
                    "VK API вернул пустой список видео, создаем iframe URL из параметров"
                )
                createIframeUrl(ownerId, videoId, accessKey)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "VkApiClient",
                "Ошибка парсинга ответа video.get: ${e.message}",
                e
            )
            createIframeUrl(ownerId, videoId, accessKey)
        }
    }

    /**
     * Создает iframe URL из параметров на случай, если API не сработал.
     */
    private fun createIframeUrl(ownerId: Long, videoId: Long, hash: String?): String {
        val hashParam = if (!hash.isNullOrBlank()) "&hash=$hash" else ""
        return "https://vk.com/video_ext.php?oid=$ownerId&id=$videoId$hashParam&__ref=vk.api"
    }

    // =============================================================================================
    //   СТАРЫЙ МЕТОД ПРЯМОЙ ССЫЛКИ (оставляем, он используется VideoView)
    // =============================================================================================

    /**
     * Получает прямую ссылку на видео файл через VK API метод video.get.
     * Если API не доступен, пытается извлечь ссылку из HTML страницы VK.
     */
    fun getVideoDirectUrl(ownerId: Long, videoId: Long, accessKey: String? = null): String? {
        val token = BuildConfig.VK_API_TOKEN
        if (token.isBlank()) {
            android.util.Log.w(
                "VkApiClient",
                "VK API токен не установлен, пробуем извлечь из HTML"
            )
            return extractVideoUrlFromHtml(ownerId, videoId, accessKey)
        }

        val params = mutableMapOf(
            "videos" to "${ownerId}_${videoId}${if (accessKey != null) "_$accessKey" else ""}",
            "access_token" to token,
            "v" to apiVersion
        )

        val url = buildUrl(method = "video.get", params = params)
        android.util.Log.d("VkApiClient", "Запрос прямой ссылки на видео: $url")
        val body = httpGet(url) ?: return extractVideoUrlFromHtml(ownerId, videoId, accessKey)

        return try {
            val json = JSONObject(body)
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                android.util.Log.e(
                    "VkApiClient",
                    "VK API ошибка при получении видео: " +
                            "${error.optString("error_msg")} (код: ${error.optInt("error_code")})"
                )
                return extractVideoUrlFromHtml(ownerId, videoId, accessKey)
            }
            val response = json.getJSONObject("response")
            val items = response.getJSONArray("items")
            if (items.length() > 0) {
                val video = items.getJSONObject(0)
                android.util.Log.d(
                    "VkApiClient",
                    "Получена информация о видео, проверяем наличие files..."
                )

                if (video.has("files")) {
                    val files = video.getJSONObject("files")
                    android.util.Log.d("VkApiClient", "Найдено поле files, извлекаем прямую ссылку...")
                    val directUrl = files.optString("mp4_1080", "")
                        .takeIf { it.isNotBlank() }
                        ?: files.optString("mp4_720", "")
                            .takeIf { it.isNotBlank() }
                        ?: files.optString("mp4_480", "")
                            .takeIf { it.isNotBlank() }
                        ?: files.optString("mp4_360", "")
                            .takeIf { it.isNotBlank() }
                        ?: files.optString("mp4_240", "")
                            .takeIf { it.isNotBlank() }

                    if (directUrl != null) {
                        android.util.Log.d(
                            "VkApiClient",
                            "Получена прямая ссылка на видео: $directUrl"
                        )
                        return directUrl
                    }
                }

                if (video.has("player")) {
                    val playerUrl = video.optString("player", "")
                    android.util.Log.d("VkApiClient", "Найден player URL: $playerUrl")
                    if (playerUrl.isNotBlank()) {
                        if (playerUrl.contains(".mp4") || playerUrl.contains(".m3u8") ||
                            playerUrl.contains(".webm") || playerUrl.contains(".3gp")
                        ) {
                            android.util.Log.d(
                                "VkApiClient",
                                "Player URL содержит прямую ссылку: $playerUrl"
                            )
                            return playerUrl
                        }
                        android.util.Log.d(
                            "VkApiClient",
                            "Используем player URL напрямую: $playerUrl"
                        )
                        return playerUrl
                    }
                }

                android.util.Log.w(
                    "VkApiClient",
                    "Не удалось получить прямую ссылку на видео из API, пробуем HTML"
                )
                extractVideoUrlFromHtml(ownerId, videoId, accessKey)
            } else {
                android.util.Log.w(
                    "VkApiClient",
                    "VK API вернул пустой список видео, пробуем HTML"
                )
                extractVideoUrlFromHtml(ownerId, videoId, accessKey)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "VkApiClient",
                "Ошибка парсинга ответа video.get: ${e.message}",
                e
            )
            extractVideoUrlFromHtml(ownerId, videoId, accessKey)
        }
    }

    /**
     * Извлекает прямую ссылку на видео из HTML страницы VK.
     */
    private fun extractVideoUrlFromHtml(
        ownerId: Long,
        videoId: Long,
        accessKey: String?
    ): String? {
        try {
            val videoPageUrl = if (ownerId < 0) {
                "https://vk.com/club${-ownerId}?z=video${ownerId}_${videoId}${
                    if (accessKey != null) "_$accessKey" else ""
                }"
            } else {
                "https://vk.com/id$ownerId?z=video${ownerId}_${videoId}${
                    if (accessKey != null) "_$accessKey" else ""
                }"
            }

            android.util.Log.d(
                "VkApiClient",
                "Попытка извлечь прямую ссылку из HTML: $videoPageUrl"
            )

            val html = httpGet(videoPageUrl) ?: return null

            val patterns = listOf(
                Regex("""["'](https?://[^"']*\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE),
                Regex("""(https?://[^"'\s]*\.mp4[^"'\s]*)""", RegexOption.IGNORE_CASE),
                Regex("""["'](https?://[^"']*\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
                Regex("""(https?://[^"'\s]*\.m3u8[^"'\s]*)""", RegexOption.IGNORE_CASE),
                Regex(""""url":"(https?://[^"]*\.mp4[^"]*)"""", RegexOption.IGNORE_CASE),
                Regex(""""url":"(https?://[^"]*\.m3u8[^"]*)"""", RegexOption.IGNORE_CASE),
                Regex(""""src":"(https?://[^"]*\.mp4[^"]*)"""", RegexOption.IGNORE_CASE),
                Regex(""""src":"(https?://[^"]*\.m3u8[^"]*)"""", RegexOption.IGNORE_CASE)
            )

            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val url = match.groupValues[1]
                    if (url.contains(".mp4") || url.contains(".m3u8")) {
                        android.util.Log.d("VkApiClient", "Найдена прямая ссылка в HTML: $url")
                        return url
                    }
                }
            }

            android.util.Log.w(
                "VkApiClient",
                "Не удалось найти прямую ссылку в HTML"
            )
            return null
        } catch (e: Exception) {
            android.util.Log.e(
                "VkApiClient",
                "Ошибка извлечения ссылки из HTML: ${e.message}",
                e
            )
            return null
        }
    }
}
