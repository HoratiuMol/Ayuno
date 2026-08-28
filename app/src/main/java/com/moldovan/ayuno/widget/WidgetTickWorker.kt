package com.moldovan.ayuno.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moldovan.ayuno.data.FastingStorage
import java.util.concurrent.TimeUnit

/**
 * Refresca el widget de pantalla de inicio cada minuto mientras haya un
 * ayuno activo, reencolándose a sí mismo (WorkManager no permite periodos
 * periódicos por debajo de 15 minutos).
 */
class WidgetTickWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        FastingWidgetProvider.updateAll(applicationContext)
        if (FastingStorage(applicationContext).getActiveSession() != null) {
            scheduleNextTick(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_tick"

        fun start(context: Context) {
            FastingWidgetProvider.updateAll(context)
            if (FastingStorage(context).getActiveSession() != null) {
                scheduleNextTick(context)
            }
        }

        fun scheduleNextTick(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetTickWorker>()
                .setInitialDelay(60, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            FastingWidgetProvider.updateAll(context)
        }
    }
}
