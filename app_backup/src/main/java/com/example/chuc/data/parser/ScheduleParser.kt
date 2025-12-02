package com.example.chuc.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Модели результата парсинга
 */
data class ParsedTimetableLesson(
    val time: String,
    val subject: String,
    val teacher: String?,
    val place: String?,
    val groups: String?,    // если в ответе есть информация о группах (для преподавателя)
    val type: String?       // лекция/практ/зачет и т.п.
)

data class ParsedTimetableDay(
    val date: String?,      // например "29.09" или "29.09.2025"
    val dayOfWeek: String?, // "Понедельник"
    val title: String?,     // комбинированный заголовок, если нужно
    val lessons: List<ParsedTimetableLesson>
)

/**
 * Робастный парсер HTML-фрагмента, возвращаемого AJAX /tt/ajxShowTT и /tt/ajxShowTTt.
 * Поддерживает оба варианта таблицы (разные порядки столбцов).
 *
 * Рекомендация: перед передачей в парсер декодируйте тело в CP1251, если в ответе кракозябры:
 * val bytes = responseBody.bytes()
 * val html = bytes.toString(Charset.forName("windows-1251"))
 */
fun parseScheduleHtmlRobust(html: String, baseUri: String = "http://miterra.chuc.ru"): List<ParsedTimetableDay> {
    val doc = Jsoup.parse(html, baseUri)
    // на странице может быть несколько таблиц, но чаще возвращается одна <table>
    val tables = doc.select("table")
    val days = mutableListOf<ParsedTimetableDay>()

    if (tables.isEmpty()) return days

    // Обрабатываем каждую таблицу — внутри неё tr.dt обозначает начало дня
    for (table in tables) {
        var currentDate: String? = null
        var currentDayOfWeek: String? = null
        var currentTitle: String? = null
        var currentLessons = mutableListOf<ParsedTimetableLesson>()

        for (tr in table.select("tr")) {
            val trClasses = tr.classNames()

            // 1) строка с датой/днём: <tr class='dt'> <td class='date'>29.09</td> <td class='dayWeek' colspan='4'>Понедельник</td>
            if (tr.hasClass("dt") || tr.selectFirst("td.date") != null) {
                // если в предыдущем дне уже накопились уроки — сохранить их
                if (currentLessons.isNotEmpty()) {
                    days.add(
                        ParsedTimetableDay(
                            date = currentDate,
                            dayOfWeek = currentDayOfWeek,
                            title = currentTitle,
                            lessons = currentLessons.toList()
                        )
                    )
                    currentLessons = mutableListOf()
                }
                // извлечём дату и день недели
                val dateTd = tr.selectFirst("td.date") ?: tr.selectFirst("td[class*=date]")
                val dayTd = tr.selectFirst("td.dayWeek") ?: tr.selectFirst("td[class*=dayWeek]")
                currentDate = dateTd?.text()?.trim()
                currentDayOfWeek = dayTd?.text()?.trim()
                currentTitle = if (currentDate != null && currentDayOfWeek != null) "$currentDate ${currentDayOfWeek}" else (dayTd?.text()?.trim() ?: null)
                continue
            }

            // 2) строки с уроками: у них обычно есть td.time и несколько td.cell (rel атрибуты)
            val time = tr.selectFirst("td.time")?.text()?.trim()
                ?: tr.selectFirst("td[class*=time]")?.text()?.trim()
                ?: tr.selectFirst("td")?.text()?.trim() ?: ""

            // Соберём map rel -> td
            val relMap = mutableMapOf<String, Element>()
            tr.select("td[rel]").forEach { td ->
                val rel = td.attr("rel")
                if (rel.isNotBlank()) relMap[rel] = td
            }
            // Если нет rel-ов, попробуем взять по порядку (fallback)
            if (relMap.isEmpty()) {
                val tds = tr.select("td")
                // предполагаем порядок [time, subject, groups/teacher, place/room, type?]
                if (tds.size >= 2) {
                    // создаём pseudo-rel по индексам "r0","r1"... (в дальнейшем используются как fallback)
                    for (i in 0 until tds.size) relMap["r$i"] = tds[i]
                }
            }

            // helper: получить текст дисциплины
            fun getSubject(td: Element?): String {
                if (td == null) return ""
                // в некоторых td есть <span class='val'> — берём его
                val spanVal = td.selectFirst(".val")
                if (spanVal != null) return spanVal.text().trim()
                // иначе чистый текст
                return td.text().trim()
            }

            // helper: получить place (если есть ссылка - href text or anchor text)
            fun getPlace(td: Element?): String? {
                if (td == null) return null
                val a = td.selectFirst("a")
                if (a != null) {
                    // если в теге <a> текст - это номер аудитории
                    val text = a.text().trim()
                    if (text.isNotEmpty()) return text
                    // иначе использовать title или href
                    val title = a.attr("title")
                    if (title.isNotBlank()) return title
                    val href = a.absUrl("href")
                    if (href.isNotBlank()) return href
                }
                val txt = td.text().trim()
                return if (txt.isNotEmpty()) txt else null
            }

            // helper: detect group vs teacher by heuristic
            fun isGroupText(s: String?): Boolean {
                if (s.isNullOrBlank()) return false
                // типичные группы: contain digits and dash e.g. ИС-1-22 or СА-1-23
                val gRegex = Regex("""[А-ЯA-Zа-яa-zёЁ0-9]+[-–—]\d+""")
                if (gRegex.containsMatchIn(s)) return true
                // groups often contain comma-separated list with '-' in items
                if (s.contains(",") && s.any { it.isDigit() && it == '-' }) return true
                return false
            }

            fun isTeacherText(s: String?): Boolean {
                if (s.isNullOrBlank()) return false
                // heuristic: teacher names often contain dots for initials "К.О." or have two parts "Фамилия И.О."
                if (s.contains(".")) return true
                // or look for pattern "Lastname I.O." (Cyrillic uppercase then dot)
                val tRegex = Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.?""")
                if (tRegex.containsMatchIn(s)) return true
                // if contains ' ' and ends with dot, likely teacher
                if (s.split(" ").size <= 3 && s.contains(" ")) return true
                return false
            }

            // Попытаемся извлечь subject, teacher, groups, place, type
            var subject = ""
            var teacher: String? = null
            var groups: String? = null
            var place: String? = null
            var type: String? = null

            // common rel keys: rel='0' -> subject; rel='1' -> group or teacher; rel='2' -> place/link; rel='3' -> type
            if (relMap.isNotEmpty()) {
                // subject from rel 0 or fallback r1
                val subTd = relMap["0"] ?: relMap["r1"] ?: relMap["r0"]
                subject = getSubject(subTd)

                // groups/teacher candidate in rel 1 or r2
                val rel1Td = relMap["1"] ?: relMap["r2"] ?: relMap["r1"]
                val rel2Td = relMap["2"] ?: relMap["r3"] ?: relMap["r2"]
                val rel3Td = relMap["3"] ?: relMap["r4"] ?: relMap["r3"]

                val rel1Text = rel1Td?.text()?.trim()
                val rel2Text = rel2Td?.text()?.trim()
                val rel3Text = rel3Td?.text()?.trim()

                // decide which one is teacher and which is group
                if (isTeacherText(rel1Text) && !isGroupText(rel1Text)) {
                    teacher = rel1Text
                } else if (isGroupText(rel1Text)) {
                    groups = rel1Text
                } else if (!rel1Text.isNullOrBlank() && rel1Td?.hasAttr("value") == true && rel1Td.attr("value").all { it.isDigit() }) {
                    // sometimes rel1 has value='2099' meaning teacher id, but text may be name
                    teacher = rel1Text
                } else {
                    // ambiguous - defer decision
                }

                // rel2 often contains place / link or teacher depending on table type
                if (teacher == null && isTeacherText(rel2Text)) {
                    teacher = rel2Text
                } else if (place == null && !rel2Text.isNullOrBlank()) {
                    place = getPlace(rel2Td)
                }

                // rel3 often contains type (Практика/Лекция) OR could be place in some layouts
                if (!rel3Text.isNullOrBlank()) {
                    val potentialType = rel3Text
                    // short list of known types
                    val knownTypes = listOf("Практика", "Лекция", "Зачет", "Экзамен", "Пр", "лекция", "практ.")
                    if (knownTypes.any { potentialType.contains(it, ignoreCase = true) }) {
                        type = potentialType
                    } else {
                        // if rel3 looks like place (contains digits), set as place
                        if (potentialType.any { it.isDigit() }) place = potentialType else {
                            // maybe teacher
                            if (teacher == null && isTeacherText(potentialType)) teacher = potentialType
                        }
                    }
                }

                // If teacher still null, try to pick from rel1/rel2 heuristics
                if (teacher == null) {
                    if (isTeacherText(rel1Text)) teacher = rel1Text
                    else if (isTeacherText(rel2Text)) teacher = rel2Text
                    else if (isTeacherText(rel3Text)) teacher = rel3Text
                }
                // If place still null, attempt to extract anchor from rel2 or rel1
                if (place == null) {
                    place = getPlace(rel2Td) ?: getPlace(rel1Td)
                }
                // If groups still null, try to read rel1 (if it looks like groups)
                if (groups == null && isGroupText(rel1Text)) groups = rel1Text
                if (groups == null && isGroupText(rel2Text)) groups = rel2Text
            } else {
                // полностью fallback: взять все <td> кроме time и попытаться разделить
                val tds = tr.select("td")
                if (tds.size >= 2) {
                    subject = tds.getOrNull(1)?.text()?.trim() ?: ""
                    // try to find teacher by dot in any td
                    for (i in 2 until tds.size) {
                        val txt = tds[i].text().trim()
                        if (isTeacherText(txt)) {
                            teacher = txt
                            break
                        } else if (isGroupText(txt)) {
                            groups = txt
                        } else if (place == null && txt.any { it.isDigit() }) {
                            place = txt
                        }
                    }
                }
            }

            // final cleanups: empty -> nulls
            if (teacher != null && teacher.isBlank()) teacher = null
            if (place != null && place.isBlank()) place = null
            if (groups != null && groups.isBlank()) groups = null
            if (type != null && type.isBlank()) type = null

            if (subject.isNotBlank()) {
                currentLessons.add(
                    ParsedTimetableLesson(
                        time = time ?: "",
                        subject = subject,
                        teacher = teacher,
                        place = place,
                        groups = groups,
                        type = type
                    )
                )
            }
        } // end for tr

        // 마지막 день в таблице
        if (currentLessons.isNotEmpty()) {
            days.add(
                ParsedTimetableDay(
                    date = currentDate,
                    dayOfWeek = currentDayOfWeek,
                    title = currentTitle,
                    lessons = currentLessons.toList()
                )
            )
        }
    } // end for table

    return days
}