package com.example.sayvis

import com.example.sayvis.identity.DevicePairing
import com.example.sayvis.identity.OwnerCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SayvisIdentityUnitTest {

    @Test
    fun verifier_roundTrip_acceptsCorrectAndRejectsWrongPassword() {
        val v = OwnerCredentials.createVerifier("Sayvis2026!".toCharArray(), iterations = 2_000)
        assertTrue(v.startsWith("pbkdf2-sha256$2000$"))
        assertTrue(OwnerCredentials.verify("Sayvis2026!".toCharArray(), v))
        assertFalse(OwnerCredentials.verify("sayvis2026!".toCharArray(), v))
        assertFalse(OwnerCredentials.verify("x".toCharArray(), "garbage"))
    }

    @Test
    fun verifier_isSaltedSoTwoHashesDiffer() {
        val a = OwnerCredentials.createVerifier("password1".toCharArray(), iterations = 1_000)
        val b = OwnerCredentials.createVerifier("password1".toCharArray(), iterations = 1_000)
        assertNotEquals(a, b)
    }

    @Test
    fun emailAndPasswordPolicy() {
        assertTrue(OwnerCredentials.isPlausibleEmail("owner@sayvis.ai"))
        assertFalse(OwnerCredentials.isPlausibleEmail("owner@"))
        assertFalse(OwnerCredentials.isPlausibleEmail("no at sign"))
        assertNull(OwnerCredentials.passwordPolicyProblem("abcdefg1", isPersian = true))
        assertEquals("رمز عبور باید حداقل ۸ نویسه باشد.", OwnerCredentials.passwordPolicyProblem("ab1", isPersian = true))
        assertEquals(OwnerCredentials.accountIdFor("Owner@Sayvis.AI"), OwnerCredentials.accountIdFor(" owner@sayvis.ai "))
    }

    @Test
    fun pairing_happyPath_andTamperedProof() {
        val offer = DevicePairing.createOffer("acct_test", now = 1_000L)
        assertEquals(8, offer.code.length)
        val fp = "SHA256:55:ab:c3:19:9e:f2:01:77:4b"
        val proof = DevicePairing.expectedProof(offer, fp)
        assertTrue(DevicePairing.verifyResponse(offer, fp, proof, now = 2_000L))
        assertTrue(DevicePairing.verifyResponse(offer, "55abc3199ef201774b", proof.lowercase(), now = 2_000L))
        val tampered = proof.dropLast(1) + if (proof.last() == '0') '1' else '0'
        assertFalse(DevicePairing.verifyResponse(offer, fp, tampered, now = 2_000L))
        assertFalse(DevicePairing.verifyResponse(offer, "SHA256:00", proof, now = 2_000L))
    }

    @Test
    fun pairing_offerExpires() {
        val offer = DevicePairing.createOffer("acct_test", now = 0L)
        val proof = DevicePairing.expectedProof(offer, "aa:bb")
        assertFalse(DevicePairing.verifyResponse(offer, "aa:bb", proof, now = DevicePairing.OFFER_TTL_MS + 1))
    }

    @Test
    fun fingerprintNormalisation() {
        assertEquals("SHA256:aa:bb:cc", DevicePairing.prettyFingerprint(" sha256:AA-BB-CC ".replace("sha256:", "SHA256:")))
    }
}
