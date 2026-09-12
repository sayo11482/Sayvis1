package com.example.sayvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SayvisAvatar(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_loop")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AvatarState.THINKING) 3000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AvatarState.SPEAKING) 600 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val coreColor: Color = when (state) {
        AvatarState.IDLE -> SayvisCyan
        AvatarState.THINKING -> SayvisGold
        AvatarState.SPEAKING -> SayvisCyan
        AvatarState.OPPORTUNITY_AWARE -> SayvisAmberWarning
        AvatarState.EMERGENCY_LOCKED -> SayvisRedAlert
        AvatarState.OFFLINE -> SayvisSilverMuted
    }

    val haloColor: Color = when (state) {
        AvatarState.IDLE -> SayvisCyan.copy(alpha = 0.35f)
        AvatarState.THINKING -> SayvisGold.copy(alpha = 0.5f)
        AvatarState.SPEAKING -> SayvisGold.copy(alpha = 0.6f)
        AvatarState.OPPORTUNITY_AWARE -> SayvisGold.copy(alpha = 0.45f)
        AvatarState.EMERGENCY_LOCKED -> SayvisRedAlert.copy(alpha = 0.5f)
        AvatarState.OFFLINE -> SayvisSilverMuted.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("sayvis_avatar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * pulse

            // 1. Ambient Glow
            drawCircle(
                color = haloColor.copy(alpha = 0.15f),
                radius = baseRadius * 1.25f,
                center = center
            )

            // 2. Rotating Outer Orbital Ring with Nodes
            rotate(rotation, pivot = center) {
                drawCircle(
                    color = haloColor,
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                val nodeCount = 6
                for (i in 0 until nodeCount) {
                    val angle = (i * (360f / nodeCount)) * (Math.PI / 180f)
                    val nodePos = Offset(
                        x = (center.x + baseRadius * cos(angle)).toFloat(),
                        y = (center.y + baseRadius * sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = if (i % 2 == 0) SayvisGold else coreColor,
                        radius = 3.dp.toPx(),
                        center = nodePos
                    )
                }
            }

            // 3. Inner Hexagonal Cognitive Core
            val hexPath = Path()
            val hexRadius = baseRadius * 0.60f
            for (i in 0 until 6) {
                val angle = (i * 60f - 30f) * (Math.PI / 180f)
                val x = (center.x + hexRadius * cos(angle)).toFloat()
                val y = (center.y + hexRadius * sin(angle)).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            drawPath(
                path = hexPath,
                color = coreColor.copy(alpha = 0.25f)
            )
            drawPath(
                path = hexPath,
                color = coreColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Central Sovereign Core Nucleus
            drawCircle(
                color = if (state == AvatarState.EMERGENCY_LOCKED) SayvisRedAlert else SayvisGold,
                radius = 7.dp.toPx() * pulse,
                center = center
            )
        }
    }
}
