package com.screentimeguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screentimeguard.data.Prefs

/** 开机或 App 更新后，如果用户之前开启了守护，就自动恢复。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                if (Prefs(context).guardEnabled) GuardService.start(context)
        }
    }
}
