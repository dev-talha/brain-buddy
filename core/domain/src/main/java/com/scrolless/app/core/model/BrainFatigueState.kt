/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Visual emotional poses for the BrainBuddy mascot across fatigue levels.
 *
 * - ENERGETIC: 0–24 reels / fatigue
 * - TIRED: 25–49 reels / fatigue
 * - EXHAUSTED: 50–99 reels / fatigue
 * - DEPLETED: 100 reels / fatigue
 */
enum class MascotPose(val minFatigue: Int, val maxFatigue: Int) {
    ENERGETIC(0, 24),
    TIRED(25, 49),
    EXHAUSTED(50, 99),
    DEPLETED(100, 100);

    companion object {
        fun fromFatigue(fatigue: Int): MascotPose {
            val clamped = fatigue.coerceIn(0, 100)
            return when {
                clamped <= 24 -> ENERGETIC
                clamped <= 49 -> TIRED
                clamped <= 99 -> EXHAUSTED
                else -> DEPLETED
            }
        }
    }
}

/**
 * State representing BrainBuddy digital wellbeing metrics.
 *
 * Distinct values:
 * - [fatigueScore]: 0-100 score reflecting short-video exhaustion and recovery.
 * - [todayReelCount]: Total detected reels today, resets at local midnight.
 * - [currentSessionCount]: Detected reels in the current active session.
 * - [lastActiveTimestampEpochMs]: Timestamp when user was last active in short-video feeds.
 * - [lastMidnightEpochDay]: Epoch day of the last recorded date for midnight rollover.
 */
@Immutable
data class BrainFatigueState(
    val fatigueScore: Int = 0,
    val todayReelCount: Int = 0,
    val currentSessionCount: Int = 0,
    val lastActiveTimestampEpochMs: Long = 0L,
    val lastMidnightEpochDay: Long = 0L,
) {
    val pose: MascotPose
        get() = MascotPose.fromFatigue(fatigueScore)

    val healthPercentage: Int
        get() = (100 - fatigueScore).coerceIn(0, 100)
}

