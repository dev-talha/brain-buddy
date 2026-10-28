/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.ui.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import com.scrolless.app.R
import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.IntervalUsage
import com.scrolless.app.core.model.MascotPose
import com.scrolless.app.core.repository.BrainFatigueRepository
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.repository.setTimerOverlayPosition
import com.scrolless.app.designsystem.util.formatAsTime
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Dynamic Top Notch Brain Mascot Pill overlay that shows real-time brain fatigue,
 * emotional mascot poses, reel count, and viewing duration.
 *
 * Positioned under the punch-hole camera / status bar. Tap to open BrainBuddy.
 */
class TimerOverlayManager @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val brainFatigueRepository: BrainFatigueRepository,
) {

    private var rootView: DragInterceptFrameLayout? = null
    private var notchPillContainer: LinearLayout? = null
    private var mascotImageView: ImageView? = null
    private var timerTextView: TextView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var snapAnimator: ValueAnimator? = null
    private var tremblingAnimator: ValueAnimator? = null
    private var velocityTracker: android.view.VelocityTracker? = null

    private lateinit var serviceContext: Context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sessionStartTime = 0L
    private var usage = IntervalUsage.NOT_STARTED
    private var windowLengthMillis = 0L
    private var timerJob: Job? = null
    private var fatigueJob: Job? = null
    private var exitAnimationJob: Job? = null
    private var screenBounds: ScreenBounds? = null

    private var currentFatigueState: BrainFatigueState = BrainFatigueState()

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showDaily(sessionStartAt: Long, dailyUsageMillis: Long) {
        val zoneId = ZoneId.systemDefault()
        val currentDate = Instant.ofEpochMilli(sessionStartAt).atZone(zoneId).toLocalDate()
        val dayStartMillis = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDayStartMillis = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        showOverlay(
            sessionStartAt = sessionStartAt,
            usage = IntervalUsage(dayStartMillis, dailyUsageMillis),
            windowLengthMillis = nextDayStartMillis - dayStartMillis,
        )
    }

    fun showInterval(sessionStartAt: Long, intervalUsage: IntervalUsage, intervalLengthMillis: Long) {
        showOverlay(sessionStartAt, intervalUsage, intervalLengthMillis)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay(sessionStartAt: Long, usage: IntervalUsage, windowLengthMillis: Long) {
        if (rootView != null) {
            cleanupView()
        }
        if (!::serviceContext.isInitialized) {
            Timber.w("Timer overlay requested before service context was attached")
            return
        }
        val wm = windowManager ?: return

        sessionStartTime = sessionStartAt
        this.usage = usage
        this.windowLengthMillis = windowLengthMillis
        currentFatigueState = brainFatigueRepository.fatigueState.value

        val paddingH = dpToPx(12f)
        val paddingV = dpToPx(6f)
        val mascotSize = dpToPx(28f)

        mascotImageView = ImageView(serviceContext).apply {
            layoutParams = LinearLayout.LayoutParams(mascotSize, mascotSize).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dpToPx(8f)
            }
            setImageResource(getMascotDrawableRes(currentFatigueState.pose))
            contentDescription = "Brain Mascot"
        }

        timerTextView = TextView(serviceContext).apply {
            text = formatNotchText(currentFatigueState, displayedDurationAt(System.currentTimeMillis()))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }

        notchPillContainer = LinearLayout(serviceContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(paddingH, paddingV, paddingH + dpToPx(2f), paddingV)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6121418")) // Translucent dark obsidian
                cornerRadius = dpToPx(24f).toFloat()
                setStroke(dpToPx(1f), Color.parseColor("#33FFFFFF"))
            }
            elevation = dpToPx(10f).toFloat()

            addView(mascotImageView)
            addView(timerTextView)
        }

        rootView = DragInterceptFrameLayout(serviceContext).apply {
            addView(
                notchPillContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            setOnTouchListener { _, event ->
                handleTouch(event)
            }
        }

        // Get saved position or default to top-center under camera
        val defaultY = dpToPx(12f) // Under camera punch-hole

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0 // Centered directly under the front punch-hole camera
            y = defaultY
        }

        screenBounds = calculateScreenBounds()

        try {
            rootView?.alpha = 1f
            wm.addView(rootView, layoutParams)

            startTimer()
            startFatigueObserver()
            rootView?.post { startEnterAnimation() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show Dynamic Notch overlay")
            cleanupView()
        }
    }

    /**
     * Triggers a brief energetic wobble/shake animation when a reel is scrolled.
     */
    fun notifyReelScrolled() {
        coroutineScope.launch(Dispatchers.Main) {
            val iv = mascotImageView ?: return@launch
            iv.animate()
                .rotation(16f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(80)
                .withEndAction {
                    iv.animate()
                        .rotation(-16f)
                        .setDuration(80)
                        .withEndAction {
                            iv.animate()
                                .rotation(0f)
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(80)
                                .start()
                        }.start()
                }.start()
        }
    }

    fun hide(sessionStartAt: Long, sessionEndAt: Long) {
        Timber.d("Hiding overlay view")
        if (sessionStartTime != sessionStartAt) {
            Timber.v("Ignoring stale overlay hide for session=%d, current=%d", sessionStartAt, sessionStartTime)
            return
        }

        timerJob?.cancel()
        timerJob = null
        fatigueJob?.cancel()
        fatigueJob = null
        stopTrembling()

        timerTextView?.text = formatNotchText(currentFatigueState, displayedDurationAt(sessionEndAt))
        startWiggleAnimation()

        exitAnimationJob?.cancel()
        exitAnimationJob = coroutineScope.launch {
            delay(SUMMARY_DISPLAY_DURATION_MS.milliseconds)
            startExitAnimation()
        }
    }

    fun dismissImmediately() {
        cleanupView()
    }

    fun cleanup() {
        cleanupView()
        coroutineScope.cancel()
    }

    private fun cleanupView() {
        val view = rootView
        rootView = null
        notchPillContainer = null
        mascotImageView = null
        timerTextView = null

        stopTrembling()

        if (view != null) {
            try {
                view.animate().cancel()
                view.visibility = View.GONE
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove overlay")
            }
        }

        timerJob?.cancel()
        timerJob = null
        fatigueJob?.cancel()
        fatigueJob = null
        exitAnimationJob?.cancel()
        exitAnimationJob = null
        snapAnimator?.cancel()
        snapAnimator = null
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (true) {
                val duration = displayedDurationAt(System.currentTimeMillis())
                timerTextView?.text = formatNotchText(currentFatigueState, duration)
                delay(1000.milliseconds)
            }
        }
    }

    private fun startFatigueObserver() {
        fatigueJob?.cancel()
        fatigueJob = coroutineScope.launch {
            brainFatigueRepository.fatigueState.collect { state ->
                currentFatigueState = state
                mascotImageView?.setImageResource(getMascotDrawableRes(state.pose))
                val duration = displayedDurationAt(System.currentTimeMillis())
                timerTextView?.text = formatNotchText(state, duration)

                if (state.fatigueScore >= 80) {
                    startTrembling()
                } else {
                    stopTrembling()
                }
            }
        }
    }

    private fun formatNotchText(state: BrainFatigueState, durationMillis: Long): String {
        val time = durationMillis.formatAsTime()
        return if (state.fatigueScore >= 80) {
            "${state.todayReelCount} Reels • $time  ⚡${state.fatigueScore}%"
        } else {
            "${state.todayReelCount} Reels • $time"
        }
    }

    private fun getMascotDrawableRes(pose: MascotPose): Int = when (pose) {
        MascotPose.ENERGETIC -> R.drawable.brain_mascot_energetic
        MascotPose.TIRED -> R.drawable.brain_mascot_tired
        MascotPose.EXHAUSTED -> R.drawable.brain_mascot_exhausted
        MascotPose.DEPLETED -> R.drawable.brain_mascot_depleted
    }

    private fun startTrembling() {
        if (tremblingAnimator != null) return
        val iv = mascotImageView ?: return
        tremblingAnimator = ValueAnimator.ofFloat(-3f, 3f).apply {
            duration = 90
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                iv.translationX = v
                iv.rotation = v * 1.5f
            }
            start()
        }
    }

    private fun stopTrembling() {
        tremblingAnimator?.cancel()
        tremblingAnimator = null
        mascotImageView?.translationX = 0f
        mascotImageView?.rotation = 0f
    }

    private fun displayedDurationAt(nowMillis: Long): Long = usage.plusSession(
        sessionStartMillis = sessionStartTime,
        sessionEndMillis = nowMillis,
        lengthMillis = windowLengthMillis,
    ).usageMillis

    private fun startEnterAnimation() {
        val view = rootView ?: return
        view.translationY = -dpToPx(50f).toFloat()
        view.alpha = 0f

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startExitAnimation() {
        val view = rootView ?: return
        view.animate()
            .translationY(-dpToPx(50f).toFloat())
            .alpha(0f)
            .setDuration(EXIT_ANIMATION_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cleanupView()
                    }
                },
            )
            .start()
    }

    private fun startWiggleAnimation() {
        val view = rootView ?: return
        ObjectAnimator.ofFloat(view, "translationY", 0f, -dpToPx(6f).toFloat(), 0f).apply {
            duration = 350
            start()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        val wm = windowManager ?: return false
        val bounds = screenBounds ?: calculateScreenBounds().also { screenBounds = it } ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                snapAnimator?.cancel()
                velocityTracker?.recycle()
                velocityTracker = android.view.VelocityTracker.obtain()
                velocityTracker?.addMovement(event)

                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                if (abs(deltaX) > 15 || abs(deltaY) > 15) {
                    isDragging = true
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY

                    try {
                        wm.updateViewLayout(rootView, params)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)

                if (!isDragging) {
                    // Tap on the notch pill -> Open BrainBuddy!
                    Timber.i("BrainBuddy Notch Pill tapped! Launching MainActivity.")
                    rootView?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    val intent = serviceContext.packageManager.getLaunchIntentForPackage(serviceContext.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    if (intent != null) {
                        serviceContext.startActivity(intent)
                    }
                } else {
                    persistOverlayPosition(params.x, params.y)
                }

                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return false
    }

    private fun persistOverlayPosition(x: Int, y: Int) {
        coroutineScope.launch {
            userSettingsStore.setTimerOverlayPosition(x, y)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            serviceContext.resources.displayMetrics,
        ).toInt()
    }

    @Suppress("DEPRECATION")
    private fun calculateScreenBounds(): ScreenBounds? {
        val wm = windowManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(metrics.windowInsets, null)
            val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
            val bounds = metrics.bounds
            ScreenBounds(
                width = bounds.width() - insets.left - insets.right,
                height = bounds.height() - insets.top - insets.bottom,
            )
        } else {
            val display = wm.defaultDisplay ?: return null
            val point = android.graphics.Point()
            display.getRealSize(point)
            ScreenBounds(width = point.x, height = point.y)
        }
    }

    companion object {
        private const val EXIT_ANIMATION_DURATION_MS = 250L
        private const val SUMMARY_DISPLAY_DURATION_MS = 1200L
    }

    private class DragInterceptFrameLayout(context: Context) : FrameLayout(context) {
        override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
            return true
        }
    }

    private data class ScreenBounds(val width: Int, val height: Int)
}
