package com.screentimeguard.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentimeguard.data.Prefs
import com.screentimeguard.service.GuardService

/** 设置页：使用上限、题目数量、权限引导、开始/停止守护。 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private var refresh by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContent {
            MaterialTheme { Surface { SettingsScreen(refresh) } }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++ // 从系统设置返回后刷新权限状态
    }

    @Composable
    private fun SettingsScreen(@Suppress("UNUSED_PARAMETER") tick: Int) {
        var limit by remember { mutableFloatStateOf(prefs.limitMinutes.toFloat()) }
        var count by remember { mutableFloatStateOf(prefs.questionCount.toFloat()) }
        var enabled by remember { mutableStateOf(prefs.guardEnabled) }

        val overlayOk = Settings.canDrawOverlays(this)
        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val notifOk = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

        Column(
            Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("屏幕时间守卫", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            Text("使用上限：${limit.toInt()} 分钟")
            Slider(
                value = limit, onValueChange = { limit = it }, valueRange = 5f..180f, steps = 34,
                onValueChangeFinished = { prefs.limitMinutes = limit.toInt() },
            )
            Text("解锁需答对：${count.toInt()} 道题")
            Slider(
                value = count, onValueChange = { count = it }, valueRange = 1f..10f, steps = 8,
                onValueChangeFinished = { prefs.questionCount = count.toInt() },
            )

            Spacer(Modifier.height(16.dp))
            Text("必要权限", style = MaterialTheme.typography.titleMedium)
            PermissionRow("显示在其他应用上层", overlayOk) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            PermissionRow("通知", notifOk) {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            PermissionRow("忽略电池优化（防止被杀）", batteryOk) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("自上次解锁已使用：${prefs.usedMillis / 60_000} 分钟")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    enabled = !enabled
                    prefs.guardEnabled = enabled
                    if (enabled) GuardService.start(this@MainActivity) else GuardService.stop(this@MainActivity)
                },
                enabled = enabled || (overlayOk && notifOk),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (enabled) "停止守护" else "开始守护") }
        }
    }

    @Composable
    private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text((if (granted) "✅ " else "⚠️ ") + label, Modifier.weight(1f))
            if (!granted) OutlinedButton(onClick = onGrant) { Text("去授权") }
        }
    }
}
