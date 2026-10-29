/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.repository

import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.BreakReminderConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface managing BrainBuddy fatigue score, daily reel metrics,
 * timestamp-based recovery, and break reminder states.
 */
interface BrainFatigueRepository {
    val fatigueState: StateFlow<BrainFatigueState>
    val breakReminderConfig: StateFlow<BreakReminderConfig>

    /**
     * Called whenever a new short-form reel/video is detected.
     */
    fun onReelWatched(count: Int = 1)

    /**
     * Called periodically during an active short-form viewing session.
     */
    fun onSessionActive(sessionDurationMs: Long)

    /**
     * Called when the user leaves short-form video feeds.
     */
    fun onSessionEnded()

    /**
     * Records baseline device screen time outside short-video feeds.
     */
    fun onGeneralScreenTime(durationMs: Long)

    /**
     * Records short-video watch time where individual reels cannot be counted (e.g. in a browser).
     */
    fun onShortVideoWatchTime(durationMs: Long)

    /**
     * Evaluates timestamp-based recovery on app open or return to screen.
     */
    fun refreshRecovery()

    /**
     * Updates break reminder settings.
     */
    fun updateBreakReminderConfig(config: BreakReminderConfig)

    /**
     * Marks the break reminder as shown at the current timestamp.
     */
    fun markBreakReminderShown()

    /**
     * Checks if a break reminder is eligible to be shown.
     */
    fun canShowBreakReminder(currentSessionDurationMs: Long): Boolean
}

