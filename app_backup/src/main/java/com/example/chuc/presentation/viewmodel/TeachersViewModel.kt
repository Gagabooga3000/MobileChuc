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
class TeachersViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    
    private val _teachersState = MutableLiveData<UiState<List<String>>>()
    val teachersState: LiveData<UiState<List<String>>> = _teachersState
    
    private val _teacherScheduleState = MutableLiveData<UiState<List<TimetableDay>>>()
    val teacherScheduleState: LiveData<UiState<List<TimetableDay>>> = _teacherScheduleState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadTeachers() {
        _teachersState.value = UiState.Loading
        viewModelScope.launch {
            val result = scheduleRepository.getTeachers()
            result.fold(
                onSuccess = { teachers ->
                    if (teachers.isEmpty()) {
                        _teachersState.value = UiState.Error("Преподаватели не найдены")
                    } else {
                        _teachersState.value = UiState.Success(teachers)
                    }
                },
                onFailure = { exception ->
                    _teachersState.value = UiState.Error(exception.message ?: "Ошибка загрузки преподавателей")
                }
            )
        }
    }
    
    fun loadTeacherSchedule(teacherName: String) {
        _teacherScheduleState.value = UiState.Loading
        _isLoading.value = true
        
        viewModelScope.launch {
            val result = scheduleRepository.getTeacherSchedule(teacherName)
            result.fold(
                onSuccess = { schedule ->
                    _teacherScheduleState.value = UiState.Success(schedule)
                },
                onFailure = { exception ->
                    _teacherScheduleState.value = UiState.Error(exception.message ?: "Ошибка загрузки расписания преподавателя")
                }
            )
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _teachersState.value = UiState.Loading
    }
    
    fun clearScheduleError() {
        _teacherScheduleState.value = UiState.Loading
    }
}

