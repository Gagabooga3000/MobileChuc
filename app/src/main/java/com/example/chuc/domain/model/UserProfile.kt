package com.example.chuc.domain.model

data class UserProfile(
    val role: UserRole,
    val groupName: String? = null,
    val teacherName: String? = null
)

