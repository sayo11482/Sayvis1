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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.settings.AiVisualStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

    // ------------------------------------------------- atomic breathing palette

    /**
     * Five-stop neon palette (violet -> cyan -> green -> gold -> rose) that cycles
     * endlessly; every view samples it at a different phase so the whole avatar
     * "breathes" in diverse colours instead of one flat tint.
     */
    val PALETTE_RGB = arrayOf(
        floatArrayOf(0.655f, 0.545f, 0.980f),
        floatArrayOf(0.220f, 0.741f, 0.973f),
        floatArrayOf(0.204f, 0.827f, 0.600f),
        floatArrayOf(0.831f, 0.686f, 0.216f),
        floatArrayOf(0.984f, 0.443f, 0.522f)
    )

    /** Continuous RGB (0..1 triple) sampled from the cycling palette at [phase] 0..1. */
    fun paletteRgb(phase: Float): FloatArray {
        val t = ((phase % 1f) + 1f) % 1f
        val scaled = t * PALETTE_RGB.size
        val i = scaled.toInt().coerceAtMost(PALETTE_RGB.size - 1)
        val j = (i + 1) % PALETTE_RGB.size
        var f = scaled - scaled.toInt()
        f = f * f * (3f - 2f * f) // smoothstep for seamless looping
        val a = PALETTE_RGB[i]
        val b = PALETTE_RGB[j]
        return floatArrayOf(
            a[0] + (b[0] - a[0]) * f,
            a[1] + (b[1] - a[1]) * f,
            a[2] + (b[2] - a[2]) * f
        )
    }

    /** Atomic breathing envelope 0..1 (one full inhale/exhale per phase turn). */
    fun breath(phase: Float): Float = 0.5f + 0.5f * sin(2.0 * PI * phase).toFloat()

    /** Point on a squashed orbit (electrons); [squash] 0..1 flattens the ellipse. */
    fun orbitPosition(angleRad: Float, squash: Float): Pair<Float, Float> =
        cos(angleRad) to sin(angleRad) * squash

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

// ==========================================================================
// Compose-side colour helpers + the four professional atomic-breathing views
// ==========================================================================

/** Palette colour at [phase] (0..1 cycling) with [alpha]. */
fun aiStyleColor(phase: Float, alpha: Float = 1f): Color {
    val rgb = AiStyleMath.paletteRgb(phase)
    return Color(rgb[0], rgb[1], rgb[2], alpha.coerceIn(0f, 1f))
}

/**
 * The four professional, animated, multidimensional "AI views". Every view shares
 * the atomic-breathing language: a nucleus that inhales/exhales, an endlessly
 * cycling five-colour neon palette and orbiting electrons — all reacting to the
 * live microphone [level] (0..1).
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
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "style_phase"
    )
    val rain by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "style_rain"
    )
    val baseColor = if (color.isUnspecified) aiStyleColor(angle) else color
    val accentColor = if (accent.isUnspecified) aiStyleColor(angle + 0.35f) else accent
    val phase = if (animate) angle else 0.35f
    val rainValue = if (animate) rain else 0.35f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * 0.84f
        val voice = level.coerceIn(0f, 1f)

        when (style) {
            AiVisualStyle.GEOMETRIC -> drawAtomicGeometric(center, radius, phase, voice, baseColor, accentColor)
            AiVisualStyle.STEREOLOGY -> drawAtomicStereology(center, radius, phase, voice, baseColor, accentColor)
            AiVisualStyle.BINARY -> drawAtomicBinary(center, radius, phase, rainValue, voice, glyphPaint())
            AiVisualStyle.HOLOGRAM -> drawAtomicHologram(center, radius, phase, voice, baseColor, accentColor)
        }
    }
}

@Composable
private fun glyphPaint(): android.graphics.Paint = androidx.compose.runtime.remember {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textAlign = android.graphics.Paint.Align.CENTER
    }
}

// ------------------------------------------------------------- GEOMETRIC

/**
 * Atomic geometric: a breathing multi-glow nucleus, a depth-shaded icosahedron
 * wireframe and three tilted electron orbits with glowing electrons + trails.
 */
