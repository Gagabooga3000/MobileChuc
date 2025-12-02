package com.example.chuc.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chuc.data.model.News
import com.example.chuc.data.repository.NewsRepository
import com.example.chuc.schedule.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val _newsState = MutableLiveData<UiState<List<News>>>()
    val newsState: LiveData<UiState<List<News>>> = _newsState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _newsState.value = UiState.Loading
            
            val result = newsRepository.getNews()
            
            result.fold(
                onSuccess = { news ->
                    _newsState.value = UiState.Success(news)
                },
                onFailure = { exception ->
                    _newsState.value = UiState.Error(exception.message ?: "Ошибка загрузки новостей")
                }
            )
            
            _isLoading.value = false
        }
    }
    
    fun refreshNews() {
        loadNews()
    }
    
    fun clearError() {
        _newsState.value = UiState.Loading
    }
}

