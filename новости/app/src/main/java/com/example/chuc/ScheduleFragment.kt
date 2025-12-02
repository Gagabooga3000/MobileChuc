package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.schedule.TimetableAdapter
import com.example.chuc.schedule.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScheduleFragment : Fragment() {
    private val repository = TimetableRepository()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var adapter: TimetableAdapter
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.schedule, container, false)
    }
    
    companion object {
        fun newInstance(): ScheduleFragment {
            return ScheduleFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.schedule_list)
        progressBar = view.findViewById(R.id.schedule_progress)
        errorView = view.findViewById(R.id.schedule_error)

        adapter = TimetableAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadTimetable()
    }

    private fun loadTimetable(groupQuery: String? = null) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            showLoading()
            try {
                val days = repository.load(groupQuery)
                val lessons = days.flatMap { it.lessons }
                if (lessons.isEmpty()) {
                    showError("Пары не найдены. Проверьте сайт или группу.")
                } else {
                    adapter.submitList(lessons)
                    showContent()
                }
            } catch (e: Exception) {
                showError(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        errorView.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        errorView.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorView.text = message
        errorView.visibility = View.VISIBLE
    }
}
