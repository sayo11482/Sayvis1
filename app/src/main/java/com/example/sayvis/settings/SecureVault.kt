package com.example.sayvis.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android-Keystore backed secret vault.
 *
 * API keys and broker passwords are never written to disk in clear text: they are
 * encrypted with an AES-256/GCM key that is generated inside, and can only be used
 * inside, the hardware-backed Android Keystore. If the Keystore is unavailable
 * (emulator without secure hardware, corrupted key) the vault degrades to an
 * obfuscated storage mode and reports [isHardwareBacked] as false so the UI can
 * warn the owner honestly instead of pretending to be secure.
 */
class SecureVault(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sayvis_secure_vault", Context.MODE_PRIVATE)

    var isHardwareBacked: Boolean = true
        private set

    private val keyStore: KeyStore? = runCatching {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey? {
        val ks = keyStore ?: return null
        return runCatching {
            val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                generator.generateKey()
            }
        }.getOrNull()
    }

    /** Encrypts [plain]; returns a self-describing Base64 payload. */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val key = getOrCreateKey()
        if (key == null) {
            isHardwareBacked = false
            return FALLBACK_PREFIX + Base64.encodeToString(softObfuscate(plain), Base64.NO_WRAP)
        }
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            isHardwareBacked = true
            KEYSTORE_PREFIX + Base64.encodeToString(iv + payload, Base64.NO_WRAP)
        }.getOrElse {
            isHardwareBacked = false
            FALLBACK_PREFIX + Base64.encodeToString(softObfuscate(plain), Base64.NO_WRAP)
        }
    }

    /** Decrypts a payload produced by [encrypt]. Tolerates legacy clear values. */
    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        return when {
            stored.startsWith(KEYSTORE_PREFIX) -> runCatching {
                val raw = Base64.decode(stored.removePrefix(KEYSTORE_PREFIX), Base64.NO_WRAP)
                val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
                val payload = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
                val key = getOrCreateKey() ?: return@runCatching stored
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(payload), Charsets.UTF_8)
            }.getOrElse { "" }

            stored.startsWith(FALLBACK_PREFIX) -> runCatching {
                String(
                    softObfuscate(Base64.decode(stored.removePrefix(FALLBACK_PREFIX), Base64.NO_WRAP)),
                    Charsets.UTF_8
                )
            }.getOrElse { "" }

            else -> stored // legacy / clear value written by an older build
        }
    }

    fun put(key: String, secret: String) {
        prefs.edit().putString(key, encrypt(secret)).apply()
    }

    fun get(key: String): String = decrypt(prefs.getString(key, "") ?: "")

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun keys(): Set<String> = prefs.all.keys.toSet()

    /**
     * Reversible byte-level mask used only when the Keystore is unavailable.
     * This is obfuscation, not encryption, and the UI says so.
     */
    private fun softObfuscate(input: ByteArray): ByteArray =
        input.mapIndexed { index, byte -> (byte.toInt() xor SOFT_MASK[index % SOFT_MASK.size]).toByte() }
            .toByteArray()

    private fun softObfuscate(input: String): ByteArray = softObfuscate(input.toByteArray(Charsets.UTF_8))

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sayvis_owner_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val KEYSTORE_PREFIX = "ks1:"
        private const val FALLBACK_PREFIX = "fb1:"
        private val SOFT_MASK = byteArrayOf(
            0x5A, 0x3C, 0x71, 0x0F, 0x66, 0x2B, 0x4D, 0x19,
            0x77, 0x08, 0x53, 0x2E, 0x61, 0x3A, 0x0C, 0x74
        )

        /** Renders a secret as ••••abcd for safe on-screen display. */
        fun mask(secret: String, visibleTail: Int = 4): String {
            if (secret.isBlank()) return ""
            val tail = secret.takeLast(visibleTail)
            return "•".repeat(8) + tail
        }
    }
}
