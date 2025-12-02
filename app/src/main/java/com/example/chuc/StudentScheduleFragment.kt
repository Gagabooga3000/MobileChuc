package com.example.chuc

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chuc.R
import com.example.chuc.data.local.SchedulePreferences
import com.example.chuc.data.local.ScheduleViewMode
import com.example.chuc.domain.model.UserProfileStorage
import com.example.chuc.domain.model.UserRole
import com.example.chuc.presentation.viewmodel.ScheduleViewModel
import com.example.chuc.schedule.GroupUiModel
import com.example.chuc.schedule.UiState
import com.example.chuc.ui.bindWeekTabs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentScheduleFragment : Fragment() {

    private val scheduleViewModel: ScheduleViewModel by viewModels()

    @javax.inject.Inject
    lateinit var schedulePreferences: SchedulePreferences

    private lateinit var headerTitle: TextView
    private lateinit var prevWeekButton: TextView
    private lateinit var nextWeekButton: TextView
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var scheduleErrorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var dayTabs: com.google.android.material.tabs.TabLayout
    private lateinit var calendarGestureDetector: GestureDetectorCompat

    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecycler()
        observe()

        val profile = UserProfileStorage.load(requireContext())

        if (profile?.role != UserRole.STUDENT || profile.groupName.isNullOrBlank()) {
            scheduleErrorText.visibility = View.VISIBLE
            scheduleErrorText.text = "Группа не выбрана.\nЗайдите в раздел «Расписание» для преподавателя и выберите свою группу."
            return
        }

        val groupModel = GroupUiModel(code = profile.groupName!!)
        scheduleViewModel.loadGroupSchedule(groupModel, weekOffset = 0)
    }

    override fun onResume() {
        super.onResume()
        // При возврате на экран проверяем и обновляем расписание, если оно устарело
        scheduleViewModel.forceRefreshSchedule()
    }

    private fun initViews(view: View) {
        headerTitle = view.findViewById(R.id.header_title_student)
        prevWeekButton = view.findViewById(R.id.prev_week_student)
        nextWeekButton = view.findViewById(R.id.next_week_student)
        scheduleRecyclerView = view.findViewById(R.id.recycler_view_schedule_student)
        scheduleErrorText = view.findViewById(R.id.schedule_error_text_student)
        progressBar = view.findViewById(R.id.schedule_progress_student)
        swipeRefreshLayout = view.findViewById(R.id.schedule_swipe_refresh_student)
        dayTabs = view.findViewById(R.id.day_tabs_student)

        prevWeekButton.setOnClickListener { scheduleViewModel.prevWeek() }
        nextWeekButton.setOnClickListener { scheduleViewModel.nextWeek() }

        swipeRefreshLayout.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary)
        swipeRefreshLayout.setOnRefreshListener {
            scheduleViewModel.forceRefreshSchedule()
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

    private fun setupRecycler() {
        scheduleAdapter = ScheduleAdapter()
        scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleRecyclerView.adapter = scheduleAdapter
        scheduleRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
    }

    private fun observe() {
        scheduleViewModel.scheduleState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing) {
                        progressBar.visibility = View.VISIBLE
                    }
                    scheduleErrorText.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
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
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    scheduleErrorText.visibility = View.VISIBLE
                    scheduleErrorText.text = state.message
                }
            }
        }
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
        fun newInstance(): StudentScheduleFragment = StudentScheduleFragment()
    }
}
