package com.example.chuc

import android.app.Application
import com.example.chuc.presentation.notifications.AppNotifications
import dagger.hilt.android.HiltAndroidApp

import com.example.chuc.presentation.theme.ThemeManager
import javax.inject.Inject

@HiltAndroidApp
class ChucApplication : Application() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var appNotifications: AppNotifications

    override fun onCreate() {
        super.onCreate()
        themeManager.applyStoredTheme()
        appNotifications.ensureChannels()
    }
}

