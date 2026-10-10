package com.zuno.assistant.voice

import java.text.Normalizer

enum class VoiceCommand { READ, REPEAT, PAUSE, RESUME, STOP, SKIP }

sealed interface ParseResult {
    data class Matched(val command: VoiceCommand) : ParseResult
    data class Ambiguous(val options: Set<VoiceCommand>) : ParseResult
    data object Unrecognized : ParseResult
}

/**
 * Turns recognized speech into a command. English, Hinglish (Roman) and Hindi (Devanagari).
 * Commands here are non-consequential (read/pause/stop...). Sending messages is NOT a voice
 * command in this MVP; see docs/STATUS.md.
 */
object VoiceCommandParser {

    private val phrases: Map<VoiceCommand, List<String>> = mapOf(
        VoiceCommand.READ to listOf(
            "read", "read messages", "read message", "padho", "padh do", "padh ke sunao",
            "sunao", "message sunao", "messages sunao", "पढ़ो", "पढ़ दो", "सुनाओ", "मैसेज सुनाओ",
        ),
        VoiceCommand.REPEAT to listOf(
            "repeat", "again", "once again", "dobara", "dobara sunao", "phir se", "phir se sunao",
            "fir se", "dubara", "दोबारा", "दुबारा", "फिर से",
        ),
        VoiceCommand.PAUSE to listOf(
            "pause", "ruko", "ruk jao", "thehro", "ek minute", "रुको", "रुक जाओ", "ठहरो",
        ),
        VoiceCommand.RESUME to listOf(
            "resume", "continue", "jaari rakho", "chalu karo", "aage badho", "जारी रखो", "चालू करो",
        ),
        VoiceCommand.STOP to listOf(
            "stop", "band karo", "band", "bas", "bas karo", "roko", "khatam", "बंद करो", "बंद", "बस", "बस करो", "रोको",
        ),
        VoiceCommand.SKIP to listOf(
            "skip", "next", "next message", "agla", "agla message", "chhodo", "छोड़ो", "अगला",
        ),
    )

    private val normalizedPhrases: Map<VoiceCommand, List<String>> =
        phrases.mapValues { (_, list) -> list.map { normalize(it) } }

    fun normalize(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFC)
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Parses a single recognized sentence. */
    fun parse(utterance: String): ParseResult {
        val padded = " ${normalize(utterance)} "
        if (padded.isBlank()) return ParseResult.Unrecognized

        // For each command, the length of its longest phrase found in the sentence.
        val scores = HashMap<VoiceCommand, Int>()
        for ((command, list) in normalizedPhrases) {
            val best = list.filter { padded.contains(" $it ") }.maxOfOrNull { it.length }
            if (best != null) scores[command] = best
        }
        if (scores.isEmpty()) return ParseResult.Unrecognized
        val top = scores.values.max()
        val winners = scores.filterValues { it == top }.keys
        return if (winners.size == 1) ParseResult.Matched(winners.first()) else ParseResult.Ambiguous(winners)
    }

    /**
     * Parses recognizer alternatives (best first). If confidence scores exist (>= 0) and the top
     * result is below [minConfidence], nothing is accepted.
     */
    fun parseAlternatives(
        alternatives: List<String>,
        confidences: FloatArray? = null,
        minConfidence: Float = 0.35f,
    ): ParseResult {
        if (alternatives.isEmpty()) return ParseResult.Unrecognized
        val top = confidences?.firstOrNull()
        if (top != null && top >= 0f && top < minConfidence) return ParseResult.Unrecognized
        for (alt in alternatives) {
            val r = parse(alt)
            if (r !is ParseResult.Unrecognized) return r
        }
        return ParseResult.Unrecognized
    }
}
