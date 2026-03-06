package com.example.ayuno.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FastingSession(
    val id: String,
    val startTime: Long,       // epoch milliseconds
    val endTime: Long? = null,
    val goalHours: Int,
    val completed: Boolean = false
)

class FastingStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ayuno_prefs", Context.MODE_PRIVATE)

    fun getActiveSession(): FastingSession? {
        val json = prefs.getString(KEY_ACTIVE, null) ?: return null
        return runCatching { sessionFromJson(JSONObject(json)) }.getOrNull()
    }

    // ÚNICO CAMBIO: startTime es ahora opcional.
    // Si el usuario indica que lleva X horas en ayuno, se pasa
    // System.currentTimeMillis() - (X * 3_600_000) como startTime.
    fun startSession(
        goalHours: Int,
        startTime: Long = System.currentTimeMillis()
    ): FastingSession {
        val session = FastingSession(
            id = System.currentTimeMillis().toString(),
            startTime = startTime,
            goalHours = goalHours
        )
        prefs.edit().putString(KEY_ACTIVE, session.toJson().toString()).apply()
        return session
    }

    fun endSession() {
        val active = getActiveSession() ?: return
        val finished = active.copy(
            endTime = System.currentTimeMillis(),
            completed = true
        )
        saveToHistory(finished)
        prefs.edit().remove(KEY_ACTIVE).apply()
    }

    fun cancelSession() {
        prefs.edit().remove(KEY_ACTIVE).apply()
    }

    fun getHistory(): List<FastingSession> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { sessionFromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun saveToHistory(session: FastingSession) {
        val history = getHistory().toMutableList()
        history.add(0, session)
        val arr = JSONArray()
        history.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun FastingSession.toJson() = JSONObject().apply {
        put("id", id)
        put("startTime", startTime)
        endTime?.let { put("endTime", it) }
        put("goalHours", goalHours)
        put("completed", completed)
    }

    private fun sessionFromJson(o: JSONObject) = FastingSession(
        id = o.getString("id"),
        startTime = o.getLong("startTime"),
        endTime = if (o.has("endTime")) o.getLong("endTime") else null,
        goalHours = o.getInt("goalHours"),
        completed = o.optBoolean("completed", false)
    )

    companion object {
        private const val KEY_ACTIVE  = "active_session"
        private const val KEY_HISTORY = "fasting_history"
    }
}