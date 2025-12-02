package com.example.chuc.data.network

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = url.host
        val existingCookies = cookieStore[domain] ?: emptyList()
        
        // Объединяем существующие куки с новыми, заменяя дублирующиеся
        val cookieMap = existingCookies.associateBy { it.name }.toMutableMap()
        
        cookies.forEach { newCookie ->
            cookieMap[newCookie.name] = newCookie
        }
        
        cookieStore[domain] = cookieMap.values.toList()
        
        Log.d("CookieJar", "Saved cookies for $domain: ${cookies.size} cookies")
        
        // Логируем важные куки
        cookies.forEach { cookie ->
            if (cookie.name.contains("session") || cookie.name.contains("auth") || cookie.name.contains("PHPSESSID")) {
                Log.d("CookieJar", "Important cookie: ${cookie.name}=${cookie.value}")
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val domain = url.host
        val cookies = cookieStore[domain] ?: emptyList()
        
        Log.d("CookieJar", "Loading cookies for URL: $url")
        Log.d("CookieJar", "Available cookies for domain $domain: ${cookies.size}")
        
        val valid = cookies.filter { cookie ->
            val matches = cookie.matches(url)
            val notExpired = cookie.expiresAt >= System.currentTimeMillis()
            Log.d("CookieJar", "Cookie ${cookie.name}: matches=$matches, notExpired=$notExpired")
            matches && notExpired
        }
        
        Log.d("CookieJar", "Loaded cookies for $domain: ${valid.size} valid cookies")
        valid.forEach { cookie ->
            Log.d("CookieJar", "Sending cookie: ${cookie.name}=${cookie.value}")
        }
        
        return valid
    }

    fun clear() {
        cookieStore.clear()
        Log.d("CookieJar", "All cookies cleared")
    }

    fun hasCookies(): Boolean = cookieStore.values.any { it.isNotEmpty() }

    fun getCookiesString(): String = cookieStore.values.flatten().joinToString("; ") { it.name + "=<redacted>" }
}