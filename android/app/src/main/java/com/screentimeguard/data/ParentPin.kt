package com.screentimeguard.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 家长密码（4–6 位数字）。
 *  - 只保存加盐的 PBKDF2 哈希，不保存明文
 *  - 连续输错 [MAX_ATTEMPTS] 次后锁定 [LOCKOUT_MS]，防止孩子暴力试密码
 */
class ParentPin(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("parent_pin", Context.MODE_PRIVATE)

    val isSet: Boolean get() = sp.contains(KEY_HASH)

    /** 剩余锁定时间（毫秒），0 表示可以输入 */
    val lockoutRemainingMs: Long
        get() = (sp.getLong(KEY_LOCKOUT_UNTIL, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun set(pin: String) {
        require(isValidFormat(pin)) { "PIN must be 4-6 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit()
            .putString(KEY_SALT, salt.b64())
            .putString(KEY_HASH, hash(pin, salt).b64())
            .putInt(KEY_FAILS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    sealed interface Result {
        data object Ok : Result
        data class Wrong(val attemptsLeft: Int) : Result
        data class LockedOut(val remainingMs: Long) : Result
    }

    fun verify(pin: String): Result {
        if (!isSet) return Result.Ok
        lockoutRemainingMs.takeIf { it > 0 }?.let { return Result.LockedOut(it) }

        val salt = Base64.decode(sp.getString(KEY_SALT, ""), Base64.NO_WRAP)
        val expected = Base64.decode(sp.getString(KEY_HASH, ""), Base64.NO_WRAP)
        if (MessageDigest.isEqual(hash(pin, salt), expected)) {
            sp.edit().putInt(KEY_FAILS, 0).apply()
            return Result.Ok
        }

        val fails = sp.getInt(KEY_FAILS, 0) + 1
        return if (fails >= MAX_ATTEMPTS) {
            sp.edit().putInt(KEY_FAILS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MS).apply()
            Result.LockedOut(LOCKOUT_MS)
        } else {
            sp.edit().putInt(KEY_FAILS, fails).apply()
            Result.Wrong(MAX_ATTEMPTS - fails)
        }
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 20_000, 256))
            .encoded

    private fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 60_000L
        fun isValidFormat(pin: String) = pin.length in 4..6 && pin.all(Char::isDigit)
    }
}
