package com.moldovan.ayuno.data

import android.content.Context
import com.moldovan.ayuno.R
import java.util.Calendar

/**
 * Mensajes motivacionales contextuales: en vez de una sola cita por día del
 * año, el texto cambia según la fase de ayuno activa o la situación de la
 * racha. Sin almacenamiento: la elección es determinista a partir de datos
 * ya disponibles (fase, horas transcurridas, día del año, racha).
 */

// Varias frases por fase (clave = FastingPhase.id).
private val PHASE_MESSAGES: Map<String, List<Int>> = mapOf(
    "postprandial" to listOf(
        R.string.motiv_postprandial_1,
        R.string.motiv_postprandial_2,
        R.string.motiv_postprandial_3,
        R.string.motiv_postprandial_4
    ),
    "glycogen" to listOf(
        R.string.motiv_glycogen_1,
        R.string.motiv_glycogen_2,
        R.string.motiv_glycogen_3,
        R.string.motiv_glycogen_4
    ),
    "early_ketosis" to listOf(
        R.string.motiv_early_ketosis_1,
        R.string.motiv_early_ketosis_2,
        R.string.motiv_early_ketosis_3,
        R.string.motiv_early_ketosis_4
    ),
    "deep_ketosis" to listOf(
        R.string.motiv_deep_ketosis_1,
        R.string.motiv_deep_ketosis_2,
        R.string.motiv_deep_ketosis_3,
        R.string.motiv_deep_ketosis_4
    ),
    "extended_ketosis" to listOf(
        R.string.motiv_extended_ketosis_1,
        R.string.motiv_extended_ketosis_2,
        R.string.motiv_extended_ketosis_3,
        R.string.motiv_extended_ketosis_4
    )
)

private val dayOfYear: Int
    get() = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

/**
 * Frase para el anillo durante un ayuno activo.
 * @param minutesToNextPhase minutos que faltan para la siguiente fase, o null
 *   si ya no hay fase posterior.
 */
fun fastingMotivation(
    context: Context,
    currentPhase: FastingPhase,
    nextPhase: FastingPhase?,
    elapsedHours: Float,
    minutesToNextPhase: Long?
): String {
    if (nextPhase != null && minutesToNextPhase != null && minutesToNextPhase in 0..59) {
        return context.getString(
            R.string.motivation_almost_next_phase,
            context.getString(nextPhase.nameRes)
        )
    }
    val pool = PHASE_MESSAGES[currentPhase.id] ?: return context.getString(currentPhase.motivationRes)
    val idx = ((elapsedHours.toInt() + dayOfYear) % pool.size + pool.size) % pool.size
    return context.getString(pool[idx])
}

/**
 * Mensaje para la tarjeta de la pantalla de inicio (sin ayuno activo).
 * Devuelve null cuando no hay nada contextual que decir: en ese caso la
 * tarjeta muestra la cita diaria de siempre.
 */
fun streakMotivation(context: Context, streak: Int, completedToday: Boolean, hasHistory: Boolean): String? = when {
    !hasHistory                     -> context.getString(R.string.streak_first_fast)
    completedToday && streak >= 2   -> context.getString(R.string.streak_completed_multi, streak)
    completedToday                  -> context.getString(R.string.streak_completed_today)
    streak >= 2                     -> context.getString(R.string.streak_ongoing, streak)
    streak == 1                     -> context.getString(R.string.streak_day_one)
    else                            -> null
}
