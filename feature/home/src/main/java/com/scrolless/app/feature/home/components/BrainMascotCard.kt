/*
 * Copyright (C) 2026 BrainBuddy
 *
 * Licensed under the GNU General Public License v3.
 */
package com.scrolless.app.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.designsystem.R as DesignR
import com.scrolless.app.core.model.BrainFatigueState
import com.scrolless.app.core.model.MascotPose
import com.scrolless.app.designsystem.theme.spacing

@Composable
fun BrainMascotCard(
    fatigueState: BrainFatigueState,
    modifier: Modifier = Modifier,
) {
    val pose = fatigueState.pose
    val fatigue = fatigueState.fatigueScore

    val (statusTitle, statusSubtitle, statusColor) = when (pose) {
        MascotPose.ENERGETIC -> Triple(
            "Brain is Fresh & Energetic! 🌟",
            "Keep up the great mindful habits.",
            Color(0xFF4CAF50),
        )
        MascotPose.TIRED -> Triple(
            "Starting to Feel Tired... 🥱",
            "Your brain is getting slightly exhausted.",
            Color(0xFFFFB300),
        )
        MascotPose.EXHAUSTED -> Triple(
            "Very Exhausted! 😴",
            "Too much scrolling. Consider taking a walk or break.",
            Color(0xFFFF7043),
        )
        MascotPose.DEPLETED -> Triple(
            "Critically Drained! 🛑",
            "Your little brain desperately needs rest.",
            Color(0xFFE53935),
        )
    }

    val mascotDrawableRes = when (pose) {
        MascotPose.ENERGETIC -> DesignR.drawable.brain_mascot_energetic
        MascotPose.TIRED -> DesignR.drawable.brain_mascot_tired
        MascotPose.EXHAUSTED -> DesignR.drawable.brain_mascot_exhausted
        MascotPose.DEPLETED -> DesignR.drawable.brain_mascot_depleted
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mascot Animated Character Presentation
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = mascotDrawableRes,
                    animationSpec = tween(durationMillis = 400),
                    label = "MascotPoseCrossfade",
                ) { drawableRes ->
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = "BrainBuddy Mascot",
                        modifier = Modifier.size(120.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = statusColor,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Fatigue Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Brain Fatigue",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "$fatigue / 100",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { fatigue / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricChip(
                    label = "Today's Reels",
                    value = "${fatigueState.todayReelCount}",
                )
                MetricChip(
                    label = "Health",
                    value = "${fatigueState.healthPercentage}%",
                )
                MetricChip(
                    label = "Recovery Rate",
                    value = "1 pt / min",
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Non-intrusive transparency disclaimer
            Text(
                text = "Motivational wellbeing companion. Values reflect break habits and mindful usage.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
