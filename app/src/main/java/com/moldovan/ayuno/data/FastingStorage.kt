package com.moldovan.ayuno.data

import android.content.Context
import android.content.SharedPreferences
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.moldovan.ayuno.widget.WidgetTickWorker
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FastingSession(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val goalHours: Int,
    val completed: Boolean = false,
    val completedPhases: List<String> = emptyList(),
    val planId: String? = null
)

class FastingStorage(context: Context) {

    private val context = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ayuno_prefs", Context.MODE_PRIVATE)

    fun getActiveSession(): FastingSession? {
        val json = prefs.getString(KEY_ACTIVE, null) ?: return null
        return runCatching { sessionFromJson(JSONObject(json)) }.getOrNull()
    }

    fun startSession(
        goalHours: Int,
        startTime: Long = System.currentTimeMillis(),
        planId: String? = null
    ): FastingSession {
        val session = FastingSession(
            id = System.currentTimeMillis().toString(),
            startTime = startTime,
            goalHours = goalHours,
            planId = planId
        )
        prefs.edit().putString(KEY_ACTIVE, session.toJson().toString()).apply()
        schedulePhaseNotifications(startTime, goalHours)
        WidgetTickWorker.start(context)
        return session
    }

    //devuelve fastingSession para mostrar diálogo
    fun endSession(): FastingSession? {
        val active = getActiveSession() ?: return null
        val elapsedHours = (System.currentTimeMillis() - active.startTime) / 3_600_000f
        val phases = FASTING_PHASES.filter { it.startHour < elapsedHours }.map { it.name }
        val finished = active.copy(
            endTime          = System.currentTimeMillis(),
            completed        = true,
            completedPhases  = phases
        )
        saveToHistory(finished)
        cancelPhaseNotifications()
        prefs.edit().remove(KEY_ACTIVE).apply()
        WidgetTickWorker.stop(context)
        return finished
    }

    fun cancelSession() {
        val active = getActiveSession() ?: return
        val elapsedHours = (System.currentTimeMillis() - active.startTime) / 3_600_000f
        val phases = FASTING_PHASES.filter { it.startHour < elapsedHours }.map { it.name }
        val cancelled = active.copy(
            endTime = System.currentTimeMillis(),
            completed = false,
            completedPhases = phases
        )
        saveToHistory(cancelled)
        cancelPhaseNotifications()
        prefs.edit().remove(KEY_ACTIVE).apply()
        WidgetTickWorker.stop(context)
    }

    fun getHistory(): List<FastingSession> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { sessionFromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun schedulePhaseNotifications(startTime: Long, goalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(PhaseNotificationWorker.WORK_TAG)

        val now = System.currentTimeMillis()
        FASTING_PHASES.forEachIndexed { index, phase ->
            if (phase.startHour == 0) return@forEachIndexed
            if (phase.startHour > goalHours) return@forEachIndexed
            val phaseStartMs = startTime + phase.startHour * 3_600_000L
            val delayMs = phaseStartMs - now
            if (delayMs <= 0) return@forEachIndexed

            val shortDesc = phase.description.substringBefore('.').take(120)
            val data = workDataOf(
                PhaseNotificationWorker.KEY_PHASE_NAME to phase.name,
                PhaseNotificationWorker.KEY_PHASE_DESC to shortDesc,
                PhaseNotificationWorker.KEY_NOTIF_ID to (NOTIF_BASE_ID + index)
            )

            val request = OneTimeWorkRequestBuilder<PhaseNotificationWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(PhaseNotificationWorker.WORK_TAG)
                .build()

            workManager.enqueue(request)
        }
    }

    private fun cancelPhaseNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(PhaseNotificationWorker.WORK_TAG)
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
        put("completedPhases", JSONArray(completedPhases))
        planId?.let { put("planId", it) }
    }

    private fun sessionFromJson(o: JSONObject) = FastingSession(
        id = o.getString("id"),
        startTime = o.getLong("startTime"),
        endTime = if (o.has("endTime")) o.getLong("endTime") else null,
        goalHours = o.getInt("goalHours"),
        completed = o.optBoolean("completed", false),
        completedPhases = runCatching {
            val arr = o.getJSONArray("completedPhases")
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList()),
        planId = if (o.has("planId")) o.getString("planId") else null
    )

    companion object {
        private const val KEY_ACTIVE   = "active_session"
        private const val KEY_HISTORY  = "fasting_history"
        private const val NOTIF_BASE_ID = 1000
    }
}

/** Días consecutivos (incluyendo hoy) con al menos un ayuno completado. */
fun computeFastingStreak(history: List<FastingSession>): Int {
    val completed = history.filter { it.completed }
    if (completed.isEmpty()) return 0
    val calendar = java.util.Calendar.getInstance()
    var streakCount = 0
    var checkDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val year = calendar.get(java.util.Calendar.YEAR)
    for (session in completed) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = session.startTime
        val sessionDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val sessionYear = cal.get(java.util.Calendar.YEAR)
        if (sessionYear == year && sessionDay == checkDay) {
            streakCount++
            checkDay--
        } else break
    }
    return streakCount
}