package com.moldovan.ayuno.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.moldovan.ayuno.MainActivity
import com.moldovan.ayuno.R
import com.moldovan.ayuno.data.FASTING_PHASES
import com.moldovan.ayuno.data.FastingStorage
import com.moldovan.ayuno.data.fastingPlanById

class FastingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FastingWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { updateWidget(context, manager, it) }
        }

        fun hasActiveWidgets(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            return manager.getAppWidgetIds(ComponentName(context, FastingWidgetProvider::class.java)).isNotEmpty()
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_fasting)

            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            val session = FastingStorage(context).getActiveSession()
            if (session == null) {
                views.setTextViewText(R.id.widget_phase, "Sin ayuno activo")
                views.setTextViewText(R.id.widget_time, "--:--")
                views.setTextViewText(R.id.widget_subtitle, "Toca para abrir Ayuno")
                views.setProgressBar(R.id.widget_progress, 1000, 0, false)
            } else {
                val now          = System.currentTimeMillis()
                val elapsedMs    = now - session.startTime
                val elapsedHours = elapsedMs / 3_600_000f

                val currentPhase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
                    ?: FASTING_PHASES.first()
                val nextPhase = FASTING_PHASES.firstOrNull { it.startHour > elapsedHours }

                val phaseStartMs = currentPhase.startHour * 3_600_000L
                val phaseEndMs   = (nextPhase?.startHour ?: (currentPhase.startHour + 4)) * 3_600_000L
                val phaseDurMs   = phaseEndMs - phaseStartMs
                val phaseElapsed = elapsedMs - phaseStartMs
                val progress     = (phaseElapsed.toFloat() / phaseDurMs).coerceIn(0f, 1f)

                val remainingMs  = if (nextPhase != null) phaseEndMs - elapsedMs else elapsedMs
                val remainingH   = (remainingMs / 3_600_000).coerceAtLeast(0)
                val remainingMin = ((remainingMs % 3_600_000) / 60_000).coerceAtLeast(0)

                val planName = fastingPlanById(session.planId)?.name ?: "Ayuno libre"

                views.setTextViewText(R.id.widget_phase, currentPhase.name.uppercase())
                views.setTextViewText(R.id.widget_time, "%02d:%02d".format(remainingH, remainingMin))
                views.setTextViewText(R.id.widget_subtitle, planName)
                views.setProgressBar(R.id.widget_progress, 1000, (progress * 1000).toInt(), false)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
