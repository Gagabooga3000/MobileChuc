package com.example.chuc.data.repository

import android.util.Log
import com.example.chuc.data.network.AuthSessionManager
import com.example.chuc.data.network.WebService
import com.example.chuc.data.parser.parseTeachersList
import com.example.chuc.data.parser.TeacherItem
import com.example.chuc.data.parser.ParsedTimetableDay
import com.example.chuc.data.parser.parseScheduleHtmlRobust
import com.example.chuc.schedule.Teacher
import com.example.chuc.schedule.TimetableDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeachersRepository @Inject constructor(
    private val webService: WebService,
    private val sessionManager: AuthSessionManager
) {
    
    suspend fun getTeachers(): Result<List<Teacher>> = withContext(Dispatchers.IO) {
        try {
            // Проверяем есть ли активная сессия
            if (!sessionManager.hasActiveSession()) {
                Log.w("TeachersRepository", "No active session, requiring login")
                return@withContext Result.failure(Exception("Требуется авторизация"))
            }
            
            Log.d("TeachersRepository", "Loading teachers list")
            
            val response = webService.getTeachers()
            
            if (response.isSuccessful) {
                val html = response.body()?.string() ?: ""
                
                // Проверяем, что не получили страницу логина
                if (html.contains("LoginForm") || html.contains("авторизаци") || html.contains("login")) {
                    Log.w("TeachersRepository", "Received login page, session expired")
                    sessionManager.clearSession()
                    Result.failure(Exception("Сессия истекла, требуется повторная авторизация"))
                } else {
                    Log.d("TeachersRepository", "Parsing teachers HTML (${html.length} chars)")
                    val teacherItems = parseTeachersList(html)
                    val teachers = teacherItems.map { item ->
                        Teacher(
                            id = item.id ?: "",
                            name = item.name,
                            department = "", // Можно добавить парсинг департамента если нужно
                            email = "",
                            phone = ""
                        )
                    }
                    Log.d("TeachersRepository", "Parsed ${teachers.size} teachers")
                    Result.success(teachers)
                }
            } else {
                Log.e("TeachersRepository", "Failed to load teachers: ${response.code()}")
                Result.failure(Exception("Ошибка загрузки преподавателей: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("TeachersRepository", "Exception loading teachers", e)
            Result.failure(e)
        }
    }
    
    suspend fun fetchParsedTeacherSchedule(teacherId: String, day: String, city: String = "Челябинск"): Result<List<TimetableDay>> = withContext(Dispatchers.IO) {
        try {
            // Проверяем есть ли активная сессия
            if (!sessionManager.hasActiveSession()) {
                Log.w("TeachersRepository", "No active session for teacher schedule")
                return@withContext Result.failure(Exception("Требуется авторизация"))
            }
            
            Log.d("TeachersRepository", "Fetching teacher schedule for: $teacherId, day: $day")
            
            val response = webService.postShowTTByTeacher(teacherId, day, city)
            val body = response.body()?.string().orEmpty()
            
            Log.d("TeachersRepository", "Response code: ${response.code()}")
            Log.d("TeachersRepository", "Response URL: ${response.raw().request.url}")
            
            if (!response.isSuccessful) {
                Log.w("TeachersRepository", "HTTP error: ${response.code()}")
                return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            }
            
            // Проверяем, что не получили страницу логина
            if (body.contains("LoginForm") || body.contains("авторизаци") || body.contains("login")) {
                Log.w("TeachersRepository", "Login page detected - session expired")
                sessionManager.clearSession()
                return@withContext Result.failure(Exception("Session expired or login required"))
            }
            
            Log.d("TeachersRepository", "Teacher schedule HTML received: ${body.length} chars")
            
            val parsed = parseScheduleHtmlRobust(body)
            if (parsed.isEmpty()) {
                Log.w("TeachersRepository", "No lessons parsed from teacher HTML")
                Result.failure(Exception("No lessons parsed (empty result)"))
            } else {
                Log.d("TeachersRepository", "Parsed ${parsed.size} days with teacher lessons")
                Result.success(mapParsedToApp(parsed))
            }
        } catch (e: Exception) {
            Log.e("TeachersRepository", "Exception loading teacher schedule", e)
            Result.failure(e)
        }
    }
    
    private fun mapParsedToApp(parsed: List<ParsedTimetableDay>): List<TimetableDay> {
        return parsed.mapIndexed { dayIndex, p ->
            TimetableDay(
                id = dayIndex,
                dateLabel = p.title ?: "День ${dayIndex + 1}",
                lessons = p.lessons.mapIndexed { lessonIndex, l ->
                    com.example.chuc.schedule.TimetableLesson(
                        id = lessonIndex,
                        time = l.time,
                        title = l.subject,
                        place = l.place ?: "",
                        teacher = l.teacher ?: "",
                        dayId = dayIndex
                    )
                }
            )
        }
    }
}
