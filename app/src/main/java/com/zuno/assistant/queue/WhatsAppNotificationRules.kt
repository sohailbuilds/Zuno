package com.zuno.assistant.queue

/** Pure helpers for interpreting WhatsApp notification text. Formats vary by app version. */
object WhatsAppNotificationRules {

    val packages: Map<String, String> = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
    )

    data class ParsedTitle(val sender: String, val group: String?)

    private val noise = listOf(
        Regex("^\\d+ (new )?messages?( from \\d+ chats?)?$", RegexOption.IGNORE_CASE),
        Regex("^checking for new messages.*", RegexOption.IGNORE_CASE),
        Regex("^whatsapp web is currently active.*", RegexOption.IGNORE_CASE),
    )

    private val countSuffix = Regex("\\s*\\(\\d+ messages?\\)\\s*$", RegexOption.IGNORE_CASE)

    /** Summary/status texts that are not real messages. */
    fun isNoise(text: String): Boolean {
        val t = text.trim()
        return t.isEmpty() || noise.any { it.matches(t) }
    }

    /** Best-effort split of a notification title into sender and (optional) group. */
    fun parseTitle(raw: String?): ParsedTitle {
        val t = raw.orEmpty().replace(countSuffix, "").trim()
        if (t.isEmpty()) return ParsedTitle("Unknown sender", null)
        val at = t.indexOf(" @ ")
        if (at > 0) return ParsedTitle(t.substring(0, at).trim(), t.substring(at + 3).trim())
        return ParsedTitle(t, null)
    }

    /**
     * Notifications repeat the last few messages of a chat. A message much older than the
     * notification itself is almost certainly one we already handled (or one from before we
     * were listening), so it is skipped. A heuristic; see docs/STATUS.md.
     */
    fun isStale(messageTimestamp: Long, notificationPostTime: Long, maxAgeMs: Long = 120_000L): Boolean =
        messageTimestamp > 0 && notificationPostTime - messageTimestamp > maxAgeMs
}
