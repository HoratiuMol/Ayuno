package com.moldovan.ayuno.data

enum class FastingDifficulty(val label: String) {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado")
}

data class FastingPlan(
    val id: String,
    val name: String,
    val subtitle: String,
    val goalHours: Int,
    val description: String,
    val difficulty: FastingDifficulty,
    val schedule: String
)

val FASTING_PLANS = listOf(
    FastingPlan(
        id          = "12_12",
        name        = "12:12",
        subtitle    = "Iniciación",
        goalHours   = 12,
        description = "Un ayuno suave de 12 horas, ideal para empezar a familiarizarte con el ayuno intermitente sin apenas esfuerzo.",
        difficulty  = FastingDifficulty.PRINCIPIANTE,
        schedule    = "Ej.: cena a las 20:00, desayuno a las 08:00"
    ),
    FastingPlan(
        id          = "14_10",
        name        = "14:10",
        subtitle    = "Progresión",
        goalHours   = 14,
        description = "Amplía ligeramente la ventana de ayuno mientras tu cuerpo se adapta al método.",
        difficulty  = FastingDifficulty.PRINCIPIANTE,
        schedule    = "Ej.: cena a las 20:00, desayuno a las 10:00"
    ),
    FastingPlan(
        id          = "16_8",
        name        = "16:8",
        subtitle    = "Leangains",
        goalHours   = 16,
        description = "El método de ayuno intermitente más popular: 16 horas de ayuno y una ventana de 8 horas para comer.",
        difficulty  = FastingDifficulty.INTERMEDIO,
        schedule    = "Ej.: cena a las 20:00, comida a las 12:00"
    ),
    FastingPlan(
        id          = "18_6",
        name        = "18:6",
        subtitle    = "Ventana reducida",
        goalHours   = 18,
        description = "Reduce la ventana de alimentación a 6 horas para profundizar en los beneficios metabólicos del ayuno.",
        difficulty  = FastingDifficulty.INTERMEDIO,
        schedule    = "Ej.: cena a las 18:00, comida a las 12:00"
    ),
    FastingPlan(
        id          = "20_4",
        name        = "20:4",
        subtitle    = "Warrior Diet",
        goalHours   = 20,
        description = "Una única ventana de 4 horas para comer al día; requiere experiencia previa con ayunos más cortos.",
        difficulty  = FastingDifficulty.AVANZADO,
        schedule    = "Ej.: una comida principal entre las 16:00 y las 20:00"
    ),
    FastingPlan(
        id          = "omad",
        name        = "OMAD",
        subtitle    = "Una comida al día",
        goalHours   = 23,
        description = "One Meal A Day: 23 horas de ayuno con una única comida diaria. Máxima simplicidad, requiere experiencia.",
        difficulty  = FastingDifficulty.AVANZADO,
        schedule    = "Ej.: una única comida a las 18:00"
    ),
    FastingPlan(
        id          = "adf",
        name        = "ADF",
        subtitle    = "Días alternos",
        goalHours   = 24,
        description = "Alternate Day Fasting: ayunos completos de 24 horas en días alternos, combinados con días de alimentación normal.",
        difficulty  = FastingDifficulty.AVANZADO,
        schedule    = "Ej.: ayuna un día sí y un día no"
    ),
    FastingPlan(
        id          = "5_2",
        name        = "5:2",
        subtitle    = "Restricción calórica",
        goalHours   = 24,
        description = "5 días de alimentación normal y 2 días no consecutivos con ingesta muy reducida. Aquí, cada día de restricción se registra como un ayuno de 24h.",
        difficulty  = FastingDifficulty.INTERMEDIO,
        schedule    = "Ej.: restricción los lunes y jueves"
    )
)

fun fastingPlanById(id: String?): FastingPlan? = FASTING_PLANS.firstOrNull { it.id == id }

/** Objetivo por defecto para un ayuno sin plan asociado ("ayuno libre"). */
const val FREE_FASTING_GOAL_HOURS = 24
