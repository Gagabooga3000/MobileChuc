package com.example.chuc.data.repository

import android.util.Log
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.model.LoginRequest
import com.example.chuc.data.model.LoginResponse
import com.example.chuc.data.model.User
import com.example.chuc.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val mediatorRepository: MediatorRepository
) {

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            if (request.username.isBlank() || request.password.isBlank()) {
                return@withContext Result.failure(Exception("Введите логин и пароль"))
            }

            val role = determineRole(request.username)
            authPreferences.saveCredentials(request.username, request.password)

            val validationResult = validateWithMediator(role)
            validationResult.fold(
                onSuccess = {
                    Result.success(
                        LoginResponse(
                            success = true,
                            message = "Успешная авторизация",
                            user = User(
                                username = request.username,
                                isLoggedIn = true,
                                lastLoginTime = System.currentTimeMillis()
                            )
                        )
                    )
                },
                onFailure = { error ->
                    authPreferences.clearCredentials()
                    Result.failure(Exception(error.message ?: "Mediator недоступен"))
                }
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            Result.failure(e)
        }
    }

    private suspend fun validateWithMediator(role: UserRole): Result<Unit> {
        val (from, to) = dateRangeForValidation()
        return when (role) {
            UserRole.STUDENT -> mediatorRepository.getStudentSchedule(from, to)
            UserRole.TEACHER -> mediatorRepository.getTeacherSchedule(from, to)
        }.map { }
    }

    private fun determineRole(username: String): UserRole {
        return if (username.any { it.isDigit() }) {
            UserRole.STUDENT
        } else {
            UserRole.TEACHER
        }
    }

    private fun dateRangeForValidation(): Pair<LocalDate, LocalDate> {
        val start = LocalDate.now()
        return start to start.plusDays(7)
    }

    suspend fun autoLogin(): Result<LoginResponse> = withContext(Dispatchers.IO) {
        if (shouldForceReset()) {
            authPreferences.clearCredentials()
            return@withContext Result.failure(Exception("Срок действия сессии истёк"))
        }

        val username = authPreferences.getUsername()
        val password = authPreferences.getPassword()

        if (username != null && password != null) {
            login(LoginRequest(username, password))
        } else {
            Result.failure(Exception("Нет сохраненных данных для авторизации"))
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        authPreferences.clearCredentials()
        Result.success(Unit)
    }

    fun isLoggedIn(): Boolean = authPreferences.isLoggedIn()

    fun hasCredentials(): Boolean = authPreferences.hasCredentials()

    fun shouldForceReset(): Boolean = authPreferences.shouldForceReset()

    fun hasActiveSession(): Boolean = authPreferences.hasActiveSession()
}
