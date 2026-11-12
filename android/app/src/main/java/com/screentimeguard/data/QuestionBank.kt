package com.screentimeguard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 题库里的一道选择题。格式与 shared/questions.json 一致。 */
data class BankQuestion(val q: String, val options: List<String>, val answer: Int)

/**
 * 题库 JSON 的解析 / 校验 / 序列化。格式：
 * ```
 * { "version": 1, "questions": [ { "q": "题目", "options": ["A","B"], "answer": 0 } ] }
 * ```
 * 也接受直接是数组 `[ {...}, {...} ]`。
 */
object BankJson {
    const val MAX_QUESTIONS = 2000

    /** 解析并校验，格式不对时抛出带中文说明的 IllegalArgumentException */
    fun parse(json: String): List<BankQuestion> {
        val trimmed = json.trim()
        val arr = try {
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).getJSONArray("questions")
        } catch (e: Exception) {
            throw IllegalArgumentException("不是有效的题库 JSON：${e.message}")
        }
        require(arr.length() <= MAX_QUESTIONS) { "题目太多（最多 $MAX_QUESTIONS 道）" }
        return (0 until arr.length()).map { i ->
            try {
                val o = arr.getJSONObject(i)
                val opts = o.getJSONArray("options")
                BankQuestion(
                    q = o.getString("q"),
                    options = (0 until opts.length()).map(opts::getString),
                    answer = o.getInt("answer"),
                ).also { validate(it)?.let { msg -> throw IllegalArgumentException(msg) } }
            } catch (e: Exception) {
                throw IllegalArgumentException("第 ${i + 1} 题格式错误：${e.message}")
            }
        }
    }

    /** 返回错误说明；合法时返回 null */
    fun validate(q: BankQuestion): String? = when {
        q.q.isBlank() -> "题目不能为空"
        q.options.size !in 2..6 -> "选项数量必须是 2–6 个"
        q.options.any { it.isBlank() } -> "选项不能为空"
        q.options.toSet().size != q.options.size -> "选项不能重复"
        q.answer !in q.options.indices -> "answer 必须是正确选项的序号（从 0 开始）"
        else -> null
    }

    fun serialize(list: List<BankQuestion>): String {
        val arr = JSONArray()
        list.forEach { q ->
            arr.put(JSONObject().put("q", q.q).put("options", JSONArray(q.options)).put("answer", q.answer))
        }
        return JSONObject().put("version", 1).put("questions", arr).toString(2)
    }
}

/** 仓库 banks/ 目录里的官方题库（公开 GitHub 仓库的 raw 网址） */
object OfficialBanks {
    private const val BASE = "https://raw.githubusercontent.com/lisheng51/lislock/main/banks/"
    val list = listOf(
        "Groep 7/8 – Alles" to BASE + "all.json",
        "Taal" to BASE + "nl-taal.json",
        "Rekenen" to BASE + "nl-rekenen.json",
        "Wereldoriëntatie" to BASE + "nl-wereld.json",
        "Engels" to BASE + "nl-engels.json",
    )
}

/**
 * 题库来源：内置（assets/questions.json）+ 自定义（家长在 App 里添加）+ 远程（从网址下载的缓存）。
 */
class QuestionBank(context: Context) {
    private val ctx = context.applicationContext
    private val sp = ctx.getSharedPreferences("bank", Context.MODE_PRIVATE)
    private val customFile = File(ctx.filesDir, "custom_questions.json")
    private val remoteFile = File(ctx.filesDir, "remote_questions.json")

    /** 是否使用内置题库 */
    var builtinEnabled: Boolean
        get() = sp.getBoolean("builtin_enabled", true)
        set(v) = sp.edit().putBoolean("builtin_enabled", v).apply()

    /** 只出题库题，不出算术题 */
    var onlyBank: Boolean
        get() = sp.getBoolean("only_bank", false)
        set(v) = sp.edit().putBoolean("only_bank", v).apply()

    /** 远程题库网址（空 = 不使用） */
    var remoteUrl: String
        get() = sp.getString("remote_url", "") ?: ""
        set(v) = sp.edit().putString("remote_url", v.trim()).apply()

    /** 上次成功更新远程题库的时间（毫秒） */
    var remoteUpdatedAt: Long
        get() = sp.getLong("remote_updated_at", 0L)
        set(v) = sp.edit().putLong("remote_updated_at", v).apply()

    /** 上次尝试更新的时间（用于失败后的重试间隔） */
    var remoteAttemptAt: Long
        get() = sp.getLong("remote_attempt_at", 0L)
        set(v) = sp.edit().putLong("remote_attempt_at", v).apply()

    /** 上次更新失败的原因（成功时为 null） */
    var remoteError: String?
        get() = sp.getString("remote_error", null)
        set(v) = sp.edit().putString("remote_error", v).apply()

    fun builtin(): List<BankQuestion> = runCatching {
        BankJson.parse(ctx.assets.open("questions.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())

    fun custom(): List<BankQuestion> = readFile(customFile)

    fun saveCustom(list: List<BankQuestion>) = customFile.writeText(BankJson.serialize(list))

    fun remote(): List<BankQuestion> = readFile(remoteFile)

    /** 保存已校验过的远程题库 */
    fun saveRemote(list: List<BankQuestion>) {
        remoteFile.writeText(BankJson.serialize(list))
        remoteUpdatedAt = System.currentTimeMillis()
        remoteError = null
    }

    fun clearRemote() {
        remoteFile.delete()
        remoteUrl = ""
        remoteUpdatedAt = 0L
        remoteError = null
    }

    /** 出题时使用的全部题目 */
    fun all(): List<BankQuestion> = (if (builtinEnabled) builtin() else emptyList()) + custom() + remote()

    private fun readFile(f: File): List<BankQuestion> =
        if (!f.exists()) emptyList() else runCatching { BankJson.parse(f.readText()) }.getOrDefault(emptyList())
}
