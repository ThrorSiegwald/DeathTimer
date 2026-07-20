package com.deathtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (PreferenceManager.isRunning(context) && !PreferenceManager.isExpired(context)) {
                val serviceIntent = Intent(context, TimerService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
