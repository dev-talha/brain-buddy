/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Configuration for the non-intrusive short-video break reminder.
 */
@Immutable
data class BreakReminderConfig(
    val isEnabled: Boolean = true,
    val thresholdMinutes: Int = DEFAULT_THRESHOLD_MINUTES,
    val cooldownMinutes: Int = DEFAULT_COOLDOWN_MINUTES,
) {
    companion object {
        const val DEFAULT_THRESHOLD_MINUTES = 20
        const val DEFAULT_COOLDOWN_MINUTES = 30
        const val REMINDER_TEXT = "You've been scrolling for a while. Your little brain needs a break."
    }
}
