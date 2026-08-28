package com.moldovan.ayuno.data

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

data class AchievementProgress(
    val achievement: Achievement,
    val unlocked: Boolean
)

private val ACHIEVEMENTS = listOf(
    Achievement("first_fast", "Primer paso", "Completa tu primer ayuno.", "🌱"),
    Achievement("streak_3", "En marcha", "Alcanza una racha de 3 días seguidos.", "🔥"),
    Achievement("streak_7", "Una semana", "Alcanza una racha de 7 días seguidos.", "🏅"),
    Achievement("streak_30", "Un mes de constancia", "Alcanza una racha de 30 días seguidos.", "🏆"),
    Achievement("streak_100", "Imparable", "Alcanza una racha de 100 días seguidos.", "💎"),
    Achievement("completed_10", "Diez ayunos", "Completa 10 ayunos.", "🔟"),
    Achievement("completed_50", "Cincuenta ayunos", "Completa 50 ayunos.", "🥇"),
    Achievement("deep_ketosis", "Cetosis profunda", "Completa un ayuno de al menos 24 horas.", "🧠"),
    Achievement("extended_ketosis", "Cetosis extendida", "Completa un ayuno de al menos 72 horas.", "🌌"),
    Achievement("plan_explorer", "Explorador de planes", "Completa ayunos con 3 planes distintos.", "🧭")
)

/**
 * Calcula los logros desbloqueados a partir del historial, sin necesidad de
 * persistencia propia: el estado se deriva siempre de `FastingStorage`.
 */
fun computeAchievements(history: List<FastingSession>, currentStreak: Int): List<AchievementProgress> {
    val completed = history.filter { it.completed }
    val completedPlans = completed.mapNotNull { it.planId }.distinct().size
    val maxDurationHours = completed.maxOfOrNull { s ->
        ((s.endTime ?: s.startTime) - s.startTime) / 3_600_000.0
    } ?: 0.0

    val unlocked = setOfNotNull(
        "first_fast".takeIf { completed.isNotEmpty() },
        "streak_3".takeIf { currentStreak >= 3 },
        "streak_7".takeIf { currentStreak >= 7 },
        "streak_30".takeIf { currentStreak >= 30 },
        "streak_100".takeIf { currentStreak >= 100 },
        "completed_10".takeIf { completed.size >= 10 },
        "completed_50".takeIf { completed.size >= 50 },
        "deep_ketosis".takeIf { maxDurationHours >= 24 },
        "extended_ketosis".takeIf { maxDurationHours >= 72 },
        "plan_explorer".takeIf { completedPlans >= 3 }
    )

    return ACHIEVEMENTS.map { AchievementProgress(it, it.id in unlocked) }
}