private fun DrawScope.drawAtomicGeometric(
    center: Offset,
    radius: Float,
    phase: Float,
    voice: Float,
    color: Color,
    accent: Color
) {
    val breath = AiStyleMath.breath(phase)
    val breathe = 1f + 0.10f * breath + 0.14f * voice
    val angle = phase * 2f * PI.toFloat()

    // Breathing nucleus: layered palette glows + bright core.
    val nucleusRadius = radius * (0.20f + 0.07f * breath + 0.05f * voice)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(aiStyleColor(phase + 0.5f, 0.55f), Color.Transparent),
            center = center,
            radius = nucleusRadius * 3.2f
        ),
        radius = nucleusRadius * 3.2f,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), accent, Color.Transparent),
            center = center,
            radius = nucleusRadius
        ),
        radius = nucleusRadius,
        center = center
    )

    // Icosahedron wireframe, edges shaded by depth and coloured by the palette.
    val projected = AiStyleMath.rotateAndProject(angle, angle * 0.6f + 0.7f)
    val wireRadius = radius * 0.92f * breathe
    val verts = ArrayList<Offset>(projected.size / 2)
    for (i in projected.indices step 2) {
        verts.add(Offset(center.x + projected[i] * wireRadius, center.y + projected[i + 1] * wireRadius))
    }
    for ((a, b) in AiStyleMath.ICOSAHEDRON_EDGES) {
        val depth = ((projected[a * 2 + 1] + 1.5f) / 3f).coerceIn(0f, 1f)
        drawLine(
            color = aiStyleColor(phase + depth * 0.3f, 0.30f + 0.55f * depth),
            start = verts[a],
            end = verts[b],
            strokeWidth = (0.9f + 1.1f * depth).dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    verts.forEachIndexed { index, v ->
        drawCircle(
            color = aiStyleColor(phase + index / 12f, 0.9f),
            radius = (1.4f + 0.8f * (index % 3 == 0).compareTo(false)).dp.toPx(),
            center = v
        )
    }

    // Three tilted electron orbits with glowing electrons and trails.
    val tilts = listOf(0.30f, 0.62f, 0.16f)
    tilts.forEachIndexed { orbitIndex, squash ->
        val orbitPhase = phase * (1f + orbitIndex * 0.35f) + orbitIndex * 0.33f
        val orbitColor = aiStyleColor(phase + orbitIndex * 0.28f, 0.75f)
        drawOval(
            color = orbitColor.copy(alpha = 0.45f),
            topLeft = Offset(center.x - wireRadius, center.y - wireRadius * squash),
            size = Size(wireRadius * 2f, wireRadius * 2f * squash),
            style = Stroke(width = 1.0f.dp.toPx())
        )
        // Electron + trail along the orbit (rotated by the global angle for depth).
        for (step in 5 downTo 0) {
            val a = orbitPhase * 2f * PI.toFloat() - step * 0.09f + angle * (if (orbitIndex == 1) -1f else 1f)
            val (ex, ey) = AiStyleMath.orbitPosition(a, squash)
            val exRot = ex * cos(angle) - ey * sin(angle)
            val eyRot = ex * sin(angle) + ey * cos(angle)
            val pos = Offset(center.x + exRot * wireRadius, center.y + eyRot * wireRadius)
            val glow = 1f - step / 6f
            drawCircle(
                color = aiStyleColor(phase + orbitIndex * 0.28f + 0.15f, (0.25f + 0.75f * glow)),
                radius = (1.2f.dp.toPx() + 2.2f.dp.toPx() * glow),
                center = pos
            )
        }
    }
}

// ------------------------------------------------------------ STEREOLOGY

/**
 * Atomic stereology: four palette-coloured section planes, a rotating radial
 * spoke lattice, breath-pulsing sampling dots on the lattice intersections and
 * a central electron pair.
 */
private fun DrawScope.drawAtomicStereology(
    center: Offset,
    radius: Float,
    phase: Float,
    voice: Float,
    color: Color,
    accent: Color
) {
    val breath = AiStyleMath.breath(phase + 0.25f)
    val angle = phase * 2f * PI.toFloat()
    val breathe = 1f + 0.06f * breath + 0.10f * voice

    // Section planes (nested, counter-rotating, palette coloured).
    val planes = listOf(1.00f, 0.78f, 0.56f, 0.34f)
    planes.forEachIndexed { index, fraction ->
        val direction = if (index % 2 == 0) angle else -angle * 1.35f
        val squash = 0.26f + 0.17f * index
        drawOval(
            color = aiStyleColor(phase + index * 0.22f, 0.85f),
            topLeft = Offset(center.x - radius * fraction * breathe, center.y - radius * fraction * squash * breathe),
            size = Size(radius * 2f * fraction * breathe, radius * 2f * fraction * squash * breathe),
            style = Stroke(width = (2.2f - index * 0.4f).dp.toPx())
        )
        // Sampling lattice: vertical chords inside each plane.
        val ticks = 9
        for (k in 0 until ticks) {
            val t = k / (ticks - 1).toFloat()
            val nx = t * 2f - 1f
            val halfChord = radius * fraction * sqrt((1f - nx * nx).coerceIn(0f, 1f))
            drawLine(
                color = color.copy(alpha = 0.16f),
                start = Offset(center.x + nx * radius * fraction * breathe, center.y - halfChord * squash * breathe),
                end = Offset(center.x + nx * radius * fraction * breathe, center.y + halfChord * squash * breathe),
                strokeWidth = 0.8f.dp.toPx()
            )
        }
    }

    // Rotating radial spokes with breath-pulsing sampling dots at intersections.
    val spokes = 12
    for (s in 0 until spokes) {
        val a = angle * 0.6f + s * (2f * PI.toFloat() / spokes)
        val (ox, oy) = AiStyleMath.orbitPosition(a, 0.55f)
        drawLine(
            color = color.copy(alpha = 0.30f),
            start = center,
            end = Offset(center.x + ox * radius * breathe, center.y + oy * radius * breathe),
            strokeWidth = 0.9f.dp.toPx()
        )
        // Sampling dot pulses with the breath, one per spoke.
        val dotRadius = radius * (0.30f + 0.10f * breath)
        drawCircle(
            color = aiStyleColor(phase + s / spokes.toFloat(), 0.85f),
            radius = 1.6f.dp.toPx() + 0.9f.dp.toPx() * breath,
            center = Offset(center.x + ox * dotRadius, center.y + oy * dotRadius)
        )
    }

    // Core: breathing nucleus + electron pair on the tight inner orbit.
    val nucleusRadius = radius * (0.13f + 0.05f * breath + 0.04f * voice)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.9f), accent.copy(alpha = 0.85f), Color.Transparent),
            center = center,
            radius = nucleusRadius * 2.6f
        ),
        radius = nucleusRadius * 2.6f,
        center = center
    )
    for (e in 0 until 2) {
        val a = -angle * 1.6f + e * PI.toFloat()
        val (ex, ey) = AiStyleMath.orbitPosition(a, 0.55f)
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = 2.0f.dp.toPx(),
            center = Offset(center.x + ex * radius * 0.22f, center.y + ey * radius * 0.22f)
        )
    }
}

