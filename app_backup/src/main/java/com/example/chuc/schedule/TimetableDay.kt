package com.example.chuc.schedule

// Простая модель для расписания без Room аннотаций
data class TimetableDay(
    val id: Int = 0,
    val dateLabel: String,
    val lessons: List<TimetableLesson>,
    val groupName: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

