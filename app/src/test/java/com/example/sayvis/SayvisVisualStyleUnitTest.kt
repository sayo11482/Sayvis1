package com.example.sayvis

import com.example.sayvis.settings.AiVisualStyle
import com.example.sayvis.ui.components.AiStyleMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Verifies the pure maths of the four multidimensional AI views: the icosahedron
 * wireframe (geometric), its perspective projection (multidimensional depth), the
 * deterministic binary 0/1 rain, and the robotic PCM chirps for voice replies.
 */
class SayvisVisualStyleUnitTest {

    // -------------------------------------------------------------- geometry

    @Test
    fun `icosahedron has 12 unit vertices and 30 edges`() {
        val verts = AiStyleMath.ICOSAHEDRON_VERTICES
        assertEquals(12, verts.size)
        verts.forEach { (x, y, z) ->
            assertEquals(1f, sqrt(x * x + y * y + z * z), 1e-4f)
        }
        assertEquals(30, AiStyleMath.ICOSAHEDRON_EDGES.size)
    }

    @Test
    fun `projection stays inside bounds and animates with the angle`() {
        val a = AiStyleMath.rotateAndProject(0.0f, 0.0f)
        val b = AiStyleMath.rotateAndProject(1.3f, 0.7f)
        assertEquals(24, a.size)
        for (v in a) assertTrue("coordinate out of bounds: $v", v in -1.5f..1.5f)
        for (v in b) assertTrue("coordinate out of bounds: $v", v in -1.5f..1.5f)
        assertTrue("different angles must project differently", a.toTypedArray().contentEquals(b.toTypedArray()).not())
    }

    // ---------------------------------------------------------------- binary

    @Test
    fun `binary rain is deterministic and scrolls with time`() {
        val first = AiStyleMath.binaryColumns(columns = 7, rows = 7, t = 0.25f)
        val again = AiStyleMath.binaryColumns(columns = 7, rows = 7, t = 0.25f)
        assertEquals(first.size, 7)
        first.forEachIndexed { c, column ->
            assertEquals(column.size, 7)
            column.forEachIndexed { r, bit -> assertEquals(bit, again[c][r]) }
        }
        val later = AiStyleMath.binaryColumns(columns = 7, rows = 7, t = 0.9f)
        assertTrue(
            "pattern should move with t",
            first.indices.any { c -> first[c].indices.any { r -> first[c][r] != later[c][r] } }
        )
    }

    // ------------------------------------------------------- robotic audio

    @Test
    fun `square tone has correct length envelope and silence`() {
        val tone = AiStyleMath.squareTone(880, 70, sampleRate = 16000)
        assertEquals(16000 * 70 / 1000, tone.size)
        assertTrue("tone must be audible", tone.any { it > Short.MAX_VALUE * 0.1 })
        assertTrue("samples must stay in Short range", tone.all { it in Short.MIN_VALUE..Short.MAX_VALUE })
        val peak = tone.maxOf { abs(it.toInt()) }
        assertTrue("soft clipping must cap the peak below 0.35 full scale", peak < Short.MAX_VALUE * 0.35)
        val silence = AiStyleMath.squareTone(0, 20)
        assertTrue(silence.all { it == 0.toShort() })
    }

    @Test
    fun `reply and ack motifs are non-empty pcm`() {
        val reply = AiStyleMath.replyPcm()
        val ack = AiStyleMath.ackPcm()
        assertTrue(reply.size > 16000 / 4) // > 250 ms of chirps
        assertTrue(reply.any { it != 0.toShort() })
        assertTrue(ack.size in 1..reply.size)
        assertTrue(ack.any { it != 0.toShort() })
    }

    // ------------------------------------------------------------ enum/store

    @Test
    fun `four visual styles with bilingual labels`() {
        assertEquals(4, AiVisualStyle.entries.size)
        AiVisualStyle.entries.forEach { style ->
            assertTrue(style.labelFa.isNotBlank())
            assertTrue(style.labelEn.isNotBlank())
            assertTrue(style.label(true) == style.labelFa)
        }
    }

    @Test
    fun `style name parser is lenient`() {
        assertEquals(AiVisualStyle.BINARY, AiVisualStyle.fromNameOrDefault("binary"))
        assertEquals(AiVisualStyle.STEREOLOGY, AiVisualStyle.fromNameOrDefault("STEREOLOGY"))
        assertEquals(AiVisualStyle.GEOMETRIC, AiVisualStyle.fromNameOrDefault("garbage"))
        assertEquals(AiVisualStyle.GEOMETRIC, AiVisualStyle.fromNameOrDefault(null))
    }

    // ------------------------------------------------- atomic breathing

    @Test
    fun `palette cycles continuously through five stops`() {
        val stops = AiStyleMath.PALETTE_RGB
        assertEquals(5, stops.size)
        // 200 samples around the cycle: every triple in range, adjacent jumps small.
        var previous = AiStyleMath.paletteRgb(0f)
        for (i in 1..200) {
            val rgb = AiStyleMath.paletteRgb(i / 200f)
            assertEquals(3, rgb.size)
            rgb.forEach { channel -> assertTrue("channel out of range: $channel", channel in 0f..1f) }
            val jump = kotlin.math.abs(rgb[0] - previous[0]) + kotlin.math.abs(rgb[1] - previous[1]) + kotlin.math.abs(rgb[2] - previous[2])
            assertTrue("palette jump too large at $i: $jump", jump < 0.25f)
            previous = rgb
        }
        // Seamless loop: phase 1 == phase 0.
        val first = AiStyleMath.paletteRgb(0f)
        val last = AiStyleMath.paletteRgb(1f)
        assertEquals(first[0], last[0], 1e-4f)
        assertEquals(first[1], last[1], 1e-4f)
        assertEquals(first[2], last[2], 1e-4f)
    }

    @Test
    fun `breath envelope oscillates smoothly between zero and one`() {
        var min = 1f
        var max = 0f
        for (i in 0..99) {
            val b = AiStyleMath.breath(i / 100f)
            min = kotlin.math.min(min, b)
            max = kotlin.math.max(max, b)
            assertTrue(b in 0f..1f)
        }
        assertTrue("breath must reach near 0: $min", min < 0.05f)
        assertTrue("breath must reach near 1: $max", max > 0.95f)
        // Periodicity: breath(p) == breath(p + 1)
        assertEquals(AiStyleMath.breath(0.3f), AiStyleMath.breath(1.3f), 1e-4f)
    }

    @Test
    fun `orbit positions stay on the unit squashed ellipse`() {
        for (i in 0..90) {
            val (x, y) = AiStyleMath.orbitPosition(i / 90f * 2f * Math.PI.toFloat(), 0.4f)
            assertTrue(x in -1.0001f..1.0001f)
            assertTrue(y in -0.4001f..0.4001f)
        }
    }

    private fun abs(v: Int): Int = if (v < 0) -v else v
}
