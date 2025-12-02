package com.example.chuc.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface WebService {
    @FormUrlEncoded
    @POST("tt/ajxShowTT")
    @Headers("X-Requested-With: XMLHttpRequest", "Accept: text/html")
    suspend fun postShowTTByGroup(
        @Field("gr") group: String,
        @Field("day") day: String,
        @Field("city") city: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("tt/ajxShowTTt")
    @Headers("X-Requested-With: XMLHttpRequest", "Accept: text/html")
    suspend fun postShowTTByTeacher(
        @Field("tid") teacherId: String,
        @Field("day") day: String,
        @Field("city") city: String
    ): Response<ResponseBody>

    @GET("site/login")
    suspend fun getLoginPage(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("site/login")
    suspend fun postLogin(@FieldMap form: Map<String, @JvmSuppressWildcards String>): Response<ResponseBody>

    @GET("tt/byGroups")
    suspend fun getGroups(): Response<ResponseBody>

    @GET("tt/byTeachers")
    suspend fun getTeachers(): Response<ResponseBody>

    @GET("tt/byGroups")
    suspend fun getGroupSchedule(@Query("gr") groupName: String): Response<ResponseBody>

    @GET("tt/byTeachers")
    suspend fun getTeacherSchedule(@Query("tid") teacherId: String): Response<ResponseBody>

    @GET("tt/byGroups")
    suspend fun getScheduleData(): Response<ResponseBody>

    @GET("news")
    suspend fun getNews(): Response<ResponseBody>

    @GET("news/{newsId}")
    suspend fun getNewsDetail(@Path("newsId") newsId: Int): Response<ResponseBody>

    // Python Parser API endpoints
    @GET("api/schedule")
    suspend fun getPythonScheduleData(): Response<ResponseBody>

    @GET("api/groups")
    suspend fun getPythonGroups(): Response<ResponseBody>

    @GET("api/teachers")
    suspend fun getPythonTeachers(): Response<ResponseBody>

    @GET("schedule/group/{groupName}")
    suspend fun getPythonGroupSchedule(@Path("groupName") groupName: String): Response<ResponseBody>

    @GET("schedule/teacher/{teacherName}")
    suspend fun getPythonTeacherSchedule(@Path("teacherName") teacherName: String): Response<ResponseBody>

    @GET("api/update")
    suspend fun triggerPythonUpdate(): Response<ResponseBody>
}