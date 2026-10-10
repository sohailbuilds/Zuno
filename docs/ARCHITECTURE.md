# Architecture

```
WhatsApp notification
   -> notif/ZunoNotificationListener   (system-bound service, WhatsApp packages only)
   -> notif/NotificationParser         (MessagingStyle -> IncomingMessage list)
   -> queue/MessageQueue               (FIFO, duplicate suppression, RAM only)
   -> voice/VoiceController            (orchestrator, main thread)
        -> rules/RuleMatcher           (user rules; pure Kotlin; no audio knowledge)
        -> voice/TtsEngine             (Android TextToSpeech)
   <- voice/VoiceRecognizer + VoiceCommandParser (commands)
Driving: driving/DrivingDetector (Play services IN_VEHICLE transitions)
         -> ActivityTransitionReceiver -> data/SettingsStore (DataStore)
         DrivingState (pure): override ON/OFF beats detection.
UI: Jetpack Compose (ui/HomeScreen, ui/RulesScreen) + ZunoViewModel. Wiring: ZunoGraph.
```

## Key decisions
- **Matching engine is separate from the voice engine.** `rules/` and `queue/` have no Android
  imports, so they run in plain JVM tests. `VoiceController` is the only place where they meet audio.
- **Rules are exact.** Text not matched by a rule is passed through untouched. There is no built-in
  number or abbreviation conversion anywhere. Precedence among overlapping matches: longest match,
  then more conditions (app/sender), then oldest rule. Literal digit patterns refuse to match inside
  other notations (`2.5`, `10:30`, `1,500`, `5/6`); use a regex rule for those on purpose.
- **Room for rules** (a growing, searchable list), **DataStore for settings** (a few flags).
- **Messages are never persisted.** Only a SHA-256 of each message is kept (RAM) for duplicates.
- **Zuno's own sentences** ("Message from X:") live in `voice/Phrases.kt`; they are not user rules.
- **Pause** stops TTS and re-speaks the held message on resume (Android TTS cannot pause mid-sentence).

## Duplicate handling
WhatsApp re-posts a chat's notification with its last few messages. Messages with a timestamp are
deduplicated by (app, sender, timestamp, text). Messages without a timestamp are deduplicated only
within 15 s. Messages older than 2 min relative to the notification are skipped (heuristic).
