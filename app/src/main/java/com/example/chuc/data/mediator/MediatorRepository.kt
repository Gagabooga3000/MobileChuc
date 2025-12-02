package com.example.chuc.data.mediator

import android.util.Log
import com.example.chuc.data.mediator.model.MediatorGroupDto
import com.example.chuc.data.mediator.model.MediatorJournalResponse
import com.example.chuc.data.mediator.model.MediatorLessonDto
import com.example.chuc.data.mediator.model.MediatorSubjectDto
import com.example.chuc.data.mediator.model.MediatorTeacherDto
import com.example.chuc.domain.model.UserProfile
import com.example.chuc.domain.model.UserRole
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class MediatorRepository @Inject constructor(
    private val api: MediatorApiService
) {

    companion object {
        private const val TAG = "MediatorRepository"
        private val apiDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    suspend fun getActiveGroups(): Result<List<MediatorGroupDto>> =
        safeCall("getActiveGroups") {
            api.getActiveGroups().rows.also {
                Log.d(TAG, "Loaded ${it.size} active groups from mediator")
            }
        }

    suspend fun getActiveTeachers(): Result<List<MediatorTeacherDto>> =
        safeCall("getActiveTeachers") {
            api.getActiveTeachers().rows.also {
                Log.d(TAG, "Loaded ${it.size} active teachers from mediator")
            }
        }

    suspend fun getStudentSchedule(
        from: LocalDate,
        to: LocalDate,
        groupId: String? = null,
        weekOffset: Int? = null
    ): Result<List<MediatorLessonDto>> =
        safeCall("getStudentSchedule") {
            api.getStudentSchedule(
                from = from.toApiDate(),
                to = to.toApiDate(),
                groupId = groupId,
                weekOffset = weekOffset
            ).rows
        }

    suspend fun getTeacherSchedule(
        from: LocalDate,
        to: LocalDate,
        teacherId: String? = null,
        weekOffset: Int? = null
    ): Result<List<MediatorLessonDto>> =
        safeCall("getTeacherSchedule") {
            api.getTeacherSchedule(
                from = from.toApiDate(),
                to = to.toApiDate(),
                teacherId = teacherId,
                weekOffset = weekOffset
            ).rows
        }

    suspend fun getStudentJournal(
        from: LocalDate,
        to: LocalDate,
        studentId: String? = null,
        subjectId: String? = null,
        subjectName: String? = null
    ): Result<MediatorJournalResponse> =
        safeCall("getStudentJournal") {
            api.getStudentJournal(
                from = from.toApiDate(),
                to = to.toApiDate(),
                studentId = studentId,
                subjectId = subjectId,
                subjectName = subjectName
            )
        }

    suspend fun getStudentJournalSubjects(
        from: LocalDate,
        to: LocalDate,
        studentId: String? = null
    ): Result<List<MediatorSubjectDto>> =
        safeCall("getStudentJournalSubjects") {
            api.getStudentJournalSubjects(from.toApiDate(), to.toApiDate(), studentId).rows
        }

    suspend fun detectCurrentProfile(role: UserRole): Result<UserProfile> {
        return when (role) {
            UserRole.STUDENT -> detectStudentProfile()
            UserRole.TEACHER -> detectTeacherProfile()
        }
    }

    suspend fun getStudentScheduleExtended(): Result<List<MediatorLessonDto>> {
        val (from, to) = extendedStudentScheduleRange()
        return getStudentSchedule(from, to)
    }

    suspend fun detectStudentProfile(): Result<UserProfile> {
        return getStudentScheduleExtended().mapCatching { lessons ->
            val group = lessons.firstOrNull { it.groupCode.isNotBlank() }?.groupCode
                ?: error("Не удалось определить группу студента — занятий нет за последние недели")
            UserProfile(
                role = UserRole.STUDENT,
                groupName = group,
                teacherName = null
            )
        }
    }

    suspend fun detectTeacherProfile(): Result<UserProfile> {
        val (from, to) = currentWeekRange()
        return getTeacherSchedule(from, to).mapCatching { lessons ->
            val teacherName = lessons.firstOrNull()?.teacherName
                ?: error("Не удалось определить имя преподавателя")
            UserProfile(
                role = UserRole.TEACHER,
                groupName = null,
                teacherName = teacherName
            )
        }
    }

    private fun LocalDate.toApiDate(): String = format(apiDateFormatter)

    private fun currentWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return start to end
    }

    private fun extendedStudentScheduleRange(): Pair<LocalDate, LocalDate> {
        val (currentStart, _) = currentWeekRange()
        val from = currentStart.minusWeeks(3)
        val to = currentStart.plusWeeks(1).plusDays(6)
        return from to to
    }

    private suspend fun <T> safeCall(tag: String, block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (http: HttpException) {
            val errorBody = http.response()?.errorBody()?.string()
            val message = buildString {
                append("Mediator error ${http.code()}")
                if (!errorBody.isNullOrBlank()) {
                    append(": ")
                    append(errorBody)
                } else {
                    append(" - ")
                    append(http.message())
                }
            }
            Log.e(TAG, "Mediator request '$tag' failed. $message", http)
            Result.failure(Exception(message, http))
        } catch (t: Throwable) {
            Log.e(TAG, "Mediator request '$tag' failed: ${t.message}", t)
            Result.failure(t)
        }
    }
}

