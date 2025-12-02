package com.example.chuc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val username: String,
    val password: String? = null, // В реальном приложении пароль не должен храниться
    val fullName: String? = null,
    val group: String? = null,
    val email: String? = null,
    val isLoggedIn: Boolean = false,
    val lastLoginTime: Long = System.currentTimeMillis()
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null
)

