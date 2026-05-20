package com.moldovan.ayuno.data

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class PhaseNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val phaseName = inputData.getString(KEY_PHASE_NAME) ?: return Result.failure()
        val phaseDesc = inputData.getString(KEY_PHASE_DESC) ?: return Result.failure()
        val notifId   = inputData.getInt(KEY_NOTIF_ID, 0)

        if (FastingStorage(applicationContext).getActiveSession() == null) return Result.success()

        NotificationHelper.showPhaseNotification(applicationContext, phaseName, phaseDesc, notifId)
        return Result.success()
    }

    companion object {
        const val KEY_PHASE_NAME = "phase_name"
        const val KEY_PHASE_DESC = "phase_desc"
        const val KEY_NOTIF_ID   = "notif_id"
        const val WORK_TAG       = "ayuno_phase_notif"
    }
}