package com.example.chuc.data.repository

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
    private val newsParser: NewsParser
) {
    
    suspend fun getNews(): Result<List<News>> = withContext(Dispatchers.IO) {
        try {
            val response = webService.getNews()
            
            if (response.isSuccessful) {
                val html = response.body()?.string() ?: ""
                val news = newsParser.parseNews(html)
                Result.success(news)
            } else {
                Result.failure(Exception("Ошибка загрузки новостей: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

