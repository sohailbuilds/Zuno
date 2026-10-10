package com.zuno.assistant.driving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Intentionally empty. Receiving BOOT_COMPLETED starts the app process, and ZunoApp.onCreate
 * re-registers driving detection if the user had enabled it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
