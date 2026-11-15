package com.screentimeguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * 兜底的锁定页面：主要锁定方式是 GuardService 里的全屏悬浮层（LockOverlay）。
 * 没有悬浮窗权限时，用户点通知会打开这个页面答题。返回键无效。
 */
class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* 锁定期间禁止返回 */ }
        })
        setContent {
            MaterialTheme { Surface { LockContent(onFinished = { finish() }) } }
        }
    }
}
