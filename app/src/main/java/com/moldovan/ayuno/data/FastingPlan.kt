package com.moldovan.ayuno.data

import androidx.annotation.StringRes
import com.moldovan.ayuno.R

enum class FastingDifficulty(@StringRes val labelRes: Int) {
    PRINCIPIANTE(R.string.difficulty_beginner),
    INTERMEDIO(R.string.difficulty_intermediate),
    AVANZADO(R.string.difficulty_advanced)
}

data class FastingPlan(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val subtitleRes: Int,
    val goalHours: Int,
    @StringRes val descriptionRes: Int,
    val difficulty: FastingDifficulty,
    @StringRes val scheduleRes: Int
)

val FASTING_PLANS = listOf(
    FastingPlan(
        id             = "12_12",
        nameRes        = R.string.plan_12_12_name,
        subtitleRes    = R.string.plan_12_12_subtitle,
        goalHours      = 12,
        descriptionRes = R.string.plan_12_12_desc,
        difficulty     = FastingDifficulty.PRINCIPIANTE,
        scheduleRes    = R.string.plan_12_12_schedule
    ),
    FastingPlan(
        id             = "14_10",
        nameRes        = R.string.plan_14_10_name,
        subtitleRes    = R.string.plan_14_10_subtitle,
        goalHours      = 14,
        descriptionRes = R.string.plan_14_10_desc,
        difficulty     = FastingDifficulty.PRINCIPIANTE,
        scheduleRes    = R.string.plan_14_10_schedule
    ),
    FastingPlan(
        id             = "16_8",
        nameRes        = R.string.plan_16_8_name,
        subtitleRes    = R.string.plan_16_8_subtitle,
        goalHours      = 16,
        descriptionRes = R.string.plan_16_8_desc,
        difficulty     = FastingDifficulty.INTERMEDIO,
        scheduleRes    = R.string.plan_16_8_schedule
    ),
    FastingPlan(
        id             = "18_6",
        nameRes        = R.string.plan_18_6_name,
        subtitleRes    = R.string.plan_18_6_subtitle,
        goalHours      = 18,
        descriptionRes = R.string.plan_18_6_desc,
        difficulty     = FastingDifficulty.INTERMEDIO,
        scheduleRes    = R.string.plan_18_6_schedule
    ),
    FastingPlan(
        id             = "20_4",
        nameRes        = R.string.plan_20_4_name,
        subtitleRes    = R.string.plan_20_4_subtitle,
        goalHours      = 20,
        descriptionRes = R.string.plan_20_4_desc,
        difficulty     = FastingDifficulty.AVANZADO,
        scheduleRes    = R.string.plan_20_4_schedule
    ),
    FastingPlan(
        id             = "omad",
        nameRes        = R.string.plan_omad_name,
        subtitleRes    = R.string.plan_omad_subtitle,
        goalHours      = 23,
        descriptionRes = R.string.plan_omad_desc,
        difficulty     = FastingDifficulty.AVANZADO,
        scheduleRes    = R.string.plan_omad_schedule
    ),
    FastingPlan(
        id             = "adf",
        nameRes        = R.string.plan_adf_name,
        subtitleRes    = R.string.plan_adf_subtitle,
        goalHours      = 24,
        descriptionRes = R.string.plan_adf_desc,
        difficulty     = FastingDifficulty.AVANZADO,
        scheduleRes    = R.string.plan_adf_schedule
    ),
    FastingPlan(
        id             = "5_2",
        nameRes        = R.string.plan_5_2_name,
        subtitleRes    = R.string.plan_5_2_subtitle,
        goalHours      = 24,
        descriptionRes = R.string.plan_5_2_desc,
        difficulty     = FastingDifficulty.INTERMEDIO,
        scheduleRes    = R.string.plan_5_2_schedule
    )
)

fun fastingPlanById(id: String?): FastingPlan? = FASTING_PLANS.firstOrNull { it.id == id }

/** Objetivo por defecto para un ayuno sin plan asociado ("ayuno libre"). */
const val FREE_FASTING_GOAL_HOURS = 24

/** True si el plan reserva una ventana de alimentación dentro del mismo día (p. ej. 16:8, OMAD). */
val FastingPlan.hasEatingWindow: Boolean get() = goalHours < 24

/** Horas de ventana de alimentación tras completar el ayuno, 0 si el plan es de día completo (ADF, 5:2). */
val FastingPlan.eatingWindowHours: Int get() = (24 - goalHours).coerceAtLeast(0)
