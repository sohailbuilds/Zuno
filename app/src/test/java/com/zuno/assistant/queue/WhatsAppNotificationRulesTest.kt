package com.zuno.assistant.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNotificationRulesTest {
    @Test fun summaryTextsAreNoise() {
        assertTrue(WhatsAppNotificationRules.isNoise("3 new messages"))
        assertTrue(WhatsAppNotificationRules.isNoise("5 messages from 2 chats"))
        assertTrue(WhatsAppNotificationRules.isNoise("Checking for new messages"))
        assertTrue(WhatsAppNotificationRules.isNoise("   "))
    }

    @Test fun realMessagesAreNotNoise() {
        assertFalse(WhatsAppNotificationRules.isNoise("3 baje milte hain"))
        assertFalse(WhatsAppNotificationRules.isNoise("hello"))
    }

    @Test fun titleParsing() {
        assertEquals(WhatsAppNotificationRules.ParsedTitle("Rahul", "Family"), WhatsAppNotificationRules.parseTitle("Rahul @ Family"))
        assertEquals(WhatsAppNotificationRules.ParsedTitle("Family", null), WhatsAppNotificationRules.parseTitle("Family (3 messages)"))
        assertEquals(WhatsAppNotificationRules.ParsedTitle("Unknown sender", null), WhatsAppNotificationRules.parseTitle(null))
        assertNull(WhatsAppNotificationRules.parseTitle("Mom").group)
    }

    @Test fun staleHeuristic() {
        assertTrue(WhatsAppNotificationRules.isStale(1_000, 1_000 + 300_000))
        assertFalse(WhatsAppNotificationRules.isStale(1_000, 1_000 + 30_000))
        assertFalse(WhatsAppNotificationRules.isStale(0, 999_999))
    }
}
