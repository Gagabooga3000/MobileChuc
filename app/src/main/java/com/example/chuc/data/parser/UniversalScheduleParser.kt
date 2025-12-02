package com.example.chuc.data.parser

import android.util.Log
import com.example.chuc.data.network.WebService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Универсальный парсер расписания
 * Поддерживает как нативный парсинг, так и Python API
 */
@Singleton
class UniversalScheduleParser @Inject constructor(
    private val webService: WebService
) {
    
    companion object {
        private const val TAG = "UniversalScheduleParser"
        private const val DEFAULT_CITY = "Челябинск"
    }
    
    /**
     * Получить расписание группы
     */
    suspend fun getGroupSchedule(groupName: String, day: String = "today"): Result<List<ScheduleDay>> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting schedule for group: $groupName")
                
                // Сначала пробуем Python API
                val pythonResult = tryPythonApiForGroup(groupName)
                if (pythonResult.isSuccess) {
                    Log.d(TAG, "Python API succeeded for group: $groupName")
                    return@withContext pythonResult
                }
                
                // Если Python API не работает, используем нативный парсинг
                Log.d(TAG, "Falling back to native parsing for group: $groupName")
                tryNativeParsingForGroup(groupName, day)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting group schedule", e)
                Result.failure(e)
            }
        }
    
    /**
     * Получить расписание преподавателя
     */
    suspend fun getTeacherSchedule(teacherName: String, day: String = "today"): Result<List<ScheduleDay>> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting schedule for teacher: $teacherName")
                
                // Сначала пробуем Python API
                val pythonResult = tryPythonApiForTeacher(teacherName)
                if (pythonResult.isSuccess) {
                    Log.d(TAG, "Python API succeeded for teacher: $teacherName")
                    return@withContext pythonResult
                }
                
                // Если Python API не работает, используем нативный парсинг
                Log.d(TAG, "Falling back to native parsing for teacher: $teacherName")
                tryNativeParsingForTeacher(teacherName, day)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting teacher schedule", e)
                Result.failure(e)
            }
        }
    
    /**
     * Получить список групп
     */
    suspend fun getGroupsList(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting groups list")
            
            // Пробуем Python API
            val pythonResult = tryPythonApiForGroups()
            if (pythonResult.isSuccess) {
                Log.d(TAG, "Python API succeeded for groups list")
                return@withContext pythonResult
            }
            
            // Fallback - возвращаем список популярных групп
            Log.d(TAG, "Using fallback groups list")
            Result.success(getDefaultGroupsList())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting groups list", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получить список преподавателей
     */
    suspend fun getTeachersList(): Result<List<Teacher>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting teachers list")
            
            // Пробуем Python API
            val pythonResult = tryPythonApiForTeachers()
            if (pythonResult.isSuccess) {
                Log.d(TAG, "Python API succeeded for teachers list")
                return@withContext pythonResult
            }
            
            // Fallback - возвращаем список популярных преподавателей
            Log.d(TAG, "Using fallback teachers list")
            Result.success(getDefaultTeachersList())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting teachers list", e)
            Result.failure(e)
        }
    }
    
    // ========== PRIVATE METHODS ==========
    
    private suspend fun tryPythonApiForGroup(groupName: String): Result<List<ScheduleDay>> {
        return try {
            val response = webService.getPythonGroupSchedule(groupName)
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val scheduleDays = parseScheduleHtml(html)
                Result.success(scheduleDays)
            } else {
                Result.failure(Exception("Python API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Python API failed for group: $groupName", e)
            Result.failure(e)
        }
    }
    
    private suspend fun tryPythonApiForTeacher(teacherName: String): Result<List<ScheduleDay>> {
        return try {
            val response = webService.getPythonTeacherSchedule(teacherName)
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val scheduleDays = parseScheduleHtml(html)
                Result.success(scheduleDays)
            } else {
                Result.failure(Exception("Python API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Python API failed for teacher: $teacherName", e)
            Result.failure(e)
        }
    }
    
    private suspend fun tryPythonApiForGroups(): Result<List<String>> {
        return try {
            val response = webService.getPythonGroups()
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val groups = parseGroupsFromHtml(html)
                Result.success(groups)
            } else {
                Result.failure(Exception("Python API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Python API failed for groups", e)
            Result.failure(e)
        }
    }
    
    private suspend fun tryPythonApiForTeachers(): Result<List<Teacher>> {
        return try {
            val response = webService.getPythonTeachers()
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val teachers = parseTeachersFromHtml(html)
                Result.success(teachers)
            } else {
                Result.failure(Exception("Python API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Python API failed for teachers", e)
            Result.failure(e)
        }
    }
    
    private suspend fun tryNativeParsingForGroup(groupName: String, day: String): Result<List<ScheduleDay>> {
        return try {
            val response = webService.postShowTTByGroup(groupName, day, DEFAULT_CITY)
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val scheduleDays = parseScheduleHtml(html)
                Result.success(scheduleDays)
            } else {
                Result.failure(Exception("Native parsing error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native parsing failed for group: $groupName", e)
            Result.failure(e)
        }
    }
    
    private suspend fun tryNativeParsingForTeacher(teacherName: String, day: String): Result<List<ScheduleDay>> {
        return try {
            val response = webService.postShowTTByTeacher(teacherName, day, DEFAULT_CITY)
            if (response.isSuccessful) {
                val html = decodeResponse(response.body())
                val scheduleDays = parseScheduleHtml(html)
                Result.success(scheduleDays)
            } else {
                Result.failure(Exception("Native parsing error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native parsing failed for teacher: $teacherName", e)
            Result.failure(e)
        }
    }
    
    private fun decodeResponse(responseBody: ResponseBody?): String {
        if (responseBody == null) return ""
        
        return try {
            val bytes = responseBody.bytes()
            // Пробуем сначала CP1251 (windows-1251)
            val cp1251Html = bytes.toString(Charset.forName("windows-1251"))
            
            // Проверяем, содержит ли результат кириллические символы
            if (cp1251Html.any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }) {
                Log.d(TAG, "Using CP1251 encoding for HTML")
                cp1251Html
            } else {
                // Если кириллицы нет, используем UTF-8
                val utf8Html = bytes.toString(Charset.forName("UTF-8"))
                Log.d(TAG, "Using UTF-8 encoding for HTML")
                utf8Html
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode HTML with CP1251, falling back to string()", e)
            responseBody.string()
        }
    }
    
    private fun parseScheduleHtml(html: String): List<ScheduleDay> {
        val doc = Jsoup.parse(html)
        val scheduleDays = mutableListOf<ScheduleDay>()
        
        // Ищем таблицы расписания
        val tables = doc.select("table")
        
        for (table in tables) {
            var currentDate: String? = null
            var currentDayOfWeek: String? = null
            var currentLessons = mutableListOf<ScheduleLesson>()
            
            for (tr in table.select("tr")) {
                // Строка с датой/днем недели
                if (tr.hasClass("dt") || tr.selectFirst("td.date") != null) {
                    // Сохраняем предыдущий день
                    if (currentLessons.isNotEmpty()) {
                        scheduleDays.add(
                            ScheduleDay(
                                date = currentDate,
                                dayOfWeek = currentDayOfWeek,
                                lessons = currentLessons.toList()
                            )
                        )
                        currentLessons = mutableListOf()
                    }
                    
                    // Извлекаем дату и день недели
                    val dateTd = tr.selectFirst("td.date")
                    val dayTd = tr.selectFirst("td.dayWeek")
                    currentDate = dateTd?.text()?.trim()
                    currentDayOfWeek = dayTd?.text()?.trim()
                    continue
                }
                
                // Строки с уроками
                val timeTd = tr.selectFirst("td.time")
                val time = timeTd?.text()?.trim() ?: ""
                
                if (time.isNotEmpty()) {
                    val cells = tr.select("td[rel]")
                    val subject = cells.getOrNull(0)?.text()?.trim() ?: ""
                    val teacher = cells.getOrNull(1)?.text()?.trim() ?: ""
                    val place = cells.getOrNull(2)?.text()?.trim() ?: ""
                    
                    if (subject.isNotEmpty()) {
                        currentLessons.add(
                            ScheduleLesson(
                                time = time,
                                subject = subject,
                                teacher = teacher,
                                place = place
                            )
                        )
                    }
                }
            }
            
            // Добавляем последний день
            if (currentLessons.isNotEmpty()) {
                scheduleDays.add(
                    ScheduleDay(
                        date = currentDate,
                        dayOfWeek = currentDayOfWeek,
                        lessons = currentLessons.toList()
                    )
                )
            }
        }
        
        return scheduleDays
    }
    
    private fun parseGroupsFromHtml(html: String): List<String> {
        val doc = Jsoup.parse(html)
        val groups = mutableListOf<String>()
        
        // Ищем элементы с группами
        val groupElements = doc.select("span.name, .group-name, .group")
        for (element in groupElements) {
            val groupName = element.text().trim()
            if (groupName.isNotEmpty() && groupName.matches(Regex(".*\\d+.*"))) {
                groups.add(groupName)
            }
        }
        
        return groups.ifEmpty { getDefaultGroupsList() }
    }
    
    private fun parseTeachersFromHtml(html: String): List<Teacher> {
        val doc = Jsoup.parse(html)
        val teachers = mutableListOf<Teacher>()
        
        // Ищем элементы с преподавателями
        val teacherElements = doc.select("span.name, .teacher-name, .teacher")
        for (element in teacherElements) {
            val teacherName = element.text().trim()
            if (teacherName.isNotEmpty() && teacherName.contains(".")) {
                teachers.add(
                    Teacher(
                        id = element.attr("rel").takeIf { it.isNotEmpty() } ?: teacherName.hashCode().toString(),
                        name = teacherName
                    )
                )
            }
        }
        
        return teachers.ifEmpty { getDefaultTeachersList() }
    }
    
    private fun getDefaultGroupsList(): List<String> {
        return listOf(
            "ИС-1-22", "ИС-2-22", "ИС-3-22",
            "ПИ-1-22", "ПИ-2-22", "ПИ-3-22",
            "СА-1-22", "СА-2-22", "СА-3-22",
            "ИБ-1-22", "ИБ-2-22", "ИБ-3-22"
        )
    }
    
    private fun getDefaultTeachersList(): List<Teacher> {
        return listOf(
            Teacher("1", "Иванов И.И."),
            Teacher("2", "Петров П.П."),
            Teacher("3", "Сидоров С.С."),
            Teacher("4", "Козлов К.К."),
            Teacher("5", "Морозов М.М.")
        )
    }
}

// ========== DATA CLASSES ==========

data class ScheduleDay(
    val date: String?,
    val dayOfWeek: String?,
    val lessons: List<ScheduleLesson>
)

data class ScheduleLesson(
    val time: String,
    val subject: String,
    val teacher: String,
    val place: String
)

data class Teacher(
    val id: String,
    val name: String
)

