package com.example.sayvis.identity

import com.example.sayvis.identity.OwnerCredentials.hexToBytes
import com.example.sayvis.identity.OwnerCredentials.toHex
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Offline-verifiable pairing handshake between this phone (the account holder)
 * and a companion device (Windows PC, laptop, web console).
 *
 * Flow (Dossier §8 / §23):
 *  1. Phone: owner is signed in -> [createOffer] shows a short pairing code.
 *  2. Companion: user signs in with the SAME e-mail + password, enters the code.
 *     The companion derives `proof = HMAC(secret, code | fingerprint)` where the
 *     secret is transmitted out-of-band inside the code payload (QR / typed).
 *  3. Phone: owner types the companion's fingerprint + proof -> [verifyResponse].
 *     On success the device is stored as PENDING-trust; the owner then decides
 *     whether to mark it TRUSTED. Nothing is executed remotely by pairing itself.
 *
 * The full network Gateway is still PLANNED; this object is the cryptographic
 * contract both sides must implement, and it is unit-tested on the JVM.
 */
object DevicePairing {

    const val OFFER_TTL_MS = 5 * 60 * 1000L
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I
    private val random = SecureRandom()

    data class Offer(
        val accountId: String,
        val code: String,
        val secretHex: String,
        val createdAt: Long,
        val expiresAt: Long
    ) {
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > expiresAt

        /** Human-typeable payload the companion needs: CODE-SECRET(first 16 hex). */
        fun sharedPayload(): String = "$code-${secretHex.take(16).uppercase()}"
    }

    fun createOffer(accountId: String, now: Long = System.currentTimeMillis()): Offer {
        val code = (1..8).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
        val secret = ByteArray(32).also { random.nextBytes(it) }
        return Offer(accountId, code, secret.toHex(), now, now + OFFER_TTL_MS)
    }

    /** What the companion must compute (kept here so both sides share one source). */
    fun expectedProof(offer: Offer, deviceFingerprint: String): String {
        val key = offer.secretHex.take(16).lowercase().hexToBytes() ?: ByteArray(0)
        val message = "${offer.code}|${normalizeFingerprint(deviceFingerprint)}|${offer.accountId}"
        return OwnerCredentials.hmac(key, message).toHex().take(16).uppercase()
    }

    fun verifyResponse(
        offer: Offer,
        deviceFingerprint: String,
        proof: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (offer.isExpired(now)) return false
        if (deviceFingerprint.isBlank() || proof.isBlank()) return false
        val expected = expectedProof(offer, deviceFingerprint).toByteArray()
        val given = proof.trim().uppercase().toByteArray()
        return MessageDigest.isEqual(expected, given)
    }

    fun normalizeFingerprint(raw: String): String =
        raw.trim().removePrefix("SHA256:").lowercase().replace(Regex("[^0-9a-f]"), "")

    fun prettyFingerprint(raw: String): String {
        val n = normalizeFingerprint(raw)
        return "SHA256:" + n.chunked(2).joinToString(":")
    }
}
