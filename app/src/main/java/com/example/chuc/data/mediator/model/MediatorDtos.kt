package com.example.chuc.data.mediator.model

import com.google.gson.annotations.SerializedName

data class MediatorGroupsResponse(
    @SerializedName("rows") val rows: List<MediatorGroupDto>
)

data class MediatorGroupDto(
    @SerializedName("id") val id: String,
    @SerializedName("public_group_id") val publicGroupId: String,
    @SerializedName("group_code") val groupCode: String,
    @SerializedName("is_active") val isActive: Int? = null,
    @SerializedName("start_year") val startYear: Int
)

data class MediatorTeachersResponse(
    @SerializedName("rows") val rows: List<MediatorTeacherDto>
)

data class MediatorTeacherDto(
    @SerializedName("id") val id: String,
    @SerializedName("public_teacher_id") val publicTeacherId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("is_active") val isActive: Int? = null
)

data class MediatorScheduleResponse(
    @SerializedName("group_id") val groupId: String?,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("rows") val rows: List<MediatorLessonDto>
)

data class MediatorLessonDto(
    @SerializedName("lesson_date") val lessonDate: String,
    @SerializedName("time_id") val timeId: Int,
    @SerializedName("time_name") val timeName: String,
    @SerializedName("group_code") val groupCode: String,
    @SerializedName("subject_public_id") val subjectPublicId: String,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_short_name") val subjectShortName: String?,
    @SerializedName("teacher_public_id") val teacherPublicId: String,
    @SerializedName("teacher_name") val teacherName: String,
    @SerializedName("room_name") val roomName: String?,
    @SerializedName("room_url") val roomUrl: String?,
    @SerializedName(
        value = "learn_type_id",
        alternate = ["lesson_type_id", "type_id"]
    ) val learnTypeId: Int? = null,
    @SerializedName(
        value = "learn_type_name",
        alternate = ["lesson_type_name", "lesson_type", "type_name", "lesson_type_text"]
    ) val learnTypeName: String? = null,
    @SerializedName("lesson_color") val lessonColor: String?
)

data class MediatorJournalResponse(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("subject_id") val subjectId: String?,
    @SerializedName("rows") val rows: List<MediatorJournalRowDto>
)

data class MediatorJournalRowDto(
    @SerializedName("student_public_id") val studentPublicId: String,
    @SerializedName("lesson_date") val lessonDate: String,
    @SerializedName("learn_type_id") val learnTypeId: Int,
    @SerializedName("time_id") val timeId: Int,
    @SerializedName("time_name") val timeName: String,
    @SerializedName("group_public_id") val groupPublicId: String,
    @SerializedName("group_code") val groupCode: String,
    @SerializedName("subject_public_id") val subjectPublicId: String,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_short_name") val subjectShortName: String?,
    @SerializedName("teacher_public_id") val teacherPublicId: String,
    @SerializedName("teacher_name") val teacherName: String,
    @SerializedName("journal_row_id") val journalRowId: Long,
    @SerializedName("grade_id") val gradeId: Int?,
    @SerializedName("grade_name") val gradeName: String?,
    @SerializedName("lesson_color") val lessonColor: String?
)

data class MediatorSubjectsResponse(
    @SerializedName("rows") val rows: List<MediatorSubjectDto>
)

data class MediatorSubjectDto(
    @SerializedName("id") val id: String,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_short_name") val subjectShortName: String?
)

