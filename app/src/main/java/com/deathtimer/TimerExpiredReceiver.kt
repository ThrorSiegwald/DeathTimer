package com.deathtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val expiryDate = dateFormat.format(Date())

        PreferenceManager.setExpired(context, true)
        PreferenceManager.setRunning(context, false)

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_expired", true)
            putExtra("expiry_date", expiryDate)
        }
        context.startActivity(notificationIntent)

        val serviceIntent = Intent(context, TimerService::class.java)
        context.startService(serviceIntent)
    }
}
