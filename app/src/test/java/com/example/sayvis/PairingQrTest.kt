package com.example.sayvis

import com.example.sayvis.identity.PairingQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** QR pairing codec + scanner-import classifier (pure, v4.0.0). */
class PairingQrTest {

    @Test
    fun `pairing payload round trip`() {
        val payload = PairingQr.buildPayload(
            account = "owner@gmail.com",
            device = "Pixel 8 Pro",
            pin = "123456",
            createdAt = 1_700_000_000_000
        )
        assertTrue(payload.startsWith("sayvis://link?"))
        val parsed = PairingQr.parsePairing(payload)
        assertNotNull(parsed)
        assertEquals("owner@gmail.com", parsed!!.account)
        assertEquals("Pixel 8 Pro", parsed.device)
        assertEquals("123456", parsed.pin)
        assertEquals(1_700_000_000_000L, parsed.createdAt)
    }

    @Test
    fun `pairing payload survives spaces and unicode device names`() {
        val payload = PairingQr.buildPayload("a.b+x@gmail.com", "گوشی سامسونگ S24", "654321", 42)
        val parsed = PairingQr.parsePairing(payload)
        assertNotNull(parsed)
        assertEquals("a.b+x@gmail.com", parsed!!.account)
        assertEquals("گوشی سامسونگ S24", parsed.device)
    }

    @Test
    fun `invalid pairings are rejected`() {
        assertNull(PairingQr.parsePairing("not-a-sayvis-link"))
        assertNull(PairingQr.parsePairing("sayvis://link?v=2&acct=a@b.c&pin=123456"))
        assertNull(PairingQr.parsePairing("sayvis://link?v=1&acct=no-at-sign&pin=123456"))
        assertNull(PairingQr.parsePairing("sayvis://link?v=1&acct=a@b.c&pin=12345")) // 5 digits
        assertNull(PairingQr.parsePairing("sayvis://link?v=1&acct=a@b.c&pin=abc123"))
    }

    @Test
    fun `classify routes each payload kind`() {
        val pairing = PairingQr.buildPayload("x@y.com", "dev", "111111", 1)
        assertTrue(PairingQr.classify(pairing) is PairingQr.Import.PairingLink)

        val gemini = "AIza" + "aB3xYz_".repeat(5) // 4 + 35 = 39 chars
        assertEquals(39, gemini.length)
        val keyImport = PairingQr.classify(gemini)
        assertTrue(keyImport is PairingQr.Import.ProviderKey)
        assertEquals("gemini", (keyImport as PairingQr.Import.ProviderKey).provider)

        assertEquals("openrouter", PairingQr.providerForKey("sk-or-v1-abcdef1234567890"))
        assertEquals("groq", PairingQr.providerForKey("gsk_AbcdEf123456"))
        assertEquals("xai", PairingQr.providerForKey("xai-AbcdEf123456"))
        assertEquals("openai", PairingQr.providerForKey("sk-AbcdEf123456"))
        assertEquals("gemini", PairingQr.providerForKey("AQ.Ab8LmNoPqrStUv"))
        assertNull(PairingQr.providerForKey("random text"))
    }

    @Test
    fun `config json import keeps provider and key`() {
        val json = "{\"provider\":\"gemini\",\"apiKey\":\"AIza" + "k".repeat(35) + "\"}"
        val config = PairingQr.classify(json)
        assertTrue(config is PairingQr.Import.Config)
        config as PairingQr.Import.Config
        assertEquals("gemini", config.provider)
        assertTrue(config.apiKey.startsWith("AIza"))
        assertNull(config.baseUrl)
        assertNull(config.model)
    }

    @Test
    fun `links and plain text fall through`() {
        val link = PairingQr.classify("https://aistudio.google.com/app/apikey")
        assertTrue(link is PairingQr.Import.Link)
        val plain = PairingQr.classify("hello sayvis")
        assertTrue(plain is PairingQr.Import.Plain)
    }

    @Test
    fun `hub e-mail validation and display name`() {
        val hub = com.example.sayvis.identity.GoogleAccountHub
        assertTrue(hub.isValidEmail("Owner@Gmail.com"))
        assertEquals("owner@gmail.com", hub.normalizeEmail(" Owner@Gmail.com "))
        assertFalse(hub.isValidEmail("no-at-sign"))
        assertFalse(hub.isValidEmail("a@b"))
        assertFalse(hub.isValidEmail("a@.b."))
        assertFalse(hub.isValidEmail("a@b..c"))
        assertFalse(hub.isValidEmail("@gmail.com"))
        assertEquals("Ali Reza", hub.displayNameFor("ali.reza@gmail.com"))
        assertEquals("Sara", hub.displayNameFor("sara@yahoo.com"))
    }

    @Test
    fun `network backoff grows and caps`() {
        val net = com.example.sayvis.net.SayvisNet
        assertEquals(600L, net.backoffDelayMs(0))
        assertEquals(1200L, net.backoffDelayMs(1))
        assertEquals(2400L, net.backoffDelayMs(2))
        assertEquals(4800L, net.backoffDelayMs(3))
        assertEquals(8000L, net.backoffDelayMs(4))
        assertEquals(8000L, net.backoffDelayMs(20))
    }

    @Test
    fun `escaped config values decode`() {
        val json = "{\"provider\":\"openai\",\"apiKey\":\"sk\\-test\\\"quoted\",\"model\":\"gpt-4o\"}"
        val config = PairingQr.classify(json)
        assertTrue(config is PairingQr.Import.Config)
        config as PairingQr.Import.Config
        assertEquals("sk-test\"quoted", config.apiKey)
        assertEquals("gpt-4o", config.model)
    }
}
