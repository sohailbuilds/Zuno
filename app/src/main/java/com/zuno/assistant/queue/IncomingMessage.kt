package com.zuno.assistant.queue

/** One message learned from a notification. Lives in RAM only; never written to disk. */
data class IncomingMessage(
    val app: String,
    val sender: String,
    val text: String,
    /** Message timestamp from the notification, or 0 if the notification didn't provide one. */
    val timestamp: Long = 0L,
    val groupName: String? = null,
    val receivedAt: Long = 0L,
)
