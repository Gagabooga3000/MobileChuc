package com.example.chuc.test

import com.example.chuc.data.repository.ScheduleRepository
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.network.WebService
import org.junit.Test

/**
 * Простой тест для проверки компиляции основных классов
 */
class CompilationTest {
    
    @Test
    fun testScheduleRepositoryCompilation() {
        // Этот тест проверяет, что классы компилируются без ошибок
        val scheduleRepoClass = ScheduleRepository::class.java
        assert(scheduleRepoClass != null)
    }
    
    @Test
    fun testAuthPreferencesCompilation() {
        val authPrefsClass = AuthPreferences::class.java
        assert(authPrefsClass != null)
    }
    
    @Test
    fun testWebServiceCompilation() {
        val webServiceClass = WebService::class.java
        assert(webServiceClass != null)
    }
}
