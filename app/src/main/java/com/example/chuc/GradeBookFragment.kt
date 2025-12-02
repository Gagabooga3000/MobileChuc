package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.presentation.viewmodel.ProfileViewModel
import com.example.chuc.presentation.viewmodel.JournalSubjectUiModel
import com.example.chuc.schedule.GradeBook
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradeBookFragment : Fragment() {
    
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var gradeBookRecyclerView: RecyclerView
    private lateinit var gradeBookProgress: ProgressBar
    private lateinit var gradeBookError: TextView
    private lateinit var gradeBookSummary: LinearLayout
    private lateinit var passedCountText: TextView
    private lateinit var notPassedCountText: TextView
    private lateinit var averageGradeText: TextView
    private lateinit var forecastText: TextView
    private lateinit var gradeBookHeader: LinearLayout
    private lateinit var gradeBookAdapter: GradeBookAdapter
    private lateinit var backButton: ImageView
    private lateinit var subjectSpinner: Spinner
    private lateinit var subjectLabel: TextView
    private var subjectItems: List<JournalSubjectUiModel> = emptyList()
    private var suppressSubjectCallback = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grade_book, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализируем ViewModel
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        
        // Находим элементы UI
        gradeBookRecyclerView = view.findViewById(R.id.grade_book_recycler_view)
        gradeBookProgress = view.findViewById(R.id.grade_book_progress)
        gradeBookError = view.findViewById(R.id.grade_book_error)
        gradeBookSummary = view.findViewById(R.id.grade_book_summary)
        passedCountText = view.findViewById(R.id.passed_count)
        notPassedCountText = view.findViewById(R.id.not_passed_count)
        averageGradeText = view.findViewById(R.id.average_grade_text)
        forecastText = view.findViewById(R.id.forecast_text)
        gradeBookHeader = view.findViewById(R.id.grade_book_header)
        backButton = view.findViewById(R.id.back_button)
        subjectSpinner = view.findViewById(R.id.subject_spinner)
        subjectLabel = view.findViewById(R.id.subject_filter_label)
        
        // Настройка RecyclerView для зачетной книжки
        gradeBookAdapter = GradeBookAdapter(emptyList())
        gradeBookRecyclerView.layoutManager = LinearLayoutManager(context).apply {
            isItemPrefetchEnabled = true
        }
        gradeBookRecyclerView.adapter = gradeBookAdapter
        gradeBookRecyclerView.setHasFixedSize(false)

        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSubjectCallback) return
                if (position in subjectItems.indices) {
                    val selected = subjectItems[position]
                    profileViewModel.loadGradeBook(selected.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ignore
            }
        }
        
        // Обработчик кнопки "Назад"
        backButton.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                requireActivity().onBackPressed()
            }
        }
        
        // Наблюдаем за состоянием зачетной книжки ПЕРЕД загрузкой
        // Это гарантирует, что мы получим все обновления состояния
        profileViewModel.gradeBookState.observe(viewLifecycleOwner, Observer { state ->
            android.util.Log.d("GradeBookFragment", "Grade book state changed: ${state.javaClass.simpleName}")
            when (state) {
                is UiState.Loading -> {
                    android.util.Log.d("GradeBookFragment", "State: Loading")
                    gradeBookProgress.visibility = View.VISIBLE
                    gradeBookError.visibility = View.GONE
                    gradeBookRecyclerView.visibility = View.GONE
                    gradeBookSummary.visibility = View.GONE
                    gradeBookHeader.visibility = View.GONE
                }
                is UiState.Success -> {
                    android.util.Log.d("GradeBookFragment", "State: Success, entries count: ${state.data.entries.size}")
                    gradeBookProgress.visibility = View.GONE
                    gradeBookError.visibility = View.GONE
                    displayGradeBook(state.data)
                }
                is UiState.Error -> {
                    android.util.Log.e("GradeBookFragment", "State: Error - ${state.message}")
                    gradeBookProgress.visibility = View.GONE
                    gradeBookError.visibility = View.VISIBLE
                    gradeBookError.text = state.message
                    gradeBookRecyclerView.visibility = View.GONE
                    gradeBookSummary.visibility = View.GONE
                    gradeBookHeader.visibility = View.GONE
                }
            }
        })

        profileViewModel.subjectsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    subjectSpinner.visibility = View.GONE
                    subjectLabel.visibility = View.GONE
                }
                is UiState.Success -> {
                    subjectItems = state.data
                    suppressSubjectCallback = true
                    val names = subjectItems.map { it.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    subjectSpinner.adapter = adapter
                    subjectSpinner.visibility = View.VISIBLE
                    subjectLabel.visibility = View.VISIBLE
                    subjectSpinner.setSelection(0, false)
                    suppressSubjectCallback = false
                }
                is UiState.Error -> {
                    subjectSpinner.visibility = View.GONE
                    subjectLabel.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
        
        // Проверяем текущее состояние после установки Observer
        profileViewModel.gradeBookState.value?.let { currentState ->
            android.util.Log.d("GradeBookFragment", "Current state after observer: ${currentState.javaClass.simpleName}")
            when (currentState) {
                is UiState.Success -> {
                    android.util.Log.d("GradeBookFragment", "Already have data: ${currentState.data.entries.size} entries")
                    displayGradeBook(currentState.data)
                }
                is UiState.Loading -> {
                    android.util.Log.d("GradeBookFragment", "Already loading")
                }
                is UiState.Error -> {
                    android.util.Log.e("GradeBookFragment", "Already have error: ${currentState.message}")
                }
            }
        }
        
        // Загружаем зачетную книжку если еще не загружена
        profileViewModel.loadJournalSubjects()
        profileViewModel.loadGradeBook()
    }
    
    private fun displayGradeBook(gradeBook: GradeBook) {
        android.util.Log.d("GradeBookFragment", "displayGradeBook called with ${gradeBook.entries.size} entries")
        
        if (gradeBook.entries.isEmpty()) {
            android.util.Log.w("GradeBookFragment", "Grade book entries are empty")
            gradeBookError.visibility = View.VISIBLE
            gradeBookError.text = "Зачетная книжка пуста"
            gradeBookRecyclerView.visibility = View.GONE
            gradeBookSummary.visibility = View.GONE
            gradeBookHeader.visibility = View.GONE
            return
        }
        
        android.util.Log.d("GradeBookFragment", "Updating adapter with ${gradeBook.entries.size} entries")
        
        // Обновляем адаптер
        gradeBookAdapter.updateEntries(gradeBook.entries)
        
        android.util.Log.d("GradeBookFragment", "Adapter item count: ${gradeBookAdapter.itemCount}")
        android.util.Log.d("GradeBookFragment", "RecyclerView visibility before: ${gradeBookRecyclerView.visibility}")
        android.util.Log.d("GradeBookFragment", "RecyclerView isShown: ${gradeBookRecyclerView.isShown}")
        
        // Отображаем сводку если есть
        gradeBook.summary?.let { summary ->
            // Форматируем текст в формате "сдано(а) X дисциплин(a)"
            val passedText = "сдано(а) ${summary.passed} дисциплин(a)"
            val notPassedText = "не сдано(а) ${summary.notPassed} дисциплин(a)"
            
            passedCountText.text = passedText
            notPassedCountText.text = notPassedText

            // Средний балл
            if (summary.averageGrade != null) {
                val avg = String.format("%.2f", summary.averageGrade)
                averageGradeText.text = "Средний балл: $avg"
                averageGradeText.visibility = View.VISIBLE
            } else {
                averageGradeText.visibility = View.GONE
            }

            // Прогноз сессии
            if (!summary.forecast.isNullOrBlank()) {
                forecastText.text = summary.forecast
                forecastText.visibility = View.VISIBLE
            } else {
                forecastText.visibility = View.GONE
            }

            gradeBookSummary.visibility = View.VISIBLE
        } ?: run {
            gradeBookSummary.visibility = View.GONE
        }
        
        // Убеждаемся, что заголовок виден
        gradeBookHeader.visibility = View.VISIBLE
        
        // Убеждаемся, что RecyclerView виден и обновлен
        if (gradeBookRecyclerView.visibility != View.VISIBLE) {
            gradeBookRecyclerView.visibility = View.VISIBLE
            // Принудительно обновляем layout после изменения видимости
            gradeBookRecyclerView.post {
                gradeBookRecyclerView.parent?.let { parent ->
                    if (parent is ViewGroup) {
                        parent.invalidate()
                        parent.requestLayout()
                    }
                }
                gradeBookRecyclerView.invalidate()
                gradeBookRecyclerView.requestLayout()
            }
        }
        
        // Убеждаемся, что адаптер обновлен
        gradeBookRecyclerView.post {
            gradeBookRecyclerView.adapter?.notifyDataSetChanged()
            // Прокручиваем к началу списка
            gradeBookRecyclerView.scrollToPosition(0)
        }
        
        android.util.Log.d("GradeBookFragment", "RecyclerView visibility after: ${gradeBookRecyclerView.visibility}")
        android.util.Log.d("GradeBookFragment", "RecyclerView height: ${gradeBookRecyclerView.height}")
        android.util.Log.d("GradeBookFragment", "RecyclerView measured height: ${gradeBookRecyclerView.measuredHeight}")
    }
    
    companion object {
        fun newInstance(): GradeBookFragment {
            return GradeBookFragment()
        }
    }
}

