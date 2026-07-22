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
        supportActionBar?.title = getString(R.string.settings_title)

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
            passwordEditText.hint = getString(R.string.password_set_hint)
        } else {
            passwordEditText.hint = getString(R.string.enter_password)
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
            Toast.makeText(this, R.string.enter_password, Toast.LENGTH_SHORT).show()
            return
        }

        if (PreferenceManager.isPasswordSet(this)) {
            showCurrentPasswordDialog(newPassword)
        } else {
            PreferenceManager.savePassword(this, newPassword)
            Toast.makeText(this, R.string.password_saved, Toast.LENGTH_SHORT).show()
            passwordEditText.text.clear()
        }
    }

    private fun showCurrentPasswordDialog(newPassword: String) {
        val editText = EditText(this).apply {
            hint = getString(R.string.enter_current_password)
            setPadding(60, 40, 60, 20)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.current_password_required_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                val currentPassword = editText.text.toString()
                if (PreferenceManager.checkPassword(this, currentPassword)) {
                    PreferenceManager.savePassword(this, newPassword)
                    Toast.makeText(this, R.string.password_updated, Toast.LENGTH_SHORT).show()
                    passwordEditText.text.clear()
                } else {
                    Toast.makeText(this, R.string.current_password_wrong, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel_dialog, null)
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
            hint = getString(R.string.current_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        val newPasswordInput = EditText(this).apply {
            hint = getString(R.string.new_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        val confirmPasswordInput = EditText(this).apply {
            hint = getString(R.string.confirm_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 20, 0, 20)
        }

        layout.addView(currentPasswordInput)
        layout.addView(newPasswordInput)
        layout.addView(confirmPasswordInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.change_password_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_change) { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (!PreferenceManager.checkPassword(this, currentPassword)) {
                    Toast.makeText(this, R.string.current_password_wrong, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.isEmpty()) {
                    Toast.makeText(this, R.string.new_password_hint, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, R.string.passwords_not_match, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                PreferenceManager.savePassword(this, newPassword)
                Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel_dialog, null)
            .show()
    }

    private fun saveTime() {
        val hours = hoursEditText.text.toString().toIntOrNull() ?: 0
        val minutes = minutesEditText.text.toString().toIntOrNull() ?: 0
        val seconds = secondsEditText.text.toString().toIntOrNull() ?: 0

        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) {
            Toast.makeText(this, R.string.time_must_be_positive, Toast.LENGTH_SHORT).show()
            return
        }

        PreferenceManager.saveCountdownTime(this, totalSeconds)
        Toast.makeText(this, getString(R.string.timer_format_saved, hours, minutes, seconds), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
