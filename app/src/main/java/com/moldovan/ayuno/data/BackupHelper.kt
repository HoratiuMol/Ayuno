package com.moldovan.ayuno.data

import android.content.Context
import org.json.JSONObject

/**
 * Copia de seguridad manual: vuelca las claves relevantes de las
 * SharedPreferences a un único JSON que el usuario guarda donde quiera
 * (Drive, almacenamiento local, correo...) y puede volver a importar más
 * tarde, incluso en otro dispositivo. Sin cuenta ni backend: el usuario
 * controla dónde vive el archivo.
 */
object BackupHelper {

    private const val BACKUP_VERSION = 1
    private const val PREFS_NAME = "ayuno_prefs"

    private val BACKUP_KEYS = listOf(
        FastingStorage.KEY_HISTORY,
        FastingStorage.KEY_ACTIVE,
        WeightStorage.KEY_WEIGHT,
        HydrationStorage.KEY_HYDRATION,
        ThemePreference.KEY_THEME
    )

    fun exportJson(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        BACKUP_KEYS.forEach { key ->
            prefs.getString(key, null)?.let { root.put(key, it) }
        }
        return root.toString(2)
    }

    fun importJson(context: Context, json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)
        require(root.has("backupVersion")) { "Archivo de copia de seguridad no válido" }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        BACKUP_KEYS.forEach { key ->
            if (root.has(key)) editor.putString(key, root.getString(key))
        }
        editor.apply()
    }
}
