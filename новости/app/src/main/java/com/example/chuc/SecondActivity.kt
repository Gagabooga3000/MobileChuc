package com.example.chuc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class SecondActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupBottomNavigation()
        
        // Показываем главную страницу по умолчанию
        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance())
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_nav)
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment.newInstance())
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(ScheduleFragment.newInstance())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.menu_container, fragment)
            .commit()
    }
}
