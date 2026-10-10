package com.zuno.assistant.data

import com.zuno.assistant.rules.SpeechRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepository(private val dao: RuleDao) {
    val rules: Flow<List<SpeechRule>> = dao.observeAll().map { list -> list.map { it.toRule() } }

    /** Inserts when id == 0, otherwise updates. */
    suspend fun save(rule: SpeechRule) {
        if (rule.id == 0L) {
            dao.insert(rule.copy(createdAt = System.currentTimeMillis()).toEntity())
        } else {
            dao.update(rule.toEntity())
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
