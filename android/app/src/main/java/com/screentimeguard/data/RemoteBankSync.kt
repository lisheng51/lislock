package com.screentimeguard.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 从网址下载远程题库。
 *  - 支持 https 和 http（http 不加密，界面上会提醒家长）
 *  - 下载后先完整校验，格式不对就保留旧的缓存，不会把题库弄坏
 *  - [refreshIfStale] 由 GuardService 定期调用：成功后 6 小时再更新，失败后 30 分钟重试
 */
object RemoteBankSync {
    private const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L
    private const val RETRY_INTERVAL_MS = 30 * 60 * 1000L
    private const val MAX_BYTES = 1_000_000

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    /** 后台定期调用：到时间了才真正下载 */
    fun refreshIfStale(context: Context) {
        val bank = QuestionBank(context)
        if (bank.remoteUrl.isBlank()) return
        val now = System.currentTimeMillis()
        if (now - bank.remoteUpdatedAt < REFRESH_INTERVAL_MS) return
        if (now - bank.remoteAttemptAt < RETRY_INTERVAL_MS) return
        refresh(context)
    }

    /** 立即下载。[onDone] 在主线程回调：成功时为题目数量，失败时为异常 */
    fun refresh(context: Context, onDone: ((Result<Int>) -> Unit)? = null) {
        val bank = QuestionBank(context)
        val url = bank.remoteUrl
        if (!running.compareAndSet(false, true)) {
            onDone?.invoke(Result.failure(IllegalStateException("正在更新中，请稍候")))
            return
        }
        bank.remoteAttemptAt = System.currentTimeMillis()
        executor.execute {
            val result = runCatching {
                require(isSupportedUrl(url)) { "网址必须以 https:// 或 http:// 开头" }
                val list = BankJson.parse(download(url))
                require(list.isNotEmpty()) { "题库是空的" }
                bank.saveRemote(list)
                list.size
            }
            result.exceptionOrNull()?.let { bank.remoteError = it.message ?: it.javaClass.simpleName }
            running.set(false)
            onDone?.let { cb -> main.post { cb(result) } }
        }
    }

    fun isSupportedUrl(url: String) =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

    fun isInsecure(url: String) = url.trim().startsWith("http://", ignoreCase = true)

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            require(code == 200) { "服务器返回 HTTP $code" }
            val bytes = conn.inputStream.use { input ->
                val buf = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buf.write(chunk, 0, n)
                    require(buf.size() <= MAX_BYTES) { "文件太大（超过 1 MB）" }
                }
                buf.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }
}
