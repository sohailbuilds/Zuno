package com.zuno.assistant.rules

/** One rule that fired on one place in the text. */
data class AppliedRule(
    val ruleId: Long,
    val start: Int,
    val endExclusive: Int,
    val original: String,
    val spoken: String,
)

data class FormatResult(
    val output: String,
    val applied: List<AppliedRule>,
)

/**
 * The matching engine. Takes text + user rules, returns text to speak.
 *
 * Guarantees:
 *  - Text not covered by a rule is returned EXACTLY as given (numbers, punctuation, everything).
 *    There is no built-in number/abbreviation conversion at all.
 *  - A rule's spoken text is inserted verbatim (regex rules may reuse groups via {1}, {2}...).
 *  - Overlaps: longest match wins, then the rule with more conditions, then the oldest rule.
 */
class RuleMatcher {

    private data class CacheKey(
        val pattern: String,
        val isRegex: Boolean,
        val ignoreCase: Boolean,
        val wholeToken: Boolean,
    )

    private class Candidate(val rule: SpeechRule, val start: Int, val end: Int, val match: MatchResult)

    private val cache = HashMap<CacheKey, Regex?>()

    fun format(
        text: String,
        rules: List<SpeechRule>,
        context: MessageContext = MessageContext(),
    ): FormatResult {
        if (text.isEmpty() || rules.isEmpty()) return FormatResult(text, emptyList())

        val candidates = ArrayList<Candidate>()
        for (rule in rules) {
            if (!rule.enabled || !conditionsMet(rule, context)) continue
            val regex = compile(rule) ?: continue
            for (m in regex.findAll(text)) {
                if (m.value.isEmpty()) continue
                candidates += Candidate(rule, m.range.first, m.range.last + 1, m)
            }
        }
        if (candidates.isEmpty()) return FormatResult(text, emptyList())

        candidates.sortWith(
            compareByDescending<Candidate> { it.end - it.start }
                .thenByDescending { it.rule.specificity }
                .thenBy { it.rule.createdAt }
                .thenBy { it.rule.id }
                .thenBy { it.start }
        )
        val accepted = ArrayList<Candidate>()
        for (c in candidates) {
            if (accepted.none { it.start < c.end && c.start < it.end }) accepted += c
        }
        accepted.sortBy { it.start }

        val sb = StringBuilder()
        val applied = ArrayList<AppliedRule>()
        var cursor = 0
        for (c in accepted) {
            sb.append(text, cursor, c.start)
            val spoken = expand(c)
            sb.append(spoken)
            applied += AppliedRule(c.rule.id, c.start, c.end, text.substring(c.start, c.end), spoken)
            cursor = c.end
        }
        sb.append(text, cursor, text.length)
        return FormatResult(sb.toString(), applied)
    }

    /** Returns an error message if the rule is unusable, otherwise null. */
    fun validate(rule: SpeechRule): String? {
        if (rule.pattern.isBlank()) return "Pattern cannot be empty."
        return try {
            buildRegex(rule)
            null
        } catch (e: Exception) {
            "Invalid pattern: ${e.message?.lineSequence()?.firstOrNull() ?: "unknown error"}"
        }
    }

    private fun conditionsMet(rule: SpeechRule, ctx: MessageContext): Boolean =
        conditionMet(rule.contextApp, ctx.app) && conditionMet(rule.contextSender, ctx.sender)

    private fun conditionMet(required: String?, actual: String?): Boolean =
        required.isNullOrBlank() ||
            (actual != null && required.trim().equals(actual.trim(), ignoreCase = true))

    private fun compile(rule: SpeechRule): Regex? {
        val key = CacheKey(rule.pattern, rule.isRegex, rule.ignoreCase, rule.wholeToken)
        synchronized(cache) {
            if (cache.containsKey(key)) return cache[key]
            val built = try {
                buildRegex(rule)
            } catch (e: Exception) {
                null
            }
            cache[key] = built
            return built
        }
    }

    private val placeholder = Regex("\\{(\\d+)\\}")

    private fun expand(c: Candidate): String {
        if (!c.rule.isRegex) return c.rule.spoken
        return placeholder.replace(c.rule.spoken) { mr ->
            val g = mr.groupValues[1].toInt()
            if (g < c.match.groups.size) c.match.groups[g]?.value ?: "" else mr.value
        }
    }

    companion object {
        private const val LETTER_DIGIT = "\\p{L}\\p{N}"

        internal fun buildRegex(rule: SpeechRule): Regex {
            require(rule.pattern.isNotEmpty()) { "empty pattern" }
            val options = if (rule.ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            if (rule.isRegex) return Regex(rule.pattern, options)

            val sb = StringBuilder()
            if (rule.wholeToken) {
                val first = rule.pattern.first()
                if (first.isLetterOrDigit()) sb.append("(?<![$LETTER_DIGIT])")
                // A digit edge must not be glued to number punctuation: 2.5, 10:30, 1,500, 5/6, 5-6
                if (first.isDigit()) sb.append("(?<!\\p{N}[.,:/\\-])")
            }
            sb.append(Regex.escape(rule.pattern))
            if (rule.wholeToken) {
                val last = rule.pattern.last()
                if (last.isLetterOrDigit()) sb.append("(?![$LETTER_DIGIT])")
                if (last.isDigit()) sb.append("(?![.,:/\\-]\\p{N})")
            }
            return Regex(sb.toString(), options)
        }
    }
}
