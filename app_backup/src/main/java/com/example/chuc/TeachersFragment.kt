package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.presentation.viewmodel.TeachersViewModel
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeachersFragment : Fragment() {
    
    private val teachersViewModel: TeachersViewModel by viewModels()
    
    private lateinit var teachersRecyclerView: RecyclerView
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var scheduleErrorText: TextView
    
    private lateinit var teachersAdapter: TeachersAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teachers, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerViews()
        observeViewModel()
        
        // Загружаем список преподавателей
        teachersViewModel.loadTeachers()
    }
    
    private fun initViews(view: View) {
        teachersRecyclerView = view.findViewById(R.id.recycler_view_teachers)
        scheduleRecyclerView = view.findViewById(R.id.recycler_view_schedule)
        progressBar = view.findViewById(R.id.progress_bar)
        errorText = view.findViewById(R.id.error_text)
        scheduleErrorText = view.findViewById(R.id.schedule_error_text)
    }
    
    private fun setupRecyclerViews() {
        // Настройка RecyclerView для преподавателей
        teachersAdapter = TeachersAdapter(emptyList()) { teacherName ->
            teachersViewModel.loadTeacherSchedule(teacherName)
        }
        teachersRecyclerView.layoutManager = LinearLayoutManager(context)
        teachersRecyclerView.adapter = teachersAdapter
        
        // Настройка RecyclerView для расписания преподавателя
        scheduleAdapter = ScheduleAdapter()
        scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleRecyclerView.adapter = scheduleAdapter
    }
    
    private fun observeViewModel() {
        // Наблюдаем за состоянием списка преподавателей
        teachersViewModel.teachersState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    teachersRecyclerView.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    teachersRecyclerView.visibility = View.VISIBLE
                    teachersAdapter.updateTeachers(state.data)
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    teachersRecyclerView.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        })
        
        // Наблюдаем за состоянием расписания преподавателя
        teachersViewModel.teacherScheduleState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    scheduleRecyclerView.visibility = View.GONE
                    scheduleErrorText.visibility = View.GONE
                }
                is UiState.Success -> {
                    scheduleRecyclerView.visibility = View.VISIBLE
                    scheduleErrorText.visibility = View.GONE
                    scheduleAdapter.updateSchedule(state.data)
                }
                is UiState.Error -> {
                    scheduleRecyclerView.visibility = View.GONE
                    scheduleErrorText.visibility = View.VISIBLE
                    scheduleErrorText.text = state.message
                }
            }
        })
        
        // Наблюдаем за состоянием загрузки
        teachersViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Можно добавить дополнительную логику для отображения загрузки расписания
        })
    }
    
    companion object {
        fun newInstance(): TeachersFragment {
            return TeachersFragment()
        }
    }
}

