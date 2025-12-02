package com.example.chuc.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.local.OfflineCache
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.mediator.model.MediatorGroupDto
import com.example.chuc.data.mediator.model.MediatorLessonDto
import com.example.chuc.data.mediator.toTimetableDays
import com.example.chuc.data.parser.UniversalScheduleParser
import com.example.chuc.data.parser.toTimetableDaysFromParser
import com.example.chuc.presentation.notifications.AppNotifications
import com.example.chuc.schedule.GroupUiModel
import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val mediatorRepository: MediatorRepository,
    private val offlineCache: OfflineCache,
    private val appNotifications: AppNotifications,
    private val universalScheduleParser: UniversalScheduleParser
) : ViewModel() {

    private val _groupsState = MutableLiveData<UiState<List<GroupUiModel>>>()
    val groupsState: LiveData<UiState<List<GroupUiModel>>> = _groupsState

    private val _scheduleState = MutableLiveData<UiState<List<TimetableDay>>>()
    val scheduleState: LiveData<UiState<List<TimetableDay>>> = _scheduleState

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var currentWeekOffset: Int = 0
        private set

    private var selectedGroup: GroupUiModel? = null
    private var cachedMediatorGroups: List<MediatorGroupDto> = emptyList()

    /** Загрузка ВСЕХ групп с сайта */
    fun loadGroups() {
        viewModelScope.launch {
            _groupsState.value = UiState.Loading
            val mediatorResult = mediatorRepository.getActiveGroups()
            mediatorResult.fold(
                onSuccess = { groups ->
                    cachedMediatorGroups = groups
                    val uiModels = groups
                        .map { it.toUiModel() }
                        .filterActualGroups()
                        .sortedBy { it.code }
                    _groupsState.value = UiState.Success(uiModels)
                },
                onFailure = { error ->
                    _groupsState.value = UiState.Error(
                        error.message ?: "Ошибка загрузки списка групп"
                    )
                }
            )
        }
    }

    /** Загрузка расписания выбранной группы */
    fun loadGroupSchedule(group: GroupUiModel, weekOffset: Int = currentWeekOffset, forceRefresh: Boolean = false) {
        selectedGroup = group
        currentWeekOffset = weekOffset
        
        // Проверяем актуальность кэша для текущей недели (weekOffset == 0)
        val isCurrentWeek = weekOffset == 0
        val isCacheFresh = offlineCache.isScheduleFresh(group.code, weekOffset)
        val shouldForceRefresh = forceRefresh || (isCurrentWeek && !isCacheFresh)
        
        Log.d(TAG, "Loading schedule for group: ${group.code}, weekOffset: $weekOffset, " +
                "isCurrentWeek: $isCurrentWeek, isCacheFresh: $isCacheFresh, shouldForceRefresh: $shouldForceRefresh")
        
        // Если требуется принудительное обновление, очищаем кэш
        if (shouldForceRefresh) {
            Log.d(TAG, "Clearing cache for group: ${group.code} due to force refresh or stale cache")
            offlineCache.clearScheduleCache(group.code)
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _scheduleState.value = UiState.Loading

            val (from, to) = resolveWeekBounds()
            val mediatorGroupId = resolveMediatorGroupId(group)

        // Для текущей недели всегда пробуем сайт, даже если кэш свежий — на сайте расписание обновляется раньше
        val preferParser = forceRefresh || isCurrentWeek || mediatorGroupId == null
            val sourcesToTry = buildList {
                if (preferParser) add(ScheduleSource.PARSER)
                if (mediatorGroupId != null) add(ScheduleSource.MEDIATOR)
                if (!preferParser) add(ScheduleSource.PARSER)
            }.distinct()

            var lastErrorMessage: String? = null

            for (source in sourcesToTry) {
                val result = when (source) {
                    ScheduleSource.MEDIATOR -> {
                        if (mediatorGroupId == null) {
                            continue
                        } else {
                            Log.d(TAG, "Trying mediator source for group ${group.code}")
                            fetchMediatorTimetable(group, mediatorGroupId, from, to)
                        }
                    }
                    ScheduleSource.PARSER -> {
                        Log.d(TAG, "Trying parser source for group ${group.code}")
                        fetchParserTimetable(group)
                    }
                }

                result.fold(
                    onSuccess = { timetable ->
                        if (timetable.isNotEmpty()) {
                            Log.d(TAG, "Schedule loaded from ${source.label}: ${timetable.size} days")
                            offlineCache.saveSchedule(group.code, currentWeekOffset, timetable)
                            _scheduleState.value = UiState.Success(timetable)
                            appNotifications.notifyScheduleUpdated(group.code)
                            _isLoading.value = false
                            return@launch
                        } else {
                            Log.w(TAG, "${source.label} returned empty timetable")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "${source.label} failed: ${error.message}", error)
                        lastErrorMessage = error.message
                    }
                )
            }

            // Если не удалось получить данные ни из одного источника, пробуем кэш
            val cached = offlineCache.loadSchedule(group.code, currentWeekOffset)
            if (!cached.isNullOrEmpty()) {
                Log.d(TAG, "Falling back to cached schedule")
                _scheduleState.value = UiState.Success(cached)
            } else {
                val message = lastErrorMessage ?: "Расписание не найдено"
                _scheduleState.value = UiState.Error(message)
            }

            _isLoading.value = false
        }
    }

    fun nextWeek() {
        selectedGroup?.let {
            currentWeekOffset += 1
            loadGroupSchedule(it, currentWeekOffset)
        }
    }

    fun prevWeek() {
        selectedGroup?.let {
            currentWeekOffset -= 1
            loadGroupSchedule(it, currentWeekOffset)
        }
    }

    fun refreshCurrentSchedule() {
        selectedGroup?.let {
            loadGroupSchedule(it, currentWeekOffset)
        }
    }

    /**
     * Принудительно обновить расписание с очисткой кэша
     */
    fun forceRefreshSchedule() {
        selectedGroup?.let { group ->
            // Загружаем расписание с принудительным обновлением
            loadGroupSchedule(group, currentWeekOffset, forceRefresh = true)
        }
    }

    fun goToDate(date: LocalDate) {
        val baseMonday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetMonday = date
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        currentWeekOffset = ChronoUnit.WEEKS.between(baseMonday, targetMonday).toInt()
        refreshCurrentSchedule()
    }

    private fun resolveWeekBounds(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val startOfWeek = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(currentWeekOffset.toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        return startOfWeek to endOfWeek
    }

    private suspend fun resolveGroupId(groupCode: String?): String? {
        val targetCode = groupCode?.takeUnless { it.isBlank() } ?: return null

        cachedMediatorGroups.firstOrNull {
            it.groupCode.equals(targetCode, ignoreCase = true)
        }?.let { dto ->
            return dto.publicGroupId.takeIf { !it.isNullOrBlank() } ?: dto.id
        }

        val refreshed = mediatorRepository.getActiveGroups()
        refreshed.getOrNull()?.let { groups ->
            cachedMediatorGroups = groups
            return groups.firstOrNull {
                it.groupCode.equals(targetCode, ignoreCase = true)
            }?.let { dto ->
                dto.publicGroupId.takeIf { !it.isNullOrBlank() } ?: dto.id
            }
        }

        return null
    }

    private suspend fun resolveMediatorGroupId(group: GroupUiModel): String? {
        return when {
            !group.publicId.isNullOrBlank() -> group.publicId
            !group.internalId.isNullOrBlank() -> group.internalId
            else -> resolveGroupId(group.code)
        }?.takeIf { it.isNotBlank() }
    }

    private suspend fun fetchGroupScheduleWithFallback(
        from: LocalDate,
        to: LocalDate,
        primaryGroupId: String,
        groupCode: String,
        weekOffset: Int
    ): Result<List<MediatorLessonDto>> {
        val primary = mediatorRepository.getStudentSchedule(
            from = from,
            to = to,
            groupId = primaryGroupId,
            weekOffset = weekOffset
        )

        if (primary.isSuccess) {
            return primary
        }

        val errorMessage = primary.exceptionOrNull()?.message.orEmpty()
        val needsFallback = errorMessage.contains(GROUP_ID_REQUIRED_ERROR, ignoreCase = true)
        val fallbackId = if (needsFallback) {
            groupCode.takeIf { it.isNotBlank() && !it.equals(primaryGroupId, ignoreCase = true) }
        } else {
            null
        }

        return if (fallbackId != null) {
            mediatorRepository.getStudentSchedule(
                from = from,
                to = to,
                groupId = fallbackId,
                weekOffset = weekOffset
            )
        } else {
            primary
        }
    }

    private fun MediatorGroupDto.toUiModel(): GroupUiModel {
        return GroupUiModel(
            code = groupCode,
            publicId = publicGroupId,
            internalId = id,
            startYear = resolveStartYear(),
            isActive = (isActive ?: 1) != 0
        )
    }

    private fun MediatorGroupDto.resolveStartYear(): Int? {
        val codeYear = parseYearFromCode(groupCode)
        if (codeYear != null) return codeYear

        val raw = startYear
        return raw
            ?.takeIf { it >= 1900 }
            ?: normalizeLegacyYear(raw)
    }

    private fun parseYearFromCode(code: String?): Int? {
        if (code.isNullOrBlank()) return null
        val match = YEAR_SUFFIX_REGEX.find(code) ?: return null
        val shortYear = match.value.toIntOrNull() ?: return null
        return if (shortYear >= CENTURY_BREAK) {
            1900 + shortYear
        } else {
            2000 + shortYear
        }
    }

    private fun normalizeLegacyYear(raw: Int?): Int? {
        if (raw == null) return null
        return when {
            raw in 30..99 -> 1900 + raw
            raw in 0..29 -> 2000 + raw
            raw in 35..80 -> 1978 + raw
            else -> null
        }
    }

    private fun List<GroupUiModel>.filterActualGroups(): List<GroupUiModel> {
        val minYear = determineMinAdmissionYear()
        val actual = this.filter { group ->
            val start = group.startYear
            start == null || start >= minYear
        }
        return if (actual.isNotEmpty()) actual else this
    }

    private fun determineMinAdmissionYear(): Int {
        val currentYear = LocalDate.now().year
        return currentYear - (MAX_ACTIVE_COURSE_YEARS - 1)
    }

    companion object {
        private const val TAG = "ScheduleViewModel"
        private const val GROUP_ID_REQUIRED_ERROR = "group_id_required"
        private const val MAX_ACTIVE_COURSE_YEARS = 4
        private const val CENTURY_BREAK = 60
        private val YEAR_SUFFIX_REGEX = "(\\d{2})(?!.*\\d)".toRegex()
    }

    private enum class ScheduleSource(val label: String) {
        MEDIATOR("Mediator API"),
        PARSER("Site parser")
    }

    private suspend fun fetchMediatorTimetable(
        group: GroupUiModel,
        mediatorGroupId: String,
        from: LocalDate,
        to: LocalDate
    ): Result<List<TimetableDay>> {
        return fetchGroupScheduleWithFallback(
            from = from,
            to = to,
            primaryGroupId = mediatorGroupId,
            groupCode = group.code,
            weekOffset = currentWeekOffset
        ).map { lessons ->
            lessons.toTimetableDays(from, to)
        }
    }

    private suspend fun fetchParserTimetable(group: GroupUiModel): Result<List<TimetableDay>> {
        return universalScheduleParser
            .getGroupSchedule(group.code)
            .map { days -> days.toTimetableDaysFromParser(group.code) }
    }
}
