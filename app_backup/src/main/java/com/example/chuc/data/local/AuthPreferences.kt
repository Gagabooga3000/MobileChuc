package com.example.chuc.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString("username", username)
            .putString("password", password)
            .putBoolean("is_logged_in", true)
            .apply()
        
        // Логируем для отладки
        android.util.Log.d("AuthPreferences", "Credentials saved for user: $username, isLoggedIn: ${isLoggedIn()}")
    }
    
    fun getUsername(): String? = prefs.getString("username", null)
    
    fun getPassword(): String? = prefs.getString("password", null)
    
    fun isLoggedIn(): Boolean {
        val result = prefs.getBoolean("is_logged_in", false)
        android.util.Log.d("AuthPreferences", "isLoggedIn: $result")
        return result
    }
    
    fun clearCredentials() {
        prefs.edit()
            .remove("username")
            .remove("password")
            .putBoolean("is_logged_in", false)
            .apply()
    }
    
    fun hasCredentials(): Boolean = getUsername() != null && getPassword() != null
}
