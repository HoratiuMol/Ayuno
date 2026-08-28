package com.moldovan.ayuno.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class WeightEntry(
    val id: String,
    val timestamp: Long,
    val weightKg: Double
)

class WeightStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ayuno_prefs", Context.MODE_PRIVATE)

    fun addEntry(weightKg: Double, timestamp: Long = System.currentTimeMillis()): WeightEntry {
        val entry = WeightEntry(
            id        = timestamp.toString(),
            timestamp = timestamp,
            weightKg  = weightKg
        )
        val entries = getEntries().toMutableList()
        entries.add(0, entry)
        entries.sortByDescending { it.timestamp }
        saveAll(entries)
        return entry
    }

    fun deleteEntry(id: String) {
        val entries = getEntries().filterNot { it.id == id }
        saveAll(entries)
    }

    fun getEntries(): List<WeightEntry> {
        val raw = prefs.getString(KEY_WEIGHT, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { entryFromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun saveAll(entries: List<WeightEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_WEIGHT, arr.toString()).apply()
    }

    private fun WeightEntry.toJson() = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("weightKg", weightKg)
    }

    private fun entryFromJson(o: JSONObject) = WeightEntry(
        id        = o.getString("id"),
        timestamp = o.getLong("timestamp"),
        weightKg  = o.getDouble("weightKg")
    )

    companion object {
        private const val KEY_WEIGHT = "weight_history"
    }
}