// --------------------------------------------------------------- BINARY

/**
 * Atomic binary: 0/1 rain inside a circular clip, every column tinted by the
 * cycling palette, bright "head" glyphs, a breathing data ring and an orbiting
 * electron around the disc.
 */
private fun DrawScope.drawAtomicBinary(
    center: Offset,
    radius: Float,
    phase: Float,
    rain: Float,
    voice: Float,
    paint: android.graphics.Paint
) {
    val breath = AiStyleMath.breath(phase)
    val angle = phase * 2f * PI.toFloat()
    val clip = Path().apply { addOval(androidx.compose.ui.graphics.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }

    clipPath(clip) {
        val columns = 7
        val rows = 9
        val grid = AiStyleMath.binaryColumns(columns, rows, rain)
        val cell = (radius * 2.1f) / columns
        paint.textSize = cell * 0.60f
        val native = drawContext.canvas.nativeCanvas
        val headRow = ((rain * rows * 2f).toInt() % rows + rows) % rows
        for (c in 0 until columns) {
            val columnColor = aiStyleColor(phase + c / columns.toFloat())
            for (r in 0 until rows) {
                val distance = ((r - headRow + rows) % rows).toFloat() / rows
                val alpha = ((1f - distance) * (1f - distance) * 235f + 25f).toInt().coerceIn(0, 255)
                val glyph = if (grid[c][r]) "1" else "0"
                val tinted = if (distance < 0.12f) {
                    android.graphics.Color.argb(alpha, 245, 245, 245) // white-hot head
                } else {
                    android.graphics.Color.argb(
                        alpha,
                        (columnColor.red * 255).toInt(),
                        (columnColor.green * 255).toInt(),
                        (columnColor.blue * 255).toInt()
                    )
                }
                paint.color = tinted
                val x = center.x - radius + cell * (c + 0.5f)
                val y = center.y - radius + (radius * 2.1f) / rows * (r + 0.85f)
                native.drawText(glyph, x, y, paint)
            }
        }
    }

    // Breathing data ring (double stroke, palette gradient).
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                aiStyleColor(phase),
                aiStyleColor(phase + 0.25f),
                aiStyleColor(phase + 0.5f),
                aiStyleColor(phase + 0.75f),
                aiStyleColor(phase)
            ),
            center = center
        ),
        radius = radius * (1.0f + 0.03f * breath),
        center = center,
        style = Stroke(width = (1.6f + 1.2f * breath).dp.toPx())
    )

    // Voice-reactive core + orbiting electron.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                aiStyleColor(phase + 0.4f, 0.35f + 0.35f * voice),
                Color.Transparent
            ),
            center = center,
            radius = radius * (0.55f + 0.15f * breath + 0.15f * voice)
        ),
        radius = radius * (0.55f + 0.15f * breath + 0.15f * voice),
        center = center
    )
    val (ex, ey) = AiStyleMath.orbitPosition(angle * 1.4f, 0.8f)
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = 2.2f.dp.toPx(),
        center = Offset(center.x + ex * radius * 0.95f, center.y + ey * radius * 0.95f)
    )
}

