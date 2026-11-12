package com.screentimeguard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.width
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
import com.screentimeguard.data.ParentPin
import com.screentimeguard.data.Prefs
import com.screentimeguard.service.GuardService

/**
 * 设置页：使用上限、题目数量、权限引导、家长密码、开始/停止守护。
 * 设置了家长密码后，修改任何设置或停止守护都要先输入密码；离开页面后自动重新上锁。
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var pin: ParentPin
    private var refresh by mutableIntStateOf(0)

    /** 本次打开页面是否已通过家长验证 */
    private var parentVerified by mutableStateOf(false)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        pin = ParentPin(this)
        setContent {
            MaterialTheme { Surface { SettingsScreen(refresh) } }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++ // 从系统设置返回后刷新权限状态
    }

    override fun onStop() {
        super.onStop()
        // 离开页面就重新上锁（去系统设置授权时也会上锁，回来再输一次即可）
        if (!isChangingConfigurations) parentVerified = false
    }

    private enum class Dialog { None, Verify, SetPin }

    @Composable
    private fun SettingsScreen(@Suppress("UNUSED_PARAMETER") tick: Int) {
        var limit by remember { mutableFloatStateOf(prefs.limitMinutes.toFloat()) }
        var count by remember { mutableFloatStateOf(prefs.questionCount.toFloat()) }
        var enabled by remember { mutableStateOf(prefs.guardEnabled) }
        var pinSet by remember { mutableStateOf(pin.isSet) }
        var dialog by remember { mutableStateOf(Dialog.None) }

        val canEdit = !pinSet || parentVerified

        val overlayOk = Settings.canDrawOverlays(this)
        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val notifOk = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        Column(
            Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("屏幕时间守卫", style = MaterialTheme.typography.headlineMedium)

            if (!canEdit) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒 设置已被家长锁定", Modifier.weight(1f))
                    Button(onClick = { dialog = Dialog.Verify }) { Text("家长解锁") }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("使用上限：${limit.toInt()} 分钟")
            Slider(
                value = limit, onValueChange = { limit = it }, valueRange = 1f..180f,
                enabled = canEdit,
                onValueChangeFinished = { prefs.limitMinutes = limit.toInt() },
            )
            Text("解锁需答对：${count.toInt()} 道题")
            Slider(
                value = count, onValueChange = { count = it }, valueRange = 1f..10f, steps = 8,
                enabled = canEdit,
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

            Spacer(Modifier.height(16.dp))
            Text("家长密码", style = MaterialTheme.typography.titleMedium)
            if (!pinSet) {
                Text("设置后，修改设置、停止守护都需要密码；孩子答不出题时也可以用密码直接解锁。")
                OutlinedButton(onClick = { dialog = Dialog.SetPin }) { Text("设置家长密码") }
            } else {
                Text("✅ 已设置")
                if (canEdit) {
                    Row {
                        OutlinedButton(onClick = { dialog = Dialog.SetPin }) { Text("修改密码") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { pin.clear(); pinSet = false }) { Text("删除密码") }
                    }
                }
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
                enabled = canEdit && (enabled || (overlayOk && notifOk)),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (enabled) "停止守护" else "开始守护") }
        }

        when (dialog) {
            Dialog.None -> Unit
            Dialog.Verify -> VerifyPinDialog(
                pin = pin,
                onSuccess = { parentVerified = true; dialog = Dialog.None },
                onDismiss = { dialog = Dialog.None },
            )
            Dialog.SetPin -> SetPinDialog(
                onSet = { pin.set(it); pinSet = true; parentVerified = true; dialog = Dialog.None },
                onDismiss = { dialog = Dialog.None },
            )
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
