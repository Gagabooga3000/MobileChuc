package com.example.chuc.data.mediator

import com.example.chuc.BuildConfig
import com.example.chuc.data.local.AuthPreferences
import okhttp3.Interceptor
import okhttp3.Response

class DebugLoginInterceptor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        if (BuildConfig.DEBUG) {
            val login = resolveDebugLogin()
            if (!login.isNullOrBlank()) {
                builder.header("X-Debug-Login", login)
            }
        } else {
            // TODO: production auth via ADFS, cookie session after /auth/login
        }

        return chain.proceed(builder.build())
    }

    private fun resolveDebugLogin(): String? {
        val saved = authPreferences.getUsername()
        if (!saved.isNullOrBlank()) return saved
        return BuildConfig.DEBUG_FALLBACK_LOGIN.takeIf { it.isNotBlank() }
    }
}

