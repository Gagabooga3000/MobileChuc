package com.example.chuc.data.network

import android.util.Log
import com.example.chuc.data.local.AuthPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val cookieJar: PersistentCookieJar
) {
    
    fun hasActiveSession(): Boolean {
        val hasCredentials = authPreferences.isLoggedIn()
        val hasCookies = cookieJar.hasCookies()
        
        Log.d("AuthSessionManager", "hasActiveSession: credentials=$hasCredentials, cookiesPresent=$hasCookies")
        return hasCredentials && hasCookies
    }

    fun clearSession() {
        cookieJar.clear()
        authPreferences.clearCredentials()
        Log.d("AuthSessionManager", "Session cleared")
    }

    fun initializeFromPreferences() {
        if (authPreferences.isLoggedIn() && cookieJar.hasCookies()) {
            Log.d("AuthSessionManager", "Session available from preferences")
        }
    }

    fun getCookiesString(): String = cookieJar.getCookiesString()
}
