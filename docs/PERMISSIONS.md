# Permissions

| Permission / access | Why | When asked |
|---|---|---|
| Notification access (system setting, `BIND_NOTIFICATION_LISTENER_SERVICE`) | Read the text WhatsApp shows in notifications | User enables it in system settings (Home has a button) |
| `RECORD_AUDIO` | Voice commands | First tap on **Listen** |
| `ACTIVITY_RECOGNITION` | Driving auto-detection (Play services) | When enabling **Auto-detect driving** |
| `RECEIVE_BOOT_COMPLETED` | Start process after reboot so detection can re-register | Normal permission, no prompt |

Not used: AccessibilityService, SMS, contacts, location, overlay, internet.
Zuno declares no INTERNET permission. (Speech recognition may be done by the system recognizer,
which can use the network on its own unless an offline language pack is installed.)

Notification access can see ALL notifications; Zuno ignores everything except WhatsApp packages
(`com.whatsapp`, `com.whatsapp.w4b`) and drops the rest immediately without reading them.

Play Store note: notification-listener apps and anything related to accessibility or message
reading need a clear in-app disclosure and Play policy review. This is a development build.
