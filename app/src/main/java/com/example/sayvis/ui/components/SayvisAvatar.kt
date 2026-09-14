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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.example.sayvis.ui.theme.SayvisRedAlert
import kotlin.math.cos
import kotlin.math.sin

/**
 * The SAYVIS avatar rendering the four atomic-breathing AI views
 * ([AiVisualStyle]): geometric wireframe with electron orbits, stereologic
 * sections, a binary 0/1 glyph ring and a hologram scan — all cycling through
 * the five-colour neon palette and reacting to the microphone [level].
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
        AvatarState.IDLE -> aiStyleColor(rotation / 360f)
        AvatarState.THINKING -> aiStyleColor(rotation / 360f + 0.1f)
        AvatarState.SPEAKING -> aiStyleColor(rotation / 360f + 0.25f)
        AvatarState.OPPORTUNITY_AWARE -> aiStyleColor(rotation / 360f + 0.55f)
        AvatarState.EMERGENCY_LOCKED -> SayvisRedAlert
        AvatarState.OFFLINE -> Color(0xFF94A3B8)
    }
    val accentColor: Color = aiStyleColor(rotation / 360f + 0.4f)

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
            val phase = rotation / 360f
            val angle = Math.toRadians(rotation.toDouble()).toFloat()

            // 1. Ambient breathing glow (palette + voice reactive).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor.copy(alpha = 0.30f + 0.15f * voiceLevel), Color.Transparent),
                    center = center,
                    radius = baseRadius * (1.35f + 0.12f * voiceLevel)
                ),
                radius = baseRadius * (1.35f + 0.12f * voiceLevel),
                center = center
            )

            // 2. Style-specific atomic view.
            when (style) {
                AiVisualStyle.GEOMETRIC -> {
                    val projected = AiStyleMath.rotateAndProject(angle, angle * 0.6f + 0.7f)
                    val wireRadius = baseRadius * 0.95f
                    val verts = ArrayList<Offset>(projected.size / 2)
                    for (i in projected.indices step 2) {
                        verts.add(Offset(center.x + projected[i] * wireRadius, center.y + projected[i + 1] * wireRadius))
                    }
                    for ((a, b) in AiStyleMath.ICOSAHEDRON_EDGES) {
                        val depth = ((projected[a * 2 + 1] + 1.5f) / 3f).coerceIn(0f, 1f)
                        drawLine(
                            color = aiStyleColor(phase + depth * 0.35f, 0.35f + 0.55f * depth),
                            start = verts[a],
                            end = verts[b],
                            strokeWidth = (0.8f + 1.0f * depth).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    // One electron orbit with a glowing electron.
                    drawOval(
                        color = accentColor.copy(alpha = 0.5f),
                        topLeft = Offset(center.x - wireRadius, center.y - wireRadius * 0.35f),
                        size = Size(wireRadius * 2f, wireRadius * 0.7f),
                        style = Stroke(width = 0.9f.dp.toPx())
                    )
                    val ea = angle * 1.5f
                    val (ex, ey) = AiStyleMath.orbitPosition(ea, 0.35f)
                    drawCircle(
                        color = Color.White,
                        radius = 2.0f.dp.toPx(),
                        center = Offset(center.x + ex * wireRadius, center.y + ey * wireRadius)
                    )
                }

                AiVisualStyle.STEREOLOGY -> {
                    val fractions = listOf(1.0f, 0.76f, 0.52f)
                    fractions.forEachIndexed { index, fraction ->
                        val direction = if (index % 2 == 0) rotation else -rotation * 1.3f
                        rotate(direction, pivot = center) {
                            val squash = 0.28f + 0.17f * index
                            drawOval(
                                color = aiStyleColor(phase + index * 0.25f, 0.85f),
                                topLeft = Offset(center.x - baseRadius * fraction, center.y - baseRadius * fraction * squash),
                                size = Size(baseRadius * 2f * fraction, baseRadius * 2f * fraction * squash),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                AiVisualStyle.BINARY -> {
                    val glyphCount = 10
                    val native = drawContext.canvas.nativeCanvas
                    glyphPaint.textSize = baseRadius * 0.30f
                    for (i in 0 until glyphCount) {
                        val glyphAngle = Math.toRadians((i * (360f / glyphCount) + rotation).toDouble())
                        val x = center.x + baseRadius * cos(glyphAngle).toFloat()
                        val y = center.y + baseRadius * sin(glyphAngle).toFloat() + glyphPaint.textSize * 0.36f
                        val bit = ((i * 31 + (rotation / 30f).toInt()) % 3) != 0
                        val glyphColor = aiStyleColor(phase + i / glyphCount.toFloat())
                        val alpha = if (i % 2 == 0) 235 else 140
                        glyphPaint.color = android.graphics.Color.argb(
                            alpha,
                            (glyphColor.red * 255).toInt(),
                            (glyphColor.green * 255).toInt(),
                            (glyphColor.blue * 255).toInt()
                        )
                        native.drawText(if (bit) "1" else "0", x, y, glyphPaint)
                    }
                }

                AiVisualStyle.HOLOGRAM -> {
                    drawCircle(
                        color = coreColor.copy(alpha = 0.8f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                    val beams = 3
                    for (b in 0 until beams) {
                        val beamPhase = ((phase) + b / beams.toFloat()) % 1f
                        val y = center.y - baseRadius + 2f * baseRadius * beamPhase
                        drawLine(
                            color = aiStyleColor(phase + b * 0.3f, 0.7f - 0.18f * b),
                            start = Offset(center.x - baseRadius * 0.96f, y),
                            end = Offset(center.x + baseRadius * 0.96f, y),
                            strokeWidth = (1.3f - b * 0.3f).dp.toPx()
                        )
                    }
                }
            }

            // 3. Inner hexagonal cognitive core.
            val hexPath = Path()
            val hexRadius = baseRadius * 0.60f
            for (i in 0 until 6) {
                val hexAngle = (i * 60f - 30f) * (Math.PI / 180f)
                val x = (center.x + hexRadius * cos(hexAngle)).toFloat()
                val y = (center.y + hexRadius * sin(hexAngle)).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            drawPath(path = hexPath, color = coreColor.copy(alpha = 0.22f))
            drawPath(path = hexPath, color = coreColor, style = Stroke(width = 2.dp.toPx()))

            // 4. Central sovereign nucleus (breathes with the voice).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accentColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = 9.dp.toPx() * (1f + 0.25f * voiceLevel)
                ),
                radius = 9.dp.toPx() * (1f + 0.25f * voiceLevel),
                center = center
            )
        }
    }
}
