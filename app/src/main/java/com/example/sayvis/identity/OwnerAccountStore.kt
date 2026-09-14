package com.example.sayvis.identity

import android.content.Context
import android.content.SharedPreferences
import com.example.sayvis.settings.SecureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local view of the SAYVIS owner account on this device. */
data class OwnerAccount(
    val accountId: String,
    val email: String,
    val createdAt: Long,
    val lastSignInAt: Long,
    /** This phone's own device fingerprint, shown to companions during pairing. */
    val thisDeviceFingerprint: String
)

sealed class AccountResult {
    data class Success(val account: OwnerAccount) : AccountResult()
    data class Failure(val messageFa: String, val messageEn: String) : AccountResult() {
        fun message(isPersian: Boolean) = if (isPersian) messageFa else messageEn
    }
}

/**
 * Owner account (e-mail + password) kept entirely on-device.
 *
 * Only the PBKDF2 verifier is stored, inside the Keystore-backed [SecureVault].
 * A cloud/Gateway sign-in can later reuse the same verifier format; until that
 * Gateway exists this store is the single source of truth for "who owns this
 * phone" and gates device pairing.
 */
class OwnerAccountStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sayvis_owner_account", Context.MODE_PRIVATE)
    private val vault = SecureVault(context.applicationContext)

    private val _account = MutableStateFlow(load())
    val account: StateFlow<OwnerAccount?> = _account.asStateFlow()

    private val _signedIn = MutableStateFlow(prefs.getBoolean(K_SESSION, false) && _account.value != null)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _activeOffer = MutableStateFlow<DevicePairing.Offer?>(null)
    val activeOffer: StateFlow<DevicePairing.Offer?> = _activeOffer.asStateFlow()

    val hasAccount: Boolean get() = _account.value != null

    fun register(email: String, password: String, isPersian: Boolean): AccountResult {
        val e = email.trim().lowercase()
        if (!OwnerCredentials.isPlausibleEmail(e)) {
            return AccountResult.Failure("ایمیل معتبر نیست.", "E-mail address is not valid.")
        }
        OwnerCredentials.passwordPolicyProblem(password, isPersian)?.let {
            return AccountResult.Failure(it, it)
        }
        if (hasAccount) {
            return AccountResult.Failure(
                "روی این دستگاه قبلاً حساب مالک ساخته شده است.",
                "An owner account already exists on this device."
            )
        }
        val now = System.currentTimeMillis()
        val account = OwnerAccount(
            accountId = OwnerCredentials.accountIdFor(e),
            email = e,
            createdAt = now,
            lastSignInAt = now,
            thisDeviceFingerprint = ensureDeviceFingerprint()
        )
        vault.put(K_VERIFIER, OwnerCredentials.createVerifier(password.toCharArray()))
        persist(account, session = true)
        return AccountResult.Success(account)
    }

    fun signIn(email: String, password: String): AccountResult {
        val account = _account.value ?: return AccountResult.Failure(
            "هنوز حسابی روی این دستگاه ساخته نشده است.", "No account exists on this device yet."
        )
        val e = email.trim().lowercase()
        val verifier = vault.get(K_VERIFIER)
        val ok = e == account.email && verifier.isNotBlank() &&
            OwnerCredentials.verify(password.toCharArray(), verifier)
        if (!ok) {
            val fails = prefs.getInt(K_FAILS, 0) + 1
            prefs.edit().putInt(K_FAILS, fails).apply()
            return AccountResult.Failure(
                "ایمیل یا رمز عبور اشتباه است. (تلاش ناموفق: $fails)",
                "E-mail or password is incorrect. (failed attempts: $fails)"
            )
        }
        val updated = account.copy(lastSignInAt = System.currentTimeMillis())
        prefs.edit().putInt(K_FAILS, 0).apply()
        persist(updated, session = true)
        return AccountResult.Success(updated)
    }

    fun signOut() {
        prefs.edit().putBoolean(K_SESSION, false).apply()
        _signedIn.value = false
        _activeOffer.value = null
    }

    fun changePassword(current: String, new: String, isPersian: Boolean): AccountResult {
        val account = _account.value ?: return AccountResult.Failure("حسابی وجود ندارد.", "No account.")
        val verifier = vault.get(K_VERIFIER)
        if (!OwnerCredentials.verify(current.toCharArray(), verifier)) {
            return AccountResult.Failure("رمز فعلی اشتباه است.", "Current password is incorrect.")
        }
        OwnerCredentials.passwordPolicyProblem(new, isPersian)?.let { return AccountResult.Failure(it, it) }
        vault.put(K_VERIFIER, OwnerCredentials.createVerifier(new.toCharArray()))
        return AccountResult.Success(account)
    }

    /** Wipes the account, verifier and any pairing offer. Devices remain in Room until revoked. */
    fun deleteAccount() {
        vault.remove(K_VERIFIER)
        prefs.edit().clear().apply()
        _account.value = null
        _signedIn.value = false
        _activeOffer.value = null
    }

    // ------------------------------------------------------------- pairing
    fun startPairingOffer(): DevicePairing.Offer? {
        val account = _account.value ?: return null
        if (!_signedIn.value) return null
        return DevicePairing.createOffer(account.accountId).also { _activeOffer.value = it }
    }

    fun cancelPairingOffer() {
        _activeOffer.value = null
    }

    /** Returns true when the companion's proof matches the active offer; consumes the offer. */
    fun completePairing(deviceFingerprint: String, proof: String): Boolean {
        val offer = _activeOffer.value ?: return false
        val ok = DevicePairing.verifyResponse(offer, deviceFingerprint, proof)
        if (ok) _activeOffer.value = null
        return ok
    }

    fun failedAttempts(): Int = prefs.getInt(K_FAILS, 0)

    // ------------------------------------------------------------- internals
    private fun ensureDeviceFingerprint(): String {
        prefs.getString(K_FINGERPRINT, null)?.let { return it }
        val bytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val fp = DevicePairing.prettyFingerprint(bytes.joinToString("") { "%02x".format(it) })
        prefs.edit().putString(K_FINGERPRINT, fp).apply()
        return fp
    }

    private fun persist(account: OwnerAccount, session: Boolean) {
        prefs.edit()
            .putString(K_ID, account.accountId)
            .putString(K_EMAIL, account.email)
            .putLong(K_CREATED, account.createdAt)
            .putLong(K_LAST, account.lastSignInAt)
            .putString(K_FINGERPRINT, account.thisDeviceFingerprint)
            .putBoolean(K_SESSION, session)
            .apply()
        _account.value = account
        _signedIn.value = session
    }

    private fun load(): OwnerAccount? {
        val id = prefs.getString(K_ID, null) ?: return null
        val email = prefs.getString(K_EMAIL, null) ?: return null
        return OwnerAccount(
            accountId = id,
            email = email,
            createdAt = prefs.getLong(K_CREATED, 0L),
            lastSignInAt = prefs.getLong(K_LAST, 0L),
            thisDeviceFingerprint = prefs.getString(K_FINGERPRINT, null) ?: ensureDeviceFingerprint()
        )
    }

    companion object {
        private const val K_ID = "acct_id"
        private const val K_EMAIL = "acct_email"
        private const val K_CREATED = "acct_created"
        private const val K_LAST = "acct_last_sign_in"
        private const val K_FINGERPRINT = "acct_device_fp"
        private const val K_SESSION = "acct_session_open"
        private const val K_FAILS = "acct_failed_attempts"
        private const val K_VERIFIER = "sec_owner_password_verifier"

        @Volatile
        private var INSTANCE: OwnerAccountStore? = null

        fun get(context: Context): OwnerAccountStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: OwnerAccountStore(context).also { INSTANCE = it }
            }
    }
}
