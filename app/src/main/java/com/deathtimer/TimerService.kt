package com.deathtimer

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var countdownThread: Thread? = null
    @Volatile
    private var isServiceRunning = false

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.deathtimer.STOP"
        const val ACTION_ADD_TIME = "com.deathtimer.ADD_TIME"
        const val EXTRA_SECONDS = "extra_seconds"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ADD_TIME -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                if (seconds > 0) {
                    val remaining = PreferenceManager.getRemainingSeconds(this) + seconds
                    PreferenceManager.saveRemainingSeconds(this, remaining)
                }
                return START_STICKY
            }
        }

        if (PreferenceManager.isExpired(this)) {
            handleExpiration()
            return START_NOT_STICKY
        }

        if (!isServiceRunning) {
            isServiceRunning = true
            startCountdown()
        }
        return START_STICKY
    }

    private fun startCountdown() {
        acquireWakeLock()

        countdownThread = Thread {
            while (isServiceRunning && !PreferenceManager.isExpired(this@TimerService)) {
                val startTime = PreferenceManager.getStartTime(this@TimerService)

                if (startTime == 0L) {
                    PreferenceManager.saveStartTime(this@TimerService, System.currentTimeMillis())
                }

                val elapsed = (System.currentTimeMillis() - PreferenceManager.getStartTime(this@TimerService)) / 1000
                val currentRemaining = PreferenceManager.getCountdownTime(this@TimerService) - elapsed

                if (currentRemaining <= 0) {
                    PreferenceManager.saveRemainingSeconds(this@TimerService, 0)
                    PreferenceManager.setExpired(this@TimerService, true)
                    PreferenceManager.setRunning(this@TimerService, false)
                    handleExpiration()
                    break
                }

                PreferenceManager.saveRemainingSeconds(this@TimerService, currentRemaining)
                updateNotification(currentRemaining)

                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }

    private fun handleExpiration() {
        val expiryDate = dateFormat.format(Date())

        showExpirationNotification(expiryDate)
        vibrateAndSound()
        showFullScreenNotification(expiryDate)
    }

    private fun vibrateAndSound() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500, 200, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Thread {
            Thread.sleep(5000)
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(this, alarmUri)
                ringtone?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showExpirationNotification(dateString: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, DeathTimerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.notif_time_up))
            .setContentText(getString(R.string.notif_death_text, dateString))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.notif_death_big, dateString)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFullScreenNotification(dateString: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_expired", true)
            putExtra("expiry_date", dateString)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, DeathTimerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.death_message))
            .setContentText(getString(R.string.death_date_format, dateString))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun updateNotification(remainingSeconds: Long) {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, DeathTimerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.notif_time_left))
            .setContentText(timeString)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.notif_stop), stopPendingIntent)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DeathTimer::TimerWakeLock"
        ).apply {
            acquire(60 * 60 * 1000L)
        }
    }

    private fun stopTimer() {
        isServiceRunning = false
        countdownThread?.interrupt()
        countdownThread = null
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        PreferenceManager.setRunning(this, false)
        PreferenceManager.setExpired(this, false)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(NOTIFICATION_ID + 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        countdownThread?.interrupt()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }
}
