package com.example.smsalarm

import android.content.Context
import androidx.core.content.edit

object MonitorConfig {

    private const val SP_NAME = "monitor_config"
    private const val KEY_ENABLED = "monitor_enabled"
    private const val KEY_DEBOUNCE_MINUTES = "debounce_minutes"
    private const val KEY_RING_DURATION_MINUTES = "ring_duration_minutes"

    const val DEFAULT_DEBOUNCE_MINUTES = 5
    const val DEFAULT_RING_DURATION_MINUTES = 10

    fun isEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_ENABLED, enabled)
            }
    }

    fun getDebounceMinutes(context: Context): Int {
        return context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DEBOUNCE_MINUTES, DEFAULT_DEBOUNCE_MINUTES)
    }

    fun setDebounceMinutes(context: Context, value: Int) {
        context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_DEBOUNCE_MINUTES, value)
            }
    }

    fun getRingDurationMinutes(context: Context): Int {
        return context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_RING_DURATION_MINUTES, DEFAULT_RING_DURATION_MINUTES)
    }

    fun setRingDurationMinutes(context: Context, value: Int) {
        context
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_RING_DURATION_MINUTES, value)
            }
    }
}
