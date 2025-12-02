package com.example.chuc.schedule

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object TimetableParser {
    private const val BASE_URL = "http://miterra.chuc.ru/tt/byGroups"

    fun fetchAndParse(groupQuery: String? = null): List<TimetableDay> {
        val url = BASE_URL + (groupQuery?.let { "?q=" + it } ?: "")
        val doc: Document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(15000)
            .get()

        // NOTE: Структура сайта может отличаться. Ниже примерный парсинг.
        val days = mutableListOf<TimetableDay>()

        // Предположим, что дни сгруппированы по секциям с классом .day
        val dayElements = doc.select(".day, .timetable-day, section")
        if (dayElements.isEmpty()) {
            // fallback: вытащим таблицы
            val tables = doc.select("table")
            for (table in tables) {
                val header = table.previousElementSibling()?.text() ?: "Расписание"
                val lessons = table.select("tr").drop(1).mapNotNull { tr ->
                    val tds = tr.select("td")
                    if (tds.size >= 3) {
                        TimetableLesson(
                            time = tds[0].text().trim(),
                            title = tds[1].text().trim(),
                            place = tds[2].text().trim()
                        )
                    } else null
                }
                if (lessons.isNotEmpty()) {
                    days += TimetableDay(header, lessons)
                }
            }
            return days
        }

        for (day in dayElements) {
            val dateLabel = day.selectFirst(".day-title, h2, h3")?.text()?.trim() ?: "День"
            val lessons = mutableListOf<TimetableLesson>()
            val rows = day.select(".lesson, tr, li")
            for (row in rows) {
                val time = row.selectFirst(".time, .lesson-time, td:nth-child(1)")?.text()?.trim()
                    ?: ""
                val title = row.selectFirst(".title, .lesson-title, td:nth-child(2)")?.text()?.trim()
                    ?: row.text().trim()
                val place = row.selectFirst(".place, .lesson-place, td:nth-child(3)")?.text()?.trim()
                    ?: ""
                if (title.isNotEmpty()) {
                    lessons += TimetableLesson(time, title, place)
                }
            }
            if (lessons.isNotEmpty()) {
                days += TimetableDay(dateLabel, lessons)
            }
        }

        return days
    }
}


