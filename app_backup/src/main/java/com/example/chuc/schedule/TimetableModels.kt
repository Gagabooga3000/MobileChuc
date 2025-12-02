package com.example.chuc.schedule

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_lessons")
data class TimetableLesson(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dayId: Int,
    val time: String,
    val title: String,
    val place: String,
    val teacher: String? = null,
    val group: String? = null,
    val lessonType: String? = null
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey
    val id: String,
    val name: String,
    val department: String? = null,
    val position: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "teacher_schedule")
data class TeacherSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val teacherId: String,
    val dayOfWeek: String,
    val time: String,
    val subject: String,
    val group: String,
    val classroom: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

// UI State models
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

// Navigation models
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object Schedule : Screen()
    object Teachers : Screen()
    object Profile : Screen()
    object Settings : Screen()
}