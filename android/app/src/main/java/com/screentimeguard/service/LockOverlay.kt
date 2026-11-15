package com.screentimeguard.service

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screentimeguard.ui.LockContent

/**
 * 全屏锁定悬浮层（TYPE_APPLICATION_OVERLAY）。
 *
 * 为什么不用启动 Activity：Android 14/15 起，后台服务启动 Activity 经常被系统静默拦截，
 * 即使有"悬浮窗"权限也不保证成功。直接在 WindowManager 上加一个全屏窗口则不受这个限制，
 * 而且 Home 键、最近任务都无法把它关掉。
 */
class LockOverlay(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var owner: OverlayOwner? = null

    val isShowing: Boolean get() = root != null

    /** 显示悬浮层。成功返回 true；没权限或系统拒绝时返回 false（调用方再走通知兜底）。 */
    fun show(): Boolean {
        if (root != null) return true
        if (!Settings.canDrawOverlays(context)) return false

        val owner = OverlayOwner().also { it.start() }
        val compose = ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    Surface { LockContent(onFinished = { hide() }) }
                }
            }
        }
        // 屏蔽返回键
        val container = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                if (event.keyCode == KeyEvent.KEYCODE_BACK) true else super.dispatchKeyEvent(event)
        }
        container.setViewTreeLifecycleOwner(owner)
        container.setViewTreeSavedStateRegistryOwner(owner)
        container.addView(compose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            // 输入家长密码时键盘不挡住输入框
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        return try {
            wm.addView(container, params)
            root = container
            this.owner = owner
            true
        } catch (e: Exception) {
            Log.w(TAG, "addView failed", e)
            owner.stop()
            false
        }
    }

    fun hide() {
        val view = root ?: return
        root = null
        runCatching { wm.removeView(view) }
        owner?.stop()
        owner = null
    }

    /** ComposeView 需要 Lifecycle 和 SavedState，Service 里没有，自己提供一个最小实现。 */
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        fun start() {
            controller.performAttach()
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private companion object {
        const val TAG = "LockOverlay"
    }
}
