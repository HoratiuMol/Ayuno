package com.moldovan.ayuno.data

import androidx.annotation.StringRes
import com.moldovan.ayuno.R

data class Achievement(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val emoji: String
)

data class AchievementProgress(
    val achievement: Achievement,
    val unlocked: Boolean
)

private val ACHIEVEMENTS = listOf(
    Achievement("first_fast", R.string.achievement_first_fast_title, R.string.achievement_first_fast_desc, "🌱"),
    Achievement("streak_3", R.string.achievement_streak_3_title, R.string.achievement_streak_3_desc, "🔥"),
    Achievement("streak_7", R.string.achievement_streak_7_title, R.string.achievement_streak_7_desc, "🏅"),
    Achievement("streak_30", R.string.achievement_streak_30_title, R.string.achievement_streak_30_desc, "🏆"),
    Achievement("streak_100", R.string.achievement_streak_100_title, R.string.achievement_streak_100_desc, "💎"),
    Achievement("completed_10", R.string.achievement_completed_10_title, R.string.achievement_completed_10_desc, "🔟"),
    Achievement("completed_50", R.string.achievement_completed_50_title, R.string.achievement_completed_50_desc, "🥇"),
    Achievement("deep_ketosis", R.string.achievement_deep_ketosis_title, R.string.achievement_deep_ketosis_desc, "🧠"),
    Achievement("extended_ketosis", R.string.achievement_extended_ketosis_title, R.string.achievement_extended_ketosis_desc, "🌌"),
    Achievement("plan_explorer", R.string.achievement_plan_explorer_title, R.string.achievement_plan_explorer_desc, "🧭")
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
