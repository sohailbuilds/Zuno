package com.zuno.assistant.queue

import java.security.MessageDigest

/**
 * FIFO queue of messages waiting to be spoken, with duplicate suppression.
 *
 * Duplicates: WhatsApp re-posts a chat's notification (with the last few messages) every time
 * something changes. A message that carries a timestamp is identified by
 * (app, sender, timestamp, text) and is accepted only once. A message WITHOUT a timestamp is
 * only suppressed if an identical one was seen within [dedupeWindowMs].
 *
 * Only a hash of the text is kept for duplicate detection; the queue itself holds the messages
 * until they are spoken. Nothing is persisted.
 */
class MessageQueue(
    private val clock: () -> Long = System::currentTimeMillis,
    private val dedupeWindowMs: Long = 15_000L,
    private val maxSeen: Int = 300,
    private val maxQueued: Int = 50,
) {
    private val queue = ArrayDeque<IncomingMessage>()
    private val seen = LinkedHashMap<String, Long>()

    /** Number of messages dropped because the queue was full (oldest are dropped first). */
    @get:Synchronized
    var droppedCount: Int = 0
        private set

    /** @return true if the message was added, false if it was a duplicate. */
    @Synchronized
    fun enqueue(message: IncomingMessage): Boolean {
        val now = clock()
        val key = fingerprint(message)
        val last = seen[key]
        val duplicate = if (message.timestamp > 0) last != null else (last != null && now - last < dedupeWindowMs)
        if (duplicate) return false

        seen.remove(key)
        seen[key] = now
        while (seen.size > maxSeen) {
            val it = seen.entries.iterator()
            it.next()
            it.remove()
        }

        if (queue.size >= maxQueued) {
            queue.removeFirst()
            droppedCount++
        }
        queue.addLast(message.copy(receivedAt = now))
        return true
    }

    @Synchronized fun poll(): IncomingMessage? = if (queue.isEmpty()) null else queue.removeFirst()
    @Synchronized fun peek(): IncomingMessage? = queue.firstOrNull()
    @Synchronized fun size(): Int = queue.size
    @Synchronized fun snapshot(): List<IncomingMessage> = queue.toList()

    /** Empties the queue. Duplicate memory is kept so stopped messages don't come back. */
    @Synchronized fun clear() = queue.clear()

    private fun fingerprint(m: IncomingMessage): String {
        val raw = "${m.app}\u0000${m.sender}\u0000${m.timestamp}\u0000${m.text.trim()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
