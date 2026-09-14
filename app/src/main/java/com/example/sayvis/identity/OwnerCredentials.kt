package com.example.sayvis.identity

import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Pure-Kotlin owner credential primitives (email + password).
 *
 * Design rules (Master Dossier §7 / §8):
 *  - The password is never stored; only a salted PBKDF2-HMAC-SHA256 verifier.
 *  - Verification is constant-time.
 *  - The same verifier format is intended to be accepted by the future SAYVIS
 *    Gateway so that a PC / web client can authenticate the same account.
 *
 * No Android dependency, so it is covered by plain JVM unit tests.
 */
object OwnerCredentials {

    const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val FORMAT = "pbkdf2-sha256"

    private val random = SecureRandom()

    /** Returns `pbkdf2-sha256$<iterations>$<saltHex>$<hashHex>`. */
    fun createVerifier(password: CharArray, iterations: Int = PBKDF2_ITERATIONS): String {
        require(password.isNotEmpty()) { "empty password" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(password, salt, iterations)
        return listOf(FORMAT, iterations.toString(), salt.toHex(), hash.toHex()).joinToString("$")
    }

    /** Constant-time check of [password] against a verifier from [createVerifier]. */
    fun verify(password: CharArray, verifier: String): Boolean {
        val parts = verifier.split("$")
        if (parts.size != 4 || parts[0] != FORMAT) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = parts[2].hexToBytes() ?: return false
        val expected = parts[3].hexToBytes() ?: return false
        val actual = derive(password, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    /** Minimal, dependency-free e-mail sanity check (RFC-lite). */
    fun isPlausibleEmail(email: String): Boolean {
        val e = email.trim()
        if (e.length < 6 || e.length > 254) return false
        val at = e.indexOf('@')
        if (at <= 0 || at != e.lastIndexOf('@')) return false
        val domain = e.substring(at + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.') && !e.any { it.isWhitespace() }
    }

    /** Strength policy: >= 8 chars, at least one letter and one digit. */
    fun passwordPolicyProblem(password: String, isPersian: Boolean): String? = when {
        password.length < 8 -> if (isPersian) "رمز عبور باید حداقل ۸ نویسه باشد." else "Password must be at least 8 characters."
        !password.any { it.isLetter() } -> if (isPersian) "رمز عبور باید حداقل یک حرف داشته باشد." else "Password needs at least one letter."
        !password.any { it.isDigit() } -> if (isPersian) "رمز عبور باید حداقل یک رقم داشته باشد." else "Password needs at least one digit."
        else -> null
    }

    /** Stable, case-insensitive account identifier derived from the e-mail. */
    fun accountIdFor(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray(Charsets.UTF_8))
        return "acct_" + digest.toHex().take(16)
    }

    /** HMAC-SHA256 helper shared with the pairing protocol. */
    fun hmac(key: ByteArray, message: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    internal fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0 || isEmpty()) return null
        return runCatching { chunked(2).map { it.toInt(16).toByte() }.toByteArray() }.getOrNull()
    }
}
