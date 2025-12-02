package com.example.chuc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chuc.R
import com.example.chuc.data.local.SchedulePreferences
import com.example.chuc.data.local.ScheduleViewMode
import com.example.chuc.presentation.viewmodel.ScheduleViewModel
import com.example.chuc.schedule.GroupUiModel
import com.example.chuc.schedule.UiState
import com.example.chuc.ui.bindWeekTabs
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId

@AndroidEntryPoint
class ScheduleFragment : Fragment() {

    private val scheduleViewModel: ScheduleViewModel by viewModels()

    @javax.inject.Inject
    lateinit var schedulePreferences: SchedulePreferences

    // список групп
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var groupsAdapter: GroupsAdapter
    private lateinit var groupsProgressBar: ProgressBar
    private lateinit var groupsErrorText: TextView
    private lateinit var searchEditText: AppCompatEditText
    private lateinit var searchContainer: View

    // расписание
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var scheduleContainer: View
    private lateinit var scheduleErrorText: TextView
    private lateinit var scheduleProgressBar: ProgressBar
    private lateinit var prevWeekButton: TextView
    private lateinit var nextWeekButton: TextView
    private lateinit var headerTitle: TextView
    private lateinit var backButton: TextView
    private lateinit var scheduleSwipeRefresh: SwipeRefreshLayout
    private lateinit var dayTabs: com.google.android.material.tabs.TabLayout
    private lateinit var calendarGestureDetector: GestureDetectorCompat

    private var allGroups: List<GroupUiModel> = emptyList()

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
        setupRecyclerViews()
        observeViewModel()

