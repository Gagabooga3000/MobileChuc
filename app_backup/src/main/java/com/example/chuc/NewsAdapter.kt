package com.example.chuc

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
        
        // Обработчик клика на "Читать далее"
        holder.readMoreBtn.setOnClickListener {
            // Здесь можно открыть детальный просмотр новости
            // Например, запустить новую активность или фрагмент
        }
        
        // Обработчик клика на всю карточку
        holder.itemView.setOnClickListener {
            // Здесь можно открыть детальный просмотр новости
        }
    }
    
    override fun getItemCount(): Int = newsList.size
    
    fun updateNews(newNewsList: List<News>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}
