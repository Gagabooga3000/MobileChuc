package com.example.chuc.data.mediator

import com.example.chuc.data.mediator.model.MediatorJournalResponse
import com.example.chuc.data.mediator.model.MediatorJournalRowDto
import com.example.chuc.data.mediator.model.MediatorLessonDto
import com.example.chuc.data.mediator.model.MediatorSubjectDto
import com.example.chuc.schedule.GradeBookEntry
import com.example.chuc.schedule.GradeBookSummary
import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.TimetableLesson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val apiDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
private val displayLocale = Locale("ru")

// Вариант по умолчанию (старый интерфейс) — строим только дни, где есть занятия
fun List<MediatorLessonDto>.toTimetableDays(): List<TimetableDay> {
    if (isEmpty()) return emptyList()
    val grouped = this.groupBy { it.lessonDate }
    val dates = grouped.keys
        .mapNotNull { runCatching { LocalDate.parse(it, apiDateFormatter) }.getOrNull() }
        .sorted()
    if (dates.isEmpty()) return emptyList()
    val from = dates.first()
    val to = dates.last()
    return toTimetableDays(from, to)
}

// Новый вариант — всегда заполняем диапазон дат от from до to (включительно),
// даже если в какой-то день нет занятий. Это нужно для календаря.
fun List<MediatorLessonDto>.toTimetableDays(from: LocalDate, to: LocalDate): List<TimetableDay> {
    if (from > to) return emptyList()

    val grouped = this.groupBy { it.lessonDate }

    val daysInRange = generateSequence(from) { current ->
        val next = current.plusDays(1)
        if (next <= to) next else null
    }.toList()

    return daysInRange.mapIndexed { index, date ->
        val dateKey = date.format(apiDateFormatter)
        val dateLabel = formatDateLabel(date)

        val lessons = grouped[dateKey].orEmpty()
            .sortedWith(compareBy<MediatorLessonDto> { it.timeId }.thenBy { it.timeName })
            .mapIndexed { lessonIndex, lesson ->
                TimetableLesson(
                    id = lessonIndex,
                    dayId = index,
                    time = lesson.timeName,
                    title = lesson.subjectName,
                    shortTitle = lesson.subjectShortName,
                    place = lesson.roomName.orEmpty(),
                    teacher = lesson.teacherName,
                    group = lesson.groupCode,
                    lessonType = mapLessonType(lesson)
                )
            }

    TimetableDay(
            id = index,
            dateLabel = dateLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() },
            lessons = lessons
        )
    }
}

private fun formatDateLabel(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, displayLocale)
    val formattedDate = date.format(displayFormatter)
    return "$dayOfWeek, $formattedDate"
}

private fun mapLessonType(lesson: MediatorLessonDto): String? {
    val apiValue = lesson.learnTypeName?.trim()
    if (!apiValue.isNullOrEmpty()) {
        return apiValue
    }

    return when (lesson.learnTypeId) {
        1 -> "Лекция"
        2 -> "Практика"
        3 -> "Лабораторная"
        4 -> "Семинар"
        5 -> "Зачёт"
        6 -> "Экзамен"
        7 -> "Консультация"
        else -> null
    }
}

fun MediatorJournalResponse.toGradeBookEntries(
    subjects: List<MediatorSubjectDto>
): List<GradeBookEntry> {
    val groupedRows = rows.groupBy { it.subjectPublicId }
    val knownSubjectIds = subjects.map { it.id }.toMutableSet()
    val combinedSubjects = subjects.toMutableList()

    rows.forEach { row ->
        if (knownSubjectIds.add(row.subjectPublicId)) {
            combinedSubjects.add(
                MediatorSubjectDto(
                    id = row.subjectPublicId,
                    subjectName = row.subjectName,
                    subjectShortName = row.subjectShortName
                )
            )
        }
    }

    return combinedSubjects
        .sortedBy { it.subjectName }
        .map { subject ->
            val columnData = Array<Pair<LocalDate?, String>?>(8) { null }
            groupedRows[subject.id].orEmpty().forEach { row ->
                val columnIndex = determineColumnIndex(row)
                if (columnIndex != null && columnIndex in columnData.indices) {
                    val date = runCatching { LocalDate.parse(row.lessonDate, apiDateFormatter) }.getOrNull()
                    val value = buildGradeValue(row)
                    val existing = columnData[columnIndex]
                    if (existing == null || (date != null && existing.first?.let { date.isAfter(it) } == true)) {
                        columnData[columnIndex] = date to value
                    }
                }
            }

            GradeBookEntry(
                discipline = subject.subjectName,
                course1Sem1 = columnData[0]?.second,
                course1Sem2 = columnData[1]?.second,
                course2Sem3 = columnData[2]?.second,
                course2Sem4 = columnData[3]?.second,
                course3Sem5 = columnData[4]?.second,
                course3Sem6 = columnData[5]?.second,
                course4Sem7 = columnData[6]?.second,
                course4Sem8 = columnData[7]?.second
            )
        }
        .filter { it.discipline.isNotBlank() }
}

