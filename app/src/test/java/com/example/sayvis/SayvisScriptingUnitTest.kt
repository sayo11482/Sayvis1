package com.example.sayvis

import com.example.sayvis.scripts.ScriptContext
import com.example.sayvis.scripts.ScriptEffect
import com.example.sayvis.scripts.ScriptEngine
import com.example.sayvis.scripts.ScriptTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the SAYVIS automation language: grammar, evaluation order, fail-closed
 * behaviour on unknown variables, and the guarantee that a script only ever *proposes*
 * effects rather than performing I/O itself.
 */
class SayvisScriptingUnitTest {

    private val engine = ScriptEngine()

    private fun context(
        battery: Int = 85,
        hour: Int = 9,
        profit: Double = 20.0,
        online: Boolean = true,
        locked: Boolean = false,
        blockedTasks: Int = 0,
        load: String = "OPTIMAL",
        gold: Double = 2580.0
    ) = ScriptContext(
        batteryPercent = battery,
        networkOnline = online,
        emergencyLockActive = locked,
        blockedTasks = blockedTasks,
        cognitiveLoad = load,
        hourOfDay = hour,
        quotes = mapOf("XAUUSD" to gold),
        accountBalance = 10000.0,
        accountEquity = 10000.0 + profit,
        dailyPnl = profit
    )

    // ------------------------------------------------------------------ grammar

    @Test
    fun validate_acceptsEveryDocumentedForm() {
        val source = """
            # comment line
            SET ${'$'}cap = 50
            WHEN battery < 20 THEN notify "low"
            IF battery > 5 THEN log "ok"
            WHEN battery < 1 THEN propose "replan"
            WHEN battery < 1 THEN block "stop"
            WHEN battery < 1 THEN webhook "https://example.com/hook" "{\"a\":1}"
            WHEN battery < 1 THEN set ${'$'}x = 2
            WHEN battery < 1 THEN set_mode PAPER
            ON MANUAL THEN log "manual run"
            ON MARKET THEN log "market run"
        """.trimIndent()

        val result = engine.validate(source)
        assertTrue("expected valid, got: ${result.errorEn}", result.valid)
        assertNull(result.errorLine)
    }

    @Test
    fun validate_rejectsUnknownActionAndReportsTheLine() {
        val source = """
            WHEN battery < 20 THEN notify "fine"
            WHEN battery < 20 THEN dance "not an action"
        """.trimIndent()

        val result = engine.validate(source)
        assertFalse(result.valid)
        assertEquals(2, result.errorLine)
        assertTrue(result.errorEn.contains("dance"))
        assertTrue(result.errorFa.isNotBlank())
    }

    @Test
    fun validate_rejectsMalformedSetAndUnknownHead() {
        assertFalse(engine.validate("SET novalue").valid)
        assertFalse(engine.validate("GARBAGE LINE").valid)
        assertFalse(engine.validate("WHEN battery < 20 THEN").valid)
        assertFalse(engine.validate("ON TELEPORT THEN log \"x\"").valid)
    }

    @Test
    fun validate_rejectsUnknownSetModeValue() {
        assertFalse(engine.validate("WHEN battery < 20 THEN set_mode EVERYTHING").valid)
        assertTrue(engine.validate("WHEN battery < 20 THEN set_mode DEMO").valid)
    }

    // ------------------------------------------------------------- expressions

    @Test
    fun arithmetic_respectsPrecedenceAndParentheses() {
        val ctx = context(battery = 85)
        assertEquals(14.0, engine.evaluate("2 + 3 * 4", ctx)?.asDouble()!!, 1e-9)
        assertEquals(20.0, engine.evaluate("(2 + 3) * 4", ctx)?.asDouble()!!, 1e-9)
        assertEquals(80.0, engine.evaluate("battery - 5", ctx)?.asDouble()!!, 1e-9)
    }

    @Test
    fun unaryMinus_doesNotSwallowBinarySubtraction() {
        // Regression: `0 - $cap` must be subtraction, not two unary minuses.
        val ctx = context(profit = -60.0)
        assertNull("SET is a statement, not an expression", engine.evaluate("SET cap = 50", ctx))

        val runResult = engine.run(
            "SET ${'$'}cap = 50\nWHEN account.profit < 0 - ${'$'}cap THEN block \"cap hit\"",
            ctx,
            "MANUAL"
        )
        assertTrue(runResult.success)
        assertEquals(1, runResult.effects.count { it is ScriptEffect.Block })
        assertEquals(
            "cap hit",
            (runResult.effects.first { it is ScriptEffect.Block } as ScriptEffect.Block).reason
        )
    }

