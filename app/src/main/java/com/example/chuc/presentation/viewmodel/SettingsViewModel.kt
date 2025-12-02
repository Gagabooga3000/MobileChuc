package com.example.chuc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.local.ParserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val parserPreferences: ParserPreferences
) : ViewModel() {
    
    /**
     * Проверить доступность парсера (теперь всегда доступен, так как парсим напрямую)
     */
    fun checkParserAvailability(): Boolean {
        // Теперь парсер всегда доступен, так как мы парсим напрямую с miterra.chuc.ru
        return true
    }
    
    /**
     * Принудительно обновить данные
     */
    fun refreshData() {
        viewModelScope.launch {
            // TODO: добавить синхронизацию с mediator при необходимости
        }
    }
    
    /**
     * Получить текущий тип парсера
     */
    fun getParserType(): String {
        return parserPreferences.getParserType()
    }
    
    /**
     * Установить тип парсера
     */
    fun setParserType(type: String) {
        parserPreferences.setParserType(type)
    }
    
    /**
     * Проверить, включен ли Python сервер
     */
    fun isPythonServerEnabled(): Boolean {
        return parserPreferences.isPythonServerEnabled()
    }
    
    /**
     * Включить/выключить Python сервер
     */
    fun setPythonServerEnabled(enabled: Boolean) {
        parserPreferences.setPythonServerEnabled(enabled)
    }
}
