package com.zuno.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Thin wrapper over Android TextToSpeech. Callbacks may arrive on a binder thread. */
class TtsEngine(context: Context, private val listener: Listener) {

    interface Listener {
        fun onSpeechDone(id: String)
        fun onSpeechError(id: String?)
        fun onInitFailed()
    }

    private var tts: TextToSpeech? = null
    private var pending: Pair<String, String>? = null

    @Volatile var ready = false
        private set

    /** True when a Hindi voice was selected (decides which fixed phrases Zuno uses). */
    @Volatile var isHindi = false
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status -> onInit(status) }
    }

    private fun onInit(status: Int) {
        val t = tts
        if (status != TextToSpeech.SUCCESS || t == null) {
            listener.onInitFailed()
            return
        }
        val hindi = Locale("hi", "IN")
        val indianEnglish = Locale("en", "IN")
        isHindi = when {
            t.isLanguageAvailable(hindi) >= TextToSpeech.LANG_AVAILABLE -> { t.setLanguage(hindi); true }
            t.isLanguageAvailable(indianEnglish) >= TextToSpeech.LANG_AVAILABLE -> { t.setLanguage(indianEnglish); false }
            else -> false
        }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId != null) listener.onSpeechDone(utteranceId)
            }
            @Deprecated("Required by the abstract class")
            override fun onError(utteranceId: String?) = listener.onSpeechError(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = listener.onSpeechError(utteranceId)
        })
        ready = true
        pending?.let { (text, id) -> pending = null; speak(text, id) }
    }

    /** Speaks [text], replacing anything currently being spoken. */
    fun speak(text: String, id: String) {
        val t = tts
        if (!ready || t == null) {
            pending = text to id
            return
        }
        val max = TextToSpeech.getMaxSpeechInputLength() - 1
        val safe = if (text.length > max) text.take(max) else text
        t.speak(safe, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        pending = null
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
