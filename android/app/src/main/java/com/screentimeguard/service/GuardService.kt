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
import com.screentimeguard.ui.LockActivity
import com.screentimeguard.ui.MainActivity

/**
 * 常驻前台服务：
 *  - 每 TICK_MS 检查一次：屏幕亮着且已解锁 → 把这段时间累加到 usedMillis
 *  - usedMillis ≥ 上限 → 进入锁定状态，拉起 LockActivity
 *  - 锁定期间如果用户切走（按 Home 等），下一个 tick 会再次拉起 LockActivity
 */
class GuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var power: PowerManager
    private lateinit var keyguard: KeyguardManager
    private var lastTick = 0L

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
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.guardEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        handler.removeCallbacks(tick)
        lastTick = SystemClock.elapsedRealtime()
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onTick() {
        val now = SystemClock.elapsedRealtime()
        // 上限保护：设备休眠导致 tick 间隔变长时，不把休眠时间算进去
        val delta = (now - lastTick).coerceAtMost(TICK_MS * 3)
        lastTick = now

        val inUse = power.isInteractive && !keyguard.isKeyguardLocked
        if (!inUse) return

        if (prefs.locked) {
            showLockScreen()
            return
        }

        prefs.usedMillis += delta
        if (prefs.usedMillis >= prefs.limitMillis) {
            prefs.locked = true
            showLockScreen()
        }
        updateNotification()
    }

    private fun showLockScreen() {
        if (LockActivity.isVisible) return
        // Android 10+ 后台启动 Activity 需要"悬浮窗"权限作为豁免
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
            // 没有悬浮窗权限时退化为全屏通知
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
            return
        }
        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
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
        private const val TICK_MS = 5_000L
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "guard"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
