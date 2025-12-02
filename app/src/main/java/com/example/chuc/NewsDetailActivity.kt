package com.example.chuc

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide

class NewsDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE).orEmpty()
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO).orEmpty()
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()

        val titleView: TextView = findViewById(R.id.detail_title)
        val dateView: TextView = findViewById(R.id.detail_date)
        val imageView: ImageView = findViewById(R.id.detail_image)
        val textView: TextView = findViewById(R.id.detail_text)
        val playVideoButton: AppCompatButton = findViewById(R.id.play_video_button)

        titleView.text = title
        dateView.text = date
        textView.text = description

        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.chucimg)
                .error(R.drawable.chucimg)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.chucimg)
        }

        // Показываем кнопку воспроизведения, если есть видео
        if (videoUrl.isNotEmpty()) {
            playVideoButton.visibility = View.VISIBLE
            playVideoButton.setOnClickListener {
                openVideo(videoUrl)
            }
            // Также можно кликнуть на изображение для воспроизведения
            imageView.setOnClickListener {
                openVideo(videoUrl)
            }
        } else {
            playVideoButton.visibility = View.GONE
        }
    }

    private fun openVideo(videoUrl: String) {
        try {
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("NewsDetailActivity", "Ошибка открытия видео: ${e.message}", e)
        }
    }

    companion object {
        const val EXTRA_TITLE = "news_title"
        const val EXTRA_DESCRIPTION = "news_description"
        const val EXTRA_IMAGE = "news_image"
        const val EXTRA_VIDEO = "news_video"
        const val EXTRA_DATE = "news_date"
    }
}
