package com.zuno.assistant.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.zuno.assistant.ZunoGraph
import com.zuno.assistant.queue.WhatsAppNotificationRules

/**
 * Receives notifications once the user enables "Notification access" for Zuno in system settings.
 * Only WhatsApp packages are looked at; everything else is ignored immediately.
 * Message text is kept in RAM only, never logged or stored.
 */
class ZunoNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WhatsAppNotificationRules.packages) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        var added = 0
        for (message in NotificationParser.parse(sbn)) {
            if (ZunoGraph.queue.enqueue(message)) added++
        }
        if (added > 0) ZunoGraph.voice.onMessagesEnqueued()
    }
}
