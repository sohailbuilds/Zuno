package com.zuno.assistant.voice

/**
 * Zuno's OWN fixed sentences (announcements and prompts), in Hindi or English depending on which
 * TTS voice was available. These are not message content and not user rules. Edit freely.
 */
class Phrases(private val hindi: Boolean) {
    fun message(sender: String, group: String?, body: String): String = when {
        hindi && group != null -> "$sender ने $group में भेजा: $body"
        hindi -> "$sender का संदेश: $body"
        group != null -> "$sender in $group: $body"
        else -> "Message from $sender: $body"
    }

    val noMessages get() = if (hindi) "कोई नया संदेश नहीं है।" else "No new messages."
    val nothingToRepeat get() = if (hindi) "दोहराने के लिए कुछ नहीं है।" else "Nothing to repeat."
    val notUnderstood get() = if (hindi) "समझ नहीं आया। फिर से बोलिए।" else "I didn't understand. Please say it again."
    val ambiguous get() = if (hindi) "एक ही कमांड बोलिए।" else "Please say one command."
}
