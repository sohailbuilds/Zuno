package com.zuno.assistant.notif

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.zuno.assistant.queue.IncomingMessage
import com.zuno.assistant.queue.WhatsAppNotificationRules

/**
 * Extracts messages from a WhatsApp notification. This only sees what WhatsApp chooses to put in
 * the notification (usually the latest few messages of a chat), NOT the chat history, and it
 * sees nothing useful if the user hides notification content.
 *
 * Notification formats differ between WhatsApp versions and devices: verify on a real device.
 */
object NotificationParser {

    fun parse(sbn: StatusBarNotification): List<IncomingMessage> {
        val app = WhatsAppNotificationRules.packages[sbn.packageName] ?: return emptyList()
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val out = ArrayList<IncomingMessage>()

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        if (style != null) {
            val convo = style.conversationTitle?.toString()?.trim()?.ifEmpty { null }
            val isGroup = style.isGroupConversation
            val userKey = style.user.key
            for (m in style.messages) {
                val text = m.text?.toString()?.trim().orEmpty()
                if (text.isEmpty() || WhatsAppNotificationRules.isNoise(text)) continue
                if (WhatsAppNotificationRules.isStale(m.timestamp, sbn.postTime)) continue
                val person = m.person
                if (person != null && userKey != null && person.key == userKey) continue // own message
                val sender = person?.name?.toString()?.trim()?.ifEmpty { null }
                    ?: convo
                    ?: WhatsAppNotificationRules.parseTitle(title).sender
                out += IncomingMessage(
                    app = app, sender = sender, text = text,
                    timestamp = m.timestamp, groupName = if (isGroup) convo else null,
                )
            }
            return out.sortedBy { it.timestamp }
        }

        // Fallback for notifications without MessagingStyle.
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isEmpty() || WhatsAppNotificationRules.isNoise(text)) return emptyList()
        val parsed = WhatsAppNotificationRules.parseTitle(title)
        return listOf(IncomingMessage(app = app, sender = parsed.sender, text = text, groupName = parsed.group))
    }
}
