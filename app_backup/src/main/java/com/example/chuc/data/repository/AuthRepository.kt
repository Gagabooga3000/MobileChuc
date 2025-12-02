package com.example.chuc.data.repository

import android.util.Log
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.model.LoginRequest
import com.example.chuc.data.model.LoginResponse
import com.example.chuc.data.model.User
import com.example.chuc.data.network.AuthSessionManager
import com.example.chuc.data.network.WebService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val webService: WebService,
    private val authPreferences: AuthPreferences,
    private val sessionManager: AuthSessionManager
) {
    
    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Attempting login for user: ${request.username}")
            
            // Сначала получаем страницу логина для извлечения CSRF токена
            val loginPageResponse = webService.getLoginPage()
            if (!loginPageResponse.isSuccessful) {
                Log.e("AuthRepository", "Failed to get login page: ${loginPageResponse.code()}")
                return@withContext Result.failure(Exception("Не удалось загрузить страницу авторизации"))
            }
            
            val loginPageHtml = loginPageResponse.body()?.string() ?: ""
            Log.d("AuthRepository", "Login page HTML received: ${loginPageHtml.length} chars")
            
            // Создаем форму для отправки
            val formData = mutableMapOf<String, String>()
            formData["LoginForm[username]"] = request.username
            formData["LoginForm[password]"] = request.password
            
            // Отправляем форму логина
            val response = webService.postLogin(formData)
            
            if (response.isSuccessful) {
                // Логируем все заголовки для отладки
                Log.d("AuthRepository", "Response headers: ${response.headers()}")
                
                // Проверяем успешность авторизации
                val isSuccess = isLoginSuccessful(response)
                
                if (isSuccess) {
                    authPreferences.saveCredentials(request.username, request.password)
                    
                    val user = User(
                        username = request.username,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis()
                    )
                    
                    Log.d("AuthRepository", "Login successful for user: ${request.username}")
                    
                    Result.success(
                        LoginResponse(
                            success = true,
                            message = "Успешная авторизация",
                            user = user
                        )
                    )
                } else {
                    Log.w("AuthRepository", "Login failed - invalid credentials")
                    Result.failure(Exception("Неверный логин или пароль"))
                }
            } else {
                Log.e("AuthRepository", "Login failed with code: ${response.code()}")
                Result.failure(Exception("Ошибка сервера: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login exception", e)
            Result.failure(e)
        }
    }
    
    private fun isLoginSuccessful(response: retrofit2.Response<okhttp3.ResponseBody>): Boolean {
        return when {
            response.code() == 302 -> {
                // Редирект после успешной авторизации
                val location = response.headers()["Location"]
                val isRedirect = location != null && !location.contains("login")
                Log.d("AuthRepository", "Redirect check: $isRedirect, location: $location")
                isRedirect
            }
            response.code() == 200 -> {
                // Проверяем содержимое ответа
                val body = response.body()?.string() ?: ""
                Log.d("AuthRepository", "Response body length: ${body.length}")
                Log.d("AuthRepository", "Response body preview: ${body.take(500)}")
                
                // Проверяем, не получили ли мы страницу логина
                val isLoginPage = body.contains("LoginForm") || 
                                body.contains("авторизаци") || 
                                body.contains("login") ||
                                body.contains("Myiitera - Login") ||
                                body.contains("password") ||
                                body.contains("username")
                
                val hasError = body.contains("Неверный логин или пароль") || 
                              body.contains("error") || 
                              body.contains("ошибка")
                
                val hasSuccess = body.contains("расписание") || 
                                body.contains("выход") || 
                                body.contains("timetable") ||
                                body.contains("групп") ||
                                body.contains("преподавател")
                
                Log.d("AuthRepository", "Body check - isLoginPage: $isLoginPage, hasError: $hasError, hasSuccess: $hasSuccess")
                
                // Если это страница логина, значит авторизация не удалась
                if (isLoginPage) {
                    Log.w("AuthRepository", "Received login page - authentication failed")
                    return false
                }
                
                // Если есть ошибки, авторизация не удалась
                if (hasError) {
                    Log.w("AuthRepository", "Found error in response - authentication failed")
                    return false
                }
                
                // Если есть признаки успешной авторизации, значит все ОК
                if (hasSuccess) {
                    Log.d("AuthRepository", "Found success indicators - authentication successful")
                    return true
                }
                
                // Если нет явных признаков ошибки или успеха, считаем неуспешным
                Log.w("AuthRepository", "No clear success/error indicators - assuming failed")
                false
            }
            else -> {
                Log.d("AuthRepository", "Unexpected response code: ${response.code()}")
                false
            }
        }
    }
    
    suspend fun autoLogin(): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val username = authPreferences.getUsername()
            val password = authPreferences.getPassword()
            
            if (username != null && password != null) {
                val request = LoginRequest(username, password)
                login(request)
            } else {
                Result.failure(Exception("Нет сохраненных данных для авторизации"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionManager.clearSession()
            authPreferences.clearCredentials()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isLoggedIn(): Boolean = authPreferences.isLoggedIn()
    
    fun hasCredentials(): Boolean = authPreferences.hasCredentials()
    
    fun hasActiveSession(): Boolean = sessionManager.hasActiveSession()
}
