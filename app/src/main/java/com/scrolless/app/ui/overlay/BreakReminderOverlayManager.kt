/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.ui.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.scrolless.app.R
import com.scrolless.app.core.model.BreakReminderConfig
import com.scrolless.app.core.repository.BrainFatigueRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Non-intrusive in-app / window overlay reminder displayed when continuous scrolling
 * reaches the configured threshold (default: 20 minutes).
 *
 * Characteristics:
 * - Does not block video playback or user interactions (FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE)
 * - Auto-dismisses after 6 seconds with smooth animation
 * - Enforces minimum 30-minute cooldown
 * - Requires no notification permissions
 */
@Singleton
class BreakReminderOverlayManager @Inject constructor(
    private val brainFatigueRepository: BrainFatigueRepository,
) {

    private var windowManager: WindowManager? = null
    private var serviceContext: Context? = null
    private var reminderView: View? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var dismissJob: Job? = null

    fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    @SuppressLint("RtlHardcoded")
    fun showIfEligible(continuousSessionDurationMs: Long) {
        val context = serviceContext ?: return
        val wm = windowManager ?: return

        if (!brainFatigueRepository.canShowBreakReminder(continuousSessionDurationMs)) {
            return
        }

        // If already visible, do not re-show
        if (reminderView != null) return

        try {
            val view = buildReminderView(context)
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(context, 48)
            }

            wm.addView(view, layoutParams)
            reminderView = view
            brainFatigueRepository.markBreakReminderShown()

            // Smooth entrance animation
            view.alpha = 0f
            view.translationY = -dpToPx(context, 20).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Auto-dismiss after 6 seconds
            dismissJob?.cancel()
            dismissJob = coroutineScope.launch {
                delay(6000L)
                dismiss()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to display break reminder overlay")
        }
    }

    fun dismiss() {
        val view = reminderView ?: return
        val wm = windowManager ?: return

        view.animate()
            .alpha(0f)
            .translationY(-dpToPx(view.context, 20).toFloat())
            .setDuration(350)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        wm.removeView(view)
                    } catch (e: Exception) {
                        Timber.w(e, "Error removing reminder view")
                    } finally {
                        if (reminderView == view) {
                            reminderView = null
                        }
                    }
                }
            })
            .start()
    }

    private fun buildReminderView(context: Context): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dpToPx(context, 14),
                dpToPx(context, 10),
                dpToPx(context, 16),
                dpToPx(context, 10),
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(context, 24).toFloat()
                setColor(Color.parseColor("#EE1F1F24")) // Dark translucent pill
                setStroke(dpToPx(context, 1), Color.parseColor("#44FFFFFF"))
            }
            elevation = dpToPx(context, 8).toFloat()
            setOnClickListener { dismiss() } // Optional tap to dismiss
        }

        // Mascot Icon
        val mascotIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 32), dpToPx(context, 32)).apply {
                rightMargin = dpToPx(context, 10)
            }
            setImageResource(com.scrolless.app.core.designsystem.R.drawable.brain_mascot_tired)
        }
        root.addView(mascotIcon)

        // Text
        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            text = BreakReminderConfig.REMINDER_TEXT
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            maxWidth = dpToPx(context, 260)
        }
        root.addView(textView)

        return root
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
    }
}

