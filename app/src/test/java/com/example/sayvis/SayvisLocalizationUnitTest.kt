package com.example.sayvis

import com.example.sayvis.ai.TranslationService
import com.example.sayvis.ai.TranslationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the hybrid localisation pipeline.
 *
 * The owner's requirement is that switching to Persian produces a *fully* Persian
 * interface, so the tests below pin the two properties that make that true:
 *  - everything the app itself generates resolves offline (dictionary or template),
 *    never falling through to raw English and never costing a model call;
 *  - machine identifiers, tickers and enum codes are recognised as non-prose and are
 *    shown as-is instead of being reported as untranslated.
 */
class SayvisLocalizationUnitTest {

    private val service = TranslationService(context = null)

    /** Real strings the app generates, drawn from seed data and the AWARE engine. */
    private val generatedStrings = listOf(
        // UIC seed
        "Primary Language & Tone",
        "Optimal Deep Work Window",
        "Strict Zero Trust Policy",
        "Lifelong Cognitive Augmentation",
        "Explicit Owner Onboarding Config",
        "AWARE telemetry analysis over 14 days",
        "Owner Strategic Mission Entry",
        // Epistemic status + provenance
        "USER_EXPLICIT",
        "SYSTEM_OBSERVED",
        "AI_INFERRED",
        "DEVICE_SIGNAL",
        // AWARE opportunities
        "Protect Peak Cognitive Window",
        "Mission Critical Blocker Mitigation",
        "Unattended Remote Device Session",
        "Conserve Energy & Save Local Checkpoint",
        "Mitigate Thermal Throttling",
        "Sustained Cognitive Intensity",
        "Workstream Blocker Cluster",
        "Circadian Rhythm Deviation",
        "Recurring Dependency Stalls",
        "Cognitive Focus Cycle Adherence",
        "Schedule Deep Work Shield",
        "Block notifications & reschedule advisory check-in to 14:00",
        "Inject Fallback Ed25519 Spec & reassign priority to high",
        "Demote session token to read-only safe mode",
        "Throttle background polling and flush uncommitted mission states to encrypted storage.",
        // Missions & devices
        "Deploy SAYVIS Multi-Device Gateway",
        "Implement Zero-Trust Action Model",
        "Device Cryptographic Identity & Pairing",
        "Integrate AWARE Opportunity Engine",
        "Enforce Strict Emergency Lockout Mechanism",
        "LIT Trading Intelligence - Risk Engine Gate",
        "Order Block & BOS Detection Module",
        "Drawdown Hard Cap Guardrails",
        "Broker Adapter Sandbox Audit",
        "Waiting for audit signoff",
        "SAYVIS Mobile Node (Galaxy S24 / Pixel)",
        "SAYVIS Master Rig (Windows 11 Agent)",
        "SAYVIS Cloud Web Console",
        // Capability tags
        "biometric_auth",
        "voice_vad",
        "controlled_powershell",
        "remote_emergency_lock",
        // Life simulation & LIT
        "Transition to full-time autonomous AI operating system engineering",
        "Early runway volatility",
        "Multi-platform distribution complexity",
        "First-mover advantage in personal AI layers",
        "Complete cognitive sovereignty",
        "Elimination of evening burnout",
        "Institutional liquidity grab at low range. Paper-trading simulation only.",
        "Macro inflation hedge inversion with bullish order flow.",
        "XAU/USD (Gold)",
        // Context + audit vocabulary
        "Deep Work Window",
        "System Booting",
        "Optimal",
        "Optimal Flow",
        "Fatigue Risk",
        "Online",
        "Offline",
        "Deep Work",
        "SUCCESS",
        "BLOCKED",
        "REJECTED",
        "OWNER",
        "SAYVIS_AGENT",
        "AWARE_ENGINE",
        "SCRIPT_ENGINE",
        "OWNER_CONFIRMED",
        "BLOCKED_EMERGENCY_LOCK",
        "POLICY_PERMITTED",
        "system.boot.verify_integrity",
        "uic.attribute.update_status",
        "opportunity.execute.attempt",
        "mission.task.toggle",
        "device.trust.toggle",
        "ai.provider.probe",
        "security.emergency_lock.engage",
        "settings.reset_all",
        "trading.gateway.connect",
        "trading.execution_mode.change",
        "trading.order.blocked",
        "automation.script.run",
        "automation.webhook.send",
        "Blocked dependency"
    )

