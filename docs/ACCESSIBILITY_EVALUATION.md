# AccessibilityService for WhatsApp chats: evaluation (NOT implemented)

Spec item: "Investigate whether supported Android accessibility features can navigate visible
WhatsApp chats and read accessible message text."

**Decision for v0.1: not implemented.** Reasons:
- Google Play restricts AccessibilityService use to apps that help users with disabilities, and
  requires a prominent disclosure; reading other apps' private message content is high-risk for
  review rejection.
- It would read content from the screen only when WhatsApp is open and unlocked; it cannot and must
  not work on a locked device.
- WhatsApp may change view IDs and flag screens; behavior varies by version/device.

Notification data (what Zuno uses) = what WhatsApp puts in the notification, usually only the last
few messages, and nothing useful if previews are hidden. It is **not** chat history.

## How to evaluate on a real device later (safe protocol)
1. Create a tiny separate prototype (not in the main app) with an AccessibilityService limited to
   `com.whatsapp` and `typeWindowContentChanged`.
2. Log only node class names and whether `text` is non-empty (never message text) while you open a chat.
3. Check: is message text exposed? Are group chats different? What happens on lock screen (expect: nothing)?
4. Decide with the policy in mind before reading any text.

## Sending / forwarding
Not implemented. If added later: explicit on-screen confirmation, high speech-recognition
confidence required, never triggered by uncertain speech, and only through supported mechanisms
(e.g. notification inline-reply actions where WhatsApp provides them).