    @Test
    fun unaryMinus_negatesALoneValue() {
        val ctx = context(profit = -60.0)
        val result = engine.run(
            "SET ${'$'}cap = 50\nWHEN account.profit < -${'$'}cap THEN block \"cap hit\"",
            ctx,
            "MANUAL"
        )
        assertTrue(result.success)
        assertEquals(1, result.effects.count { it is ScriptEffect.Block })
    }

    @Test
    fun divisionByZero_failsInsteadOfReturningInfinity() {
        assertNull(engine.evaluate("10 / 0", context()))
    }

    @Test
    fun stringConcatenation_isSupported() {
        assertEquals("ab", engine.evaluate("\"a\" + \"b\"", context())?.render())
    }

    @Test
    fun unknownVariables_failClosedInsteadOfFiring() {
        val result = engine.run("WHEN nothing.here > 5 THEN notify \"should never fire\"", context(), "MANUAL")
        assertTrue(result.success)
        assertTrue(result.effects.isEmpty())
    }

    // ------------------------------------------------------------- interpolation

    @Test
    fun interpolation_substitutesLiveContextValues() {
        val result = engine.run(
            "WHEN battery < 20 THEN notify \"Battery is at {battery}%\"",
            context(battery = 17),
            "MANUAL"
        )
        assertTrue(result.success)
        val notify = result.effects.filterIsInstance<ScriptEffect.Notify>().single()
        assertEquals("Battery is at 17%", notify.message)
    }

    // ---------------------------------------------------------------- triggers

    @Test
    fun onTrigger_onlyFiresForTheMatchingEvent() {
        val source = "ON MARKET THEN log \"market only\""
        assertTrue(engine.run(source, context(), "MANUAL").effects.isEmpty())
        assertEquals(1, engine.run(source, context(), ScriptTrigger.MARKET.name).effects.size)
    }

    @Test
    fun everyStarterScript_parsesAndRuns() {
        ScriptEngine.starterScripts().forEach { script ->
            assertTrue("${script.name} failed to validate: ${script.id}", engine.validate(script.source).valid)
            // Run against a stressed context so at least one rule should fire.
            val stressed = context(battery = 8, hour = 23, profit = -80.0, online = false, locked = true, gold = 2700.0)
            val result = engine.run(script.source, stressed, script.trigger.name)
            assertTrue("${script.name} errored: ${result.errorEn}", result.success)
        }
    }

    @Test
    fun starterScripts_haveDistinctIdsAndKnownTriggers() {
        val starters = ScriptEngine.starterScripts()
        assertTrue(starters.size >= 3)
        assertEquals(starters.size, starters.map { it.id }.distinct().size)
        starters.forEach { assertTrue(ScriptTrigger.entries.contains(it.trigger)) }
    }

    // ----------------------------------------------------------------- effects

    @Test
    fun scriptNeverPerformsIo_itOnlyReturnsEffects() {
        val result = engine.run(
            "WHEN network.online == false THEN webhook \"https://example.com/hook\" \"{\\\"state\\\":\\\"offline\\\"}\"",
            context(online = false),
            "MANUAL"
        )
        assertTrue(result.success)
        val webhook = result.effects.filterIsInstance<ScriptEffect.Webhook>().single()
        assertEquals("https://example.com/hook", webhook.url)
        assertTrue(webhook.payload.contains("offline"))
    }

    @Test
    fun reference_isProvidedInBothLanguages() {
        val fa = ScriptEngine.reference(isPersian = true)
        val en = ScriptEngine.reference(isPersian = false)
        assertTrue(fa.contains("WHEN"))
        assertTrue(en.contains("WHEN"))
        assertFalse(fa == en)
        // The documented trigger names must match what the parser accepts.
        listOf("MANUAL", "CONTEXT_CHANGE", "SCHEDULE", "MARKET").forEach {
            assertTrue("FA reference is missing $it", fa.contains(it))
            assertTrue("EN reference is missing $it", en.contains(it))
        }
    }
}
