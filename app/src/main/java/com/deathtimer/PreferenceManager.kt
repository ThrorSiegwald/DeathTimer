package com.deathtimer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferenceManager {

    private const val PREFS_NAME = "death_timer_prefs"
    private const val KEY_PASSWORD = "password"
    private const val KEY_COUNTDOWN_SECONDS = "countdown_seconds"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_REMAINING_SECONDS = "remaining_seconds"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_IS_EXPIRED = "is_expired"
    private const val KEY_EXPIRY_TIME = "expiry_time"
    private const val KEY_PASSWORD_SET = "password_set"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassword(context: Context, password: String) {
        getEncryptedPrefs(context).edit {
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_PASSWORD_SET, true)
        }
    }

    fun checkPassword(context: Context, password: String): Boolean {
        val stored = getEncryptedPrefs(context).getString(KEY_PASSWORD, null)
        return stored == password
    }

    fun isPasswordSet(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_PASSWORD_SET, false)
    }

    fun saveCountdownTime(context: Context, seconds: Int) {
        getEncryptedPrefs(context).edit {
            putInt(KEY_COUNTDOWN_SECONDS, seconds)
        }
    }

    fun getCountdownTime(context: Context): Int {
        return getEncryptedPrefs(context).getInt(KEY_COUNTDOWN_SECONDS, 3600)
    }

    fun setRunning(context: Context, running: Boolean) {
        getEncryptedPrefs(context).edit {
            putBoolean(KEY_IS_RUNNING, running)
        }
    }

    fun isRunning(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_IS_RUNNING, false)
    }

    fun saveRemainingSeconds(context: Context, seconds: Long) {
        getEncryptedPrefs(context).edit {
            putLong(KEY_REMAINING_SECONDS, seconds)
        }
    }

    fun getRemainingSeconds(context: Context): Long {
        return getEncryptedPrefs(context).getLong(KEY_REMAINING_SECONDS, 0)
    }

    fun saveStartTime(context: Context, time: Long) {
        getEncryptedPrefs(context).edit {
            putLong(KEY_START_TIME, time)
        }
    }

    fun getStartTime(context: Context): Long {
        return getEncryptedPrefs(context).getLong(KEY_START_TIME, 0)
    }

    fun setExpired(context: Context, expired: Boolean) {
        getEncryptedPrefs(context).edit {
            putBoolean(KEY_IS_EXPIRED, expired)
        }
    }

    fun isExpired(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_IS_EXPIRED, false)
    }

    fun getExpiryTime(context: Context): Long {
        return getEncryptedPrefs(context).getLong(KEY_EXPIRY_TIME, 0)
    }
}
