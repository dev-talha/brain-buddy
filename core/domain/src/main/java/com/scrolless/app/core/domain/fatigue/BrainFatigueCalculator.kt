/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.domain.fatigue

import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.BreakReminderConfig

/**
 * Pure calculation logic for BrainBuddy fatigue, recovery, and reminder intervals.
 *
 * All recovery calculations are derived strictly from timestamps, eliminating the
 * need for continuous background timers or battery-draining wake locks.
 */
object BrainFatigueCalculator {

    const val MAX_FATIGUE = 100
    const val MIN_FATIGUE = 0
    const val RECOVERY_GRACE_PERIOD_MINUTES = 5L
    const val RECOVERY_RATE_PER_MINUTE = 1

    /**
     * Calculates the recovered fatigue score after being away from short-form feeds.
     *
     * Rules:
     * - First 5 minutes away: 0 fatigue reduction (grace period).
     * - After 5 minutes: -1 fatigue point per additional minute away.
     * - Bounded between 0 and 100.
     *
     * @param currentFatigue Current fatigue score before recovery (0..100)
     * @param lastActiveEpochMs Epoch timestamp when the user last watched short videos
     * @param nowEpochMs Current epoch timestamp
     */
    fun calculateRecoveredFatigue(
        currentFatigue: Int,
        lastActiveEpochMs: Long,
        nowEpochMs: Long,
    ): Int {
        if (currentFatigue <= MIN_FATIGUE) return MIN_FATIGUE
        if (lastActiveEpochMs <= 0L || nowEpochMs <= lastActiveEpochMs) return currentFatigue.coerceIn(MIN_FATIGUE, MAX_FATIGUE)

        val elapsedMs = nowEpochMs - lastActiveEpochMs
        val elapsedMinutes = elapsedMs / 60_000L

        if (elapsedMinutes <= RECOVERY_GRACE_PERIOD_MINUTES) {
            return currentFatigue.coerceIn(MIN_FATIGUE, MAX_FATIGUE)
        }

        val activeRecoveryMinutes = elapsedMinutes - RECOVERY_GRACE_PERIOD_MINUTES
        val fatigueReduction = (activeRecoveryMinutes * RECOVERY_RATE_PER_MINUTE).toInt()

        return (currentFatigue - fatigueReduction).coerceIn(MIN_FATIGUE, MAX_FATIGUE)
    }

    /**
     * Increments fatigue score when new reels are detected.
     */
    fun addReelFatigue(currentFatigue: Int, newReels: Int = 1): Int {
        if (newReels <= 0) return currentFatigue
        return (currentFatigue + newReels).coerceIn(MIN_FATIGUE, MAX_FATIGUE)
    }

    /**
     * Watch-time fallback calculation when individual reel boundaries cannot be
     * reliably identified. Converts continuous watch time into fatigue points
     * without duplicate counting.
     *
     * Rule: 1 fatigue point per 30 seconds of short-video watch time.
     */
    fun addWatchTimeFatigue(
        currentFatigue: Int,
        watchTimeMs: Long,
        alreadyCreditedReels: Int = 0,
    ): Int {
        val watchTimePoints = (watchTimeMs / 30_000L).toInt()
        val additionalPoints = (watchTimePoints - alreadyCreditedReels).coerceAtLeast(0)
        return (currentFatigue + additionalPoints).coerceIn(MIN_FATIGUE, MAX_FATIGUE)
    }

    /**
     * Evaluates if a non-intrusive break reminder should be triggered.
     *
     * @param continuousSessionDurationMs Duration of current continuous short-video session in ms
     * @param lastReminderShownEpochMs When the reminder was last displayed
     * @param nowEpochMs Current epoch ms
     * @param config User reminder configuration
     */
    fun shouldTriggerBreakReminder(
        continuousSessionDurationMs: Long,
        lastReminderShownEpochMs: Long,
        nowEpochMs: Long,
        config: BreakReminderConfig,
    ): Boolean {
        if (!config.isEnabled) return false

        val thresholdMs = config.thresholdMinutes * 60_000L
        if (continuousSessionDurationMs < thresholdMs) return false

        if (lastReminderShownEpochMs > 0L) {
            val cooldownMs = config.cooldownMinutes * 60_000L
            if ((nowEpochMs - lastReminderShownEpochMs) < cooldownMs) {
                return false
            }
        }

        return true
    }

    /**
     * Updates [BrainFatigueState] on local midnight boundary.
     *
     * Daily reel count resets to 0 at midnight, but fatigue score does NOT abruptly
     * reset to 0; it continues its timestamp-based recovery path.
     */
    fun applyMidnightRollover(
        state: BrainFatigueState,
        currentEpochDay: Long,
    ): BrainFatigueState {
        return if (state.lastMidnightEpochDay > 0L && currentEpochDay > state.lastMidnightEpochDay) {
            state.copy(
                todayReelCount = 0,
                lastMidnightEpochDay = currentEpochDay,
            )
        } else if (state.lastMidnightEpochDay == 0L) {
            state.copy(lastMidnightEpochDay = currentEpochDay)
        } else {
            state
        }
    }
}
