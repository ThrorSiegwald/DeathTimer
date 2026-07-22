package com.deathtimer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var passwordEditText: EditText
    private lateinit var hoursEditText: EditText
    private lateinit var minutesEditText: EditText
    private lateinit var secondsEditText: EditText
    private lateinit var savePasswordButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var saveTimeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки"

        passwordEditText = findViewById(R.id.passwordEditText)
        hoursEditText = findViewById(R.id.hoursEditText)
        minutesEditText = findViewById(R.id.minutesEditText)
        secondsEditText = findViewById(R.id.secondsEditText)
        savePasswordButton = findViewById(R.id.savePasswordButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        saveTimeButton = findViewById(R.id.saveTimeButton)

        loadCurrentSettings()

        savePasswordButton.setOnClickListener { savePassword() }
        changePasswordButton.setOnClickListener { showChangePasswordDialog() }
        saveTimeButton.setOnClickListener { saveTime() }

        updateChangePasswordButton()
    }

    private fun loadCurrentSettings() {
        if (PreferenceManager.isPasswordSet(this)) {
            passwordEditText.hint = "Пароль установлен (введите новый)"
        } else {
            passwordEditText.hint = "Введите пароль"
        }

        val countdownSeconds = PreferenceManager.getCountdownTime(this)
        val hours = countdownSeconds / 3600
        val minutes = (countdownSeconds % 3600) / 60
        val seconds = countdownSeconds % 60

        hoursEditText.setText(hours.toString())
        minutesEditText.setText(minutes.toString())
        secondsEditText.setText(seconds.toString())
    }

    private fun savePassword() {
        val newPassword = passwordEditText.text.toString().trim()
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
            return
        }

        if (PreferenceManager.isPasswordSet(this)) {
            showCurrentPasswordDialog(newPassword)
        } else {
            PreferenceManager.savePassword(this, newPassword)
            Toast.makeText(this, "Пароль сохранён", Toast.LENGTH_SHORT).show()
            passwordEditText.text.clear()
        }
    }

    private fun showCurrentPasswordDialog(newPassword: String) {
        val editText = EditText(this).apply {
            hint = "Введите текущий пароль"
            setPadding(60, 40, 60, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Требуется текущий пароль")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val currentPassword = editText.text.toString()
                if (PreferenceManager.checkPassword(this, currentPassword)) {
                    PreferenceManager.savePassword(this, newPassword)
                    Toast.makeText(this, "Пароль обновлён", Toast.LENGTH_SHORT).show()
                    passwordEditText.text.clear()
                } else {
                    Toast.makeText(this, "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateChangePasswordButton() {
        changePasswordButton.isEnabled = PreferenceManager.isPasswordSet(this)
        if (!PreferenceManager.isPasswordSet(this)) {
            changePasswordButton.alpha = 0.5f
        } else {
            changePasswordButton.alpha = 1.0f
        }
    }

    private fun showChangePasswordDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val currentPasswordInput = EditText(this).apply {
            hint = "Текущий пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        val newPasswordInput = EditText(this).apply {
            hint = "Новый пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        val confirmPasswordInput = EditText(this).apply {
            hint = "Подтвердите новый пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        layout.addView(currentPasswordInput)
        layout.addView(newPasswordInput)
        layout.addView(confirmPasswordInput)

        AlertDialog.Builder(this)
            .setTitle("Смена пароля")
            .setView(layout)
            .setPositiveButton("Сменить") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (!PreferenceManager.checkPassword(this, currentPassword)) {
                    Toast.makeText(this, "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.isEmpty()) {
                    Toast.makeText(this, "Введите новый пароль", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                PreferenceManager.savePassword(this, newPassword)
                Toast.makeText(this, "Пароль успешно изменён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveTime() {
        val hours = hoursEditText.text.toString().toIntOrNull() ?: 0
        val minutes = minutesEditText.text.toString().toIntOrNull() ?: 0
        val seconds = secondsEditText.text.toString().toIntOrNull() ?: 0

        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) {
            Toast.makeText(this, "Установите время больше 0", Toast.LENGTH_SHORT).show()
            return
        }

        PreferenceManager.saveCountdownTime(this, totalSeconds)
        Toast.makeText(this, "Время сохранено: ${hours}ч ${minutes}м ${seconds}с", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
