package com.example.chuc.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модели данных для работы с Python парсером
 */

data class ScheduleResponse(
    @SerializedName("groups") val groups: List<GroupSchedule>,
    @SerializedName("teachers") val teachers: List<TeacherSchedule>,
    @SerializedName("last_update") val lastUpdate: String?,
    @SerializedName("error") val error: String?
)

data class GroupSchedule(
    @SerializedName("name") val name: String,
    @SerializedName("schedule") val schedule: List<DaySchedule>
)

data class TeacherSchedule(
    @SerializedName("name") val name: String,
    @SerializedName("schedule") val schedule: List<DaySchedule>
)

data class DaySchedule(
    @SerializedName("day") val day: String,
    @SerializedName("date") val date: String?,
    @SerializedName("lessons") val lessons: List<Lesson>
)

data class Lesson(
    @SerializedName("time") val time: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("teacher") val teacher: String?,
    @SerializedName("place") val place: String?,
    @SerializedName("group") val group: String?
)

data class GroupsResponse(
    @SerializedName("groups") val groups: List<GroupSchedule>
)

data class TeachersResponse(
    @SerializedName("teachers") val teachers: List<TeacherSchedule>
)

data class UpdateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("last_update") val lastUpdate: String?,
    @SerializedName("message") val message: String?
)

