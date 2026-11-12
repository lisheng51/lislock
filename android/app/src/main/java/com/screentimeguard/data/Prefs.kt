package com.screentimeguard.data

import android.content.Context

/** 所有持久化状态。单位：毫秒 / 分钟。 */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("guard", Context.MODE_PRIVATE)

    /** 使用上限（分钟），默认 60 */
    var limitMinutes: Int
        get() = sp.getInt(KEY_LIMIT, 60)
        set(v) = sp.edit().putInt(KEY_LIMIT, v.coerceIn(1, 24 * 60)).apply()

    /** 解锁需答对的题数，默认 3 */
    var questionCount: Int
        get() = sp.getInt(KEY_QCOUNT, 3)
        set(v) = sp.edit().putInt(KEY_QCOUNT, v.coerceIn(1, 10)).apply()

    /** 自上次解锁以来累计的使用时长 */
    var usedMillis: Long
        get() = sp.getLong(KEY_USED, 0L)
        set(v) = sp.edit().putLong(KEY_USED, v).apply()

    /** 是否处于锁定状态（需答题） */
    var locked: Boolean
        get() = sp.getBoolean(KEY_LOCKED, false)
        set(v) = sp.edit().putBoolean(KEY_LOCKED, v).apply()

    /** 用户是否开启了守护 */
    var guardEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(v) = sp.edit().putBoolean(KEY_ENABLED, v).apply()

    val limitMillis: Long get() = limitMinutes * 60_000L

    /** 答题通过：清零计时并解除锁定 */
    fun unlock() {
        sp.edit().putLong(KEY_USED, 0L).putBoolean(KEY_LOCKED, false).apply()
    }

    private companion object {
        const val KEY_LIMIT = "limit_minutes"
        const val KEY_QCOUNT = "question_count"
        const val KEY_USED = "used_millis"
        const val KEY_LOCKED = "locked"
        const val KEY_ENABLED = "guard_enabled"
    }
}
