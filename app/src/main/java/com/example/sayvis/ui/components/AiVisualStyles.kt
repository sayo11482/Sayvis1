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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.settings.AiVisualStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure math + deterministic patterns behind the four "AI visual style" views.
 * No Android/Compose types here, so everything is unit-testable on the JVM.
 */
object AiStyleMath {

    const val PROJECTION_FOV = 3.2f

    /** Golden-ratio icosahedron: 12 vertices on the unit sphere. */
    val ICOSAHEDRON_VERTICES: List<Triple<Float, Float, Float>> by lazy {
        val phi = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val raw = ArrayList<Triple<Float, Float, Float>>(12)
        for (a in listOf(-1f, 1f)) {
            for (b in listOf(-phi, phi)) {
                raw.add(Triple(0f, a.toFloat(), b))
                raw.add(Triple(a.toFloat(), b, 0f))
                raw.add(Triple(b, 0f, a.toFloat()))
            }
        }
        raw.map { (x, y, z) ->
            val len = sqrt(x * x + y * y + z * z)
            Triple(x / len, y / len, z / len)
        }
    }

    /** Vertex index pairs whose distance equals the (single) edge length: 30 edges. */
    val ICOSAHEDRON_EDGES: List<Pair<Int, Int>> by lazy {
        val verts = ICOSAHEDRON_VERTICES
        var minDist = Float.MAX_VALUE
        for (i in verts.indices) {
            for (j in i + 1 until verts.size) {
                val (x1, y1, z1) = verts[i]
                val (x2, y2, z2) = verts[j]
                val d = sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
                if (d < minDist) minDist = d
            }
        }
        val edges = ArrayList<Pair<Int, Int>>(30)
        for (i in verts.indices) {
            for (j in i + 1 until verts.size) {
                val (x1, y1, z1) = verts[i]
                val (x2, y2, z2) = verts[j]
                val d = sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
                if (abs(d - minDist) < minDist * 0.05f) edges.add(i to j)
            }
        }
        edges
    }

    /**
     * Rotates the unit-sphere vertices by [angleYRad]/[angleXRad] and applies a
     * perspective projection. Output: flat [x0,y0,x1,y1,…] in the range [-1, 1]
     * (multiply by the wanted radius on screen).
     */
    fun rotateAndProject(angleYRad: Float, angleXRad: Float): FloatArray {
        val verts = ICOSAHEDRON_VERTICES
        val out = FloatArray(verts.size * 2)
        val cy = cos(angleYRad); val sy = sin(angleYRad)
        val cx = cos(angleXRad); val sx = sin(angleXRad)
        for (i in verts.indices) {
            val (x, y, z) = verts[i]
            // Y-axis rotation
            val x1 = x * cy + z * sy
            val z1 = -x * sy + z * cy
            // X-axis rotation
            val y2 = y * cx - z1 * sx
            val z2 = y * sx + z1 * cx
            val scale = PROJECTION_FOV / (PROJECTION_FOV - z2)
            out[i * 2] = (x1 * scale).coerceIn(-1.5f, 1.5f)
            out[i * 2 + 1] = (y2 * scale).coerceIn(-1.5f, 1.5f)
        }
        return out
    }

    /**
     * Deterministic binary glyph matrix ("0"/"1" rain). [t] in 0..1 scrolls the
     * pattern; identical inputs always yield identical output (no per-frame RNG).
     */
    fun binaryColumns(columns: Int, rows: Int, t: Float): Array<BooleanArray> {
        val grid = Array(columns) { BooleanArray(rows) }
        for (c in 0 until columns) {
            for (r in 0 until rows) {
                val stream = c * 7919
                val cell = (r + kotlin.math.floor(t * rows * 2.0).toInt()) * 104729 + stream
                grid[c][r] = ((cell xor (cell ushr 7)) * 2654435761L) and 0x10000L != 0L
            }
        }
        return grid
    }

