package com.example.chuc

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.chuc.domain.model.UserProfileStorage
import com.example.chuc.domain.model.UserRole
import com.example.chuc.presentation.viewmodel.MainViewModel
import com.example.chuc.schedule.Screen
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SecondActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupBottomNavigation()
        
        // Наблюдаем за изменениями экрана
        mainViewModel.currentScreen.observe(this, Observer { screen ->
            loadFragmentForScreen(screen)
        })
        
        // Показываем главную страницу по умолчанию
        if (savedInstanceState == null) {
            mainViewModel.navigateTo(Screen.Home)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_nav)
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    mainViewModel.navigateTo(Screen.Home)
                    true
                }
                R.id.nav_schedule -> {
                    mainViewModel.navigateTo(Screen.Schedule)
                    true
                }
                R.id.nav_teachers -> {
                    mainViewModel.navigateTo(Screen.Teachers)
                    true
                }
                R.id.nav_profile -> {
                    mainViewModel.navigateTo(Screen.Profile)
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun loadFragmentForScreen(screen: Screen) {
        val fragment: Fragment? = when (screen) {
            Screen.Home -> HomeFragment.newInstance()

            Screen.Schedule -> {
                // решаем, какое окно расписания показывать
                val profile = UserProfileStorage.load(this)
                if (profile?.role == UserRole.TEACHER) {
                    // преподаватель → список групп
                    ScheduleFragment.newInstance()
                } else {
                    // студент → своё расписание
                    StudentScheduleFragment.newInstance()
                }
            }

            Screen.Teachers -> TeachersFragment.newInstance()
            Screen.Profile -> ProfileFragment.newInstance()

            Screen.Login -> null
        }

        fragment?.let { loadFragment(it) }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.menu_container, fragment)
            .commit()
    }
    
    override fun onBackPressed() {
        if (!mainViewModel.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
