package com.zuno.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.zuno.assistant.queue.MessageQueue
import com.zuno.assistant.rules.MessageContext
import com.zuno.assistant.rules.RuleMatcher
import com.zuno.assistant.rules.SpeechRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates the voice side: pulls messages from the queue, runs them through the user's
 * rules ([RuleMatcher]) and hands the resulting text to TTS; handles voice commands.
 *
 * All state changes happen on the main thread. The matching engine knows nothing about audio.
 */
class VoiceController(
    context: Context,
    private val queue: MessageQueue,
    private val matcher: RuleMatcher,
    private val rulesProvider: () -> List<SpeechRule>,
    private val drivingActive: () -> Boolean,
    private val autoRead: () -> Boolean,
) : TtsEngine.Listener {

    private val main = Handler(Looper.getMainLooper())
    private val tts = TtsEngine(context.applicationContext, this)
    private val recognizer = VoiceRecognizer(
        context.applicationContext, ::onRecognized, ::onRecognitionError
    )

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()
    private val _lastHeard = MutableStateFlow<String?>(null)
    val lastHeard: StateFlow<String?> = _lastHeard.asStateFlow()
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val phrases get() = Phrases(tts.isHindi)

    private var seq = 0
    private var activeId: String? = null
    private var advanceAfterSpeech = false
    private var lastSpoken: String? = null
    private var resumeText: String? = null
    private var paused = false

    // ---- entry points (safe from any thread) ------------------------------------------------

    /** Called after new messages were added to the queue. */
    fun onMessagesEnqueued() = main.post {
        refreshQueueSize()
        val idle = _state.value is VoiceState.Idle || _state.value is VoiceState.Error
        if (autoRead() && drivingActive() && !paused && idle) speakNext()
    }

    fun execute(command: VoiceCommand) = main.post { dispatch(command) }

    fun startListening() = main.post {
        if (_state.value is VoiceState.Listening) return@post
        if (_state.value is VoiceState.Speaking) {
            // Hold the current message while the user speaks; "resume" continues it.
            tts.stop()
            activeId = null
            paused = true
        }
        _state.value = VoiceState.Listening
        recognizer.start("hi-IN")
    }

    /** Speaks [text] after applying the user's rules (no message context). Used by the test buttons. */
    fun speakFormatted(text: String) = main.post {
        val out = matcher.format(text, rulesProvider(), MessageContext()).output
        speak(out, advance = false, who = "Test")
    }

    // ---- commands ---------------------------------------------------------------------------

    private fun dispatch(command: VoiceCommand) {
        when (command) {
            VoiceCommand.READ -> {
                paused = false
                resumeText = null
                if (queue.size() == 0) speakSystem(phrases.noMessages) else speakNext()
            }
            VoiceCommand.REPEAT -> {
                paused = false
                val text = lastSpoken
                if (text == null) speakSystem(phrases.nothingToRepeat) else speak(text, true, "Repeat")
            }
            VoiceCommand.PAUSE -> {
                paused = true
                tts.stop()
                activeId = null
                _state.value = VoiceState.Paused
            }
            VoiceCommand.RESUME -> {
                paused = false
                val text = resumeText
                if (text != null) speak(text, true, "Resume") else speakNext()
            }
            VoiceCommand.STOP -> {
                paused = false
                tts.stop()
                activeId = null
                resumeText = null
                queue.clear()
                refreshQueueSize()
                _state.value = VoiceState.Idle
            }
            VoiceCommand.SKIP -> {
                paused = false
                tts.stop()
                activeId = null
                resumeText = null
                speakNext()
            }
        }
    }

    // ---- speaking ---------------------------------------------------------------------------

    private fun speakNext() {
        val msg = queue.poll()
        refreshQueueSize()
        if (msg == null) {
            _state.value = VoiceState.Idle
            return
        }
        val rules = rulesProvider()
        val ctx = MessageContext(app = msg.app, sender = msg.sender)
        val sender = matcher.format(msg.sender, rules, ctx).output
        val group = msg.groupName?.let { matcher.format(it, rules, ctx).output }
        val body = matcher.format(msg.text, rules, ctx).output
        speak(phrases.message(sender, group, body), advance = true, who = msg.sender)
    }

    private fun speakSystem(text: String) = speak(text, advance = false, who = "Zuno")

    private fun speak(text: String, advance: Boolean, who: String) {
        val id = "zuno-${++seq}"
        activeId = id
        advanceAfterSpeech = advance
        if (advance) {
            lastSpoken = text
            resumeText = text
        }
        _state.value = VoiceState.Speaking(who)
        tts.speak(text, id)
    }

    private fun refreshQueueSize() {
        _queueSize.value = queue.size()
    }

    // ---- TTS callbacks (binder thread) -> main ----------------------------------------------

    override fun onSpeechDone(id: String) {
        main.post {
            if (id != activeId) return@post
            activeId = null
            if (advanceAfterSpeech) resumeText = null
            when {
                paused -> _state.value = VoiceState.Paused
                advanceAfterSpeech -> speakNext()
                else -> _state.value = VoiceState.Idle
            }
        }
    }

    override fun onSpeechError(id: String?) {
        main.post {
            activeId = null
            _state.value = VoiceState.Error("Text-to-speech failed.")
        }
    }

    override fun onInitFailed() {
        main.post { _state.value = VoiceState.Error("No text-to-speech engine available.") }
    }

    // ---- recognizer callbacks (main thread) -------------------------------------------------

    private fun onRecognized(alternatives: List<String>, confidences: FloatArray?) {
        _state.value = VoiceState.Processing
        _lastHeard.value = alternatives.firstOrNull()
        when (val r = VoiceCommandParser.parseAlternatives(alternatives, confidences)) {
            is ParseResult.Matched -> dispatch(r.command)
            is ParseResult.Ambiguous -> speakSystem(phrases.ambiguous)
            ParseResult.Unrecognized -> speakSystem(phrases.notUnderstood)
        }
    }

    private fun onRecognitionError(message: String) {
        _state.value = VoiceState.Error(message)
    }
}