    /** One soft-clipped square-wave tone; [freqHz] 0 = silence. Pure JVM. */
    fun squareTone(freqHz: Int, durationMs: Int, sampleRate: Int = 16000): ShortArray {
        val n = sampleRate * durationMs / 1000
        val out = ShortArray(n)
        val attack = (sampleRate * 0.004f).toInt().coerceAtLeast(1)
        val release = (sampleRate * 0.010f).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            val raw = if (freqHz <= 0) 0.0 else sin(2.0 * PI * freqHz * i / sampleRate)
            val square = when {
                raw > 0.0 -> 1.0
                raw < 0.0 -> -1.0
                else -> 0.0
            }
            var env = 1f
            if (i < attack) env = i.toFloat() / attack
            if (i > n - release) env = ((n - i).toFloat() / release).coerceIn(0f, 1f)
            out[i] = (square * env * 0.30 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Concatenates PCM segments. */
    fun concat(vararg parts: ShortArray): ShortArray {
        var total = 0
        parts.forEach { total += it.size }
        val out = ShortArray(total)
        var offset = 0
        parts.forEach { part ->
            System.arraycopy(part, 0, out, offset, part.size)
            offset += part.size
        }
        return out
    }

    /** Rising three-chirp "answer" motif played when SAYVIS answers a voice. */
    fun replyPcm(): ShortArray = concat(
        silence(30), squareTone(620, 70), silence(18),
        squareTone(830, 70), silence(18), squareTone(1100, 92)
    )

    /** Short two-tone acknowledgement for a recognised owner voice. */
    fun ackPcm(): ShortArray = concat(squareTone(980, 55), silence(14), squareTone(1400, 70))

    private fun silence(ms: Int): ShortArray = ShortArray(16000 * ms / 1000)
}

/**
 * The four professional, animated, multidimensional "AI views": geometric
 * wireframes, stereologic cross-sections, binary 0/1 code rain and a hologram
 * scan. Used in Settings (selector + live preview) and by the avatar itself.
 * [level] (0..1, the live microphone amplitude) makes every view react to voice.
 */
@Composable
fun AiStyleCanvas(
    style: AiVisualStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    accent: Color = Color.Unspecified,
    level: Float = 0f,
    animate: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "ai_style")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "style_angle"
    )
    val rain by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "style_rain"
    )
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "style_scan"
    )
    val baseColor = if (color.isUnspecified) com.example.sayvis.ui.theme.SayvisCyan else color
    val accentColor = if (accent.isUnspecified) com.example.sayvis.ui.theme.SayvisGold else accent
    val angleValue = if (animate) angle else 0.8f
    val rainValue = if (animate) rain else 0.35f
    val scanValue = if (animate) scan else 0.4f
    val glyphPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * 0.82f
        val voiceBoost = 1f + level.coerceIn(0f, 1f) * 0.22f

        when (style) {
            AiVisualStyle.GEOMETRIC -> drawGeometric(center, radius * voiceBoost, angleValue, baseColor, accentColor)
            AiVisualStyle.STEREOLOGY -> drawStereology(center, radius * voiceBoost, angleValue, baseColor, accentColor)
            AiVisualStyle.BINARY -> drawBinary(center, radius, rainValue, baseColor, accentColor, glyphPaint, level)
            AiVisualStyle.HOLOGRAM -> drawHologram(center, radius, scanValue, angleValue, baseColor, accentColor)
        }
    }
}

