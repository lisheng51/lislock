package com.screentimeguard.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import com.screentimeguard.R
import com.screentimeguard.data.Prefs
import com.screentimeguard.data.RemoteBankSync
import com.screentimeguard.ui.LockActivity
import com.screentimeguard.ui.MainActivity

/**
 * 常驻前台服务：
 *  - 每 TICK_MS 检查一次：屏幕亮着且已解锁 → 把这段时间累加到 usedMillis
 *  - usedMillis ≥ 上限 → 进入锁定状态，显示全屏锁定悬浮层（LockOverlay）
 *  - 悬浮层盖在所有 App 之上，Home 键 / 最近任务都关不掉
 *  - 没有悬浮窗权限时，退化为全屏通知，点通知打开 LockActivity
 */
class GuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var power: PowerManager
    private lateinit var keyguard: KeyguardManager
    private var lastTick = 0L
    private lateinit var overlay: LockOverlay

    private val tick = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        power = getSystemService(PowerManager::class.java)
        keyguard = getSystemService(KeyguardManager::class.java)
        overlay = LockOverlay(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须先 startForeground：用 startForegroundService 启动后不调用会被系统判定崩溃
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        if (!prefs.guardEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        if (intent?.action == ACTION_LOCK_NOW) {
            // 设置页的"立即测试锁定"：直接锁定，不计入当天超时次数（不影响难度）
            prefs.locked = true
            showLockScreen()
        }
        handler.removeCallbacks(tick)
        lastTick = SystemClock.elapsedRealtime()
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        overlay.hide()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onTick() {
        val now = SystemClock.elapsedRealtime()
        // 上限保护：设备休眠导致 tick 间隔变长时，不把休眠时间算进去
        val delta = (now - lastTick).coerceAtMost(TICK_MS * 3)
        lastTick = now

        // 远程题库：到时间才会真正下载（成功 6 小时一次，失败 30 分钟重试）
        RemoteBankSync.refreshIfStale(this)

        val inUse = power.isInteractive && !keyguard.isKeyguardLocked
        if (!inUse) return

        if (prefs.locked) {
            showLockScreen()
            return
        }

        prefs.usedMillis += delta
        if (prefs.usedMillis >= prefs.limitMillis) {
            prefs.lock()
            showLockScreen()
        }
        updateNotification()
    }

    private fun showLockScreen() {
        // 首选：全屏悬浮层（不受 Android 14/15 后台启动 Activity 的限制）
        val shown = overlay.show()
        // 兜底：没有悬浮窗权限时，用全屏通知提醒，点通知打开 LockActivity
        lastLockError = if (shown) null else if (!Settings.canDrawOverlays(this)) "没有悬浮窗权限" else "系统拒绝显示悬浮层"
        updateNotification()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val locked = prefs.locked
        val target = if (locked) LockActivity::class.java else MainActivity::class.java
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (locked) getString(R.string.notif_locked)
        else getString(R.string.notif_running, (prefs.usedMillis / 60_000).toInt(), prefs.limitMinutes)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply { if (locked) setFullScreenIntent(pi, true) }
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.channel_guard), NotificationManager.IMPORTANCE_HIGH
        ).apply { setSound(null, null) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        /** 服务是否在运行（设置页显示状态用） */
        @Volatile var isRunning = false
            private set

        /** 上次锁定失败的原因（设置页显示，便于排查） */
        @Volatile var lastLockError: String? = null
            private set

        private const val ACTION_LOCK_NOW = "com.screentimeguard.LOCK_NOW"
        private const val TICK_MS = 5_000L
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "guard"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GuardService::class.java))
        }

        /** 立即锁定（测试用） */
        fun lockNow(context: Context) {
            context.startForegroundService(
                Intent(context, GuardService::class.java).setAction(ACTION_LOCK_NOW)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
