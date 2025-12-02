package com.example.chuc.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.model.LoginRequest
import com.example.chuc.data.model.LoginResponse
import com.example.chuc.data.repository.AuthRepository
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableLiveData<UiState<LoginResponse>>()
    val loginState: LiveData<UiState<LoginResponse>> = _loginState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("Пожалуйста, заполните все поля")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _loginState.value = UiState.Loading

            val result = authRepository.login(LoginRequest(username, password))
            
            result.fold(
                onSuccess = { response ->
                    _loginState.value = UiState.Success(response)
                    // Сессия автоматически сохраняется в AuthRepository
                },
                onFailure = { exception ->
                    _loginState.value = UiState.Error(exception.message ?: "Ошибка авторизации")
                }
            )
            
            _isLoading.value = false
        }
    }
    
    fun autoLogin() {
        if (!authRepository.hasCredentials()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _loginState.value = UiState.Loading

            val result = authRepository.autoLogin()
            result.fold(
                onSuccess = { response -> _loginState.value = UiState.Success(response) },
                onFailure = { error ->
                    _loginState.value = UiState.Error(error.message ?: "Не удалось восстановить сессию")
                }
            )
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _loginState.value = UiState.Loading
    }
}