// ------------------------------------------------------------- HOLOGRAM

/**
 * Atomic hologram: perspective grid, breathing radar rings, a rotating hex
 * emitter with a gradient stroke, a scan beam with a glowing band and a core
 * electron.
 */
private fun DrawScope.drawAtomicHologram(
    center: Offset,
    radius: Float,
    phase: Float,
    voice: Float,
    color: Color,
    accent: Color
) {
    val breath = AiStyleMath.breath(phase + 0.4f)
    val angle = phase * 2f * PI.toFloat()
    val horizon = center.y + radius * 0.30f

    // Perspective grid.
    for (i in -3..3) {
        val y = horizon + i * radius * 0.22f
        val spread = 1f + abs(i) * 0.35f
        drawLine(
            color = aiStyleColor(phase + 0.1f * abs(i), 0.45f),
            start = Offset(center.x - radius * spread * 0.8f, y),
            end = Offset(center.x + radius * spread * 0.8f, y),
            strokeWidth = 1f.dp.toPx()
        )
    }
    for (i in -4..4) {
        val x = center.x + i * radius * 0.25f
        drawLine(
            color = color.copy(alpha = 0.28f),
            start = Offset(x, horizon),
            end = Offset(center.x + i * radius * 0.9f, center.y - radius * 0.55f),
            strokeWidth = 0.8f.dp.toPx()
        )
    }

    // Radar rings expanding with the breath.
    for (r in 0 until 3) {
        val ringPhase = ((phase + r / 3f) % 1f)
        val ringRadius = radius * (0.25f + 0.75f * ringPhase)
        drawCircle(
            color = aiStyleColor(phase + r * 0.3f, (1f - ringPhase) * 0.8f),
            radius = ringRadius,
            center = Offset(center.x, center.y - radius * 0.1f),
            style = Stroke(width = (1.6f * (1f - ringPhase) + 0.4f).dp.toPx())
        )
    }

    // Rotating hexagonal emitter with a gradient stroke.
    val hex = Path()
    val hexRadius = radius * 0.52f * (1f + 0.05f * breath + 0.06f * voice)
    for (i in 0 until 6) {
        val a = angle + i * (PI.toFloat() / 3f)
        val x = center.x + hexRadius * cos(a)
        val y = center.y - radius * 0.10f + hexRadius * sin(a) * 0.55f
        if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
    }
    hex.close()
    drawPath(hex, color = color.copy(alpha = 0.20f))
    drawPath(
        hex,
        brush = Brush.sweepGradient(
            colors = listOf(accent, aiStyleColor(phase + 0.45f), accent),
            center = center
        ),
        style = Stroke(width = 1.8f.dp.toPx())
    )

    // Scan beam + glowing band clipped to the hex.
    val beamY = center.y - radius + (radius * 2f) * ((phase * 1.3f) % 1f)
    drawLine(
        color = accent.copy(alpha = 0.95f),
        start = Offset(center.x - radius, beamY),
        end = Offset(center.x + radius, beamY),
        strokeWidth = 1.8f.dp.toPx()
    )
    clipPath(hex) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = 0.30f)),
                startY = beamY - 14f.dp.toPx(),
                endY = beamY
            ),
            topLeft = Offset(center.x - radius, beamY - 14f.dp.toPx()),
            size = Size(radius * 2f, 14f.dp.toPx())
        )
    }

    // Core electron hovering in the emitter.
    val (ex, ey) = AiStyleMath.orbitPosition(angle * 1.8f, 0.5f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, accent.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(center.x + ex * hexRadius * 0.7f, center.y - radius * 0.10f + ey * hexRadius * 0.4f),
            radius = 7f.dp.toPx()
        ),
        radius = 7f.dp.toPx(),
        center = Offset(center.x + ex * hexRadius * 0.7f, center.y - radius * 0.10f + ey * hexRadius * 0.4f)
    )
}
