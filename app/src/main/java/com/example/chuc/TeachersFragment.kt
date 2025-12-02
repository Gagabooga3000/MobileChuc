package com.example.chuc

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.R
import com.example.chuc.data.local.SchedulePreferences
import com.example.chuc.data.local.ScheduleViewMode
import com.example.chuc.presentation.viewmodel.TeachersViewModel
import com.example.chuc.schedule.TeacherUiModel
import com.example.chuc.schedule.UiState
import com.example.chuc.ui.bindWeekTabs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeachersFragment : Fragment() {

    private val teachersViewModel: TeachersViewModel by viewModels()

    @javax.inject.Inject
    lateinit var schedulePreferences: SchedulePreferences

    private lateinit var teachersRecyclerView: RecyclerView
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var scheduleErrorText: TextView
    private lateinit var backButton: TextView
    private lateinit var scheduleContainer: View
    private lateinit var prevWeekTeacher: TextView
    private lateinit var nextWeekTeacher: TextView
    private lateinit var headerTitle: TextView
    private lateinit var searchEditText: AppCompatEditText
    private lateinit var searchContainer: View
    private lateinit var dayTabsTeachers: com.google.android.material.tabs.TabLayout
    private lateinit var calendarGestureDetector: GestureDetectorCompat

    private lateinit var teachersAdapter: TeachersAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter

    private var allTeachers: List<TeacherUiModel> = emptyList()

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
        backButton = view.findViewById(R.id.back_button)
        scheduleContainer = view.findViewById(R.id.schedule_container)
        prevWeekTeacher = view.findViewById(R.id.prev_week_teacher)
        nextWeekTeacher = view.findViewById(R.id.next_week_teacher)
        headerTitle = view.findViewById(R.id.header_title)
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchContainer = view.findViewById(R.id.search_container)
        dayTabsTeachers = view.findViewById(R.id.day_tabs_teachers)

        // Настройка поиска
        setupSearch()

        // Кнопка "Назад"
        backButton.setOnClickListener {
            // Скрываем расписание и показываем список преподавателей
            scheduleContainer.visibility = View.GONE
            scheduleErrorText.visibility = View.VISIBLE
            teachersRecyclerView.visibility = View.VISIBLE
            searchContainer.visibility = View.VISIBLE

            // Возвращаем заголовок
            headerTitle.text = "Преподаватели"

            // Сбрасываем поиск и клавиатуру
            searchEditText.setText("")
            searchEditText.clearFocus()
            hideKeyboard()

            // Показываем полный список преподавателей
            filterTeachers("")
        }

        prevWeekTeacher.setOnClickListener {
            teachersViewModel.prevWeek()
        }
        nextWeekTeacher.setOnClickListener {
            teachersViewModel.nextWeek()
        }

        // Жесты свайпа по календарю для смены недели
        calendarGestureDetector = GestureDetectorCompat(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 == null) return false
                    val deltaX = e2.x - e1.x
                    if (kotlin.math.abs(deltaX) < 100 || kotlin.math.abs(velocityX) < 200) return false
                    if (deltaX < 0) {
                        teachersViewModel.nextWeek()
                    } else {
                        teachersViewModel.prevWeek()
                    }
                    return true
                }
            })
        dayTabsTeachers.setOnTouchListener { _, event ->
            calendarGestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupRecyclerViews() {
        // Список преподавателей
        teachersAdapter = TeachersAdapter(emptyList()) { teacher ->
            // Обновляем заголовок
            headerTitle.text = teacher.name

            // Скрываем поиск
            searchContainer.visibility = View.GONE

            // Убираем фокус и клавиатуру
            searchEditText.clearFocus()
            hideKeyboard()

            // Загружаем расписание выбранного препода
            teachersViewModel.loadTeacherSchedule(teacher)
        }
        teachersRecyclerView.layoutManager = LinearLayoutManager(context)
        teachersRecyclerView.adapter = teachersAdapter
        teachersRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)

        // Расписание преподавателя
        scheduleAdapter = ScheduleAdapter()
        scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleRecyclerView.adapter = scheduleAdapter
        scheduleRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
    }

    private fun observeViewModel() {
        // Состояние списка преподавателей
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

                    allTeachers = state.data
                    filterTeachers(searchEditText.text?.toString() ?: "")
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    teachersRecyclerView.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        })

        // Состояние расписания преподавателя
        teachersViewModel.teacherScheduleState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    scheduleContainer.visibility = View.GONE
                    scheduleErrorText.visibility = View.GONE

                    // Пока грузим — показываем список
                    teachersRecyclerView.visibility = View.VISIBLE
                    // Поиск уже скрыт после выбора преподавателя
                    searchContainer.visibility = View.GONE
                }
                is UiState.Success -> {
                    // Показать только расписание
                    teachersRecyclerView.visibility = View.GONE
                    searchContainer.visibility = View.GONE
                    scheduleContainer.visibility = View.VISIBLE
                    scheduleErrorText.visibility = View.GONE

                    val mode = schedulePreferences.getViewMode()
                    if (mode == ScheduleViewMode.CALENDAR) {
                        setupDayTabs(state.data)
                    } else {
                        dayTabsTeachers.visibility = View.GONE
                        scheduleAdapter.updateSchedule(state.data)
                    }
                }
                is UiState.Error -> {
                    // При ошибке — вернуть список и поиск
                    teachersRecyclerView.visibility = View.VISIBLE
                    searchContainer.visibility = View.VISIBLE
                    scheduleContainer.visibility = View.GONE
                    scheduleErrorText.visibility = View.VISIBLE
                    scheduleErrorText.text = state.message
                }
            }
        })

        teachersViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            // Доп. индикацию можно добавить при желании
        })
    }

    private fun setupDayTabs(days: List<com.example.chuc.schedule.TimetableDay>) {
        if (days.isEmpty()) {
            dayTabsTeachers.visibility = View.GONE
            scheduleAdapter.updateSchedule(emptyList())
            return
        }
        val inflater = LayoutInflater.from(requireContext())
        dayTabsTeachers.bindWeekTabs(days, inflater) { selectedDay ->
            scheduleAdapter.updateSchedule(listOf(selectedDay))
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTeachers(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterTeachers(query: String) {
        if (allTeachers.isEmpty()) return

        val filtered = if (query.isBlank()) {
            allTeachers
        } else {
            val queryLower = query.lowercase().trim()
            allTeachers.filter { teacher ->
                val parts = teacher.name.split(" ")
                if (parts.isNotEmpty()) {
                    parts[0].lowercase().contains(queryLower)
                } else {
                    teacher.name.lowercase().contains(queryLower)
                }
            }
        }

        teachersAdapter.updateTeachers(filtered)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = view ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        fun newInstance(): TeachersFragment = TeachersFragment()
    }
}
