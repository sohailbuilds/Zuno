# Status: what is tested, what is not, known limits

## Verified (with evidence)
Run on JDK 21 with Kotlin 1.9.24 (`kotlinc`) and JUnit 4.13.2 — output in `docs/jvm-test-output.txt`:
**52 tests, 0 failures.**
- Custom rules (`RuleMatcherTest`): exact spoken output, regex groups, precedence (longest, conditional),
  disabled rules, ignore case, whole-token, Hindi script, invalid regex, empty spoken (silence).
- Number preservation: no rules / unrelated rules leave `1,200.50`, `10:30`, `07/10/2026`, phone numbers
  untouched; digit rules don't match inside `2.5`, `1,500`, `5/6`, `10:5`.
- Conflict detection (`ConflictDetectorTest`): ambiguous and redundant duplicates, overlaps, self-edit, disabled.
- Queue (`MessageQueueTest`): arrival order, sender preserved, duplicate reposts ignored (with/without
  timestamp window), overflow drops oldest and counts it.
- Voice command parsing (`VoiceCommandParserTest`): English/Hinglish/Devanagari, ambiguity, confidence gate.
- WhatsApp text helpers and driving-state logic.

## NOT verified (code exists, never compiled or run)
Gradle build / APK, Compose UI, Room, DataStore, TextToSpeech, SpeechRecognizer (incl. Hindi/Hinglish
accuracy), NotificationListenerService, MessagingStyle parsing of real WhatsApp notifications, Activity
Recognition, boot behavior, battery-restriction behavior, auto-read flow end to end.
Expect some compile errors on first build; they should be small.

## Not implemented
AccessibilityService chat reading, sending/forwarding, hands-free continuous listening,
a foreground service, instrumented (emulator) tests.

## Known platform limitations
- Notification content is not chat history; hidden previews give nothing readable.
- Nothing is read from a locked-device chat; Zuno does not bypass lock-screen protection.
- Driving detection is approximate (false positives/negatives, delay, needs Play services).
- Android TTS can't pause mid-sentence; pause/resume restarts the message.
- Hindi recognition quality depends on the device's recognizer and installed language packs.
- Roman-script Hinglish read by a Hindi voice may sound odd; rules let you correct specific cases.
- Aggressive OEM battery savers can kill background processes.
- WhatsApp notification format differs by version; group/sender parsing is best-effort.
