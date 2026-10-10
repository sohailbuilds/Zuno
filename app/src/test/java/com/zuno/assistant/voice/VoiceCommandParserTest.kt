package com.zuno.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {
    private fun cmd(s: String) = (VoiceCommandParser.parse(s) as ParseResult.Matched).command

    @Test fun englishAndHinglish() {
        assertEquals(VoiceCommand.READ, cmd("padho"))
        assertEquals(VoiceCommand.READ, cmd("Messages sunao!"))
        assertEquals(VoiceCommand.REPEAT, cmd("dobara sunao"))
        assertEquals(VoiceCommand.PAUSE, cmd("ek minute"))
        assertEquals(VoiceCommand.STOP, cmd("band karo"))
        assertEquals(VoiceCommand.RESUME, cmd("aage badho"))
        assertEquals(VoiceCommand.SKIP, cmd("next"))
    }

    @Test fun devanagari() {
        assertEquals(VoiceCommand.READ, cmd("पढ़ो"))
        assertEquals(VoiceCommand.PAUSE, cmd("रुको"))
        assertEquals(VoiceCommand.STOP, cmd("बंद करो"))
        assertEquals(VoiceCommand.SKIP, cmd("अगला"))
    }

    @Test fun wordsInsideOtherWordsDoNotMatch() {
        assertEquals(ParseResult.Unrecognized, VoiceCommandParser.parse("basket"))
    }

    @Test fun conflictingCommandsAreAmbiguous_notGuessed() {
        val r = VoiceCommandParser.parse("stop next")
        assertTrue(r is ParseResult.Ambiguous)
        assertEquals(setOf(VoiceCommand.STOP, VoiceCommand.SKIP), (r as ParseResult.Ambiguous).options)
    }

    @Test fun longerPhraseWins() {
        assertEquals(VoiceCommand.REPEAT, cmd("dobara sunao"))   // not READ ("sunao")
    }

    @Test fun unknownAndEmpty() {
        assertEquals(ParseResult.Unrecognized, VoiceCommandParser.parse("hello there"))
        assertEquals(ParseResult.Unrecognized, VoiceCommandParser.parse("   "))
    }

    @Test fun lowConfidenceIsRejected() {
        assertEquals(ParseResult.Unrecognized, VoiceCommandParser.parseAlternatives(listOf("stop"), floatArrayOf(0.1f)))
        assertEquals(ParseResult.Matched(VoiceCommand.STOP), VoiceCommandParser.parseAlternatives(listOf("stop"), floatArrayOf(-1f)))
        assertEquals(ParseResult.Matched(VoiceCommand.STOP), VoiceCommandParser.parseAlternatives(listOf("stop"), floatArrayOf(0.9f)))
    }

    @Test fun laterAlternativeIsUsedWhenTopOneIsUnknown() {
        assertEquals(ParseResult.Matched(VoiceCommand.PAUSE), VoiceCommandParser.parseAlternatives(listOf("xyz", "pause")))
    }
}
