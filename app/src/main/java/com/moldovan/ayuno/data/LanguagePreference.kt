package com.moldovan.ayuno.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage { SPANISH, ENGLISH, SYSTEM }

private const val PREFS = "ayuno_prefs"
private const val KEY_LANGUAGE = "app_language"

object LanguagePreference {

    // Se ejecuta una única vez, en el primer arranque, para que el idioma
    // por defecto de la app sea español aunque el idioma del sistema no lo sea.
    fun ensureDefaultLanguage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LANGUAGE)) {
            prefs.edit().putString(KEY_LANGUAGE, AppLanguage.SPANISH.name).apply()
        }
    }

    fun getLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_LANGUAGE, AppLanguage.SPANISH.name)) {
            AppLanguage.ENGLISH.name -> AppLanguage.ENGLISH
            AppLanguage.SYSTEM.name  -> AppLanguage.SYSTEM
            else                     -> AppLanguage.SPANISH
        }
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    // Envuelve el contexto base con el idioma guardado. Debe llamarse desde
    // attachBaseContext() de la Activity para que los recursos (strings,
    // stringResource) se resuelvan ya en el idioma elegido.
    fun applyLanguage(context: Context): Context {
        val language = getLanguage(context)
        if (language == AppLanguage.SYSTEM) return context

        val locale = Locale.forLanguageTag(if (language == AppLanguage.ENGLISH) "en" else "es")
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
