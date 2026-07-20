package com.deathtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var deathMessageTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var qrScanButton: Button
    private lateinit var settingsButton: Button
    private lateinit var expiredContainer: LinearLayout
    private lateinit var timerContainer: LinearLayout

    private var countdownThread: Thread? = null
    @Volatile
    private var isRunning = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val NOTIFICATION_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        requestPermissions()
        checkExpiredState(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkExpiredState(intent)
    }

    private fun initViews() {
        timerTextView = findViewById(R.id.timerTextView)
        statusTextView = findViewById(R.id.statusTextView)
        deathMessageTextView = findViewById(R.id.deathMessageTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        qrScanButton = findViewById(R.id.qrScanButton)
        settingsButton = findViewById(R.id.settingsButton)
        expiredContainer = findViewById(R.id.expiredContainer)
        timerContainer = findViewById(R.id.timerContainer)

        startButton.setOnClickListener { startTimer() }
        stopButton.setOnClickListener { stopTimer() }
        resetButton.setOnClickListener { resetTimer() }
        qrScanButton.setOnClickListener { openQRScanner() }
        settingsButton.setOnClickListener { openSettings() }

        updateUI()
    }

    private fun checkExpiredState(intent: Intent?) {
        if (intent?.getBooleanExtra("show_expired", false) == true) {
            val date = intent.getStringExtra("expiry_date") ?: SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss", Locale.getDefault()
            ).format(Date())
            showExpiredScreen(date)
        } else if (PreferenceManager.isExpired(this)) {
            val date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss", Locale.getDefault()
            ).format(Date(PreferenceManager.getExpiryTime(this)))
            showExpiredScreen(date)
        } else if (PreferenceManager.isRunning(this)) {
            isRunning = true
            startLocalCountdown()
        }
    }

    private fun showExpiredScreen(date: String) {
        timerContainer.alpha = 0.3f
        expiredContainer.visibility = android.view.View.VISIBLE
        deathMessageTextView.text = "ТЫ МЕРТВ"
        statusTextView.text = "Дата смерти: $date"
        startButton.isEnabled = false
        stopButton.isEnabled = false
        resetButton.isEnabled = false
        qrScanButton.isEnabled = false
    }

    private fun startTimer() {
        if (!PreferenceManager.isPasswordSet(this)) {
            Toast.makeText(this, "Сначала задайте пароль в настройках", Toast.LENGTH_LONG).show()
            openSettings()
            return
        }

        val countdownSeconds = PreferenceManager.getCountdownTime(this)
        PreferenceManager.saveStartTime(this, System.currentTimeMillis())
        PreferenceManager.saveRemainingSeconds(this, countdownSeconds.toLong())
        PreferenceManager.setRunning(this, true)
        PreferenceManager.setExpired(this, false)

        isRunning = true

        val serviceIntent = Intent(this, TimerService::class.java)
        startForegroundService(serviceIntent)

        startLocalCountdown()
        updateUI()
    }

    private fun startLocalCountdown() {
        countdownThread?.interrupt()
        countdownThread = Thread {
            while (isRunning && !PreferenceManager.isExpired(this)) {
                val elapsed = (System.currentTimeMillis() - PreferenceManager.getStartTime(this)) / 1000
                val remaining = PreferenceManager.getCountdownTime(this) - elapsed

                if (remaining <= 0) {
                    PreferenceManager.saveRemainingSeconds(this, 0)
                    PreferenceManager.setExpired(this, true)
                    PreferenceManager.setRunning(this, false)
                    isRunning = false
                    runOnUiThread {
                        showExpiredScreen(
                            SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                                .format(Date())
                        )
                    }
                    break
                }

                PreferenceManager.saveRemainingSeconds(this, remaining)
                val hours = remaining / 3600
                val minutes = (remaining % 3600) / 60
                val seconds = remaining % 60
                val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                runOnUiThread {
                    timerTextView.text = timeString
                    statusTextView.text = "До конца осталось..."
                }

                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }

    private fun stopTimer() {
        if (!PreferenceManager.isPasswordSet(this)) {
            Toast.makeText(this, "Невозможно без пароля", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = PasswordDialogFragment.newInstance { success ->
            if (success) {
                performStop()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show(supportFragmentManager, "password")
    }

    private fun performStop() {
        isRunning = false
        countdownThread?.interrupt()
        countdownThread = null

        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(serviceIntent)

        PreferenceManager.setRunning(this, false)
        PreferenceManager.setExpired(this, false)
        PreferenceManager.saveRemainingSeconds(this, 0)

        updateUI()
    }

    private fun resetTimer() {
        if (!PreferenceManager.isPasswordSet(this)) {
            Toast.makeText(this, "Невозможно без пароля", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = PasswordDialogFragment.newInstance { success ->
            if (success) {
                performStop()
                timerTextView.text = "00:00:00"
                statusTextView.text = "Таймер не запущен"
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show(supportFragmentManager, "password")
    }

    private fun openQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            return
        }
        val intent = Intent(this, QRScannerActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun updateUI() {
        if (PreferenceManager.isExpired(this)) {
            timerContainer.alpha = 0.3f
            expiredContainer.visibility = android.view.View.VISIBLE
            startButton.isEnabled = false
            stopButton.isEnabled = false
            resetButton.isEnabled = false
            qrScanButton.isEnabled = false
        } else if (PreferenceManager.isRunning(this)) {
            val remaining = PreferenceManager.getRemainingSeconds(this)
            val hours = remaining / 3600
            val minutes = (remaining % 3600) / 60
            val seconds = remaining % 60
            timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            statusTextView.text = "До конца осталось..."
            startButton.isEnabled = false
            stopButton.isEnabled = true
            resetButton.isEnabled = true
            qrScanButton.isEnabled = true
            settingsButton.isEnabled = false
        } else {
            timerTextView.text = "00:00:00"
            statusTextView.text = "Таймер не запущен"
            startButton.isEnabled = true
            stopButton.isEnabled = false
            resetButton.isEnabled = false
            qrScanButton.isEnabled = false
            settingsButton.isEnabled = true
            expiredContainer.visibility = android.view.View.GONE
            timerContainer.alpha = 1.0f
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), NOTIFICATION_PERMISSION_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        checkExpiredState(intent)
        if (!isRunning && PreferenceManager.isRunning(this) && !PreferenceManager.isExpired(this)) {
            isRunning = true
            startLocalCountdown()
        }
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownThread?.interrupt()
    }
}
