package com.example.chuc.test

import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.network.WebService
import com.example.chuc.data.mediator.MediatorRepository
import org.junit.Test

/**
 * Простой тест для проверки компиляции основных классов
 */
class CompilationTest {
    
    @Test
    fun testMediatorRepositoryCompilation() {
        val mediatorRepoClass = MediatorRepository::class.java
        assert(mediatorRepoClass != null)
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
