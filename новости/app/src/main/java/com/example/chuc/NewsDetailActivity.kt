package com.example.chuc

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class NewsDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE).orEmpty()
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()

        val titleView: TextView = findViewById(R.id.detail_title)
        val dateView: TextView = findViewById(R.id.detail_date)
        val imageView: ImageView = findViewById(R.id.detail_image)
        val textView: TextView = findViewById(R.id.detail_text)

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
    }

    companion object {
        const val EXTRA_TITLE = "news_title"
        const val EXTRA_DESCRIPTION = "news_description"
        const val EXTRA_IMAGE = "news_image"
        const val EXTRA_DATE = "news_date"
    }
}



