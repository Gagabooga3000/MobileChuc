package com.example.chuc.data.parser

import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.TimetableLesson

/**
 * Мапперы для преобразования результатов парсинга расписания
 * в универсальные модели TimetableDay/TimetableLesson, используемые в приложении.
 */
fun List<ScheduleDay>.toTimetableDaysFromParser(groupCode: String): List<TimetableDay> {
    if (isEmpty()) return emptyList()

    return this.mapIndexed { dayIndex, day ->
        val dateLabel = buildDateLabel(dayIndex, day)
        val lessons = day.lessons.mapIndexed { lessonIndex, lesson ->
            val normalizedTime = lesson.time.trim().ifBlank { "—" }
            val normalizedSubject = lesson.subject.trim().ifBlank { "Без названия" }
            val normalizedPlace = lesson.place.trim()
            val normalizedTeacher = lesson.teacher.trim().takeIf { it.isNotBlank() }

            TimetableLesson(
                id = lessonIndex,
                dayId = dayIndex,
                time = normalizedTime,
                title = normalizedSubject,
                shortTitle = null,
                place = normalizedPlace,
                teacher = normalizedTeacher,
                group = groupCode,
                lessonType = null
            )
        }

        TimetableDay(
            id = dayIndex,
            dateLabel = dateLabel,
            lessons = lessons,
            groupName = groupCode
        )
    }
}

private fun buildDateLabel(index: Int, day: ScheduleDay): String {
    val parts = listOfNotNull(
        day.dayOfWeek?.takeIf { it.isNotBlank() },
        day.date?.takeIf { it.isNotBlank() }
    )
    return if (parts.isNotEmpty()) {
        parts.joinToString(", ")
    } else {
        "День ${index + 1}"
    }
}


