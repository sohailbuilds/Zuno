package com.zuno.assistant.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageQueueTest {
    private var now = 1_000_000L
    private fun queue(maxQueued: Int = 50) = MessageQueue(clock = { now }, maxQueued = maxQueued)
    private fun msg(text: String, sender: String = "Raj", ts: Long = 0L) =
        IncomingMessage(app = "WhatsApp", sender = sender, text = text, timestamp = ts)

    @Test fun messagesAreReturnedInArrivalOrder() {
        val q = queue()
        q.enqueue(msg("one", ts = 1)); q.enqueue(msg("two", "Mom", ts = 2)); q.enqueue(msg("three", ts = 3))
        assertEquals(listOf("one", "two", "three"), generateSequence { q.poll() }.map { it.text }.toList())
    }

    @Test fun senderIsPreserved() {
        val q = queue()
        q.enqueue(msg("hi", "Mom", ts = 5))
        assertEquals("Mom", q.poll()!!.sender)
    }

    @Test fun repostedMessageWithSameTimestampIsIgnored_evenMuchLater() {
        val q = queue()
        assertTrue(q.enqueue(msg("hello", ts = 111)))
        now += 10 * 60_000
        assertFalse(q.enqueue(msg("hello", ts = 111)))
        assertEquals(1, q.size())
    }

    @Test fun duplicateDoesNotReorderQueue() {
        val q = queue()
        q.enqueue(msg("a", ts = 1)); q.enqueue(msg("b", ts = 2)); q.enqueue(msg("a", ts = 1)); q.enqueue(msg("c", ts = 3))
        assertEquals(listOf("a", "b", "c"), q.snapshot().map { it.text })
    }

    @Test fun sameTextWithDifferentTimestampsIsKept() {
        val q = queue()
        assertTrue(q.enqueue(msg("ok", ts = 1)))
        assertTrue(q.enqueue(msg("ok", ts = 2)))
        assertEquals(2, q.size())
    }

    @Test fun sameTextFromDifferentSendersIsKept() {
        val q = queue()
        assertTrue(q.enqueue(msg("ok", "A", ts = 1)))
        assertTrue(q.enqueue(msg("ok", "B", ts = 1)))
    }

    @Test fun withoutTimestamp_duplicateIsSuppressedOnlyInsideTheWindow() {
        val q = queue()
        assertTrue(q.enqueue(msg("hey")))
        now += 5_000
        assertFalse(q.enqueue(msg("hey")))
        now += 20_000
        assertTrue(q.enqueue(msg("hey")))
    }

    @Test fun fullQueueDropsOldestAndCountsIt() {
        val q = queue(maxQueued = 3)
        (1..5).forEach { q.enqueue(msg("m$it", ts = it.toLong())) }
        assertEquals(listOf("m3", "m4", "m5"), q.snapshot().map { it.text })
        assertEquals(2, q.droppedCount)
    }

    @Test fun pollOnEmptyReturnsNull() {
        assertNull(queue().poll())
        assertNull(queue().peek())
    }

    @Test fun clearEmptiesQueueButStoppedMessagesStayDeduplicated() {
        val q = queue()
        q.enqueue(msg("a", ts = 1))
        q.clear()
        assertEquals(0, q.size())
        assertFalse(q.enqueue(msg("a", ts = 1)))
    }

    @Test fun receivedAtIsStamped() {
        val q = queue()
        q.enqueue(msg("a", ts = 1))
        assertEquals(now, q.peek()!!.receivedAt)
    }
}
