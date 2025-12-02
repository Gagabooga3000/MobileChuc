package com.example.chuc.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("parser_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_PARSER_TYPE = "parser_type"
        private const val KEY_PYTHON_SERVER_URL = "python_server_url"
        private const val KEY_PYTHON_SERVER_ENABLED = "python_server_enabled"
        
        const val PARSER_TYPE_NATIVE = "native"
        const val PARSER_TYPE_PYTHON = "python"
        const val PARSER_TYPE_AUTO = "auto"
        
        const val DEFAULT_PYTHON_SERVER_URL = "http://localhost:5000/"
    }
    
    /**
     * Получить текущий тип парсера
     */
    fun getParserType(): String {
        return prefs.getString(KEY_PARSER_TYPE, PARSER_TYPE_AUTO) ?: PARSER_TYPE_AUTO
    }
    
    /**
     * Установить тип парсера
     */
    fun setParserType(type: String) {
        prefs.edit().putString(KEY_PARSER_TYPE, type).apply()
    }
    
    /**
     * Получить URL Python сервера
     */
    fun getPythonServerUrl(): String {
        return prefs.getString(KEY_PYTHON_SERVER_URL, DEFAULT_PYTHON_SERVER_URL) ?: DEFAULT_PYTHON_SERVER_URL
    }
    
    /**
     * Установить URL Python сервера
     */
    fun setPythonServerUrl(url: String) {
        prefs.edit().putString(KEY_PYTHON_SERVER_URL, url).apply()
    }
    
    /**
     * Проверить, включен ли Python сервер
     */
    fun isPythonServerEnabled(): Boolean {
        return prefs.getBoolean(KEY_PYTHON_SERVER_ENABLED, true)
    }
    
    /**
     * Включить/выключить Python сервер
     */
    fun setPythonServerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PYTHON_SERVER_ENABLED, enabled).apply()
    }
    
    /**
     * Проверить, должен ли использоваться Python парсер
     */
    fun shouldUsePythonParser(): Boolean {
        val parserType = getParserType()
        return when (parserType) {
            PARSER_TYPE_PYTHON -> true
            PARSER_TYPE_NATIVE -> false
            PARSER_TYPE_AUTO -> isPythonServerEnabled()
            else -> false
        }
    }
}