    // ---------------------------------------------------------------- tier 1

    @Test
    fun everyStringTheAppGenerates_resolvesOfflineWithoutFallingThroughToEnglish() {
        val unresolved = generatedStrings.filter {
            service.resolveOffline(it).source == TranslationSource.FAILED
        }
        assertTrue("these strings fall through to English: $unresolved", unresolved.isEmpty())

        // A handful of entries are deliberately bilingual: a ticker keeps its symbol and a
        // product name keeps its brand ("XAU/USD (Gold)" -> "XAU/USD (طلا)"). Everything
        // else must come back majority-Persian, and nothing may come back untouched.
        val bilingualAllowance = setOf("XAU/USD (Gold)", "AWARE_ENGINE")
        generatedStrings.forEach { source ->
            val result = service.resolveOffline(source)
            assertEquals("unexpected tier for \"$source\"", TranslationSource.DICTIONARY, result.source)
            assertNotEquals("\"$source\" was returned unchanged", source, result.text)
            if (source !in bilingualAllowance) {
                assertTrue(
                    "\"$source\" resolved to \"${result.text}\" which is not Persian",
                    service.isAlreadyPersian(result.text)
                )
            }
        }
    }

    @Test
    fun pureProseOutput_isEntirelyPersian() {
        listOf(
            "Conserve Energy & Save Local Checkpoint",
            "Waiting for audit signoff",
            "Implement Zero-Trust Action Model",
            "Enforce Strict Emergency Lockout Mechanism",
            "Elimination of evening burnout",
            "Deploy SAYVIS Multi-Device Gateway"
        ).forEach { source ->
            val result = service.resolveOffline(source)
            assertTrue(
                "\"$source\" -> \"${result.text}\" is not fully Persian",
                service.isAlreadyPersian(result.text) && !service.needsTranslation(result.text)
            )
        }
    }

    @Test
    fun dictionaryHits_areReportedAsHandWrittenNotMachineTranslated() {
        // The "machine translated" marker must only appear for real model output.
        val result = service.resolveOffline("Strict Zero Trust Policy")
        assertEquals(TranslationSource.DICTIONARY, result.source)
        assertFalse(result.isMachine)
    }

    @Test
    fun auditCodesAndCapabilityTags_areTranslatedEvenThoughTheyLookTechnical() {
        // Exact dictionary matches win over the technical-token guard.
        assertEquals(
            TranslationSource.DICTIONARY,
            service.resolveOffline("trading.order.submit").source
        )
        assertEquals(
            TranslationSource.DICTIONARY,
            service.resolveOffline("biometric_auth").source
        )
        assertEquals(
            TranslationSource.DICTIONARY,
            service.resolveOffline("power_save_mode_enable").source
        )
    }

    // ---------------------------------------------------------------- tier 2

    @Test
    fun interpolatedNumbers_resolveOfflineWithoutAModelCall() {
        val battery = service.resolveOffline(
            "Battery level at 17%. Recommend optimizing background services and " +
                "securing local database checkpoint."
        )
        assertEquals(TranslationSource.DICTIONARY, battery.source)
        assertTrue(battery.text.contains("17"))
        assertTrue(service.isAlreadyPersian(battery.text))

        val sessions = service.resolveOffline(
            "Detected 3 consecutive high-effort sessions. Rest interval recommended."
        )
        assertEquals(TranslationSource.DICTIONARY, sessions.source)
        assertTrue(sessions.text.contains("3"))

        val progress = service.resolveOffline("42% complete")
        assertEquals(TranslationSource.DICTIONARY, progress.source)
        assertTrue(progress.text.contains("42"))
    }

