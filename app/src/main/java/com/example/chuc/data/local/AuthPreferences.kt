package com.example.chuc.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        scheduleWeeklyReset(LocalDate.now())
        
        android.util.Log.d("AuthPreferences", "Credentials saved for user: $username, isLoggedIn: ${isLoggedIn()}")
    }
    
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)
    
    fun isLoggedIn(): Boolean {
        val result = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        android.util.Log.d("AuthPreferences", "isLoggedIn: $result")
        return result
    }
    
    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_LAST_LOGIN_EPOCH_DAY)
            .remove(KEY_NEXT_RESET_EPOCH_DAY)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
    
    fun hasCredentials(): Boolean = getUsername() != null && getPassword() != null

    fun shouldForceReset(today: LocalDate = LocalDate.now()): Boolean {
        val nextResetEpoch = prefs.getLong(KEY_NEXT_RESET_EPOCH_DAY, -1L)
        return nextResetEpoch > 0 && today.toEpochDay() >= nextResetEpoch
    }

    fun hasActiveSession(today: LocalDate = LocalDate.now()): Boolean {
        return isLoggedIn() && !shouldForceReset(today) && hasCredentials()
    }

    private fun scheduleWeeklyReset(loginDate: LocalDate) {
        val nextResetDate = loginDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        prefs.edit()
            .putLong(KEY_LAST_LOGIN_EPOCH_DAY, loginDate.toEpochDay())
            .putLong(KEY_NEXT_RESET_EPOCH_DAY, nextResetDate.toEpochDay())
            .apply()
    }

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_LOGIN_EPOCH_DAY = "last_login_day"
        private const val KEY_NEXT_RESET_EPOCH_DAY = "next_reset_day"
    }
}
