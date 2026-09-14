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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.settings.AiVisualStyle
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import kotlin.math.cos
import kotlin.math.sin

/**
 * The SAYVIS avatar with the four selectable multidimensional AI views
 * ([AiVisualStyle]): geometric wireframe, stereologic sections, binary 0/1 ring
 * and hologram scan — all voice-reactive through [level] (0..1 mic amplitude).
 */
@Composable
fun SayvisAvatar(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    style: AiVisualStyle = AiVisualStyle.GEOMETRIC,
    level: Float = 0f
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

    val glyphPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
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
            val voiceLevel = level.coerceIn(0f, 1f)

            // 1. Ambient glow (voice-reactive).
            drawCircle(
                color = haloColor.copy(alpha = 0.15f),
                radius = baseRadius * (1.25f + 0.10f * voiceLevel),
                center = center
            )

            // 2. Style-specific outer view.
            when (style) {
                AiVisualStyle.GEOMETRIC -> {
                    // Projected icosahedron wireframe rotating around the core.
                    val projected = AiStyleMath.rotateAndProject(
                        Math.toRadians(rotation.toDouble()).toFloat(),
                        Math.toRadians((rotation * 0.6 + 40.0)).toFloat()
                    )
                    val wireRadius = baseRadius * 0.95f
                    val verts = ArrayList<Offset>(projected.size / 2)
                    for (i in projected.indices step 2) {
                        verts.add(Offset(center.x + projected[i] * wireRadius, center.y + projected[i + 1] * wireRadius))
                    }
                    for ((a, b) in AiStyleMath.ICOSAHEDRON_EDGES) {
                        drawLine(
                            color = haloColor,
                            start = verts[a],
                            end = verts[b],
                            strokeWidth = 1.1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                AiVisualStyle.STEREOLOGY -> {
                    // Nested counter-rotating section planes.
                    val fractions = listOf(1.0f, 0.74f, 0.5f)
                    fractions.forEachIndexed { index, fraction ->
                        val direction = if (index % 2 == 0) rotation else -rotation * 1.3f
                        rotate(direction, pivot = center) {
                            val squash = 0.30f + 0.18f * index
                            drawOval(
                                color = if (index == 1) SayvisGold.copy(alpha = 0.55f) else haloColor,
                                topLeft = Offset(center.x - baseRadius * fraction, center.y - baseRadius * fraction * squash),
                                size = androidx.compose.ui.geometry.Size(
                                    baseRadius * 2f * fraction,
                                    baseRadius * 2f * fraction * squash
                                ),
                                style = Stroke(width = 1.3.dp.toPx())
                            )
                        }
                    }
                }

                AiVisualStyle.BINARY -> {
                    // Ring of 0/1 glyphs rotating around the core.
                    val glyphCount = 10
                    val native = drawContext.canvas.nativeCanvas
                    glyphPaint.textSize = baseRadius * 0.30f
                    for (i in 0 until glyphCount) {
                        val angle = Math.toRadians((i * (360f / glyphCount) + rotation).toDouble())
                        val x = center.x + baseRadius * cos(angle).toFloat()
                        val y = center.y + baseRadius * sin(angle).toFloat() + glyphPaint.textSize * 0.36f
                        val bit = ((i * 31 + (rotation / 30f).toInt()) % 3) != 0
                        val alpha = if (i % 2 == 0) 230 else 130
                        glyphPaint.color = android.graphics.Color.argb(
                            alpha,
                            (coreColor.red * 255).toInt(),
                            (coreColor.green * 255).toInt(),
                            (coreColor.blue * 255).toInt()
                        )
                        native.drawText(if (bit) "1" else "0", x, y, glyphPaint)
                    }
                }

                AiVisualStyle.HOLOGRAM -> {
                    // Dashed-feel outer circle + moving horizontal scan beams.
                    drawCircle(
                        color = haloColor,
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                    val beams = 3
                    for (b in 0 until beams) {
                        val phase = ((rotation / 360f) + b / beams.toFloat()) % 1f
                        val y = center.y - baseRadius + 2f * baseRadius * phase
                        drawLine(
                            color = SayvisGold.copy(alpha = 0.65f - 0.18f * b),
                            start = Offset(center.x - baseRadius * 0.96f, y),
                            end = Offset(center.x + baseRadius * 0.96f, y),
                            strokeWidth = (1.4f - b * 0.3f).dp.toPx()
                        )
                    }
                }
            }

            // 3. Inner hexagonal cognitive core (shared by every style).
            val hexPath = Path()
            val hexRadius = baseRadius * 0.60f
            for (i in 0 until 6) {
                val angle = (i * 60f - 30f) * (Math.PI / 180f)
                val x = (center.x + hexRadius * cos(angle)).toFloat()
                val y = (center.y + hexRadius * sin(angle)).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            drawPath(path = hexPath, color = coreColor.copy(alpha = 0.25f))
            drawPath(path = hexPath, color = coreColor, style = Stroke(width = 2.dp.toPx()))

            // 4. Central sovereign nucleus (grows slightly with the voice level).
            drawCircle(
                color = if (state == AvatarState.EMERGENCY_LOCKED) SayvisRedAlert else SayvisGold,
                radius = 7.dp.toPx() * pulse * (1f + 0.25f * voiceLevel),
                center = center
            )
        }
    }
}
