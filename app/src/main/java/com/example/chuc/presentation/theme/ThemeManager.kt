package com.example.chuc.presentation.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun applyStoredTheme() {
        AppCompatDelegate.setDefaultNightMode(getCurrentTheme().nightMode)
    }

    fun toggleTheme(): ThemeMode {
        val nextMode = if (getCurrentTheme() == ThemeMode.DARK) {
            ThemeMode.LIGHT
        } else {
            ThemeMode.DARK
        }
        setTheme(nextMode)
        return nextMode
    }

    fun setTheme(themeMode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode.nightMode).apply()
        AppCompatDelegate.setDefaultNightMode(themeMode.nightMode)
    }

    fun getCurrentTheme(): ThemeMode {
        val savedMode = prefs.getInt(KEY_THEME_MODE, ThemeMode.DARK.nightMode)
        return ThemeMode.fromNightMode(savedMode)
    }

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}

enum class ThemeMode(val nightMode: Int) {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromNightMode(@AppCompatDelegate.NightMode nightMode: Int): ThemeMode {
            return values().firstOrNull { it.nightMode == nightMode } ?: DARK
        }
    }
}

