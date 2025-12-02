package com.example.chuc.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chuc.schedule.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    
    private val _currentScreen = MutableLiveData<Screen>(Screen.Home)
    val currentScreen: LiveData<Screen> = _currentScreen
    
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }
    
    fun onBackPressed(): Boolean {
        return when (_currentScreen.value) {
            Screen.Login -> false
            else -> {
                navigateTo(Screen.Home)
                true
            }
        }
    }
}
