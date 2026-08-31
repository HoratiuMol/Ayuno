package com.moldovan.ayuno.data

import java.util.Calendar

/**
 * Mensajes motivacionales contextuales: en vez de una sola cita por día del
 * año, el texto cambia según la fase de ayuno activa o la situación de la
 * racha. Sin almacenamiento: la elección es determinista a partir de datos
 * ya disponibles (fase, horas transcurridas, día del año, racha).
 */

// Varias frases por fase (clave = FastingPhase.name).
private val PHASE_MESSAGES: Map<String, List<String>> = mapOf(
    "Fase postprandial" to listOf(
        "La disciplina empieza cuando terminas de comer.",
        "El reloj ya corre a tu favor.",
        "Aún digieres: el ayuno de verdad llega en unas horas.",
        "Nada que hacer ahora salvo dejar pasar el tiempo."
    ),
    "Quema de reservas" to listOf(
        "Tu cuerpo ya tira de las reservas.",
        "Cada hora que pasa quemas un poco más de glucógeno.",
        "El hambre es una ola: sube y luego baja.",
        "Bebe agua; buena parte del hambre es sed."
    ),
    "Cetosis temprana" to listOf(
        "La incomodidad forja control y claridad.",
        "Estás entrando en cetosis: la grasa es ahora tu combustible.",
        "Lo difícil de hoy es la fuerza de mañana.",
        "Ya has llegado más lejos que la mayoría."
    ),
    "Cetosis profunda" to listOf(
        "Cetosis plena: la mente se aclara y la grasa arde.",
        "La autofagia está limpiando tus células ahora mismo.",
        "A partir de aquí el hambre afloja. Sigue.",
        "Estás quemando grasa, no fuerza de voluntad."
    ),
    "Cetosis extendida" to listOf(
        "Tu cuerpo se renueva desde dentro.",
        "Máxima autofagia: territorio de élite.",
        "Escucha a tu cuerpo y para si algo no va bien.",
        "El hambre tiende a desaparecer a partir del tercer día."
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
    currentPhase: FastingPhase,
    nextPhase: FastingPhase?,
    elapsedHours: Float,
    minutesToNextPhase: Long?
): String {
    if (nextPhase != null && minutesToNextPhase != null && minutesToNextPhase in 0..59) {
        return "Casi en «${nextPhase.name}». Aguanta un poco más."
    }
    val pool = PHASE_MESSAGES[currentPhase.name] ?: return currentPhase.motivation
    val idx = ((elapsedHours.toInt() + dayOfYear) % pool.size + pool.size) % pool.size
    return pool[idx]
}

/**
 * Mensaje para la tarjeta de la pantalla de inicio (sin ayuno activo).
 * Devuelve null cuando no hay nada contextual que decir: en ese caso la
 * tarjeta muestra la cita diaria de siempre.
 */
fun streakMotivation(streak: Int, completedToday: Boolean, hasHistory: Boolean): String? = when {
    !hasHistory                     -> "Tu primer ayuno empieza con un solo toque."
    completedToday && streak >= 2   -> "Racha de $streak días asegurada hoy. 🔥"
    completedToday                  -> "Ayuno de hoy completado. Mañana, otra vez."
    streak >= 2                     -> "Llevas $streak días seguidos. No rompas la cadena hoy."
    streak == 1                     -> "Ayer lo lograste. Encadena hoy el segundo día."
    else                            -> null
}
