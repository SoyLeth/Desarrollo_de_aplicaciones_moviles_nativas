package com.example.tarea2

import android.content.Context
import android.content.SharedPreferences

object ThemePrefs {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK = "isDarkModeEnabled"

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false)

    fun setDark(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, enabled).apply()
    }
}
