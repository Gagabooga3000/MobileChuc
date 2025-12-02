package com.example.chuc.data.parser

import org.jsoup.Jsoup

data class TeacherItem(
    val id: String?,   // rel attribute (может быть пустым)
    val name: String
)

/**
 * Парсер списка преподавателей со страницы /tt/byTeachers
 * ищет элементы: div#list span.name (в вашем HTML это так)
 * возвращает список TeacherItem(id, name)
 */
fun parseTeachersList(html: String, baseUri: String = "http://miterra.chuc.ru"): List<TeacherItem> {
    val doc = Jsoup.parse(html, baseUri)
    val list = mutableListOf<TeacherItem>()
    val spans = doc.select("#list span.name, #list span")
    spans.forEach { s ->
        val name = s.text().trim()
        if (name.isNotBlank()) {
            val id = s.attr("rel").takeIf { it.isNotBlank() }
            list.add(TeacherItem(id = id, name = name))
        }
    }
    return list
}