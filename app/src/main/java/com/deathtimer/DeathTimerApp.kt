package com.deathtimer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DeathTimerApp : Application() {

    companion object {
        const val CHANNEL_ID = "death_timer_channel"
        const val CHANNEL_NAME = "Death Timer"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Death timer notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
