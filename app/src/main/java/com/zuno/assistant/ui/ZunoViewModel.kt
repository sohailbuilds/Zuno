package com.zuno.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zuno.assistant.ZunoGraph
import com.zuno.assistant.data.Settings
import com.zuno.assistant.driving.DrivingOverride
import com.zuno.assistant.rules.ConflictDetector
import com.zuno.assistant.rules.MessageContext
import com.zuno.assistant.rules.RuleConflict
import com.zuno.assistant.rules.SpeechRule
import com.zuno.assistant.voice.VoiceCommand
import com.zuno.assistant.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZunoViewModel(app: Application) : AndroidViewModel(app) {
    private val g = ZunoGraph

    val rules: StateFlow<List<SpeechRule>> =
        g.ruleRepository.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<Settings> =
        g.settings.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    val voiceState: StateFlow<VoiceState> = g.voice.state
    val lastHeard: StateFlow<String?> = g.voice.lastHeard
    val queueSize: StateFlow<Int> = g.voice.queueSize

    private val _detectorMessage = MutableStateFlow<String?>(null)
    val detectorMessage: StateFlow<String?> = _detectorMessage

    // ---- voice ------------------------------------------------------------------------------
    fun listen() = g.voice.startListening()
    fun command(c: VoiceCommand) = g.voice.execute(c)
    fun speakTest(text: String) = g.voice.speakFormatted(text)

    // ---- driving ----------------------------------------------------------------------------
    fun setOverride(o: DrivingOverride) = viewModelScope.launch { g.settings.setOverride(o) }
    fun setAutoRead(v: Boolean) = viewModelScope.launch { g.settings.setAutoRead(v) }

    fun setAutoDetect(enabled: Boolean) {
        viewModelScope.launch {
            g.settings.setAutoDetect(enabled)
            if (enabled) {
                g.detector.start { _detectorMessage.value = it }
            } else {
                g.detector.stop()
                g.settings.setDetected(false)
                _detectorMessage.value = "Driving auto-detection is off."
            }
        }
    }

    // ---- rules ------------------------------------------------------------------------------
    fun validate(rule: SpeechRule): String? = g.matcher.validate(rule)

    fun conflicts(rule: SpeechRule): List<RuleConflict> = ConflictDetector.detect(rule, rules.value)

    /** What [input] would sound like with this rule (edited or new) added to the saved rules. */
    fun preview(rule: SpeechRule, input: String, ctx: MessageContext): String {
        val others = rules.value.filter { rule.id == 0L || it.id != rule.id }
        return g.matcher.format(input, others + rule.copy(enabled = true), ctx).output
    }

    fun saveRule(rule: SpeechRule, onDone: (String?) -> Unit) {
        val error = validate(rule)
        if (error != null) {
            onDone(error)
            return
        }
        viewModelScope.launch {
            g.ruleRepository.save(rule)
            onDone(null)
        }
    }

    fun deleteRule(id: Long) = viewModelScope.launch { g.ruleRepository.delete(id) }
    fun setRuleEnabled(id: Long, enabled: Boolean) = viewModelScope.launch { g.ruleRepository.setEnabled(id, enabled) }
}