    @Test
    fun englishFragmentsNestedInsideASentence_areTranslatedRecursively() {
        val known = service.resolveOffline("Resolve Blocker in 'Deploy SAYVIS Multi-Device Gateway'")
        assertEquals(TranslationSource.DICTIONARY, known.source)
        assertFalse(
            "nested fragment stayed English: ${known.text}",
            known.text.contains("Deploy SAYVIS Multi-Device Gateway")
        )
        assertTrue(service.isAlreadyPersian(known.text))

        val partiallyKnown = service.resolveOffline(
            "Task 'Crypto Key Exchange Protocol' is blocked: External dependency."
        )
        assertEquals(TranslationSource.DICTIONARY, partiallyKnown.source)
        assertTrue(partiallyKnown.text.contains("مسدود است"))
        // "External dependency" is in the dictionary, so the reason must not stay English.
        assertFalse(partiallyKnown.text.contains("External dependency"))
    }

    @Test
    fun persianDigitPreference_isAppliedToDictionaryOutput() {
        val latin = service.resolveOffline("42% complete", persianDigits = false).text
        val persian = service.resolveOffline("42% complete", persianDigits = true).text
        assertTrue("expected latin digits in: $latin", latin.contains("42"))
        assertTrue("expected persian digits in: $persian", persian.contains("۴۲"))
        assertFalse(persian.contains("42"))
    }

    @Test
    fun persianDigitPreference_neverCorruptsSentencePunctuation() {
        // Regression: a full stop in a translated sentence is punctuation, not a decimal
        // separator, and a comma is not a thousands separator.
        val text = service.resolveOffline(
            "Battery level at 17%. Recommend optimizing background services and " +
                "securing local database checkpoint.",
            persianDigits = true
        ).text
        assertTrue("expected Persian digits in: $text", text.contains("۱۷"))
        assertTrue("sentence period must survive: $text", text.contains("است."))
        assertFalse("comma must not become a thousands separator: $text", text.contains("٬"))
    }

    // ------------------------------------------------- non-prose is left alone

    @Test
    fun machineIdentifiers_areNeverSentToTheModelAndNeverReportedAsUntranslated() {
        val identifiers = listOf(
            "some_unknown_snake_case_flag",
            "unknown.dotted.action.code",
            "BTC/USDT",
            "EUR-USD",
            "SHA256:7e:92:4a:c1",
            "gemini-2.5-flash",
            "volume=0.10",
            "ICMarkets-Demo"
        )
        identifiers.forEach { token ->
            assertFalse("\"$token\" should not trigger a model call", service.needsTranslation(token))
            assertEquals(
                "\"$token\" must be ORIGINAL, not FAILED",
                TranslationSource.ORIGINAL,
                service.resolveOffline(token).source
            )
            assertEquals(token, service.resolveOffline(token).text)
        }
    }

    @Test
    fun shortAllCapsTokens_areTreatedAsCodesNotProse() {
        listOf("EURUSD", "XAUUSD", "BOS", "CHOCH", "FVG").forEach {
            assertFalse("\"$it\" should not trigger a model call", service.needsTranslation(it))
        }
        // But real prose still does.
        assertTrue(service.needsTranslation("Waiting for audit signoff"))
        assertTrue(service.needsTranslation("Conserve energy and save the local checkpoint"))
    }

    @Test
    fun alreadyPersianText_isPassedThroughUntouched() {
        val persian = "سطح باتری ۱۷٪ است"
        assertTrue(service.isAlreadyPersian(persian))
        assertFalse(service.needsTranslation(persian))
        val result = service.resolveOffline(persian)
        assertEquals(TranslationSource.ORIGINAL, result.source)
        assertEquals(persian, result.text)
    }

    @Test
    fun blankAndNumericText_neverTriggersTranslation() {
        assertFalse(service.needsTranslation(""))
        assertFalse(service.needsTranslation("   "))
        assertFalse(service.needsTranslation("09:00 - 11:30"))
        assertFalse(service.needsTranslation("1250"))
    }

    @Test
    fun genuinelyUnknownProse_isReportedSoTheCallerCanEscalateToTheModel() {
        val novel = "The quarterly liquidity review flagged an unusual spread widening"
        assertTrue(service.needsTranslation(novel))
        val result = service.resolveOffline(novel)
        assertEquals(TranslationSource.FAILED, result.source)
        assertEquals(novel, result.text)
    }

    // ------------------------------------------------------------------ cache

    @Test
    fun cacheStartsEmpty_andClearingItIsSafe() {
        assertEquals(0, service.cacheSize())
        assertNull(service.cached("anything"))
        service.clearCache()
        assertEquals(0, service.cacheSize())
    }
}
