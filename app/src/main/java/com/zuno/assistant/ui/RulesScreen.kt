package com.zuno.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zuno.assistant.rules.ConflictDetector
import com.zuno.assistant.rules.ConflictType
import com.zuno.assistant.rules.MessageContext
import com.zuno.assistant.rules.SpeechRule

@Composable
fun RulesScreen(vm: ZunoViewModel) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SpeechRule?>(null) }
    var deleting by remember { mutableStateOf<SpeechRule?>(null) }

    val shown = remember(rules, query) {
        val q = query.trim()
        if (q.isEmpty()) rules else rules.filter {
            it.pattern.contains(q, true) || it.spoken.contains(q, true) ||
                (it.contextApp ?: "").contains(q, true) || (it.contextSender ?: "").contains(q, true)
        }
    }
    val conflictCount = remember(rules) { ConflictDetector.detectAll(rules).size }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "You decide how things are spoken. Zuno never invents a pronunciation: " +
                "text with no matching rule is spoken exactly as it arrived.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Search rules") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            Button(onClick = { editing = SpeechRule(pattern = "", spoken = "") }) { Text("Add") }
        }
        if (conflictCount > 0) {
            Text(
                "$conflictCount rule conflict(s) found. Open a rule to see details.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { rule ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("“${rule.pattern}”  →  “${rule.spoken}”", style = MaterialTheme.typography.titleSmall)
                        val tags = buildList {
                            if (rule.isRegex) add("regex")
                            if (rule.ignoreCase) add("ignore case")
                            rule.contextApp?.takeIf { it.isNotBlank() }?.let { add("app: $it") }
                            rule.contextSender?.takeIf { it.isNotBlank() }?.let { add("sender: $it") }
                        }
                        if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = rule.enabled, onCheckedChange = { vm.setRuleEnabled(rule.id, it) })
                            TextButton(onClick = { editing = rule }) { Text("Edit") }
                            TextButton(onClick = { deleting = rule }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { RuleEditorDialog(it, vm, onDismiss = { editing = null }) }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete rule?") },
            text = { Text("“${r.pattern}” → “${r.spoken}”") },
            confirmButton = { TextButton(onClick = { vm.deleteRule(r.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RuleEditorDialog(initial: SpeechRule, vm: ZunoViewModel, onDismiss: () -> Unit) {
    var pattern by remember { mutableStateOf(initial.pattern) }
    var spoken by remember { mutableStateOf(initial.spoken) }
    var isRegex by remember { mutableStateOf(initial.isRegex) }
    var ignoreCase by remember { mutableStateOf(initial.ignoreCase) }
    var wholeToken by remember { mutableStateOf(initial.wholeToken) }
    var app by remember { mutableStateOf(initial.contextApp.orEmpty()) }
    var sender by remember { mutableStateOf(initial.contextSender.orEmpty()) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var testInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val draft = SpeechRule(
        id = initial.id, pattern = pattern, isRegex = isRegex, ignoreCase = ignoreCase,
        wholeToken = wholeToken, spoken = spoken,
        contextApp = app.ifBlank { null }, contextSender = sender.ifBlank { null },
        enabled = enabled, createdAt = initial.createdAt,
    )
    val conflicts = if (pattern.isBlank()) emptyList() else vm.conflicts(draft)
    val blocking = conflicts.any { it.type == ConflictType.DUPLICATE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New rule" else "Edit rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pattern, { pattern = it }, label = { Text("When Zuno sees (original text)") })
                OutlinedTextField(spoken, { spoken = it }, label = { Text("Say exactly (leave empty = say nothing)") })
                LabeledCheck("Pattern is a regular expression (use {1}, {2} for groups)", isRegex) { isRegex = it }
                LabeledCheck("Ignore upper/lower case", ignoreCase) { ignoreCase = it }
                if (!isRegex) {
                    LabeledCheck("Whole token only (won't match inside 2.5, 10:30, words)", wholeToken) { wholeToken = it }
                }
                OutlinedTextField(app, { app = it }, label = { Text("Only for app (optional), e.g. WhatsApp") })
                OutlinedTextField(sender, { sender = it }, label = { Text("Only for sender (optional)") })
                LabeledCheck("Enabled", enabled) { enabled = it }

                Text("Test", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(testInput, { testInput = it }, label = { Text("Sample message text") })
                if (testInput.isNotEmpty() && pattern.isNotBlank()) {
                    val ctx = MessageContext(app = app.ifBlank { null }, sender = sender.ifBlank { null })
                    Text("Result: " + vm.preview(draft, testInput, ctx))
                    OutlinedButton(onClick = { vm.speakTest(testInput) }) { Text("Speak sample (saved rules)") }
                }
                conflicts.forEach {
                    Text(
                        it.message,
                        color = if (it.type == ConflictType.DUPLICATE) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !blocking,
                onClick = { vm.saveRule(draft) { err -> if (err == null) onDismiss() else error = err } },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LabeledCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
