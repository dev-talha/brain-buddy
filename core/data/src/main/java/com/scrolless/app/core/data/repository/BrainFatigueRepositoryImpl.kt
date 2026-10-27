/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.domain.fatigue.BrainFatigueCalculator
import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.BreakReminderConfig
import com.scrolless.app.core.repository.BrainFatigueRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class BrainFatigueRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider,
) : BrainFatigueRepository {

    companion object {
        private const val PREFS_NAME = "brain_buddy_fatigue_prefs"
        private const val KEY_FATIGUE_SCORE = "fatigue_score"
        private const val KEY_TODAY_REEL_COUNT = "today_reel_count"
        private const val KEY_CURRENT_SESSION_COUNT = "current_session_count"
        private const val KEY_LAST_ACTIVE_TIMESTAMP = "last_active_timestamp"
        private const val KEY_LAST_MIDNIGHT_EPOCH_DAY = "last_midnight_epoch_day"
        private const val KEY_LAST_REMINDER_SHOWN = "last_reminder_shown"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_THRESHOLD = "reminder_threshold"
        private const val KEY_REMINDER_COOLDOWN = "reminder_cooldown"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _fatigueState: MutableStateFlow<BrainFatigueState>
    override val fatigueState: StateFlow<BrainFatigueState>

    private val _breakReminderConfig: MutableStateFlow<BreakReminderConfig>
    override val breakReminderConfig: StateFlow<BreakReminderConfig>

    private var lastReminderShownTimestamp: Long = 0L

    init {
        val initialFatigue = prefs.getInt(KEY_FATIGUE_SCORE, 0)
        val initialTodayReels = prefs.getInt(KEY_TODAY_REEL_COUNT, 0)
        val initialSessionCount = prefs.getInt(KEY_CURRENT_SESSION_COUNT, 0)
        val initialLastActive = prefs.getLong(KEY_LAST_ACTIVE_TIMESTAMP, 0L)
        val initialMidnightDay = prefs.getLong(KEY_LAST_MIDNIGHT_EPOCH_DAY, 0L)
        lastReminderShownTimestamp = prefs.getLong(KEY_LAST_REMINDER_SHOWN, 0L)

        val reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        val reminderThreshold = prefs.getInt(KEY_REMINDER_THRESHOLD, BreakReminderConfig.DEFAULT_THRESHOLD_MINUTES)
        val reminderCooldown = prefs.getInt(KEY_REMINDER_COOLDOWN, BreakReminderConfig.DEFAULT_COOLDOWN_MINUTES)

        _breakReminderConfig = MutableStateFlow(
            BreakReminderConfig(
                isEnabled = reminderEnabled,
                thresholdMinutes = reminderThreshold,
                cooldownMinutes = reminderCooldown,
            ),
        )
        breakReminderConfig = _breakReminderConfig.asStateFlow()

        val rawState = BrainFatigueState(
            fatigueScore = initialFatigue,
            todayReelCount = initialTodayReels,
            currentSessionCount = initialSessionCount,
            lastActiveTimestampEpochMs = initialLastActive,
            lastMidnightEpochDay = initialMidnightDay,
        )

        // Apply recovery & rollover on startup
        val now = timeProvider.currentTimeInMillis()
        val currentEpochDay = timeProvider.localDateNow().toEpochDay()
        val rolledState = BrainFatigueCalculator.applyMidnightRollover(rawState, currentEpochDay)
        val recoveredFatigue = BrainFatigueCalculator.calculateRecoveredFatigue(
            rolledState.fatigueScore,
            rolledState.lastActiveTimestampEpochMs,
            now,
        )

        val finalInitialState = rolledState.copy(fatigueScore = recoveredFatigue)
        _fatigueState = MutableStateFlow(finalInitialState)
        fatigueState = _fatigueState.asStateFlow()
        persistState(finalInitialState)
    }

    override fun onReelWatched(count: Int) {
        val now = timeProvider.currentTimeInMillis()
        val currentEpochDay = timeProvider.localDateNow().toEpochDay()
        val current = _fatigueState.value

        val stateAfterMidnight = BrainFatigueCalculator.applyMidnightRollover(current, currentEpochDay)
        val recoveredFatigue = BrainFatigueCalculator.calculateRecoveredFatigue(
            stateAfterMidnight.fatigueScore,
            stateAfterMidnight.lastActiveTimestampEpochMs,
            now,
        )
        val newFatigue = BrainFatigueCalculator.addReelFatigue(recoveredFatigue, count)

        val newState = stateAfterMidnight.copy(
            fatigueScore = newFatigue,
            todayReelCount = stateAfterMidnight.todayReelCount + count,
            currentSessionCount = stateAfterMidnight.currentSessionCount + count,
            lastActiveTimestampEpochMs = now,
            lastMidnightEpochDay = currentEpochDay,
        )
        _fatigueState.value = newState
        persistState(newState)
    }

    override fun onSessionActive(sessionDurationMs: Long) {
        val now = timeProvider.currentTimeInMillis()
        val current = _fatigueState.value
        val newState = current.copy(lastActiveTimestampEpochMs = now)
        _fatigueState.value = newState
        persistState(newState)
    }

    override fun onSessionEnded() {
        val now = timeProvider.currentTimeInMillis()
        val current = _fatigueState.value
        val newState = current.copy(
            currentSessionCount = 0,
            lastActiveTimestampEpochMs = now,
        )
        _fatigueState.value = newState
        persistState(newState)
    }

    override fun refreshRecovery() {
        val now = timeProvider.currentTimeInMillis()
        val currentEpochDay = timeProvider.localDateNow().toEpochDay()
        val current = _fatigueState.value

        val rolled = BrainFatigueCalculator.applyMidnightRollover(current, currentEpochDay)
        val recovered = BrainFatigueCalculator.calculateRecoveredFatigue(
            rolled.fatigueScore,
            rolled.lastActiveTimestampEpochMs,
            now,
        )
        if (recovered != current.fatigueScore || rolled.todayReelCount != current.todayReelCount) {
            val newState = rolled.copy(fatigueScore = recovered)
            _fatigueState.value = newState
            persistState(newState)
        }
    }

    override fun updateBreakReminderConfig(config: BreakReminderConfig) {
        _breakReminderConfig.value = config
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, config.isEnabled)
            .putInt(KEY_REMINDER_THRESHOLD, config.thresholdMinutes)
            .putInt(KEY_REMINDER_COOLDOWN, config.cooldownMinutes)
            .apply()
    }

    override fun markBreakReminderShown() {
        lastReminderShownTimestamp = timeProvider.currentTimeInMillis()
        prefs.edit().putLong(KEY_LAST_REMINDER_SHOWN, lastReminderShownTimestamp).apply()
    }

    override fun canShowBreakReminder(currentSessionDurationMs: Long): Boolean {
        val now = timeProvider.currentTimeInMillis()
        return BrainFatigueCalculator.shouldTriggerBreakReminder(
            continuousSessionDurationMs = currentSessionDurationMs,
            lastReminderShownEpochMs = lastReminderShownTimestamp,
            nowEpochMs = now,
            config = _breakReminderConfig.value,
        )
    }

    private fun persistState(state: BrainFatigueState) {
        prefs.edit()
            .putInt(KEY_FATIGUE_SCORE, state.fatigueScore)
            .putInt(KEY_TODAY_REEL_COUNT, state.todayReelCount)
            .putInt(KEY_CURRENT_SESSION_COUNT, state.currentSessionCount)
            .putLong(KEY_LAST_ACTIVE_TIMESTAMP, state.lastActiveTimestampEpochMs)
            .putLong(KEY_LAST_MIDNIGHT_EPOCH_DAY, state.lastMidnightEpochDay)
            .apply()
    }
}
