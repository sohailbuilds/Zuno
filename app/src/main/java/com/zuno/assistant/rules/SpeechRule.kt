package com.zuno.assistant.rules

/**
 * One user-defined speech rule: "when you see [pattern], say exactly [spoken]".
 *
 * Pure Kotlin on purpose (no Android imports) so it can be unit-tested on the JVM
 * and stays independent of the voice engine.
 */
data class SpeechRule(
    val id: Long = 0,
    /** Text to look for. Literal by default; a regular expression if [isRegex]. */
    val pattern: String,
    val isRegex: Boolean = false,
    val ignoreCase: Boolean = false,
    /**
     * Literal rules only: don't match when the pattern is glued to other letters/digits,
     * or (for digit edges) to number punctuation like "2.5", "10:30", "1,500".
     */
    val wholeToken: Boolean = true,
    /** Exactly what the user wants spoken. Empty = speak nothing for the match. */
    val spoken: String,
    /** Optional condition: only apply for this app (e.g. "WhatsApp"). */
    val contextApp: String? = null,
    /** Optional condition: only apply for this sender name. */
    val contextSender: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0L,
) {
    /** How many optional conditions this rule has (more = more specific). */
    val specificity: Int
        get() = (if (contextApp.isNullOrBlank()) 0 else 1) +
            (if (contextSender.isNullOrBlank()) 0 else 1)
}

/** Where a piece of text came from. Used to evaluate optional rule conditions. */
data class MessageContext(
    val app: String? = null,
    val sender: String? = null,
)
