package com.example.chuc.data.parser

import com.example.chuc.data.model.News
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsParser @Inject constructor() {
    
    fun parseNews(html: String): List<News> {
        val doc: Document = Jsoup.parse(html)
        val newsList = mutableListOf<News>()
        
        try {
            // Поиск новостей по различным селекторам
            val newsElements = doc.select(".news, .news-item, .article, .post")
            
            if (newsElements.isEmpty()) {
                // Fallback: поиск в списках или таблицах
                val lists = doc.select("ul, ol, table")
                parseNewsFromLists(lists, newsList)
            } else {
                parseNewsElements(newsElements, newsList)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return newsList
    }
    
    fun parseNewsDetail(html: String, newsId: Int): News {
        val doc: Document = Jsoup.parse(html)
        
        val title = doc.selectFirst("h1, .title, .news-title")?.text()?.trim() ?: "Новость"
        val content = doc.selectFirst(".content, .article-content, .news-content")?.text()?.trim() ?: ""
        val date = doc.selectFirst(".date, .published, .time")?.text()?.trim() ?: ""
        val author = doc.selectFirst(".author, .by")?.text()?.trim()
        val imageUrl = doc.selectFirst("img")?.attr("src")
        
        return News(
            id = newsId,
            title = title,
            description = content.take(200) + if (content.length > 200) "..." else "",
            content = content,
            imageUrl = imageUrl,
            date = date,
            author = author
        )
    }
    
    private fun parseNewsElements(elements: org.jsoup.select.Elements, newsList: MutableList<News>) {
        for ((index, element) in elements.withIndex()) {
            val news = parseNewsElement(element, index + 1)
            if (news != null) {
                newsList.add(news)
            }
        }
    }
    
    private fun parseNewsFromLists(lists: org.jsoup.select.Elements, newsList: MutableList<News>) {
        for (list in lists) {
            val items = list.select("li, tr")
            
            for ((index, item) in items.withIndex()) {
                val news = parseNewsItem(item, index + 1)
                if (news != null) {
                    newsList.add(news)
                }
            }
        }
    }
    
    private fun parseNewsElement(element: org.jsoup.nodes.Element, id: Int): News? {
        val title = element.selectFirst("h1, h2, h3, .title, .news-title, a")?.text()?.trim()
        val description = element.selectFirst(".description, .summary, .excerpt, p")?.text()?.trim()
        val date = element.selectFirst(".date, .published, .time")?.text()?.trim()
        val imageUrl = element.selectFirst("img")?.attr("src")
        val link = element.selectFirst("a")?.attr("href")
        
        if (title.isNullOrEmpty()) return null
        
        return News(
            id = id,
            title = title,
            description = description ?: "",
            imageUrl = imageUrl,
            date = date ?: "",
            isImportant = element.hasClass("important") || element.hasClass("urgent")
        )
    }
    
    private fun parseNewsItem(item: org.jsoup.nodes.Element, id: Int): News? {
        val text = item.text().trim()
        if (text.isEmpty()) return null
        
        // Простое разделение текста на заголовок и описание
        val parts = text.split("\n", limit = 2)
        val title = parts[0].trim()
        val description = if (parts.size > 1) parts[1].trim() else ""
        
        return News(
            id = id,
            title = title,
            description = description,
            date = ""
        )
    }
}

