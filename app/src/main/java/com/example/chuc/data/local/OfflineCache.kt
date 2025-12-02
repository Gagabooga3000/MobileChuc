package com.example.chuc.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.chuc.data.model.News
import com.example.chuc.presentation.viewmodel.JournalRowUiModel
import com.example.chuc.schedule.GradeBook
import com.example.chuc.schedule.TeacherUiModel
import com.example.chuc.schedule.TimetableDay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineCache @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    private val newsListType = object : TypeToken<List<News>>() {}.type
    private val timetableListType = object : TypeToken<List<TimetableDay>>() {}.type
    private val teacherListType = object : TypeToken<List<TeacherUiModel>>() {}.type
    private val gradeBookType = object : TypeToken<GradeBook>() {}.type
    private val journalRowsType = object : TypeToken<List<JournalRowUiModel>>() {}.type

    fun saveNews(news: List<News>) {
        runCatching {
            val json = gson.toJson(news, newsListType)
            prefs.edit()
                .putString(KEY_NEWS_JSON, json)
                .putLong(KEY_NEWS_UPDATED_AT, System.currentTimeMillis())
                .apply()
        }
    }

    fun loadNews(): List<News>? {
        val json = prefs.getString(KEY_NEWS_JSON, null) ?: return null
        return runCatching {
            gson.fromJson<List<News>>(json, newsListType)
        }.getOrNull()
    }

    fun saveSchedule(
        groupCode: String,
        weekOffset: Int,
        timetable: List<TimetableDay>
    ) {
        runCatching {
            val key = scheduleKey(groupCode, weekOffset)
            val json = gson.toJson(timetable, timetableListType)
            prefs.edit()
                .putString(key, json)
                .putLong("${key}_time", System.currentTimeMillis())
                .apply()
        }
    }

    fun loadSchedule(
        groupCode: String,
        weekOffset: Int
    ): List<TimetableDay>? {
        val key = scheduleKey(groupCode, weekOffset)
        val json = prefs.getString(key, null) ?: return null
        return runCatching {
            gson.fromJson<List<TimetableDay>>(json, timetableListType)
        }.getOrNull()
    }

    /**
     * Получить время последнего обновления расписания
     */
    fun getScheduleLastUpdateTime(
        groupCode: String,
        weekOffset: Int
    ): Long? {
        val key = scheduleKey(groupCode, weekOffset)
        val timeKey = "${key}_time"
        return if (prefs.contains(timeKey)) {
            prefs.getLong(timeKey, 0L).takeIf { it > 0 }
        } else {
            null
        }
    }

    /**
     * Проверить, актуально ли расписание (не старше указанного времени в миллисекундах)
     */
    fun isScheduleFresh(
        groupCode: String,
        weekOffset: Int,
        maxAgeMillis: Long = DEFAULT_SCHEDULE_MAX_AGE_MS
    ): Boolean {
        val lastUpdate = getScheduleLastUpdateTime(groupCode, weekOffset) ?: return false
        val now = System.currentTimeMillis()
        return (now - lastUpdate) < maxAgeMillis
    }

    private fun scheduleKey(groupCode: String, weekOffset: Int): String {
        val normalizedCode = groupCode.trim().lowercase()
        return "${KEY_SCHEDULE_JSON}_${normalizedCode}_$weekOffset"
    }

    fun saveGradeBook(gradeBook: GradeBook) {
        runCatching {
            val json = gson.toJson(gradeBook, gradeBookType)
            prefs.edit()
                .putString(KEY_GRADE_BOOK_JSON, json)
                .apply()
        }
    }

    fun loadGradeBook(): GradeBook? {
        val json = prefs.getString(KEY_GRADE_BOOK_JSON, null) ?: return null
        return runCatching {
            gson.fromJson<GradeBook>(json, gradeBookType)
        }.getOrNull()
    }

    fun saveJournalRows(rows: List<JournalRowUiModel>) {
        runCatching {
            val json = gson.toJson(rows, journalRowsType)
            prefs.edit()
                .putString(KEY_JOURNAL_ROWS_JSON, json)
                .apply()
        }
    }

    fun loadJournalRows(): List<JournalRowUiModel>? {
        val json = prefs.getString(KEY_JOURNAL_ROWS_JSON, null) ?: return null
        return runCatching {
            gson.fromJson<List<JournalRowUiModel>>(json, journalRowsType)
        }.getOrNull()
    }

    fun saveTeachers(teachers: List<TeacherUiModel>) {
        runCatching {
            val json = gson.toJson(teachers, teacherListType)
            prefs.edit()
                .putString(KEY_TEACHERS_JSON, json)
                .apply()
        }
    }

    fun loadTeachers(): List<TeacherUiModel>? {
        val json = prefs.getString(KEY_TEACHERS_JSON, null) ?: return null
        return runCatching {
            gson.fromJson<List<TeacherUiModel>>(json, teacherListType)
        }.getOrNull()
    }

    fun saveGradeSummary(passed: Int, notPassed: Int) {
        prefs.edit()
            .putInt(KEY_GRADES_PASSED, passed)
            .putInt(KEY_GRADES_NOT_PASSED, notPassed)
            .apply()
    }

    fun loadGradeSummary(): Pair<Int, Int>? {
        if (!prefs.contains(KEY_GRADES_PASSED) || !prefs.contains(KEY_GRADES_NOT_PASSED)) {
            return null
        }
        val passed = prefs.getInt(KEY_GRADES_PASSED, 0)
        val notPassed = prefs.getInt(KEY_GRADES_NOT_PASSED, 0)
        return passed to notPassed
    }

    /**
     * Очистить кэш расписания для конкретной группы
     */
    fun clearScheduleCache(groupCode: String) {
        runCatching {
            val editor = prefs.edit()
            val normalizedCode = groupCode.trim().lowercase()
            val allKeys = prefs.all.keys
            allKeys.forEach { key ->
                if (key.startsWith("${KEY_SCHEDULE_JSON}_${normalizedCode}_")) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    /**
     * Очистить весь кэш расписания
     */
    fun clearAllScheduleCache() {
        runCatching {
            val editor = prefs.edit()
            val allKeys = prefs.all.keys
            allKeys.forEach { key ->
                if (key.startsWith(KEY_SCHEDULE_JSON)) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "offline_cache"
        private const val KEY_NEWS_JSON = "news_json"
        private const val KEY_NEWS_UPDATED_AT = "news_updated_at"
        private const val KEY_SCHEDULE_JSON = "schedule_json"
        private const val KEY_GRADE_BOOK_JSON = "grade_book_json"
        private const val KEY_JOURNAL_ROWS_JSON = "journal_rows_json"
        private const val KEY_TEACHERS_JSON = "teachers_json"
        private const val KEY_GRADES_PASSED = "grades_passed"
        private const val KEY_GRADES_NOT_PASSED = "grades_not_passed"
        
        // Максимальный возраст кэша расписания: 1 час (3600000 мс)
        // Для текущей недели - 30 минут (1800000 мс)
        private const val DEFAULT_SCHEDULE_MAX_AGE_MS = 30 * 60 * 1000L // 30 минут
    }
}


