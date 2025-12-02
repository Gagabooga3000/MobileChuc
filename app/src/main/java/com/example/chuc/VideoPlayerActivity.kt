package com.example.chuc

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private val vkApiClient = VkApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: run {
            finish()
            return
        }

        videoView = findViewById(R.id.video_view)
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.video_progress)
        errorText = findViewById(R.id.video_error)

        setupVideoView()
        setupWebView()
        loadVideo(videoUrl)
    }

    // -------------------- Настройка VideoView --------------------

    private fun setupVideoView() {
        videoView.setOnPreparedListener { mediaPlayer ->
            progressBar.visibility = View.GONE
            errorText.visibility = View.GONE
            mediaPlayer.start()
            // Полноэкранный режим
            mediaPlayer.setVideoScalingMode(
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            )
        }

        videoView.setOnErrorListener { _, what, extra ->
            android.util.Log.e(
                "VideoPlayerActivity",
                "Ошибка воспроизведения видео: what=$what, extra=$extra"
            )
            progressBar.visibility = View.GONE
            errorText.visibility = View.VISIBLE
            errorText.text = "Ошибка воспроизведения видео"
            true
        }

        videoView.setOnCompletionListener {
            // Видео завершено
        }
    }

    // -------------------- Настройка WebView для VK iframe --------------------

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            loadsImagesAutomatically = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                android.util.Log.e(
                    "VideoPlayerActivity",
                    "Ошибка загрузки WebView: $description ($errorCode)"
                )
                progressBar.visibility = View.GONE
                errorText.visibility = View.VISIBLE
                errorText.text = "Ошибка загрузки плеера VK"
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    if (progressBar.visibility != View.VISIBLE) {
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // -------------------- Загрузка видео --------------------

    private fun loadVideo(videoUrl: String) {
        android.util.Log.d("VideoPlayerActivity", "Загрузка видео: $videoUrl")
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        // VK-ссылки теперь открываем через iframe в WebView
        if (videoUrl.contains("vk.com")) {
            android.util.Log.d(
                "VideoPlayerActivity",
                "Обнаружена VK ссылка, используем WebView / iframe"
            )

            // Если это уже готовый iframe URL вида video_ext.php — используем напрямую
            if (videoUrl.contains("video_ext.php")) {
                android.util.Log.d(
                    "VideoPlayerActivity",
                    "Обнаружен готовый iframe URL, используем напрямую"
                )
                loadVkIframe(videoUrl)
            } else {
                // Иначе парсим ссылку и получаем iframe URL через VkApiClient
                parseAndLoadVkVideo(videoUrl)
            }
            return
        }

        // Если это прямая ссылка на видео файл
        if (videoUrl.contains(".mp4") || videoUrl.contains(".m3u8") ||
            videoUrl.contains(".webm") || videoUrl.contains(".3gp") ||
            (videoUrl.startsWith("http") && !videoUrl.contains("vk.com"))
        ) {
            android.util.Log.d(
                "VideoPlayerActivity",
                "Прямая ссылка на видео, используем VideoView"
            )
            loadDirectVideo(videoUrl)
        } else {
            // Для других случаев пробуем загрузить как есть через VideoView
            android.util.Log.d("VideoPlayerActivity", "Пробуем загрузить как есть (VideoView)")
            loadDirectVideo(videoUrl)
        }
    }

    // Прямое видео через VideoView
    private fun loadDirectVideo(videoUrl: String) {
        try {
            webView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE

            videoView.setVideoURI(Uri.parse(videoUrl))
            videoView.requestFocus()
        } catch (e: Exception) {
            android.util.Log.e(
                "VideoPlayerActivity",
                "Ошибка загрузки видео: ${e.message}",
                e
            )
            progressBar.visibility = View.GONE
            errorText.visibility = View.VISIBLE
            errorText.text = "Ошибка загрузки видео: ${e.message}"
        }
    }

    // Загрузка VK-видео через iframe
    private fun loadVkIframe(iframeUrl: String) {
        try {
            videoView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE

            val html = """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                </head>
                <body style="margin:0;padding:0;background-color:#000000;">
                    <iframe 
                        src="$iframeUrl"
                        width="100%" 
                        height="100%" 
                        frameborder="0" 
                        allow="autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;" 
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(
                "https://vk.com",
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "VideoPlayerActivity",
                "Ошибка загрузки iframe VK: ${e.message}",
                e
            )
            progressBar.visibility = View.GONE
            errorText.visibility = View.VISIBLE
            errorText.text = "Ошибка загрузки плеера VK"
        }
    }

    // Парсим vk-ссылку и загружаем iframe
    private fun parseAndLoadVkVideo(videoUrl: String) {
        try {
            // Ищем паттерн:
            // video-123_456_accessKey  или  https://vk.com/...z=video-123_456_accessKey
            var videoMatch = Regex("video(-?\\d+)_(\\d+)(?:_([^&\\s]+))?").find(videoUrl)

            // Если не нашли, пробуем без префикса "video"
            if (videoMatch == null) {
                videoMatch = Regex("(-?\\d+)_(\\d+)(?:_([^&\\s]+))?").find(videoUrl)
            }

            if (videoMatch != null) {
                val ownerIdStr = videoMatch.groupValues[1]
                val videoIdStr = videoMatch.groupValues[2]
                val accessKey = videoMatch.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }

                val ownerId = ownerIdStr.toLongOrNull()
                val videoId = videoIdStr.toLongOrNull()

                if (ownerId != null && videoId != null) {
                    android.util.Log.d(
                        "VideoPlayerActivity",
                        "Парсинг VK видео: ownerId=$ownerId, videoId=$videoId, accessKey=$accessKey"
                    )

                    lifecycleScope.launch {
                        try {
                            val iframeUrl = withContext(Dispatchers.IO) {
                                vkApiClient.getVideoIframeUrl(ownerId, videoId, accessKey)
                            }

                            if (!iframeUrl.isNullOrBlank()) {
                                android.util.Log.d(
                                    "VideoPlayerActivity",
                                    "Получен iframe URL: $iframeUrl"
                                )
                                loadVkIframe(iframeUrl)
                            } else {
                                android.util.Log.w(
                                    "VideoPlayerActivity",
                                    "Не удалось получить iframe URL, пробуем исходный URL через VideoView"
                                )
                                loadDirectVideo(videoUrl)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "VideoPlayerActivity",
                                "Ошибка получения iframe URL: ${e.message}",
                                e
                            )
                            loadDirectVideo(videoUrl)
                        }
                    }
                    return
                }
            }

            android.util.Log.w(
                "VideoPlayerActivity",
                "Не удалось распарсить VK ссылку: $videoUrl, пробуем VideoView"
            )
            loadDirectVideo(videoUrl)
        } catch (e: Exception) {
            android.util.Log.e(
                "VideoPlayerActivity",
                "Ошибка парсинга VK ссылки: ${e.message}",
                e
            )
            loadDirectVideo(videoUrl)
        }
    }

    // -------------------- Жизненный цикл --------------------

    override fun onPause() {
        super.onPause()
        if (::videoView.isInitialized) {
            videoView.pause()
        }
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::videoView.isInitialized && videoView.canSeekForward()) {
            videoView.resume()
        }
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onDestroy() {
        if (::videoView.isInitialized) {
            videoView.stopPlayback()
        }
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
    }
}
