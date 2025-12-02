package com.example.chuc.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.repository.ScheduleRepository
import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _scheduleState = MutableLiveData<UiState<List<TimetableDay>>>()
    val scheduleState: LiveData<UiState<List<TimetableDay>>> = _scheduleState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSchedule(groupName: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _scheduleState.value = UiState.Loading

                // Если группа не указана, пытаемся определить её автоматически
                val targetGroup = if (groupName != null) {
                    groupName
                } else {
                    // Пытаемся получить группу студента из его профиля
                    val groupResult = scheduleRepository.getStudentGroup()
                    groupResult.getOrElse { "ИС-1-22" } // Fallback к дефолтной группе
                }

                // Загружаем расписание для определенной группы
                val result = scheduleRepository.getGroupSchedule(targetGroup)

                result.fold(
                    onSuccess = { schedule ->
                        _scheduleState.value = UiState.Success(schedule)
                    },
                    onFailure = { exception ->
                        _scheduleState.value = UiState.Error(exception.message ?: "Ошибка загрузки расписания")
                    }
                )
            } catch (e: Exception) {
                _scheduleState.value = UiState.Error("Неожиданная ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSchedule() {
        loadSchedule()
    }
    
    fun refreshScheduleWithNewGroup() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _scheduleState.value = UiState.Loading

                // Принудительно обновляем группу студента
                val groupResult = scheduleRepository.refreshStudentGroup()
                val targetGroup = groupResult.getOrElse { "ИС-1-22" }

                // Загружаем расписание для обновленной группы
                val result = scheduleRepository.getGroupSchedule(targetGroup)

                result.fold(
                    onSuccess = { schedule ->
                        _scheduleState.value = UiState.Success(schedule)
                    },
                    onFailure = { exception ->
                        _scheduleState.value = UiState.Error(exception.message ?: "Ошибка загрузки расписания")
                    }
                )
            } catch (e: Exception) {
                _scheduleState.value = UiState.Error("Неожиданная ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadScheduleForGroup(groupName: String) {
        loadSchedule(groupName)
    }

    fun clearError() {
        _scheduleState.value = UiState.Loading
    }
}
