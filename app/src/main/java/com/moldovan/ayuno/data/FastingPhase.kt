package com.moldovan.ayuno.data

import androidx.annotation.StringRes
import com.moldovan.ayuno.R

data class FastingPhase(
    val id: String,
    @StringRes val nameRes: Int,
    val startHour: Int,
    val endHour: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val motivationRes: Int,
    val benefitsRes: List<Int>,
    val cautionsRes: List<Int>,
    @StringRes val hungerLevelRes: Int,
    val hungerEmoji: String
)

val FASTING_PHASES = listOf(
    FastingPhase(
        id             = "postprandial",
        nameRes        = R.string.phase_postprandial_name,
        startHour      = 0,
        endHour        = 6,
        descriptionRes = R.string.phase_postprandial_desc,
        motivationRes  = R.string.phase_postprandial_motivation,
        benefitsRes    = listOf(
            R.string.phase_postprandial_benefit_1,
            R.string.phase_postprandial_benefit_2
        ),
        cautionsRes    = listOf(R.string.phase_postprandial_caution_1),
        hungerLevelRes = R.string.hunger_none,
        hungerEmoji    = "😌"
    ),
    FastingPhase(
        id             = "glycogen",
        nameRes        = R.string.phase_glycogen_name,
        startHour      = 6,
        endHour        = 16,
        descriptionRes = R.string.phase_glycogen_desc,
        motivationRes  = R.string.phase_glycogen_motivation,
        benefitsRes    = listOf(
            R.string.phase_glycogen_benefit_1,
            R.string.phase_glycogen_benefit_2,
            R.string.phase_glycogen_benefit_3,
            R.string.phase_glycogen_benefit_4
        ),
        cautionsRes    = listOf(
            R.string.phase_glycogen_caution_1,
            R.string.phase_glycogen_caution_2
        ),
        hungerLevelRes = R.string.hunger_increasing,
        hungerEmoji    = "😐"
    ),
    FastingPhase(
        id             = "early_ketosis",
        nameRes        = R.string.phase_early_ketosis_name,
        startHour      = 16,
        endHour        = 24,
        descriptionRes = R.string.phase_early_ketosis_desc,
        motivationRes  = R.string.phase_early_ketosis_motivation,
        benefitsRes    = listOf(
            R.string.phase_early_ketosis_benefit_1,
            R.string.phase_early_ketosis_benefit_2,
            R.string.phase_early_ketosis_benefit_3,
            R.string.phase_early_ketosis_benefit_4
        ),
        cautionsRes    = listOf(
            R.string.phase_early_ketosis_caution_1,
            R.string.phase_early_ketosis_caution_2
        ),
        hungerLevelRes = R.string.hunger_high,
        hungerEmoji    = "😣"
    ),
    FastingPhase(
        id             = "deep_ketosis",
        nameRes        = R.string.phase_deep_ketosis_name,
        startHour      = 24,
        endHour        = 72,
        descriptionRes = R.string.phase_deep_ketosis_desc,
        motivationRes  = R.string.phase_deep_ketosis_motivation,
        benefitsRes    = listOf(
            R.string.phase_deep_ketosis_benefit_1,
            R.string.phase_deep_ketosis_benefit_2,
            R.string.phase_deep_ketosis_benefit_3,
            R.string.phase_deep_ketosis_benefit_4,
            R.string.phase_deep_ketosis_benefit_5
        ),
        cautionsRes    = listOf(
            R.string.phase_deep_ketosis_caution_1,
            R.string.phase_deep_ketosis_caution_2,
            R.string.phase_deep_ketosis_caution_3,
            R.string.phase_deep_ketosis_caution_4
        ),
        hungerLevelRes = R.string.hunger_decreasing,
        hungerEmoji    = "🙂"
    ),
    FastingPhase(
        id             = "extended_ketosis",
        nameRes        = R.string.phase_extended_ketosis_name,
        startHour      = 72,
        endHour        = 96,
        descriptionRes = R.string.phase_extended_ketosis_desc,
        motivationRes  = R.string.phase_extended_ketosis_motivation,
        benefitsRes    = listOf(
            R.string.phase_extended_ketosis_benefit_1,
            R.string.phase_extended_ketosis_benefit_2,
            R.string.phase_extended_ketosis_benefit_3
        ),
        cautionsRes    = listOf(
            R.string.phase_extended_ketosis_caution_1,
            R.string.phase_extended_ketosis_caution_2,
            R.string.phase_extended_ketosis_caution_3,
            R.string.phase_extended_ketosis_caution_4,
            R.string.phase_extended_ketosis_caution_5
        ),
        hungerLevelRes = R.string.hunger_low,
        hungerEmoji    = "😶"
    )
)

fun fastingPhaseById(id: String): FastingPhase? = FASTING_PHASES.firstOrNull { it.id == id }