private fun DrawScope.drawGeometric(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    accent: Color,
) {
    val projected = AiStyleMath.rotateAndProject(angle, angle * 0.6f + 0.7f)
    val verts = ArrayList<Offset>(projected.size / 2)
    for (i in projected.indices step 2) {
        verts.add(Offset(center.x + projected[i] * radius, center.y + projected[i + 1] * radius))
    }
    // Depth-aware edges: nearer edges brighter.
    for ((a, b) in AiStyleMath.ICOSAHEDRON_EDGES) {
        val za = projected[a * 2 + 1]
        val depthAlpha = 0.35f + 0.45f * ((za + 1.5f) / 3f)
        drawLine(
            color = color.copy(alpha = depthAlpha),
            start = verts[a],
            end = verts[b],
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    verts.forEachIndexed { index, v ->
        drawCircle(
            color = if (index % 3 == 0) accent else color,
            radius = 1.9.dp.toPx(),
            center = v
        )
    }
    drawCircle(color = accent.copy(alpha = 0.55f), radius = 2.6.dp.toPx(), center = center)
}

private fun DrawScope.drawStereology(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    accent: Color,
) {
    // Three nested, counter-rotating section planes + sampling lattice.
    val planes = listOf(0.95f, 0.68f, 0.42f)
    planes.forEachIndexed { index, fraction ->
        val tilt = if (index % 2 == 0) angle else -angle * 1.35f
        val squash = 0.32f + 0.16f * index
        drawOval(
            color = if (index == 1) accent.copy(alpha = 0.8f) else color.copy(alpha = 0.85f),
            topLeft = Offset(center.x - radius * fraction, center.y - radius * fraction * squash),
            size = androidx.compose.ui.geometry.Size(radius * 2f * fraction, radius * 2f * fraction * squash),
            style = Stroke(width = (2.1f - index * 0.4f).dp.toPx())
        )
        // Lattice ticks on each plane (stereological sampling grid).
        val ticks = 8
        for (k in 0 until ticks) {
            val t = k / ticks.toFloat()
            val x = center.x + (t * 2f - 1f) * radius * fraction
            val halfChord = radius * fraction * sqrt((1f - (t * 2f - 1f) * (t * 2f - 1f)).coerceIn(0f, 1f))
            drawLine(
                color = color.copy(alpha = 0.22f),
                start = Offset(x, center.y - halfChord * squash),
                end = Offset(x, center.y + halfChord * squash),
                strokeWidth = 0.9f.dp.toPx()
            )
        }
    }
    // Core probe axis.
    drawLine(
        color = accent.copy(alpha = 0.9f),
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1.2f.dp.toPx()
    )
}

private fun DrawScope.drawBinary(
    center: Offset,
    radius: Float,
    rain: Float,
    color: Color,
    accent: Color,
    glyphPaint: android.graphics.Paint,
    level: Float,
) {
    val columns = 7
    val rows = 7
    val grid = AiStyleMath.binaryColumns(columns, rows, rain)
    val cell = radius * 2f / columns
    glyphPaint.textSize = cell * 0.62f
    val native = drawContext.canvas.nativeCanvas
    for (c in 0 until columns) {
        for (r in 0 until rows) {
            val x = center.x - radius + cell * (c + 0.5f)
            val y = center.y - radius + cell * (r + 0.85f)
            val glyph = if (grid[c][r]) "1" else "0"
            val highlight = (r == (columns - 1 - c) % rows)
            glyphPaint.color = android.graphics.Color.argb(
                (if (highlight) 235 else 110).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            native.drawText(glyph, x, y, glyphPaint)
        }
    }
    // Voice-reactive core disc.
    drawCircle(
        color = accent.copy(alpha = 0.16f + 0.25f * level.coerceIn(0f, 1f)),
        radius = radius * (0.34f + 0.18f * level.coerceIn(0f, 1f)),
        center = center
    )
    drawCircle(color = accent, radius = 2.4f.dp.toPx(), center = center, style = Stroke(1.4f.dp.toPx()))
}

private fun DrawScope.drawHologram(
    center: Offset,
    radius: Float,
    scan: Float,
    angle: Float,
    color: Color,
    accent: Color,
) {
    // Horizon grid: perspective lines converging on the centre band.
    val gridColor = color.copy(alpha = 0.5f)
    for (i in -3..3) {
        val y = center.y + radius * 0.28f + i * radius * 0.22f
        val spread = 1f + abs(i) * 0.35f
        drawLine(
            color = gridColor,
            start = Offset(center.x - radius * spread * 0.8f, y),
            end = Offset(center.x + radius * spread * 0.8f, y),
            strokeWidth = 1f.dp.toPx()
        )
    }
    for (i in -4..4) {
        val x = center.x + i * radius * 0.25f
        drawLine(
            color = gridColor.copy(alpha = 0.6f),
            start = Offset(x, center.y + radius * 0.28f),
            end = Offset(center.x + i * radius * 0.9f, center.y - radius * 0.5f),
            strokeWidth = 0.9f.dp.toPx()
        )
    }
    // Rotating holographic hexagon emitter.
    val hex = Path()
    val hexRadius = radius * 0.5f
    for (i in 0 until 6) {
        val a = angle + i * (PI.toFloat() / 3f)
        val x = center.x + hexRadius * cos(a)
        val y = center.y - radius * 0.12f + hexRadius * sin(a) * 0.55f
        if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
    }
    hex.close()
    drawPath(hex, color = color.copy(alpha = 0.28f))
    drawPath(hex, color = color, style = Stroke(1.6f.dp.toPx()))
    // Moving scan beam clipped to the view.
    val beamY = center.y - radius + (radius * 2f) * scan
    drawLine(
        color = accent.copy(alpha = 0.95f),
        start = Offset(center.x - radius, beamY),
        end = Offset(center.x + radius, beamY),
        strokeWidth = 1.8f.dp.toPx()
    )
    drawLine(
        color = accent.copy(alpha = 0.30f),
        start = Offset(center.x - radius, beamY - 5f.dp.toPx()),
        end = Offset(center.x + radius, beamY - 5f.dp.toPx()),
        strokeWidth = 0.9f.dp.toPx()
    )
    clipPath(hex) {
        drawRect(
            color = accent.copy(alpha = 0.10f),
            topLeft = Offset(center.x - radius, beamY - 6f.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(radius * 2f, 8f.dp.toPx())
        )
    }
}
