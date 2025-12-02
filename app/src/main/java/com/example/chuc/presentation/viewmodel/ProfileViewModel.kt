package com.example.chuc.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.local.OfflineCache
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.mediator.model.MediatorJournalRowDto
import com.example.chuc.data.mediator.model.MediatorSubjectDto
import com.example.chuc.data.mediator.toGradeBookEntries
import com.example.chuc.data.mediator.toGradeBookSummary
import com.example.chuc.domain.model.UserProfile
import com.example.chuc.domain.model.UserProfileStorage
import com.example.chuc.domain.model.UserRole
import com.example.chuc.presentation.notifications.AppNotifications
import com.example.chuc.schedule.GradeBook
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

data class ProfileUiModel(
    val fullName: String,
    val group: String,
    val course: String,
    val specialty: String,
    val role: UserRole
)

data class JournalSubjectUiModel(
    val id: String?,
    val name: String
)

data class JournalRowUiModel(
    val displayDate: String,
    val subject: String,
    val teacher: String,
    val timeAndPlace: String,
    val gradeLabel: String,
    val isPositive: Boolean
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val mediatorRepository: MediatorRepository,
    private val offlineCache: OfflineCache,
    private val appNotifications: AppNotifications,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _profileState = MutableLiveData<UiState<ProfileUiModel>>()
    val profileState: LiveData<UiState<ProfileUiModel>> = _profileState

    private val _logoutState = MutableLiveData<UiState<Boolean>>()
    val logoutState: LiveData<UiState<Boolean>> = _logoutState

    private val _subjectsState = MutableLiveData<UiState<List<JournalSubjectUiModel>>>()
    val subjectsState: LiveData<UiState<List<JournalSubjectUiModel>>> = _subjectsState

    private val _gradeBookState = MutableLiveData<UiState<GradeBook>>()
    val gradeBookState: LiveData<UiState<GradeBook>> = _gradeBookState

    private val _courseSubjectsState = MutableLiveData<UiState<List<JournalSubjectUiModel>>>()
    val courseSubjectsState: LiveData<UiState<List<JournalSubjectUiModel>>> = _courseSubjectsState

    private val _courseJournalState = MutableLiveData<UiState<List<JournalRowUiModel>>>()
    val courseJournalState: LiveData<UiState<List<JournalRowUiModel>>> = _courseJournalState

    private var cachedSubjects: List<MediatorSubjectDto> = emptyList()
    private var admissionYearCache: Int? = null
    private val displayLocale = Locale("ru")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", displayLocale)

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _profileState.value = UiState.Loading
        viewModelScope.launch {
            val profileResult = fetchProfileFromMediator()
            _profileState.value = profileResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { error ->
                    // Оффлайн-фолбэк: пробуем взять сохранённый профиль
                    val stored = UserProfileStorage.load(context)
                    if (stored != null) {
                        val ui = ProfileUiModel(
                            fullName = authPreferences.getUsername() ?: "Пользователь",
                            group = stored.groupName.orEmpty(),
                            course = "",
                            specialty = "",
                            role = stored.role
                        )
                        UiState.Success(ui)
                    } else {
                        UiState.Error(error.message ?: "Не удалось загрузить профиль")
                    }
                }
            )
        }
    }

    private suspend fun fetchProfileFromMediator(): Result<ProfileUiModel> = withContext(Dispatchers.IO) {
        val storedProfile = UserProfileStorage.load(context)
        val role = storedProfile?.role ?: determineRoleFromLogin()
        return@withContext when (role) {
            UserRole.STUDENT -> mediatorRepository.getStudentScheduleExtended().map { lessons ->
                val group = storedProfile?.groupName ?: lessons.firstOrNull()?.groupCode ?: "Не определена"
                val fullName = authPreferences.getUsername() ?: "Студент"
                val course = extractCourse(group)
                val specialty = extractSpecialty(group)
                admissionYearCache = admissionYearFromGroup(group)
                UserProfileStorage.save(
                    context,
                    UserProfile(role = UserRole.STUDENT, groupName = group, teacherName = null)
                )
                ProfileUiModel(
                    fullName = fullName,
                    group = group,
                    course = course,
                    specialty = specialty,
                    role = UserRole.STUDENT
                )
            }
            UserRole.TEACHER -> {
                val (from, to) = currentWeekRange()
                mediatorRepository.getTeacherSchedule(from, to).map { lessons ->
                    val teacherName = lessons.firstOrNull()?.teacherName
                        ?: (authPreferences.getUsername() ?: "Преподаватель")
                    UserProfileStorage.save(
                        context,
                        UserProfile(role = UserRole.TEACHER, groupName = null, teacherName = teacherName)
                    )
                    ProfileUiModel(
                        fullName = teacherName,
                        group = "",
                        course = "",
                        specialty = "",
                        role = UserRole.TEACHER
                    )
                }
            }
        }
    }

    fun logout() {
        _logoutState.value = UiState.Loading
        viewModelScope.launch {
            authPreferences.clearCredentials()
            UserProfileStorage.clear(context)
            _logoutState.value = UiState.Success(true)
        }
    }

    fun loadJournalSubjects() {
        _subjectsState.value = UiState.Loading
        viewModelScope.launch {
            val (from, to) = resolveAcademicYearBounds()
            val result = mediatorRepository.getStudentJournalSubjects(from, to)
            _subjectsState.value = result.fold(
                onSuccess = { subjects ->
                    cachedSubjects = subjects
                    val uiItems = buildSubjectUiList(subjects)
                    UiState.Success(uiItems)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Не удалось загрузить список предметов")
                }
            )
        }
    }

    fun loadGradeBook(subjectId: String? = null) {
        if (_gradeBookState.value is UiState.Loading) return
        _gradeBookState.value = UiState.Loading

        viewModelScope.launch {
            val result = loadGradeBookFromMediator(subjectId)
            _gradeBookState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Не удалось загрузить зачетную книжку") }
            )
        }
    }

    private suspend fun loadGradeBookFromMediator(subjectId: String?): Result<GradeBook> = withContext(Dispatchers.IO) {
        val (from, to) = resolveAcademicYearBounds()
        val journalResult = mediatorRepository.getStudentJournal(from, to, subjectId = subjectId)
        if (journalResult.isFailure) {
            val cached = offlineCache.loadGradeBook()
            return@withContext if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(journalResult.exceptionOrNull() ?: Exception("Не удалось загрузить зачетную книжку"))
            }
        }
        val journal = journalResult.getOrThrow()
        val subjects = ensureSubjectsCached(from, to)
        val filteredSubjects = if (subjectId.isNullOrBlank()) {
            subjects
        } else {
            subjects.filter { it.id == subjectId }
        }
        val entries = journal.toGradeBookEntries(filteredSubjects)
        if (entries.isEmpty()) {
            return@withContext Result.failure(Exception("Нет данных по успеваемости"))
        }
        val gradeBook = GradeBook(
            studentName = authPreferences.getUsername() ?: "Студент",
            entries = entries,
            summary = journal.toGradeBookSummary()
        )
        gradeBook.summary?.let { summary ->
            val previous = offlineCache.loadGradeSummary()
            offlineCache.saveGradeSummary(summary.passed, summary.notPassed)
            if (previous != null) {
                val (prevPassed, _) = previous
                val diff = summary.passed - prevPassed
                if (diff > 0) {
                    appNotifications.notifyNewGrades(diff)
                }
            }
        }
        offlineCache.saveGradeBook(gradeBook)
        Result.success(gradeBook)
    }

    fun loadCourseSubjects(courseIndex: Int) {
        _courseSubjectsState.value = UiState.Loading
        val range = calculateCourseRange(courseIndex)
        if (range == null) {
            _courseSubjectsState.value = UiState.Error("Не удалось определить год поступления")
            return
        }
        viewModelScope.launch {
            val result = mediatorRepository.getStudentJournalSubjects(range.first, range.second)
            _courseSubjectsState.value = result.fold(
                onSuccess = { subjects -> UiState.Success(buildSubjectUiList(subjects)) },
                onFailure = { error -> UiState.Error(error.message ?: "Ошибка загрузки предметов") }
            )
        }
    }

    fun loadCourseJournal(courseIndex: Int, subjectId: String?) {
        _courseJournalState.value = UiState.Loading
        val range = calculateCourseRange(courseIndex)
        if (range == null) {
            _courseJournalState.value = UiState.Error("Не удалось определить год поступления")
            return
        }
        viewModelScope.launch {
            val result = mediatorRepository.getStudentJournal(
                from = range.first,
                to = range.second,
                subjectId = subjectId
            )
            _courseJournalState.value = result.fold(
                onSuccess = { response ->
                    val rows = response.rows.map { it.toUiModel() }
                    if (rows.isEmpty()) {
                        UiState.Error("Оценок за выбранный период нет")
                    } else {
                        offlineCache.saveJournalRows(rows)
                        UiState.Success(rows)
                    }
                },
                onFailure = { error ->
                    val cached = offlineCache.loadJournalRows()
                    if (!cached.isNullOrEmpty()) {
                        UiState.Success(cached)
                    } else {
                        UiState.Error(error.message ?: "Ошибка загрузки журнала")
                    }
                }
            )
        }
    }

    private suspend fun ensureSubjectsCached(from: LocalDate, to: LocalDate): List<MediatorSubjectDto> {
        if (cachedSubjects.isNotEmpty()) return cachedSubjects
        val subjects = mediatorRepository.getStudentJournalSubjects(from, to).getOrElse { throw it }
        cachedSubjects = subjects
        return subjects
    }

    private fun determineRoleFromLogin(): UserRole {
        val login = authPreferences.getUsername().orEmpty()
        return if (login.any { it.isDigit() }) UserRole.STUDENT else UserRole.TEACHER
    }

    private fun currentWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return start to start.plusDays(6)
    }

    private fun resolveAcademicYearBounds(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val startYear = if (today.monthValue >= 9) today.year else today.year - 1
        val start = LocalDate.of(startYear, 9, 1)
        val end = start.plusYears(1).minusDays(1)
        return start to end
    }

    private fun extractCourse(group: String): String {
        val yearPattern = Regex(".*-\\d+-?(\\d{2})$")
        val yearMatch = yearPattern.find(group)
        
        if (yearMatch != null) {
            val admissionYear = yearMatch.groupValues[1].toIntOrNull()
            if (admissionYear != null) {
                val fullYear = if (admissionYear < 50) 2000 + admissionYear else 1900 + admissionYear
                val currentYear = LocalDate.now().year
                val course = (currentYear - fullYear + 1).coerceIn(1, 6)
                return "$course курс"
            }
        }
        
        return ""
    }

    private fun extractSpecialty(group: String): String {
        return when {
            group.startsWith("ИС", ignoreCase = true) ||
                    group.startsWith("ПИ", ignoreCase = true) ->
                "Информационные системы и технологии"
            group.startsWith("ДК", ignoreCase = true) ->
                "Дизайн и компьютерные технологии"
            group.startsWith("ЭК", ignoreCase = true) ||
                    group.startsWith("БД", ignoreCase = true) ->
                "Экономика и бизнес"
            group.startsWith("Ю", ignoreCase = true) ->
                "Юриспруденция"
            group.startsWith("ЧС", ignoreCase = true) ->
                "Пожарная безопасность и ЧС"
            else -> ""
        }
    }

    private fun buildSubjectUiList(subjects: List<MediatorSubjectDto>): List<JournalSubjectUiModel> {
        val base = subjects
            .sortedBy { it.subjectName }
            .map { JournalSubjectUiModel(id = it.id, name = it.subjectName) }
        return listOf(JournalSubjectUiModel(null, "Все предметы")) + base
    }

    private fun calculateCourseRange(courseIndex: Int): Pair<LocalDate, LocalDate>? {
        val profile = UserProfileStorage.load(context)
        val group = profile?.groupName ?: return null
        val admissionYear = admissionYearCache ?: admissionYearFromGroup(group) ?: return null
        val start = LocalDate.of(admissionYear, 9, 1).plusYears((courseIndex - 1).toLong())
        val end = start.plusYears(1).minusDays(1)
        return start to end
    }

    private fun admissionYearFromGroup(group: String): Int? {
        val yearPattern = Regex(".*-\\d+-?(\\d{2})$")
        val yearMatch = yearPattern.find(group) ?: return null
        val shortYear = yearMatch.groupValues[1].toIntOrNull() ?: return null
        return if (shortYear < 50) 2000 + shortYear else 1900 + shortYear
    }

    private fun MediatorJournalRowDto.toUiModel(): JournalRowUiModel {
        val date = runCatching { LocalDate.parse(lessonDate) }.getOrNull()
        val dateLabel = date?.let {
            val dow = it.dayOfWeek.getDisplayName(TextStyle.FULL, displayLocale)
            "${it.format(dateFormatter)} • ${dow.replaceFirstChar { c -> c.titlecase(displayLocale) }}"
        } ?: lessonDate
        val gradeString = gradeName ?: gradeId?.toString() ?: "—"
        val isPositive = !(gradeString.equals("н", true) || gradeString.contains("н/к", true))
        val time = if (groupCode.isNotBlank()) {
            "$timeName • $groupCode"
        } else {
            timeName
        }
        return JournalRowUiModel(
            displayDate = dateLabel,
            subject = subjectName,
            teacher = teacherName,
            timeAndPlace = time,
            gradeLabel = "Оценка: $gradeString",
            isPositive = isPositive
        )
    }
}

