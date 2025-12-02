package com.example.chuc.schedule

/**
 * Модель данных для зачетной книжки
 */

// Запись в зачетной книжке (одна дисциплина)
data class GradeBookEntry(
    val discipline: String,           // Название дисциплины
    val course1Sem1: String? = null,  // 1 курс, 1 семестр
    val course1Sem2: String? = null,  // 1 курс, 2 семестр
    val course2Sem3: String? = null,  // 2 курс, 3 семестр
    val course2Sem4: String? = null,  // 2 курс, 4 семестр
    val course3Sem5: String? = null,  // 3 курс, 5 семестр
    val course3Sem6: String? = null,  // 3 курс, 6 семестр
    val course4Sem7: String? = null,  // 4 курс, 7 семестр
    val course4Sem8: String? = null   // 4 курс, 8 семестр
)

// Полная зачетная книжка
data class GradeBook(
    val studentName: String,              // ФИО студента
    val entries: List<GradeBookEntry>,    // Список дисциплин
    val summary: GradeBookSummary? = null // Сводка (если есть)
)

// Сводка по зачетной книжке
data class GradeBookSummary(
    val passed: Int,           // Сдано дисциплин
    val notPassed: Int,        // Не сдано дисциплин
    val averageGrade: Double?, // Средний балл (если можно посчитать)
    val forecast: String?      // Текстовый прогноз по сессии
)

