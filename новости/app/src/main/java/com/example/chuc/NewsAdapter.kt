package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NewsAdapter(private var newsList: MutableList<News>) : 
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
        
        // Загружаем изображение (или дефолт)
        if (news.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(news.imageUrl)
                .placeholder(R.drawable.chucimg)
                .error(R.drawable.chucimg)
                .into(holder.newsImage)
        } else {
            holder.newsImage.setImageResource(R.drawable.chucimg)
        }
        
        // Обработчик клика на "Читать далее" — открываем экран с полным постом
        holder.readMoreBtn.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, NewsDetailActivity::class.java).apply {
                putExtra(NewsDetailActivity.EXTRA_TITLE, news.title)
                putExtra(NewsDetailActivity.EXTRA_DESCRIPTION, news.description)
                putExtra(NewsDetailActivity.EXTRA_IMAGE, news.imageUrl)
                putExtra(NewsDetailActivity.EXTRA_DATE, news.date)
            }
            context.startActivity(intent)
        }
        
        // Обработчик клика на всю карточку
        holder.itemView.setOnClickListener {
            // Здесь можно открыть детальный просмотр новости
        }
    }
    
    override fun getItemCount(): Int = newsList.size

    fun updateNews(newNewsList: List<News>) {
        newsList.clear()
        newsList.addAll(newNewsList)
        notifyDataSetChanged()
    }

    fun appendNews(more: List<News>) {
        val start = newsList.size
        newsList.addAll(more)
        notifyItemRangeInserted(start, more.size)
    }
}
