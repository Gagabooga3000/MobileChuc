package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.presentation.viewmodel.ScheduleViewModel
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleFragment : Fragment() {
    
    private val scheduleViewModel: ScheduleViewModel by viewModels()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var groupNameText: TextView
    private lateinit var refreshGroupButton: Button
    
    private lateinit var scheduleAdapter: ScheduleAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        observeViewModel()
        
        // Загружаем расписание
        scheduleViewModel.loadSchedule()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_schedule)
        progressBar = view.findViewById(R.id.progress_bar)
        errorText = view.findViewById(R.id.error_text)
        groupNameText = view.findViewById(R.id.group_name)
        refreshGroupButton = view.findViewById(R.id.refresh_group_button)
        
        // Настраиваем кнопку обновления группы
        refreshGroupButton.setOnClickListener {
            scheduleViewModel.refreshScheduleWithNewGroup()
        }
    }
    
    private fun setupRecyclerView() {
        // Настройка RecyclerView для расписания
        scheduleAdapter = ScheduleAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = scheduleAdapter
    }
    
    private fun observeViewModel() {
        // Наблюдаем за состоянием расписания
        scheduleViewModel.scheduleState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    scheduleAdapter.updateSchedule(state.data)
                    
                    // Отображаем название группы если есть данные
                    if (state.data.isNotEmpty()) {
                        val firstDay = state.data.first()
                        if (!firstDay.groupName.isNullOrBlank()) {
                            groupNameText.text = firstDay.groupName
                            groupNameText.visibility = View.VISIBLE
                        }
                    }
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        })
        
        // Наблюдаем за состоянием загрузки
        scheduleViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
    }
    
    companion object {
        fun newInstance(): ScheduleFragment {
            return ScheduleFragment()
        }
    }
}
