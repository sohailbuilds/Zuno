package com.zuno.assistant.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleMatcherTest {
    private val m = RuleMatcher()

    private fun fmt(text: String, vararg rules: SpeechRule, ctx: MessageContext = MessageContext()) =
        m.format(text, rules.toList(), ctx).output

    // ---- number / text preservation ------------------------------------------------------

    @Test fun noRules_textIsReturnedUnchanged() {
        val text = "Rs 1,200.50 at 10:30 on 07/10/2026 call +91 98765 43210"
        assertEquals(text, m.format(text, emptyList()).output)
    }

    @Test fun unrelatedRule_leavesNumbersUntouched() {
        val text = "Rs 1,200.50 at 10:30 on 07/10/2026 call +91 98765 43210"
        assertEquals(text, fmt(text, SpeechRule(pattern = "hello", spoken = "hi")))
    }

    @Test fun digitRule_doesNotMatchInsideOtherNumberNotations() {
        val rule = SpeechRule(pattern = "5", spoken = "FIVE")
        for (text in listOf("rating 2.5", "ratio 1,5", "page 5/6", "range 5-6", "age 15", "paid 1,500", "at 10:5")) {
            assertEquals("must not touch: $text", text, fmt(text, rule))
        }
        assertEquals("I have FIVE apples", fmt("I have 5 apples", rule))
        assertEquals("score FIVE.", fmt("score 5.", rule)) // sentence-ending full stop is not a decimal
    }

    @Test fun decimalRule_matchesOnlyTheExactNotation() {
        val rule = SpeechRule(pattern = "2.5", spoken = "dhaai")
        assertEquals("rating dhaai stars", fmt("rating 2.5 stars", rule))
        assertEquals("rating 12.5 and 2.55", fmt("rating 12.5 and 2.55", rule))
    }

    // ---- user output is exact -------------------------------------------------------------

    @Test fun spokenTextIsInsertedVerbatim() {
        val spoken = "  Be Right  Back!! 50% "
        assertEquals("a " + spoken + " b", fmt("a brb b", SpeechRule(pattern = "brb", spoken = spoken)))
    }

    @Test fun regexGroupsCanBeReused() {
        val rule = SpeechRule(pattern = "(\\d+)/(\\d+)", isRegex = true, spoken = "{1} by {2}")
        assertEquals("see 3 by 4 now", fmt("see 3/4 now", rule))
    }

    @Test fun regexPlaceholderWithoutGroupIsLeftAsWritten() {
        val rule = SpeechRule(pattern = "(\\d+)", isRegex = true, spoken = "{1}-{7}")
        assertEquals("x 12-{7} y", fmt("x 12 y", rule))
    }

    @Test fun emptySpokenMeansSilence() {
        assertEquals("hi  there", fmt("hi 😀 there", SpeechRule(pattern = "😀", spoken = "")))
    }

    // ---- precedence -----------------------------------------------------------------------

    @Test fun longestMatchWins() {
        val a = SpeechRule(id = 1, pattern = "ab", spoken = "1", wholeToken = false)
        val b = SpeechRule(id = 2, pattern = "abc", spoken = "2", wholeToken = false)
        assertEquals("2", fmt("abc", a, b))
        assertEquals("2", fmt("abc", b, a))
    }

    @Test fun conditionalRuleBeatsGeneralRule_onlyInItsContext() {
        val general = SpeechRule(id = 1, pattern = "ok", spoken = "theek hai")
        val mom = SpeechRule(id = 2, pattern = "ok", spoken = "haan beta", contextSender = "Mom")
        assertEquals("haan beta", fmt("ok", general, mom, ctx = MessageContext(sender = "mom")))
        assertEquals("theek hai", fmt("ok", general, mom, ctx = MessageContext(sender = "Raj")))
        assertEquals("theek hai", fmt("ok", general, mom))
    }

    @Test fun appCondition() {
        val r = SpeechRule(pattern = "ok", spoken = "WA-ok", contextApp = "WhatsApp")
        assertEquals("WA-ok", fmt("ok", r, ctx = MessageContext(app = "WhatsApp")))
        assertEquals("ok", fmt("ok", r, ctx = MessageContext(app = "Other")))
    }

    @Test fun disabledRulesAreIgnored() {
        assertEquals("brb", fmt("brb", SpeechRule(pattern = "brb", spoken = "x", enabled = false)))
    }

    @Test fun multipleOccurrencesAreAllReplaced() {
        assertEquals("x b x", fmt("a b a", SpeechRule(pattern = "a", spoken = "x")))
    }

    // ---- matching options -----------------------------------------------------------------

    @Test fun ignoreCase() {
        val r = SpeechRule(pattern = "brb", spoken = "x", ignoreCase = true)
        assertEquals("x", fmt("BRB", r))
        assertEquals("BRB", fmt("BRB", r.copy(ignoreCase = false)))
    }

    @Test fun wholeToken_doesNotMatchInsideWords() {
        val r = SpeechRule(pattern = "ok", spoken = "x")
        assertEquals("book", fmt("book", r))
        assertEquals("box", fmt("book", r.copy(wholeToken = false)))
    }

    @Test fun hindiScript() {
        assertEquals("tomorrow मिलते हैं", fmt("कल मिलते हैं", SpeechRule(pattern = "कल", spoken = "tomorrow")))
        assertEquals("tomorrow milte hain", fmt("kal milte hain", SpeechRule(pattern = "kal", spoken = "tomorrow")))
    }

    // ---- validation -----------------------------------------------------------------------

    @Test fun invalidRegexIsRejectedAndIgnored() {
        val bad = SpeechRule(pattern = "(", isRegex = true, spoken = "x")
        assertNotNull(m.validate(bad))
        assertEquals("a ( b", fmt("a ( b", bad))
    }

    @Test fun blankPatternIsRejected() {
        assertNotNull(m.validate(SpeechRule(pattern = "  ", spoken = "x")))
        assertNull(m.validate(SpeechRule(pattern = "ok", spoken = "x")))
    }

    @Test fun appliedRulesReportTheOriginalText() {
        val res = m.format("say brb now", listOf(SpeechRule(id = 9, pattern = "brb", spoken = "later")))
        assertEquals(1, res.applied.size)
        assertEquals("brb", res.applied[0].original)
        assertEquals(9L, res.applied[0].ruleId)
        assertTrue(res.output.contains("later"))
    }
}
