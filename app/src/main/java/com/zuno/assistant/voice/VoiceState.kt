package com.zuno.assistant.voice

/** What the voice side is doing right now. */
sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data object Processing : VoiceState
    data class Speaking(val who: String) : VoiceState
    data object Paused : VoiceState
    data class Error(val message: String) : VoiceState
}
