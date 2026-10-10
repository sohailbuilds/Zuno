package com.zuno.assistant.rules

enum class ConflictType {
    /** Same input and same conditions: the outcome would be ambiguous or redundant. */
    DUPLICATE,
    /** One literal pattern contains the other: the longer one wins. Informational. */
    OVERLAP,
}

data class RuleConflict(
    val type: ConflictType,
    val a: SpeechRule,
    val b: SpeechRule,
    val message: String,
)

object ConflictDetector {

    /** Conflicts between [candidate] and the [existing] rules (the candidate's own id is ignored). */
    fun detect(candidate: SpeechRule, existing: List<SpeechRule>): List<RuleConflict> =
        existing
            .filter { it.enabled && (candidate.id == 0L || it.id != candidate.id) }
            .mapNotNull { compare(candidate, it) }

    /** Every conflicting pair in [rules]. */
    fun detectAll(rules: List<SpeechRule>): List<RuleConflict> {
        val active = rules.filter { it.enabled }
        val out = ArrayList<RuleConflict>()
        for (i in active.indices) for (j in i + 1 until active.size) {
            compare(active[i], active[j])?.let { out += it }
        }
        return out
    }

    private fun compare(a: SpeechRule, b: SpeechRule): RuleConflict? {
        if (!a.enabled || !b.enabled) return null
        val ci = a.ignoreCase || b.ignoreCase
        val sameKind = a.isRegex == b.isRegex
        val sameConditions = norm(a.contextApp) == norm(b.contextApp) &&
            norm(a.contextSender) == norm(b.contextSender)

        if (sameKind && sameConditions && a.pattern.equals(b.pattern, ignoreCase = ci)) {
            val msg = if (a.spoken == b.spoken) {
                "Duplicate rule: '${a.pattern}' is already defined with the same spoken text."
            } else {
                "Ambiguous: '${a.pattern}' already exists with different spoken text " +
                    "('${b.spoken}' vs '${a.spoken}')."
            }
            return RuleConflict(ConflictType.DUPLICATE, a, b, msg)
        }

        if (!a.isRegex && !b.isRegex && conditionsOverlap(a, b) && a.pattern != b.pattern) {
            val longer = if (a.pattern.length >= b.pattern.length) a else b
            val shorter = if (longer === a) b else a
            if (longer.pattern.contains(shorter.pattern, ignoreCase = ci)) {
                return RuleConflict(
                    ConflictType.OVERLAP, a, b,
                    "'${longer.pattern}' contains '${shorter.pattern}'. " +
                        "Where both match, the longer pattern wins."
                )
            }
        }
        return null
    }

    private fun norm(s: String?): String = s?.trim()?.lowercase().orEmpty()

    private fun conditionsOverlap(a: SpeechRule, b: SpeechRule): Boolean =
        fieldsOverlap(a.contextApp, b.contextApp) && fieldsOverlap(a.contextSender, b.contextSender)

    private fun fieldsOverlap(x: String?, y: String?): Boolean =
        norm(x).isEmpty() || norm(y).isEmpty() || norm(x) == norm(y)
}
