package com.example.chuc.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chuc.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotifications @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val manager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_UPDATES,
                "Обновления расписания и новостей",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Изменения расписания, новые оценки и объявления"
            }
            val systemManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.createNotificationChannel(channel)
        }
    }

    fun notifyNewsUpdated(newCount: Int) {
        if (newCount <= 0) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_news)
            .setContentTitle("Новые новости")
            .setContentText("Появилось $newCount новых объявлений")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(ID_NEWS, notification)
    }

    fun notifyScheduleUpdated(groupName: String?) {
        ensureChannels()
        val title = "Расписание обновлено"
        val text = groupName?.let { "Обновлено расписание для группы $it" }
            ?: "Обновлено расписание"
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(ID_SCHEDULE, notification)
    }

    fun notifyNewGrades(newCount: Int) {
        if (newCount <= 0) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_profile)
            .setContentTitle("Новые оценки")
            .setContentText("Появилось $newCount новых оценок в журнале")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(ID_GRADES, notification)
    }

    companion object {
        private const val CHANNEL_UPDATES = "updates"
        private const val ID_NEWS = 1001
        private const val ID_SCHEDULE = 1002
        private const val ID_GRADES = 1003
    }
}


