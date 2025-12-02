package com.example.chuc.data.repository

import android.util.Log
import com.example.chuc.VkApiClient
import com.example.chuc.VkNewsParser
import com.example.chuc.data.local.OfflineCache
import com.example.chuc.data.model.News
import com.example.chuc.data.network.WebService
import com.example.chuc.data.parser.NewsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val webService: WebService,
    private val newsParser: NewsParser,
    private val vkApiClient: VkApiClient,
    private val vkNewsParser: VkNewsParser,
    private val offlineCache: OfflineCache
) {
    
    suspend fun getNews(): Result<List<News>> = withContext(Dispatchers.IO) {
        try {
            Log.d("NewsRepository", "Начинаем загрузку новостей...")
            
            // Сначала пробуем загрузить новости через VK API
            val vkNews = try {
                Log.d("NewsRepository", "Пробуем VK API...")
                val news = vkApiClient.getWallPosts("chuc_che", 50, 0)
                Log.d("NewsRepository", "VK API вернул ${news.size} новостей")
                news
            } catch (e: Exception) {
                Log.e("NewsRepository", "VK API ошибка: ${e.message}")
                emptyList()
            }
            
            // Если VK API не сработал, пробуем парсер VK
            val parsedNews = if (vkNews.isEmpty()) {
                try {
                    Log.d("NewsRepository", "Пробуем VK парсер...")
                    val news = vkNewsParser.fetchLatestNews(50)
                    Log.d("NewsRepository", "VK парсер вернул ${news.size} новостей")
                    news
                } catch (e: Exception) {
                    Log.e("NewsRepository", "VK парсер ошибка: ${e.message}")
                    emptyList()
                }
            } else {
                vkNews
            }
            
            // Если VK не сработал, пробуем основной сайт
            if (parsedNews.isEmpty()) {
                Log.d("NewsRepository", "Пробуем основной сайт...")
                val response = webService.getNews()
                
                if (response.isSuccessful) {
                    val html = response.body()?.string() ?: ""
                    val news = newsParser.parseNews(html)
                    Log.d("NewsRepository", "Основной сайт вернул ${news.size} новостей")
                    offlineCache.saveNews(news)
                    Result.success(news)
                } else {
                    Log.e("NewsRepository", "Основной сайт ошибка: ${response.code()}")
                    val cached = offlineCache.loadNews()
                    if (!cached.isNullOrEmpty()) {
                        Log.w("NewsRepository", "Возвращаем кэш новостей после ошибки сайта, размер: ${cached.size}")
                        Result.success(cached)
                    } else {
                        Result.failure(Exception("Ошибка загрузки новостей: ${response.code()}"))
                    }
                }
            } else {
                Log.d("NewsRepository", "Возвращаем VK новости: ${parsedNews.size}")
                offlineCache.saveNews(parsedNews)
                Result.success(parsedNews)
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Общая ошибка: ${e.message}")
            val cached = offlineCache.loadNews()
            if (!cached.isNullOrEmpty()) {
                Log.w("NewsRepository", "Возвращаем кэш новостей после общей ошибки, размер: ${cached.size}")
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getNewsDetail(newsId: Int): Result<News> = withContext(Dispatchers.IO) {
        try {
            val response = webService.getNewsDetail(newsId)
            
            if (response.isSuccessful) {
                val html = response.body()?.string() ?: ""
                val news = newsParser.parseNewsDetail(html, newsId)
                Result.success(news)
            } else {
                Result.failure(Exception("Ошибка загрузки детальной новости: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

