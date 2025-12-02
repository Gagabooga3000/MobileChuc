package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chuc.R
import com.example.chuc.data.model.News
import com.example.chuc.presentation.viewmodel.NewsViewModel
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private val newsViewModel: NewsViewModel by viewModels()
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var refreshButton: Button
    
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
        setupRefreshControls(view)
        observeViewModel()
        
        // Загружаем новости при создании фрагмента
        newsViewModel.loadNews()
    }
    
    private fun setupRecyclerView(view: View) {
        newsRecyclerView = view.findViewById(R.id.news_recycler_view)
        newsAdapter = NewsAdapter(emptyList())

        newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
        }
    }

    private fun setupRefreshControls(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.home_swipe_refresh)
        refreshButton = view.findViewById(R.id.home_refresh_button)
        swipeRefreshLayout.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary)
        swipeRefreshLayout.setOnRefreshListener {
            newsViewModel.loadNews()
        }
        refreshButton.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            newsViewModel.loadNews()
        }
    }
    
    private fun observeViewModel() {
        newsViewModel.newsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    swipeRefreshLayout.isRefreshing = true
                }
                is UiState.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    newsAdapter.updateNews(state.data)
                }
                is UiState.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    newsAdapter.updateNews(getFallbackNews())
                }
            }
        })
    }
    
    private fun getFallbackNews(): List<News> {
        return listOf(
            News(
                id = 1,
                title = "Открытие нового учебного года",
                description = "1 сентября состоялось торжественное открытие нового учебного года. Студентов поздравили с началом учебы и пожелали успехов в освоении новых знаний.",
                imageUrl = null,
                date = "01.09.2025",
                isImportant = true
            ),
            News(
                id = 2,
                title = "Научная конференция студентов",
                description = "15 октября пройдет ежегодная научная конференция студентов. Приглашаем всех желающих принять участие и представить свои исследовательские работы.",
                imageUrl = null,
                date = "15.10.2025",
                isImportant = false
            ),
            News(
                id = 3,
                title = "Спортивные соревнования",
                description = "В университете проходят межфакультетские спортивные соревнования. Поддержите свою команду!",
                imageUrl = null,
                date = "20.09.2025",
                isImportant = false
            ),
            News(
                id = 4,
                title = "Стипендиальная программа",
                description = "Объявлен конкурс на получение повышенной стипендии. Подробности на сайте университета.",
                imageUrl = null,
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
