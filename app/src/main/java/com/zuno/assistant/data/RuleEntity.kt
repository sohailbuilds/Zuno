package com.zuno.assistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zuno.assistant.rules.SpeechRule

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val isRegex: Boolean,
    val ignoreCase: Boolean,
    val wholeToken: Boolean,
    val spoken: String,
    val contextApp: String?,
    val contextSender: String?,
    val enabled: Boolean,
    val createdAt: Long,
)

fun RuleEntity.toRule() = SpeechRule(
    id = id, pattern = pattern, isRegex = isRegex, ignoreCase = ignoreCase, wholeToken = wholeToken,
    spoken = spoken, contextApp = contextApp, contextSender = contextSender,
    enabled = enabled, createdAt = createdAt,
)

fun SpeechRule.toEntity() = RuleEntity(
    id = id, pattern = pattern, isRegex = isRegex, ignoreCase = ignoreCase, wholeToken = wholeToken,
    spoken = spoken, contextApp = contextApp?.trim()?.ifEmpty { null },
    contextSender = contextSender?.trim()?.ifEmpty { null },
    enabled = enabled, createdAt = createdAt,
)