fun MediatorJournalResponse.toGradeBookSummary(): GradeBookSummary? {
    val passed = rows.count { !it.gradeName.isNullOrBlank() }
    val notPassed = rows.count { it.gradeName.isNullOrBlank() }

    if (passed == 0 && notPassed == 0) {
        return null
    }

    val numericGrades = rows.mapNotNull { row ->
        parseNumericGrade(row)
    }
    val average = if (numericGrades.isNotEmpty()) {
        numericGrades.average()
    } else {
        null
    }
    val forecast = average?.let { avg ->
        when {
            avg >= 4.5 -> "Отличный прогноз: высокий шанс закрыть сессию на «4–5»"
            avg >= 3.5 -> "Хороший прогноз: при текущем темпе сессия закроется успешно"
            avg >= 3.0 -> "Нужна осторожность: часть дисциплин под риском"
            else -> "Высокий риск задолженностей, стоит подтянуть оценки"
        }
    }

    return GradeBookSummary(
        passed = passed,
        notPassed = notPassed,
        averageGrade = average,
        forecast = forecast
    )
}

private fun determineColumnIndex(row: MediatorJournalRowDto): Int? {
    val lessonDate = runCatching { LocalDate.parse(row.lessonDate, apiDateFormatter) }.getOrNull()
        ?: return null

    val admissionYear = extractAdmissionYear(row.groupCode) ?: lessonDate.year
    var course = lessonDate.year - admissionYear
    if (lessonDate.monthValue >= 9) {
        course += 1
    }
    val boundedCourse = course.coerceIn(1, 4)
    val semester = if (lessonDate.monthValue in 9..12) 1 else 2
    return (boundedCourse - 1) * 2 + (semester - 1)
}

private fun extractAdmissionYear(groupCode: String?): Int? {
    if (groupCode.isNullOrBlank()) return null
    val yearMatch = Regex("(\\d{2,4})$").find(groupCode) ?: return null
    val raw = yearMatch.groupValues[1]
    val year = raw.toIntOrNull() ?: return null
    return if (raw.length == 2) {
        if (year >= 50) 1900 + year else 2000 + year
    } else {
        year
    }
}

private fun buildGradeValue(row: MediatorJournalRowDto): String {
    val base = row.gradeName ?: row.gradeId?.toString() ?: "-"
    return if (row.lessonDate.isNotBlank()) {
        "$base (${row.lessonDate})"
    } else {
        base
    }
}

private fun parseNumericGrade(row: MediatorJournalRowDto): Int? {
    val name = row.gradeName?.lowercase()?.trim().orEmpty()

    // 1) Пытаемся вытащить число из gradeName
    val directNumber = Regex("(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (directNumber != null && directNumber in 2..5) {
        return directNumber
    }

    // 2) Если gradeId уже числовой и в диапазоне, используем его
    val id = row.gradeId
    if (id != null && id in 2..5) {
        return id
    }

    // 3) Маппинг по текстовым оценкам (на всякий случай)
    return when {
        name.contains("отлич") -> 5
        name.contains("хорош") -> 4
        name.contains("удовл") -> 3
        name.contains("зач") -> 5          // зачёт считаем положительным максимумом
        name.contains("незач") -> 2
        else -> null
    }
}

