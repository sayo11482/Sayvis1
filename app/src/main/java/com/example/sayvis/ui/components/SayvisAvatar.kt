package com.example.sayvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.settings.AiVisualStyle
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGoldLight
import com.example.sayvis.ui.theme.SayvisRedAlert
import kotlin.math.cos
import kotlin.math.sin

/**
 * The SAYVIS atomic core (v5.1.0) — the "breathing reactor" the owner asked
 * for: a golden nucleus around three elliptical electron orbits that never
 * stop moving, exactly like breathing:
 *
 *  - IDLE             slow spin + a ~2.4s inhale/exhale glow (rest breathing);
 *  - THINKING         the reactor spins up (3x rotation) and burns brighter;
 *  - SPEAKING         nucleus pulses with every cycle (voice-breath);
 *  - OPPORTUNITY_AWARE an electron flare sweeps the orbits in gold;
 *  - EMERGENCY_LOCKED orbits freeze into alert red.
 *
 * The microphone [level] feeds the nucleus radius and the electron trails so
 * the core literally breathes with ambient activity. All legacy parameters
 * ([style], [level]) stay for compatibility — every style now renders this
 * signature golden atom.
 */
@Composable
fun SayvisAvatar(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    style: AiVisualStyle = AiVisualStyle.GEOMETRIC,
    level: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "atomic_core")

    // Orbit spin: the core "thinks faster" — 3x when THINKING, frozen when locked.
    val spinDuration = when (state) {
        AvatarState.THINKING -> 3000
        AvatarState.SPEAKING -> 5200
        AvatarState.EMERGENCY_LOCKED -> 24000
        else -> 9000
    }
    val spin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = spinDuration, easing = LinearEasing)),
        label = "orbit_spin"
    )
    val spin2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = (spinDuration * 14) / 10, easing = LinearEasing)),
        label = "orbit_spin_reverse"
    )

    // Breathing: nucleus scale + glow, faster when speaking (voice-breath).
    val breathDuration = when (state) {
        AvatarState.SPEAKING -> 700
        AvatarState.THINKING -> 1100
        else -> 2400
    }
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(breathDuration), repeatMode = RepeatMode.Reverse),
        label = "core_breath"
    )

    // Electron flare sweep (opportunity awareness = a comet laps the orbits).
    val flare by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AvatarState.OPPORTUNITY_AWARE) 1400 else 6000, easing = LinearEasing)
        ),
        label = "electron_flare"
    )

    val voiceLevel = level.coerceIn(0f, 1f)

    val nucleusColor = when (state) {
        AvatarState.EMERGENCY_LOCKED -> SayvisRedAlert
        AvatarState.OFFLINE -> Color(0xFF8A929C)
        else -> SayvisGoldLight
    }
    val orbitColor = when (state) {
        AvatarState.EMERGENCY_LOCKED -> SayvisRedAlert
        AvatarState.OFFLINE -> Color(0xFF6E757E)
        else -> SayvisGold
    }

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("sayvis_avatar")
    ) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val baseRadius = size.toPx() / 2f

        // Ambient halo — the exhale of the reactor.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    nucleusColor.copy(alpha = 0.16f + 0.10f * breath - 0.08f + 0.10f * voiceLevel),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * (0.82f + 0.10f * breath)
            ),
            radius = baseRadius * (0.82f + 0.10f * breath),
            center = center
        )

        // Three golden elliptical orbits at distinct angles — the atom.
        val orbitStroke = 1.6.dp.toPx()
        drawOrbit(center, baseRadius * 0.92f, 0.42f, spin, orbitColor, orbitStroke)
        drawOrbit(center, baseRadius * 0.84f, 0.42f, spin + 60f, orbitColor.copy(alpha = 0.85f), orbitStroke)
        drawOrbit(center, baseRadius * 0.74f, 0.40f, spin2, orbitColor.copy(alpha = 0.7f), orbitStroke * 0.9f)

        // Electrons riding the outer two orbits (three per orbit).
        drawElectrons(center, baseRadius * 0.92f, 0.42f, spin, nucleusColor, flare, 0)
        drawElectrons(center, baseRadius * 0.84f, 0.42f, spin + 60f, nucleusColor, 1f - flare, 1)

        // Opportunity flare: a comet sweeping the outermost orbit.
        if (state == AvatarState.OPPORTUNITY_AWARE) {
            val angle = flare * 360f
            val rad = Math.toRadians(angle.toDouble())
            val rx = baseRadius * 0.92f
            val ry = rx * 0.42f
            val ex = center.x + rx * cos(rad).toFloat()
            val ey = center.y + ry * sin(rad).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, nucleusColor, Color.Transparent),
                    center = Offset(ex, ey),
                    radius = 7.dp.toPx()
                ),
                radius = 7.dp.toPx(),
                center = Offset(ex, ey)
            )
        }

        // The nucleus — the breathing heart of SAYVIS.
        val nucleusRadius = baseRadius * (0.26f + 0.05f * breath + 0.06f * voiceLevel)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.98f),
                    nucleusColor,
                    orbitColor.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = center,
                radius = nucleusRadius * 1.5f
            ),
            radius = nucleusRadius * 1.5f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = nucleusRadius * 0.34f,
            center = center
        )
    }
}

/** One elliptical orbit rotated by [angle] degrees around [center]. */
private fun DrawScope.drawOrbit(
    center: Offset,
    radius: Float,
    squash: Float,
    angle: Float,
    color: Color,
    stroke: Float
) {
    rotate(degrees = angle, pivot = center) {
        drawOval(
            brush = Brush.horizontalGradient(
                listOf(color.copy(alpha = 0.4f), color, color.copy(alpha = 0.4f))
            ),
            topLeft = Offset(center.x - radius, center.y - radius * squash),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f * squash),
            style = Stroke(width = stroke)
        )
    }
}

/** Three electrons on one orbit; the leading one carries the [flare] glow. */
private fun DrawScope.drawElectrons(
    center: Offset,
    radius: Float,
    squash: Float,
    angle: Float,
    color: Color,
    flare: Float,
    seed: Int
) {
    for (i in 0 until 3) {
        val degree = angle + i * 120f + seed * 37f
        val rad = Math.toRadians(degree.toDouble())
        val ex = center.x + radius * cos(rad).toFloat()
        val ey = center.y + radius * squash * sin(rad).toFloat()
        val isLeader = (i + seed) % 3 == 0
        val glowRadius = if (isLeader) 3.2.dp.toPx() * (0.8f + 0.6f * flare) else 2.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isLeader) 0.95f else 0.75f),
                    color,
                    Color.Transparent
                ),
                center = Offset(ex, ey),
                radius = glowRadius * 1.6f
            ),
            radius = glowRadius * 1.6f,
            center = Offset(ex, ey)
        )
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = glowRadius * 0.4f, center = Offset(ex, ey))
    }
}
