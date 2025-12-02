package com.example.chuc

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chuc.data.model.News

class NewsAdapter(private var newsList: List<News>) : 
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
    
    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val newsImage: ImageView = itemView.findViewById(R.id.news_image)
        val videoPlayIcon: ImageView = itemView.findViewById(R.id.video_play_icon)
        val newsTitle: TextView = itemView.findViewById(R.id.news_title)
        val newsDescription: TextView = itemView.findViewById(R.id.news_description)
        val newsDate: TextView = itemView.findViewById(R.id.news_date)
        val readMoreBtn: TextView = itemView.findViewById(R.id.read_more_btn)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        
        holder.newsTitle.text = news.title
        holder.newsDescription.text = news.description
        holder.newsDate.text = news.date
        
        // Загружаем изображение с помощью Glide
        if (!news.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(news.imageUrl)
                .placeholder(R.drawable.chucimg)
                .error(R.drawable.chucimg)
                .into(holder.newsImage)
        } else {
            holder.newsImage.setImageResource(R.drawable.chucimg)
        }
        
        // Показываем иконку воспроизведения, если есть видео
        if (!news.videoUrl.isNullOrEmpty()) {
            holder.videoPlayIcon.visibility = View.VISIBLE
            // Обработчик клика на иконку воспроизведения - открывает видео
            holder.videoPlayIcon.setOnClickListener { view ->
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                openVideo(holder.itemView.context, news.videoUrl!!)
            }
            // Обработчик клика на изображение видео - открывает видео
            holder.newsImage.setOnClickListener { view ->
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                openVideo(holder.itemView.context, news.videoUrl!!)
            }
        } else {
            holder.videoPlayIcon.visibility = View.GONE
            // Если нет видео, клик на изображение открывает детали
            holder.newsImage.setOnClickListener(null)
        }
        
        // Обработчик клика на "Читать далее" - всегда открывает детали новости
        holder.readMoreBtn.setOnClickListener {
            openNewsDetail(holder.itemView.context, news)
        }
        
        // Обработчик клика на всю карточку - всегда открывает детали новости
        holder.itemView.setOnClickListener {
            openNewsDetail(holder.itemView.context, news)
        }
    }
    
    override fun getItemCount(): Int = newsList.size
    
    fun updateNews(newNewsList: List<News>) {
        android.util.Log.d("NewsAdapter", "updateNews вызван с ${newNewsList.size} новостями")
        newsList = newNewsList
        android.util.Log.d("NewsAdapter", "newsList обновлен, размер: ${newsList.size}")
        notifyDataSetChanged()
        android.util.Log.d("NewsAdapter", "getItemCount после обновления: ${itemCount}")
    }
    
    private fun openNewsDetail(context: android.content.Context, news: News) {
        val intent = Intent(context, NewsDetailActivity::class.java).apply {
            putExtra(NewsDetailActivity.EXTRA_TITLE, news.title)
            putExtra(NewsDetailActivity.EXTRA_DESCRIPTION, news.content ?: news.description)
            putExtra(NewsDetailActivity.EXTRA_IMAGE, news.imageUrl ?: "")
            putExtra(NewsDetailActivity.EXTRA_VIDEO, news.videoUrl ?: "")
            putExtra(NewsDetailActivity.EXTRA_DATE, news.date)
        }
        context.startActivity(intent)
    }
    
    private fun openVideo(context: android.content.Context, videoUrl: String) {
        try {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("NewsAdapter", "Ошибка открытия видео: ${e.message}", e)
        }
    }
}
