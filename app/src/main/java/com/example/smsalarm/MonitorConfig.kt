package com.example.smsalarm

import android.content.Context
import androidx.core.content.edit

object MonitorConfig {

    private const val SP_NAME = "monitor_config"
    private const val KEY_ENABLED = "monitor_enabled"

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
}
