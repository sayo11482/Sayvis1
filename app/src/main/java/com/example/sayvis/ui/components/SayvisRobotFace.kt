package com.example.sayvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.sayvis.ui.AvatarState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The live SAYVIS robot — the launcher-art robot head recreated in vector:
 * a chrome helmet with the SAYVIS plate, glowing colour-shifting eyes, the
 * purple mouth vent that changes colour while thinking, orbiting neck cables,
 * and the hexagonal lattice chest core with a pulsing nucleus.
 *
 * Everything breathes ([AiStyleMath.breath]) and cycles the neon palette; the
 * eyes/intensity react to the avatar state and the microphone [level].
 */
@Composable
fun SayvisRobotFace(
    state: AvatarState,
    modifier: Modifier = Modifier,
    level: Float = 0f,
    animate: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "robot_live")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "robot_phase"
    )
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "robot_flicker"
    )
    val paint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val phaseValue = if (animate) phase else 0.35f
    val flickerValue = if (animate) flicker else 0.5f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val u = min(w, h) / 3.3f
        val cx = w / 2f
        val cy = h * 0.40f
        val angle = phaseValue * 2f * Math.PI.toFloat()
        val voice = level.coerceIn(0f, 1f)
        val thinking = state == AvatarState.THINKING
        val locked = state == AvatarState.EMERGENCY_LOCKED
        val speaking = state == AvatarState.SPEAKING

        // Eye/mouth colour: palette cycle, fast-forwarded while thinking.
        val eyePhase = phaseValue * (if (thinking) 2.2f else 1f) + (if (speaking) 0.15f else 0f)
        val eyeColor = if (locked) Color(0xFFEF4444) else aiStyleColor(eyePhase)
        val mouthColor = if (locked) Color(0xFFEF4444) else aiStyleColor(eyePhase + 0.18f)
        val breath = AiStyleMath.breath(phaseValue)

        // ------------------------------------------------------ background
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF04060B), Color(0xFF0A0D16), Color(0xFF05070D))),
            size = size
        )
        // Dim server-rack dot walls on both edges.
        val racks = AiStyleMath.binaryColumns(5, 16, phaseValue * 0.35f)
        val dot = 1.6f.dp.toPx()
        for (c in 0 until 5) {
            for (r in 0 until 16) {
                if (!racks[c][r]) continue
                val rackColor = if (c % 2 == 0) Color(0xFFD4AF37) else Color(0xFF38BDF8)
                drawCircle(
                    color = rackColor.copy(alpha = 0.10f + 0.10f * breath),
                    radius = dot,
                    center = Offset(4.dp.toPx() + c * 4.4f.dp.toPx(), 8.dp.toPx() + r * 4.4f.dp.toPx())
                )
                drawCircle(
                    color = rackColor.copy(alpha = 0.10f + 0.10f * breath),
                    radius = dot,
                    center = Offset(w - 4.dp.toPx() - c * 4.4f.dp.toPx(), 8.dp.toPx() + r * 4.4f.dp.toPx())
                )
            }
        }

        // --------------------------------------------------- neck cables (behind)
        val cableAlphaBase = 0.45f + 0.30f * breath + 0.25f * voice
        for (side in listOf(-1, 1)) {
            for (i in 0 until 5) {
                val radius = u * (1.10f + 0.09f * i)
                val start = -150f + i * 14f
                val sweep = 115f + 8f * i
                val glow = cableAlphaBase * (1f - i * 0.13f) * (0.75f + 0.25f * flickerValue)
                rotate(degrees = side * (6f + 2f * sin(angle + i).toFloat()), pivot = Offset(cx, cy)) {
                    drawArc(
                        color = Color(0xFF8B5CF6).copy(alpha = glow * 0.55f),
                        startAngle = if (side < 0) 180f + (150f - start - sweep) else start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 1.7f),
                        style = Stroke(width = 3.4f.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFFD8B4FE).copy(alpha = glow),
                        startAngle = if (side < 0) 180f + (150f - start - sweep) else start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 1.7f),
                        style = Stroke(width = 1.2f.dp.toPx())
                    )
                }
            }
        }

        // ------------------------------------------------------- shoulders + neck
        val shoulderTop = cy + u * 1.30f
        drawPath(
            path = Path().apply {
                moveTo(cx - u * 1.15f, h.toFloat())
                lineTo(cx - u * 0.95f, shoulderTop + u * 0.16f)
                quadraticBezierTo(cx - u * 0.55f, shoulderTop - u * 0.12f, cx, shoulderTop - u * 0.16f)
                quadraticBezierTo(cx + u * 0.55f, shoulderTop - u * 0.12f, cx + u * 0.95f, shoulderTop + u * 0.16f)
                lineTo(cx + u * 1.15f, h.toFloat())
                close()
            },
            brush = Brush.verticalGradient(listOf(Color(0xFF232837), Color(0xFF0D1018)))
        )
        drawLine(
            color = Color(0xFFA78BFA).copy(alpha = 0.5f + 0.3f * breath),
            start = Offset(cx - u * 0.95f, shoulderTop + u * 0.16f),
            end = Offset(cx + u * 0.95f, shoulderTop + u * 0.16f),
            strokeWidth = 1.1f.dp.toPx()
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF1A1F2C), Color(0xFF0B0E15))),
            topLeft = Offset(cx - u * 0.26f, cy + u * 0.92f),
            size = Size(u * 0.52f, u * 0.5f)
        )

        // --------------------------------------------------------- helmet (chrome)
        drawOval(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF11141C),
                    0.22f to Color(0xFF39404F),
                    0.40f to Color(0xFFB9C1D0),
                    0.52f to Color(0xFF7C8598),
                    0.78f to Color(0xFF2A303D),
                    1.00f to Color(0xFF0D1017)
                )
            ),
            topLeft = Offset(cx - u * 0.95f, cy - u * 1.02f),
            size = Size(u * 1.9f, u * 2.04f)
        )
        // Faceplate shadow.
        drawOval(
            color = Color(0xFF0A0D13).copy(alpha = 0.82f),
            topLeft = Offset(cx - u * 0.64f, cy - u * 0.62f),
            size = Size(u * 1.28f, u * 1.44f)
        )
        // Ear pods.
        for (side in listOf(-1, 1)) {
            val podX = cx + side * u * 0.92f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF39404F), Color(0xFF12151D))),
                topLeft = Offset(podX - u * 0.09f, cy - u * 0.22f),
                size = Size(u * 0.18f, u * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, eyeColor, Color.Transparent),
                    center = Offset(podX, cy + 0.02f * u),
                    radius = u * 0.07f
                ),
                radius = u * 0.07f * (1f + 0.2f * breath),
                center = Offset(podX, cy + 0.02f * u)
            )
        }

        // ------------------------------------------------------- forehead label
        val native = drawContext.canvas.nativeCanvas
        paint.textSize = u * 0.155f
        paint.color = android.graphics.Color.argb(235, 213, 220, 232)
        paint.setShadowLayer(u * 0.03f, 0f, 0f, android.graphics.Color.argb(120, 139, 92, 246))
        native.drawText("SAYVIS", cx, cy - u * 0.52f, paint)
        paint.clearShadowLayer()

        // ---------------------------------------------------------------- eyes
        val eyeAlpha = 0.55f + 0.35f * breath + 0.30f * voice + (if (thinking) 0.25f else 0f)
        val eyeGlowRadius = u * (0.20f + 0.05f * breath + 0.05f * voice)
        val eyeY = cy - u * 0.12f
        for (side in listOf(-1, 1)) {
            val eyeX = cx + side * u * 0.33f
            // Glow halo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(eyeColor.copy(alpha = eyeAlpha.coerceIn(0f, 1f)), Color.Transparent),
                    center = Offset(eyeX, eyeY),
                    radius = eyeGlowRadius
                ),
                radius = eyeGlowRadius,
                center = Offset(eyeX, eyeY)
            )
            // Angular glowing eye shape.
            val eye = Path().apply {
                moveTo(eyeX - side * u * 0.20f, eyeY - u * 0.015f)
                lineTo(eyeX - side * u * 0.04f, eyeY - u * 0.085f)
                lineTo(eyeX + side * u * 0.20f, eyeY - u * 0.035f)
                lineTo(eyeX + side * u * 0.05f, eyeY + u * 0.065f)
                close()
            }
            drawPath(eye, color = eyeColor.copy(alpha = eyeAlpha.coerceIn(0f, 1f)))
            drawPath(
                eye,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.85f), eyeColor.copy(alpha = 0.15f))
                )
            )
        }

        // Nose ridge.
        drawLine(
            color = Color(0xFF1E2330),
            start = Offset(cx, cy - u * 0.02f),
            end = Offset(cx, cy + u * 0.24f),
            strokeWidth = 3.5f.dp.toPx()
        )

        // -------------------------------------------------------- mouth vent
        val mouthY = cy + u * 0.40f
        val slits = 5
        val mouthColorArgb = android.graphics.Color.argb(
            ((0.45f + 0.45f * breath + (if (thinking) 0.25f else 0f)).coerceIn(0f, 1f) * 255).toInt(),
            (mouthColor.red * 255).toInt(),
            (mouthColor.green * 255).toInt(),
            (mouthColor.blue * 255).toInt()
        )
        drawRoundRect(
            color = Color(0xFF07090F).copy(alpha = 0.9f),
            topLeft = Offset(cx - u * 0.24f, mouthY - u * 0.075f),
            size = Size(u * 0.48f, u * 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.04f)
        )
        for (i in 0 until slits) {
            val slitX = cx - u * 0.18f + i * u * 0.09f
            val slitH = when {
                speaking -> u * (0.05f + 0.05f * abs(sin(angle * 3f + i)))
                thinking -> u * (0.05f + 0.035f * breath)
                else -> u * 0.055f
            }
            drawRoundRect(
                color = androidx.compose.ui.graphics.Color(mouthColorArgb),
                topLeft = Offset(slitX, mouthY - slitH / 2f),
                size = Size(u * 0.035f, slitH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.017f)
            )
        }

        // ------------------------------------------------- chest hexagonal core
        val coreCenter = Offset(cx, cy + u * 1.72f)
        val coreRadius = u * 0.46f
        val coreHex = Path().apply {
            for (i in 0 until 6) {
                val a = Math.toRadians((60.0 * i - 30.0))
                val x = coreCenter.x + coreRadius * cos(a).toFloat()
                val y = coreCenter.y + coreRadius * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            coreHex,
            brush = Brush.verticalGradient(listOf(Color(0xFF2B3140), Color(0xFF0A0D14)))
        )
        drawPath(coreHex, color = Color(0xFF9CA3AF).copy(alpha = 0.8f), style = Stroke(width = 2.2f.dp.toPx()))
        drawPath(coreHex, color = Color(0xFF7C5CFF).copy(alpha = 0.35f + 0.3f * breath), style = Stroke(width = 0.8f.dp.toPx()))

        // Lattice grid clipped inside the hex.
        clipPath(coreHex) {
            val step = u * 0.115f
            for (k in -6..6) {
                val offset = k * step
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.40f),
                    start = Offset(coreCenter.x + offset, coreCenter.y - coreRadius),
                    end = Offset(coreCenter.x + offset - coreRadius, coreCenter.y + coreRadius),
                    strokeWidth = 0.7f.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.40f),
                    start = Offset(coreCenter.x + offset, coreCenter.y - coreRadius),
                    end = Offset(coreCenter.x + offset + coreRadius, coreCenter.y + coreRadius),
                    strokeWidth = 0.7f.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.30f),
                    start = Offset(coreCenter.x - coreRadius, coreCenter.y + offset),
                    end = Offset(coreCenter.x + coreRadius, coreCenter.y + offset),
                    strokeWidth = 0.7f.dp.toPx()
                )
            }
            // Pulsing nucleus.
            val nucleus = coreRadius * (0.30f + 0.10f * breath + 0.08f * voice + (if (thinking) 0.06f else 0f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE879F9), Color.Transparent),
                    center = coreCenter,
                    radius = nucleus
                ),
                radius = nucleus,
                center = coreCenter
            )
            // Orbit ring + electron.
            rotate(degrees = phaseValue * 160f, pivot = coreCenter) {
                drawOval(
                    color = Color(0xFFE879F9).copy(alpha = 0.75f),
                    topLeft = Offset(coreCenter.x - nucleus * 1.7f, coreCenter.y - nucleus * 0.65f),
                    size = Size(nucleus * 3.4f, nucleus * 1.3f),
                    style = Stroke(width = 1.0f.dp.toPx())
                )
            }
            drawCircle(
                color = Color.White,
                radius = 2f.dp.toPx(),
                center = Offset(
                    coreCenter.x + nucleus * 1.7f * cos(angle * 1.3f),
                    coreCenter.y + nucleus * 0.65f * sin(angle * 1.3f)
                )
            )
        }
    }
}

