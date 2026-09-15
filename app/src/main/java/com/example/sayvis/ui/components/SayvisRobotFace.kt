package com.example.sayvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.sayvis.ui.AvatarState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The SAYVIS machine, redesigned: one calm, minimal, robotic presence.
 *
 * Composition (professional restraint — nothing more):
 *   • one orbit ring with a single satellite dot,
 *   • one squircle head with a dark visor,
 *   • two light bars for eyes (a single scanning bar while thinking),
 *   • a thin mouth line (equaliser while speaking, pulse dots thinking),
 *   • a small antenna and the gold SAYVIS chin notch.
 *
 * The palette cycles through the atomic [aiStyleColor]; THINKING accelerates
 * the hue and the scan; EMERGENCY_LOCKED pins everything to alarm red; the
 * whole face breathes via a global scale + soft glow. No allocations inside
 * the draw pass.
 */
@Composable
fun SayvisRobotFace(
    state: AvatarState,
    level: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sayvis_robot")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 5200, easing = LinearEasing)),
        label = "phase"
    )
    val breathPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3400, easing = LinearEasing)),
        label = "breath"
    )

    modifier.drawBehind {
        val locked = state == AvatarState.EMERGENCY_LOCKED
        val thinking = state == AvatarState.THINKING
        val speaking = state == AvatarState.SPEAKING
        val offline = state == AvatarState.OFFLINE

        val breath = AiStyleMath.naturalBreath(breathPhase)
        val eyePhase = if (thinking) phase * 2.2f else phase
        val core = if (locked) Color(0xFFEF4444) else aiStyleColor(eyePhase).let {
            if (offline) it.copy(alpha = 0.35f) else it
        }

        val d = density
        fun px(dp: Float): Float = dp * d

        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = 1f + 0.012f * breath * (if (thinking) 1.6f else 1f)

        withTransform({ scale(scale, scale, pivot = Offset(cx, cy)) }) {
            // --- ambient glow: one soft, quiet halo ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        core.copy(alpha = 0.10f + 0.05f * breath + level * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.52f
                ),
                radius = size.minDimension * 0.52f,
                center = Offset(cx, cy)
            )

            // --- orbit ring + single satellite ---
            val ringR = size.minDimension * 0.435f
            drawCircle(
                color = core.copy(alpha = 0.16f),
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = px(1.2f))
            )
            val satAngle = (if (thinking) 2.2f else 1f) * phase * 2f * PI.toFloat()
            val sat = Offset(cx + ringR * cos(satAngle), cy + ringR * 0.94f * sin(satAngle))
            drawCircle(color = core.copy(alpha = 0.30f), radius = px(7f), center = sat)
            drawCircle(color = core, radius = px(2.6f), center = sat)

            // --- head: squircle with a subtle vertical sheen ---
            val headW = size.width * 0.46f
            val headH = size.height * 0.36f
            val headTop = cy - headH * 0.56f
            val headTL = Offset(cx - headW / 2f, headTop)
            val headSize = Size(headW, headH)
            val headRadius = CornerRadius(px(30f))
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF121C2E), Color(0xFF0A1120)),
                    startY = headTop,
                    endY = headTop + headH
                ),
                topLeft = headTL,
                size = headSize,
                cornerRadius = headRadius
            )
            drawRoundRect(
                color = core.copy(alpha = 0.55f + 0.2f * breath),
                topLeft = headTL,
                size = headSize,
                cornerRadius = headRadius,
                style = Stroke(width = px(1.4f))
            )

            // --- antenna ---
            val stemTop = Offset(cx, headTop - px(11f))
            drawLine(
                color = core.copy(alpha = 0.6f),
                start = stemTop,
                end = Offset(cx, headTop + px(1f)),
                strokeWidth = px(1.4f),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = core.copy(alpha = if (thinking) 0.6f + 0.4f * breath else 0.75f),
                radius = px(3f),
                center = stemTop
            )

            // --- visor ---
            val visorW = headW * 0.72f
            val visorH = headH * 0.34f
            val visorTL = Offset(cx - visorW / 2f, headTop + headH * 0.24f)
            drawRoundRect(
                color = Color(0xFF05090F),
                topLeft = visorTL,
                size = Size(visorW, visorH),
                cornerRadius = CornerRadius(px(14f))
            )

            // --- eyes ---
            val eyeY = visorTL.y + visorH / 2f
            val eyeColor = core.copy(alpha = 0.72f + 0.28f * breath)
            val eyeW = visorW * 0.22f
            val gap = visorW * 0.16f
            when {
                locked -> {
                    // Flat static dashes — a halted machine.
                    val flatH = px(2.4f)
                    drawRoundRect(
                        color = eyeColor.copy(alpha = 0.6f),
                        topLeft = Offset(cx - gap / 2f - eyeW, eyeY - flatH / 2f),
                        size = Size(eyeW, flatH),
                        cornerRadius = CornerRadius(flatH / 2f)
                    )
                    drawRoundRect(
                        color = eyeColor.copy(alpha = 0.6f),
                        topLeft = Offset(cx + gap / 2f, eyeY - flatH / 2f),
                        size = Size(eyeW, flatH),
                        cornerRadius = CornerRadius(flatH / 2f)
                    )
                }
                thinking -> {
                    // One scanning bar sweeping the visor.
                    val sweep = (phase * 2f) % 1f
                    val barW = visorW * 0.30f
                    val x = visorTL.x + px(6f) + sweep * (visorW - barW - px(12f))
                    val barH = px(4.8f)
                    drawRoundRect(
                        color = eyeColor,
                        topLeft = Offset(x, eyeY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barH / 2f)
                    )
                }
                else -> {
                    // Two calm light bars; speaking adds a subtle level lift.
                    val lift = if (speaking) (level * 2.2f).coerceAtMost(1.4f) else 0f
                    val eyeH = px(4.8f) + lift * px(1.6f)
                    drawRoundRect(
                        color = eyeColor,
                        topLeft = Offset(cx - gap / 2f - eyeW, eyeY - eyeH / 2f),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(eyeH / 2f)
                    )
                    drawRoundRect(
                        color = eyeColor,
                        topLeft = Offset(cx + gap / 2f, eyeY - eyeH / 2f),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(eyeH / 2f)
                    )
                }
            }

            // --- mouth ---
            val mouthY = headTop + headH * 0.72f
            when {
                speaking -> {
                    val bars = 5
                    val barW = px(2.6f)
                    val gapM = px(3.4f)
                    val total = bars * barW + (bars - 1) * gapM
                    var x = cx - total / 2f
                    repeat(bars) { i ->
                        val wave = (sin((phase * 4f + i * 0.7f) * 2f * PI) * 0.5f + 0.5f).toFloat()
                        val h = px(3.5f) + wave * px(10f)
                        drawRoundRect(
                            color = core.copy(alpha = 0.85f),
                            topLeft = Offset(x, mouthY - h / 2f),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(barW / 2f)
                        )
                        x += barW + gapM
                    }
                }
                thinking -> {
                    val dotR = px(1.8f)
                    val gapD = px(7f)
                    repeat(3) { i ->
                        val pulse = (sin((phase * 2f - i * 0.22f) * 2f * PI) * 0.5f + 0.5f).toFloat()
                        drawCircle(
                            color = core.copy(alpha = 0.25f + 0.6f * pulse),
                            radius = dotR,
                            center = Offset(cx + (i - 1) * gapD, mouthY)
                        )
                    }
                }
                else -> {
                    val lineW = headW * 0.16f
                    drawLine(
                        color = core.copy(alpha = 0.4f),
                        start = Offset(cx - lineW / 2f, mouthY),
                        end = Offset(cx + lineW / 2f, mouthY),
                        strokeWidth = px(1.6f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // --- chin notch: the SAYVIS plate reduced to one quiet gold tick ---
            drawRoundRect(
                color = Color(0xFFC7CCD3).copy(alpha = if (offline) 0.2f else 0.5f),
                topLeft = Offset(cx - px(9f), headTop + headH + px(5f)),
                size = Size(px(18f), px(1.6f)),
                cornerRadius = CornerRadius(px(0.8f))
            )
        }
    }
}
