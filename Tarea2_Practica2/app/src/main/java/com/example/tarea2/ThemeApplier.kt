package com.example.tarea2

import android.app.Activity

object ThemeApplier {
    /** Llamar en cada Activity ANTES de setContentView(...) */
    fun apply(activity: Activity) {
        val dark = ThemePrefs.isDark(activity)
        activity.setTheme(if (dark) R.style.Theme_Tarea2_Dark else R.style.Theme_Tarea2_Light)
    }
}
