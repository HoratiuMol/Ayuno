package com.moldovan.ayuno.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class HydrationType(val label: String, val emoji: String) {
    WATER("Agua", "💧"),
    COFFEE("Café", "☕"),
    TEA("Infusión", "🍵")
}

data class HydrationEntry(
    val id: String,
    val timestamp: Long,
    val type: HydrationType,
    val amountMl: Int
)

class HydrationStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ayuno_prefs", Context.MODE_PRIVATE)

    fun addEntry(type: HydrationType, amountMl: Int, timestamp: Long = System.currentTimeMillis()): HydrationEntry {
        val entry = HydrationEntry(
            id        = timestamp.toString() + "_" + type.name,
            timestamp = timestamp,
            type      = type,
            amountMl  = amountMl
        )
        val entries = getEntries().toMutableList()
        entries.add(0, entry)
        saveAll(entries)
        return entry
    }

    fun getTodayEntries(): List<HydrationEntry> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return getEntries().filter { it.timestamp >= startOfDay }
    }

    fun getEntries(): List<HydrationEntry> {
        val raw = prefs.getString(KEY_HYDRATION, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { entryFromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun saveAll(entries: List<HydrationEntry>) {
        // Conserva solo los últimos 30 días para no crecer indefinidamente.
        val cutoff = System.currentTimeMillis() - 30L * 24 * 3_600_000
        val trimmed = entries.filter { it.timestamp >= cutoff }
        val arr = JSONArray()
        trimmed.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_HYDRATION, arr.toString()).apply()
    }

    private fun HydrationEntry.toJson() = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("type", type.name)
        put("amountMl", amountMl)
    }

    private fun entryFromJson(o: JSONObject) = HydrationEntry(
        id        = o.getString("id"),
        timestamp = o.getLong("timestamp"),
        type      = runCatching { HydrationType.valueOf(o.getString("type")) }.getOrDefault(HydrationType.WATER),
        amountMl  = o.getInt("amountMl")
    )

    companion object {
        private const val KEY_HYDRATION = "hydration_history"
    }
}