        scheduleViewModel.loadGroups()
    }

    override fun onResume() {
        super.onResume()
        // При возврате на экран проверяем и обновляем расписание, если оно устарело
        if (scheduleContainer.visibility == View.VISIBLE) {
            scheduleViewModel.forceRefreshSchedule()
        }
    }

    private fun initViews(view: View) {
        groupsRecyclerView = view.findViewById(R.id.recycler_view_groups)
        groupsProgressBar = view.findViewById(R.id.groups_progress)
        groupsErrorText = view.findViewById(R.id.groups_error)
        searchEditText = view.findViewById(R.id.search_group_edit_text)
        searchContainer = view.findViewById(R.id.search_container)

        scheduleRecyclerView = view.findViewById(R.id.recycler_view_schedule)
        scheduleContainer = view.findViewById(R.id.schedule_container)
        scheduleErrorText = view.findViewById(R.id.error_text)
        scheduleProgressBar = view.findViewById(R.id.progress_bar)
        scheduleSwipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.schedule_swipe_refresh)
        prevWeekButton = view.findViewById(R.id.prev_week)
        nextWeekButton = view.findViewById(R.id.next_week)
        headerTitle = view.findViewById(R.id.header_title)
        backButton = view.findViewById(R.id.back_button)
        dayTabs = view.findViewById(R.id.day_tabs)

        scheduleSwipeRefresh.isEnabled = false
        scheduleSwipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary)
        scheduleSwipeRefresh.setOnRefreshListener {
            scheduleViewModel.forceRefreshSchedule()
        }

        // Поиск по группам
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterGroups(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Кнопка "назад" — вернуться к списку групп
        backButton.setOnClickListener {
            scheduleContainer.visibility = View.GONE
            backButton.visibility = View.GONE
            scheduleErrorText.visibility = View.GONE
            groupsRecyclerView.visibility = View.VISIBLE
            searchContainer.visibility = View.VISIBLE
            headerTitle.text = "Расписание"
            scheduleSwipeRefresh.isEnabled = false
            scheduleSwipeRefresh.isRefreshing = false
        }

        prevWeekButton.setOnClickListener {
            scheduleViewModel.prevWeek()
        }

        nextWeekButton.setOnClickListener {
            scheduleViewModel.nextWeek()
        }

        // Жесты свайпа по календарю для смены недели
        calendarGestureDetector = GestureDetectorCompat(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 == null) return false
                    val deltaX = e2.x - e1.x
                    if (kotlin.math.abs(deltaX) < 100 || kotlin.math.abs(velocityX) < 200) return false
                    if (deltaX < 0) {
                        scheduleViewModel.nextWeek()
                    } else {
                        scheduleViewModel.prevWeek()
                    }
                    return true
                }
            })
        dayTabs.setOnTouchListener { _, event ->
            calendarGestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupRecyclerViews() {
        // список групп
        groupsAdapter = GroupsAdapter(emptyList()) { group ->
            // при выборе группы загружаем её расписание
            headerTitle.text = group.code
            searchContainer.visibility = View.GONE
            groupsRecyclerView.visibility = View.GONE
            groupsErrorText.visibility = View.GONE
            scheduleContainer.visibility = View.VISIBLE
            scheduleSwipeRefresh.isEnabled = true
            scheduleViewModel.loadGroupSchedule(group, weekOffset = 0)
        }
        groupsRecyclerView.layoutManager = LinearLayoutManager(context)
        groupsRecyclerView.adapter = groupsAdapter
        groupsRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)

        // расписание группы
        scheduleAdapter = ScheduleAdapter()
        scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleRecyclerView.adapter = scheduleAdapter
        scheduleRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
    }

    private fun observeViewModel() {
        // список групп
        scheduleViewModel.groupsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    groupsProgressBar.visibility = View.VISIBLE
                    groupsErrorText.visibility = View.GONE
                    groupsRecyclerView.visibility = View.GONE
                }
                is UiState.Success -> {
                    groupsProgressBar.visibility = View.GONE
                    groupsErrorText.visibility = View.GONE
                    groupsRecyclerView.visibility = View.VISIBLE

                    allGroups = state.data
                    filterGroups(searchEditText.text?.toString().orEmpty())
                }
                is UiState.Error -> {
                    groupsProgressBar.visibility = View.GONE
                    groupsRecyclerView.visibility = View.GONE
                    groupsErrorText.visibility = View.VISIBLE
                    groupsErrorText.text = state.message
                }
            }
        })

        // расписание выбранной группы
        scheduleViewModel.scheduleState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    if (!scheduleSwipeRefresh.isRefreshing) {
                        scheduleProgressBar.visibility = View.VISIBLE
                    }
                }
                is UiState.Success -> {
                    scheduleProgressBar.visibility = View.GONE
                    scheduleSwipeRefresh.isRefreshing = false
                    scheduleContainer.visibility = View.VISIBLE
                    backButton.visibility = View.VISIBLE
                    scheduleErrorText.visibility = View.GONE

                    val mode = schedulePreferences.getViewMode()
                    if (mode == ScheduleViewMode.CALENDAR) {
                        setupDayTabs(state.data)
                    } else {
                        dayTabs.visibility = View.GONE
                        scheduleAdapter.updateSchedule(state.data)
                    }
                }
                is UiState.Error -> {
                    scheduleProgressBar.visibility = View.GONE
                    scheduleSwipeRefresh.isRefreshing = false
                    scheduleSwipeRefresh.isEnabled = false
                    scheduleContainer.visibility = View.GONE
                    scheduleErrorText.visibility = View.VISIBLE
                    scheduleErrorText.text = state.message
                    groupsRecyclerView.visibility = View.VISIBLE
                    searchContainer.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun filterGroups(query: String) {
        if (allGroups.isEmpty()) return

        val q = query.lowercase().trim()
        val filtered = if (q.isEmpty()) {
            allGroups
        } else {
            allGroups.filter { it.code.lowercase().contains(q) }
        }

        groupsAdapter.updateGroups(filtered)
    }

    private fun setupDayTabs(days: List<com.example.chuc.schedule.TimetableDay>) {
        if (days.isEmpty()) {
            dayTabs.visibility = View.GONE
            scheduleAdapter.updateSchedule(emptyList())
            return
        }

        val inflater = LayoutInflater.from(requireContext())
        dayTabs.bindWeekTabs(days, inflater) { selectedDay ->
            scheduleAdapter.updateSchedule(listOf(selectedDay))
        }
    }

    companion object {
        fun newInstance(): ScheduleFragment = ScheduleFragment()
    }
}
