package com.example.chuc.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ScheduleViewMode {
    LIST,
    CALENDAR
}

@Singleton
class SchedulePreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getViewMode(): ScheduleViewMode {
        val name = prefs.getString(KEY_VIEW_MODE, ScheduleViewMode.LIST.name)
        return runCatching { ScheduleViewMode.valueOf(name ?: ScheduleViewMode.LIST.name) }
            .getOrDefault(ScheduleViewMode.LIST)
    }

    fun setViewMode(mode: ScheduleViewMode) {
        prefs.edit().putString(KEY_VIEW_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "schedule_prefs"
        private const val KEY_VIEW_MODE = "view_mode"
    }
}


