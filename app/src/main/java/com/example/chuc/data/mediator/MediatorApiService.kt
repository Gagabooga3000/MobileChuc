package com.example.chuc.data.mediator

import com.example.chuc.data.mediator.model.MediatorGroupsResponse
import com.example.chuc.data.mediator.model.MediatorJournalResponse
import com.example.chuc.data.mediator.model.MediatorScheduleResponse
import com.example.chuc.data.mediator.model.MediatorSubjectsResponse
import com.example.chuc.data.mediator.model.MediatorTeachersResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MediatorApiService {

    @GET("/api/v1/groups/active")
    suspend fun getActiveGroups(): MediatorGroupsResponse

    @GET("/api/v1/teachers/active")
    suspend fun getActiveTeachers(): MediatorTeachersResponse

    @GET("/api/v1/students/me/schedule")
    suspend fun getStudentSchedule(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("group_id") groupId: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): MediatorScheduleResponse

    @GET("/api/v1/teachers/me/schedule")
    suspend fun getTeacherSchedule(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("teacher_id") teacherId: String? = null,
        @Query("week_offset") weekOffset: Int? = null
    ): MediatorScheduleResponse

    @GET("/api/v1/students/me/journal")
    suspend fun getStudentJournal(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("student_id") studentId: String? = null,
        @Query("subject_id") subjectId: String? = null,
        @Query("subject_name") subjectName: String? = null
    ): MediatorJournalResponse

    @GET("/api/v1/students/me/journal/subjects")
    suspend fun getStudentJournalSubjects(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("student_id") studentId: String? = null
    ): MediatorSubjectsResponse
}

