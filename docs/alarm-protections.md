# Alarm protections in this fork

The four Special settings switches are available in both debug and release builds. They are
per-alarm, survive restarts, and are copied with an alarm. Upgrading the database from version
10 to 11 preserves existing alarms and leaves every new protection off. Enable them in the
alarm editor, return to the editor, and save.

## Behavior

| Setting | Behavior | Access |
| --- | --- | --- |
| Do not leave alarm | Returns to the alarm after a window change to another app or the launcher. Keeps QR scanning and the in-app emergency task usable. | Enable QRAlarm alarm protections in Android Accessibility. |
| Power-off guard | Attempts Back when a recognized system power menu appears. Matches Android/Samsung dialog classes or paired English/Arabic power-off and restart labels. | Same accessibility service. |
| Block volume down | Blocks volume-down/mute keys in the alarm activity; accessibility extends key interception outside it. A short loop restores reductions below the starting alarm volume. Volume up works. Zero system volume is raised only to the minimum audible step. | No extra access for the service/activity guard; accessibility for global keys. |
| Keep ringer on | Restores normal ringer mode while ringing; restores the prior silent/vibration mode on stop when permitted. It does not change the global DND policy. | Grant Do Not Disturb access. |

The accessibility service observes window changes and keys only while the relevant protections
are active. Power-menu text is inspected only in recognized system UI packages, processed in
memory, and neither saved nor transmitted. Android Settings, permission prompts, calls and
emergency UI remain accessible. There is no transparent full-screen blocking overlay.

An ordinary app cannot guarantee blocking shutdown, force-stop, or a hardware reboot. Power-menu
recognition and activity restoration depend on Android, OEM firmware, and language. This is
best-effort support, not a device-owner/kiosk implementation. Other enabled accessibility
services can take precedence over key filtering.

Protections end on snooze/dismiss/service destruction. An active-session state is held only in
memory: a stale database running flag cannot enable protections after process death. Alarm
volume and ringer changes are restored on orderly shutdown; Android cannot run cleanup after a
hard process kill, power loss, or forced reboot. When a second alarm replaces the first, the old
session is disarmed and its audio state restored before starting the new one.

## Galaxy S25 Ultra / One UI setup and device validation

The Samsung-specific addition is guidance shown on Samsung devices, not an unverified performance
patch. Set app battery use to Unrestricted. Remove the app from Sleeping/Deep sleeping apps and
add it to Never sleeping apps if that option is available. Check notifications, exact alarms and
full-screen alarm access. The wording of these settings varies by One UI version. A sideloaded
build may require Allow restricted settings in App info before accessibility can be enabled.

Test each setting independently and together on the actual phone:

1. Set a near-future QR alarm. Save, reopen, restart the app, and verify the four switches persist.
2. With all four off, verify ordinary volume/ringer behavior is unchanged.
3. With volume protection on, test buttons and the volume slider. Test both custom volume and
   zero system alarm volume. Confirm volume up works and prior volume returns after dismissal.
4. With ringer protection on, start from vibrate/silent with policy access granted and denied.
   Revoke access during ringing. Confirm no crash and restoration when access permits it.
5. While ringing, try Home, Recents, screen lock/unlock, the power menu, the QR scanner, temporary
   mute and the emergency task. Verify system permission prompts and calls remain usable.
6. Snooze/dismiss from every supported path, then verify normal app navigation and hardware keys.
7. Schedule overlapping alarms with different protections. Confirm the new alarm replaces the old
   and an old alarm screen cannot stop the new session.
8. Leave a scheduled alarm overnight with the screen locked, usual Sleep/DND mode and battery
   saver settings. Test after reboot, including before first unlock (accessibility may be unavailable
   before unlock; the normal alarm service and audio protection still run).
9. Repeat the power-menu tests in English and Arabic. Record Android/One UI version and the exact
   behavior; do not infer Samsung reliability from an emulator.

## Build and tests

Use Java 21 and Android SDK 37, matching upstream. A private signing key is optional for builds:
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

Debug builds use `com.sweak.qralarm.fork.debug` so they can coexist with official QRAlarm. Release
builds retain the original application ID; they are unsigned unless `keystore.properties` exists.
A differently signed APK cannot update an existing official installation. Use the side-by-side
build during testing and keep a consistent signing key for subsequent personal updates.

No new runtime libraries are introduced. JUnit supplies local policy tests. AndroidX Test Runner
and Room Testing support the device migration test. `:app:connectedDebugAndroidTest` checks an
actual version-10 database upgrade. Room exports the version-11 schema during compilation; keep
the generated schema with future migration changes. GitHub Actions compiles debug/release,
executes the unit tests, runs lint, and uploads debug APKs, generated schemas and reports.

Implementation references:

- Shirish's experimental guard informed the investigation; this implementation separates the
  four options and removes the debug-only, maximum-volume and blocking-overlay behavior:
  https://github.com/shirish2121/qralarm-android/commit/7cba9307b16fdd759b27a5e3186110e464e10a33
- Android accessibility lifecycle and global actions:
  https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Samsung background app management:
  https://developer.samsung.com/mobile/app-management.html
