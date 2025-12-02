package com.example.chuc.schedule

data class TimetableDay(
    val dateLabel: String,
    val lessons: List<TimetableLesson>
)

data class TimetableLesson(
    val time: String,
    val title: String,
    val place: String
)


