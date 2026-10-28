/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.core.domain.fatigue

import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.BreakReminderConfig
import com.scrolless.app.core.model.MascotPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainFatigueCalculatorTest {

    private val oneMinuteMs = 60_000L

    @Test
    fun `reel detection increases fatigue up to maximum cap of 100`() {
        var fatigue = 0
        fatigue = BrainFatigueCalculator.addReelFatigue(fatigue, 25)
        assertEquals(25, fatigue)

        fatigue = BrainFatigueCalculator.addReelFatigue(fatigue, 80)
        assertEquals(100, fatigue) // Capped at 100
    }

    @Test
    fun `recovery grace period - first 5 minutes away produces zero recovery`() {
        val startEpoch = 1_000_000L
        val initialFatigue = 100

        // 4 minutes away: still within 5 min grace period
        val fourMinutesLater = startEpoch + 4 * oneMinuteMs
        val fatigueAt4Min = BrainFatigueCalculator.calculateRecoveredFatigue(
            initialFatigue,
            startEpoch,
            fourMinutesLater,
        )
        assertEquals(100, fatigueAt4Min)

        // Exactly 5 minutes away
        val fiveMinutesLater = startEpoch + 5 * oneMinuteMs
        val fatigueAt5Min = BrainFatigueCalculator.calculateRecoveredFatigue(
            initialFatigue,
            startEpoch,
            fiveMinutesLater,
        )
        assertEquals(100, fatigueAt5Min)
    }

    @Test
    fun `exact prompt recovery calculations match specification`() {
        val startEpoch = 1_000_000L
        val initialFatigue = 100

        // 20-minute break: (20 - 5) = 15 reduction -> 85
        val twentyMinLater = startEpoch + 20 * oneMinuteMs
        val fatigueAt20Min = BrainFatigueCalculator.calculateRecoveredFatigue(
            initialFatigue,
            startEpoch,
            twentyMinLater,
        )
        assertEquals(85, fatigueAt20Min)
        assertEquals(MascotPose.EXHAUSTED, MascotPose.fromFatigue(fatigueAt20Min))

        // 60-minute break: (60 - 5) = 55 reduction -> 45
        val sixtyMinLater = startEpoch + 60 * oneMinuteMs
        val fatigueAt60Min = BrainFatigueCalculator.calculateRecoveredFatigue(
            initialFatigue,
            startEpoch,
            sixtyMinLater,
        )
        assertEquals(45, fatigueAt60Min)
        assertEquals(MascotPose.TIRED, MascotPose.fromFatigue(fatigueAt60Min))

        // 120-minute break: (120 - 5) = 115 reduction -> capped at 0
        val twoHoursLater = startEpoch + 120 * oneMinuteMs
        val fatigueAt120Min = BrainFatigueCalculator.calculateRecoveredFatigue(
            initialFatigue,
            startEpoch,
            twoHoursLater,
        )
        assertEquals(0, fatigueAt120Min)
        assertEquals(MascotPose.ENERGETIC, MascotPose.fromFatigue(fatigueAt120Min))
    }

    @Test
    fun `separate reel counter and brain fatigue progression matches prompt example`() {
        // Step 1: User watches 100 reels
        var todayCount = 100
        var fatigue = 100
        val t0 = 10_000_000L

        // Step 2: User takes 20-minute break
        val t1 = t0 + 20 * oneMinuteMs
        fatigue = BrainFatigueCalculator.calculateRecoveredFatigue(fatigue, t0, t1)
        assertEquals(85, fatigue)
        assertEquals(100, todayCount) // Daily count does NOT decrease on breaks

        // Step 3: User watches 10 more reels
        todayCount += 10
        fatigue = BrainFatigueCalculator.addReelFatigue(fatigue, 10)
        assertEquals(110, todayCount)
        assertEquals(95, fatigue)
    }

    @Test
    fun `midnight rollover resets todayReelCount but preserves fatigue score`() {
        val stateBeforeMidnight = BrainFatigueState(
            fatigueScore = 85,
            todayReelCount = 120,
            currentSessionCount = 20,
            lastActiveTimestampEpochMs = 1_000_000L,
            lastMidnightEpochDay = 20000L,
        )

        val nextDayEpoch = 20001L
        val stateAfterMidnight = BrainFatigueCalculator.applyMidnightRollover(
            stateBeforeMidnight,
            nextDayEpoch,
        )

        assertEquals(0, stateAfterMidnight.todayReelCount) // Reset to 0
        assertEquals(85, stateAfterMidnight.fatigueScore) // Preserved
        assertEquals(20001L, stateAfterMidnight.lastMidnightEpochDay)
    }

    @Test
    fun `break reminder triggers at 20 minutes continuous watch and respects 30 minute cooldown`() {
        val config = BreakReminderConfig(
            isEnabled = true,
            thresholdMinutes = 20,
            cooldownMinutes = 30,
        )

        val tNow = 2_000_000L

        // 19 minutes continuous: below threshold
        assertFalse(
            BrainFatigueCalculator.shouldTriggerBreakReminder(
                continuousSessionDurationMs = 19 * oneMinuteMs,
                lastReminderShownEpochMs = 0L,
                nowEpochMs = tNow,
                config = config,
            ),
        )

        // 20 minutes continuous: triggers
        assertTrue(
            BrainFatigueCalculator.shouldTriggerBreakReminder(
                continuousSessionDurationMs = 20 * oneMinuteMs,
                lastReminderShownEpochMs = 0L,
                nowEpochMs = tNow,
                config = config,
            ),
        )

        // Reminder shown at tNow. 10 minutes later (still continuous): within 30 min cooldown -> does not retrigger
        val t10MinLater = tNow + 10 * oneMinuteMs
        assertFalse(
            BrainFatigueCalculator.shouldTriggerBreakReminder(
                continuousSessionDurationMs = 30 * oneMinuteMs,
                lastReminderShownEpochMs = tNow,
                nowEpochMs = t10MinLater,
                config = config,
            ),
        )

        // 31 minutes after last reminder: cooldown elapsed -> triggers again
        val t31MinLater = tNow + 31 * oneMinuteMs
        assertTrue(
            BrainFatigueCalculator.shouldTriggerBreakReminder(
                continuousSessionDurationMs = 51 * oneMinuteMs,
                lastReminderShownEpochMs = tNow,
                nowEpochMs = t31MinLater,
                config = config,
            ),
        )
    }

    @Test
    fun `disabled reminder never triggers`() {
        val config = BreakReminderConfig(isEnabled = false)
        assertFalse(
            BrainFatigueCalculator.shouldTriggerBreakReminder(
                continuousSessionDurationMs = 60 * oneMinuteMs,
                lastReminderShownEpochMs = 0L,
                nowEpochMs = 1_000_000L,
                config = config,
            ),
        )
    }

    @Test
    fun `general screen time produces gentle baseline fatigue at 1 pt per 5 minutes`() {
        var fatigue = 10

        // 4 minutes: not yet reached 5 min threshold -> 0 change
        val fourMinutesMs = 4 * oneMinuteMs
        fatigue = BrainFatigueCalculator.addGeneralScreenTimeFatigue(fatigue, fourMinutesMs)
        assertEquals(10, fatigue)

        // 15 minutes: 15 / 5 = 3 points added
        val fifteenMinutesMs = 15 * oneMinuteMs
        fatigue = BrainFatigueCalculator.addGeneralScreenTimeFatigue(fatigue, fifteenMinutesMs)
        assertEquals(13, fatigue)
    }

    @Test
    fun `mascot trembling is activated only when fatigue reaches 80 or higher`() {
        assertFalse(BrainFatigueCalculator.shouldMascotTremble(0))
        assertFalse(BrainFatigueCalculator.shouldMascotTremble(49))
        assertFalse(BrainFatigueCalculator.shouldMascotTremble(79))

        assertTrue(BrainFatigueCalculator.shouldMascotTremble(80))
        assertTrue(BrainFatigueCalculator.shouldMascotTremble(95))
        assertTrue(BrainFatigueCalculator.shouldMascotTremble(100))
    }
}

