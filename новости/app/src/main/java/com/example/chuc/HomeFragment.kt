package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private var isLoading = false
    private var nextOffset = 0
    private val pageSize = 100
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        loadNewsFromVkApi()
    }
    
    private fun setupRecyclerView(view: View) {
        newsRecyclerView = view.findViewById(R.id.news_recycler_view)
        newsAdapter = NewsAdapter(getNewsList().toMutableList())
        
        newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val visible = lm.childCount
                    val total = lm.itemCount
                    val first = lm.findFirstVisibleItemPosition()
                    if (!isLoading && (visible + first) >= total - 4) {
                        loadMore()
                    }
                }
            })
        }
    }
    
    private fun loadVkNews() {
        lifecycleScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                try {
                    VkNewsParser().fetchLatestNews(limit = 20)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (parsed.isNotEmpty()) {
                newsAdapter.updateNews(parsed)
            } else {
                newsAdapter.updateNews(getNewsList())
            }
        }
    }

    private fun loadNewsFromVkApi() {
        lifecycleScope.launch {
            val apiList = withContext(Dispatchers.IO) {
                try {
                    nextOffset = 0
                    VkApiClient().getWallPosts(domain = "chuc_che", count = pageSize, offset = nextOffset)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (apiList.isNotEmpty()) {
                newsAdapter.updateNews(apiList)
                nextOffset += apiList.size
            } else {
                loadVkNews() // fallback to HTML parsing
            }
        }
    }

    private fun loadMore() {
        isLoading = true
        lifecycleScope.launch {
            val more = withContext(Dispatchers.IO) {
                try {
                    VkApiClient().getWallPosts(domain = "chuc_che", count = pageSize, offset = nextOffset)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (more.isNotEmpty()) {
                newsAdapter.appendNews(more)
                nextOffset += more.size
            }
            isLoading = false
        }
    }
    
    private fun getNewsList(): List<News> {
        return listOf(
            News(
                id = 1,
                title = "Открытие нового учебного года",
                description = "1 сентября состоялось торжественное открытие нового учебного года. Студентов поздравили с началом учебы и пожелали успехов в освоении новых знаний.",
                imageUrl = "",
                date = "01.09.2025",
                isImportant = true
            ),
            News(
                id = 2,
                title = "Научная конференция студентов",
                description = "15 октября пройдет ежегодная научная конференция студентов. Приглашаем всех желающих принять участие и представить свои исследовательские работы.",
                imageUrl = "",
                date = "15.10.2025",
                isImportant = false
            ),
            News(
                id = 3,
                title = "Спортивные соревнования",
                description = "В университете проходят межфакультетские спортивные соревнования. Поддержите свою команду!",
                imageUrl = "",
                date = "20.09.2025",
                isImportant = false
            ),
            News(
                id = 4,
                title = "Стипендиальная программа",
                description = "Объявлен конкурс на получение повышенной стипендии. Подробности на сайте университета.",
                imageUrl = "",
                date = "18.09.2025",
                isImportant = true
            )
        )
    }
    
    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
