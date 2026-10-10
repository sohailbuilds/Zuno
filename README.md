# Zuno — voice-first Android messaging assistant (MVP, v0.1)

Zuno reads incoming WhatsApp notifications aloud while you drive, and lets you control it by voice
(Hindi / Hinglish / English). **You** decide how numbers, abbreviations and phrases are spoken
through a rules screen — Zuno never invents a pronunciation.

> **Status:** the pure-Kotlin core is tested (52 JVM unit tests pass). The Android parts
> (UI, TTS, speech recognition, notification listener, driving detection) were written but
> **not yet compiled or run** — see `docs/STATUS.md` for the honest list.

## Setup
1. Install **Android Studio** (Koala 2024.1 or newer) with Android SDK 34.
2. `File > Open` this folder. Studio downloads Gradle 8.7 (see `gradle/wrapper/gradle-wrapper.properties`)
   and syncs. (The `gradlew` scripts/jar are not included; if you want them, run `gradle wrapper`
   once from a terminal with Gradle installed.)
3. Run on a **real phone** (USB debugging on). Emulators lack WhatsApp notifications, Hindi voices
   and driving detection.
4. In the app:
   - **Home > Open notification access settings** > enable Zuno.
   - Tap **Test speech** to check TTS (install a Hindi voice in system TTS settings if needed).
   - Tap **Listen** to grant the microphone and try "padho", "dobara", "ruko", "band karo".
   - Turn on **Auto-detect driving** (asks for Activity Recognition), or set Driving mode **On** manually.
5. Add rules in **Speech rules**.

## Run the unit tests
- Android Studio: right-click `app/src/test` > Run, or `./gradlew testDebugUnitTest`.
- No Android SDK needed for the core logic (it is plain Kotlin + JUnit).

## What it does (MVP)
- Queue WhatsApp notification messages (sender known when WhatsApp provides it), skip duplicates.
- Auto-read while driving mode is active; always readable on demand with **Read**.
- Voice commands: read, repeat, pause, resume, skip, stop (non-consequential only).
- Custom speech rules: pattern or regex, exact spoken text, optional app/sender condition,
  edit/delete/test/search/conflict detection, stored locally (Room).

## What it deliberately does NOT do (yet)
- No sending/forwarding of messages. No reading of full chat history. No AccessibilityService.
  Reasons and a test plan: `docs/ACCESSIBILITY_EVALUATION.md`.

More: `docs/ARCHITECTURE.md`, `docs/PERMISSIONS.md`, `docs/STATUS.md`.

## Build without Android Studio (low-end PC)
Push this folder to a GitHub repository (folder contents at the repo root). The workflow in
`.github/workflows/build.yml` runs the unit tests and builds the debug APK. Open the repo's
**Actions** tab > latest run > **Artifacts** > `zuno-debug-apk`, then install the APK on your phone.
If a run fails, download the `build-reports` artifact or copy the error log from the failed step.
