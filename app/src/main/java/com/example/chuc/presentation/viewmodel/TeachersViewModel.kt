package com.example.chuc.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.local.OfflineCache
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.mediator.model.MediatorTeacherDto
import com.example.chuc.data.mediator.toTimetableDays
import com.example.chuc.domain.model.UserProfileStorage
import com.example.chuc.domain.model.UserRole
import com.example.chuc.schedule.TeacherUiModel
import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TeachersViewModel @Inject constructor(
    private val mediatorRepository: MediatorRepository,
    private val authPreferences: AuthPreferences,
    private val offlineCache: OfflineCache,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private var weekOffset: Int = 0
    private var selectedTeacher: TeacherUiModel? = null
    private var studentTeacherIds: Set<String>? = null
    private var studentTeacherNames: Set<String>? = null
    private var isStudentCacheReady: Boolean = false
    
    private val _teachersState = MutableLiveData<UiState<List<TeacherUiModel>>>()
    val teachersState: LiveData<UiState<List<TeacherUiModel>>> = _teachersState
    
    private val _teacherScheduleState = MutableLiveData<UiState<List<TimetableDay>>>()
    val teacherScheduleState: LiveData<UiState<List<TimetableDay>>> = _teacherScheduleState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadTeachers() {
        _teachersState.value = UiState.Loading
        viewModelScope.launch {
            val mediatorResult = mediatorRepository.getActiveTeachers()
            mediatorResult.fold(
                onSuccess = { teachers ->
                    val nonZeroTeachers = teachers.filter { it.isActive != 0 }
                    val effectiveTeachers = if (nonZeroTeachers.isNotEmpty()) nonZeroTeachers else teachers
                    val uiModels = effectiveTeachers
                        .map { it.toUiModel() }
                        .sortedBy { it.name }
                    val filtered = filterTeachersForCurrentUser(uiModels)
                    offlineCache.saveTeachers(filtered)
                    _teachersState.value = UiState.Success(filtered)
                },
                onFailure = { error ->
                    val cached = offlineCache.loadTeachers()
                    _teachersState.value = if (!cached.isNullOrEmpty()) {
                        UiState.Success(cached)
                    } else {
                        UiState.Error(error.message ?: "Ошибка загрузки преподавателей")
                    }
                }
            )
        }
    }
    
    fun loadTeacherSchedule(teacher: TeacherUiModel) {
        _teacherScheduleState.value = UiState.Loading
        _isLoading.value = true
        val isSameTeacher = selectedTeacher?.name == teacher.name &&
                selectedTeacher?.publicId == teacher.publicId
        if (!isSameTeacher) {
            weekOffset = 0
        }
        selectedTeacher = teacher
        
        viewModelScope.launch {
            val (from, to) = resolveWeekBounds()
            val mediatorId = teacher.publicId ?: teacher.internalId
            if (mediatorId.isNullOrBlank()) {
                _teacherScheduleState.value = UiState.Error("У преподавателя отсутствует идентификатор в БД")
                _isLoading.value = false
                return@launch
            }
            val mediatorResult = mediatorRepository.getTeacherSchedule(
                from = from,
                to = to,
                teacherId = mediatorId,
                weekOffset = weekOffset
            )
                .map { lessons -> lessons.toTimetableDays(from, to) }

            mediatorResult.fold(
                onSuccess = { timetable ->
                    if (timetable.isNotEmpty()) {
                        // Кэшируем расписание преподавателя для оффлайна
                        offlineCache.saveSchedule("t:$mediatorId", weekOffset, timetable)
                        _teacherScheduleState.value = UiState.Success(timetable)
                    } else {
                        _teacherScheduleState.value = UiState.Error("Расписание не найдено")
                    }
                },
                onFailure = { error ->
                    // Оффлайн-фолбэк: пробуем взять последнее известное расписание этого преподавателя
                    val cached = offlineCache.loadSchedule("t:$mediatorId", weekOffset)
                    _teacherScheduleState.value = if (!cached.isNullOrEmpty()) {
                        UiState.Success(cached)
                    } else {
                        val message = if (error.message?.contains("Unable to resolve host", ignoreCase = true) == true) {
                            "Нет подключения к сети и нет сохранённого расписания."
                        } else {
                            error.message ?: "Ошибка загрузки расписания преподавателя"
                        }
                        UiState.Error(message)
                    }
                }
            )
            _isLoading.value = false
        }
    }
    
    fun nextWeek() {
        weekOffset += 1
        selectedTeacher?.let { loadTeacherSchedule(it) }
    }
    
    fun prevWeek() {
        weekOffset -= 1
        selectedTeacher?.let { loadTeacherSchedule(it) }
    }

    fun goToDate(date: LocalDate) {
        val baseMonday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetMonday = date
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        weekOffset = ChronoUnit.WEEKS.between(baseMonday, targetMonday).toInt()
        selectedTeacher?.let { loadTeacherSchedule(it) }
    }
    
    fun clearError() {
        _teachersState.value = UiState.Loading
    }
    
    fun clearScheduleError() {
        _teacherScheduleState.value = UiState.Loading
    }

    private fun resolveWeekBounds(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val startOfWeek = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(weekOffset.toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        return startOfWeek to endOfWeek
    }

    private fun currentWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return start to end
    }

    private suspend fun filterTeachersForCurrentUser(
        allTeachers: List<TeacherUiModel>
    ): List<TeacherUiModel> {
        val profile = withContext(Dispatchers.IO) { UserProfileStorage.load(context) }
        val role = profile?.role ?: determineRoleFromLogin()
        return when (role) {
            UserRole.STUDENT -> filterTeachersForStudent(allTeachers)
            UserRole.TEACHER -> filterTeachersForTeacher(allTeachers)
        }
    }

    private fun filterTeachersForTeacher(allTeachers: List<TeacherUiModel>): List<TeacherUiModel> {
        val active = allTeachers.filter { it.isActive }
        return if (active.isNotEmpty()) active else allTeachers
    }

    private suspend fun filterTeachersForStudent(
        allTeachers: List<TeacherUiModel>
    ): List<TeacherUiModel> {
        val cacheReady = ensureStudentTeacherCache()
        if (!cacheReady) {
            return allTeachers
        }

        val ids = studentTeacherIds ?: emptySet()
        val names = studentTeacherNames ?: emptySet()

        if (ids.isEmpty() && names.isEmpty()) {
            return emptyList()
        }

        return allTeachers.filter { teacher ->
            val normalizedName = teacher.name.normalizeTeacherName()
            val normalizedPublicId = teacher.publicId?.normalizeTeacherId()
            val normalizedInternalId = teacher.internalId?.normalizeTeacherId()
            val idMatch = (normalizedPublicId != null && ids.contains(normalizedPublicId)) ||
                    (normalizedInternalId != null && ids.contains(normalizedInternalId))
            val nameMatch = names.contains(normalizedName)
            idMatch || nameMatch
        }
    }

    private suspend fun ensureStudentTeacherCache(): Boolean {
        if (isStudentCacheReady) return true
        val (from, to) = extendedStudentScheduleRange()
        val lessonsResult = withContext(Dispatchers.IO) {
            mediatorRepository.getStudentSchedule(from, to)
        }
        return lessonsResult.fold(
            onSuccess = { lessons ->
                studentTeacherIds = lessons
                    .mapNotNull { lesson ->
                        lesson.teacherPublicId
                            ?.takeIf { it.isNotBlank() }
                            ?.normalizeTeacherId()
                    }
                    .toSet()
                studentTeacherNames = lessons
                    .mapNotNull { lesson ->
                        lesson.teacherName
                            ?.takeIf { it.isNotBlank() }
                            ?.normalizeTeacherName()
                    }
                    .toSet()
                isStudentCacheReady = true
                true
            },
            onFailure = {
                false
            }
        )
    }

    private fun extendedStudentScheduleRange(): Pair<LocalDate, LocalDate> {
        val (currentStart, _) = currentWeekRange()
        val from = currentStart.minusWeeks(3)
        val to = currentStart.plusWeeks(1).plusDays(6)
        return from to to
    }

    private fun determineRoleFromLogin(): UserRole {
        val login = authPreferences.getUsername().orEmpty()
        return if (login.any { it.isDigit() }) {
            UserRole.STUDENT
        } else {
            UserRole.TEACHER
        }
    }

    private fun String.normalizeTeacherName(): String {
        return WHITESPACE_REGEX.replace(
            this.trim().lowercase(Locale.getDefault()),
            " "
        )
    }

    private fun String.normalizeTeacherId(): String {
        return this.trim().lowercase(Locale.getDefault())
    }

    private fun MediatorTeacherDto.toUiModel(): TeacherUiModel {
        return TeacherUiModel(
            name = displayName,
            publicId = publicTeacherId,
            internalId = id,
            isActive = isActive != 0
        )
    }

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}

