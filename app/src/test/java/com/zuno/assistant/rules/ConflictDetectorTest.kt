package com.zuno.assistant.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectorTest {
    private fun r(id: Long, p: String, s: String, app: String? = null, sender: String? = null, enabled: Boolean = true) =
        SpeechRule(id = id, pattern = p, spoken = s, contextApp = app, contextSender = sender, enabled = enabled)

    @Test fun samePatternDifferentSpoken_isAmbiguousDuplicate() {
        val c = ConflictDetector.detect(r(0, "ok", "A"), listOf(r(1, "ok", "B")))
        assertEquals(1, c.size)
        assertEquals(ConflictType.DUPLICATE, c[0].type)
        assertTrue(c[0].message.contains("Ambiguous"))
    }

    @Test fun samePatternSameSpoken_isRedundantDuplicate() {
        val c = ConflictDetector.detect(r(0, "ok", "A"), listOf(r(1, "ok", "A")))
        assertEquals(ConflictType.DUPLICATE, c.single().type)
        assertTrue(c[0].message.startsWith("Duplicate"))
    }

    @Test fun sameConditionsAreRequiredForDuplicate() {
        val c = ConflictDetector.detect(r(0, "ok", "A", sender = "Mom"), listOf(r(1, "ok", "B")))
        assertTrue(c.isEmpty())
    }

    @Test fun containedLiteralIsReportedAsOverlap() {
        val c = ConflictDetector.detect(r(0, "10.30", "A"), listOf(r(1, "10", "B")))
        assertEquals(ConflictType.OVERLAP, c.single().type)
    }

    @Test fun editingARuleDoesNotConflictWithItself() {
        assertTrue(ConflictDetector.detect(r(1, "ok", "A"), listOf(r(1, "ok", "B"))).isEmpty())
    }

    @Test fun disabledRulesDoNotConflict() {
        assertTrue(ConflictDetector.detect(r(0, "ok", "A"), listOf(r(1, "ok", "B", enabled = false))).isEmpty())
    }

    @Test fun detectAllFindsPairs() {
        val all = listOf(r(1, "ok", "A"), r(2, "ok", "B"), r(3, "zzz", "C"))
        assertEquals(1, ConflictDetector.detectAll(all).size)
    }
}
