package com.moldovan.ayuno.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class ThemePreference(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ayuno_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        return when (prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name  -> ThemeMode.DARK
            else                 -> ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}