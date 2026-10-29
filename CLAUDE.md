# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

VoidKeyboard is an Android input method (IME) for devices with a **physical hardware keyboard**. It has no on-screen QWERTY layout. It works by intercepting hardware key events in `InputMethodService.onKeyDown/onKeyUp`. The only on-screen UI is a symbols keyboard that the SYM key toggles, plus overlay popups.

Single module `:app`, package `dev.coletz.voidkeyboard`, Kotlin, minSdk 34, no Compose and no DI. Debug builds use the application ID suffix `.dev`, so they install alongside release builds.

## Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # install dev.coletz.voidkeyboard.dev on a connected device
./gradlew assembleRelease        # minified release build
./gradlew lint                   # Android lint
```

AGP 8.13 / Gradle 8.13 don't run on very new JDKs. If the default `java` is too new, set `JAVA_HOME` to JDK 17 or 21.

```bash
adb logcat -s VoidKeyboard       # all app logs (see Logger.kt)
```

There are no tests yet: `app/src/test` and `app/src/androidTest` are empty. `app/release/` (APK and baseline profiles) is committed, so a release build shows up as modified files in git.

## Architecture

**`ImeService`** holds nearly all of the logic, as one state machine over hardware keys:
- **Character input**: `KeyCodeMapper` maps Android keycodes to a primary char and a secondary char (the secondary one is used while ALT is held). The service commits text itself and does not let the framework handle letter keys.
- **Capitalization**: `shouldCapitalize()` combines `ShiftState` (OFF / HELD / CAPS_LOCK; a double-tap within 500ms locks it) with `InputConnection.getCursorCapsMode()`. Auto-caps therefore happens only where the editor asks for it (`textCapSentences`/`Words`/`Characters`).
- **Long-press alternatives**: on key down the base char is committed right away and the key is tracked with `event.startTracking()`. When `onKeyLongPress` fires, the committed char is deleted and `AlternativesDialog` opens. While it is open, a key that maps to a digit 1–9 via ALT picks that alternative and BACK just closes the popup. Any other key closes it and is then processed normally. `trackedKeys` stops repeats and stray key-ups during this flow: `CONSUMED` marks keys whose key-up must not reach the app.
- **SYM key**: it is both a toggle and a modifier. A tap (down then up with no other key) toggles the on-screen `R.xml.sym_keyboard`, using the deprecated `KeyboardView`. SYM held with another key is used as a shortcut: T/F/G/H = arrows, R/Y = Home/End, W = select word, Space = IME picker. Any other key is sent with Ctrl held. `SymKeyState.UsedAsModifier` keeps the toggle from firing on release.
- **Voice input**: `CustomKeyCode.KEYCODE_MICROPHONE` (667) starts `SpeechRecognizer` (it does not fire while ALT is held), and pressing it again stops it. If the RECORD_AUDIO permission is missing, it is requested through `MicrophonePermissionActivity`, a transparent activity, because an IME service cannot request permissions directly.

`CustomKeyCode` values (666 emoji, 667 microphone) are non-standard keycodes that the target device's hardware sends.

**Popups** (`AlternativesDialog`, `VoiceInputDialog`) are `Dialog`s built in code inside a `TYPE_APPLICATION_OVERLAY` window context. They need the `SYSTEM_ALERT_WINDOW` permission. `SettingsActivity` asks for notifications and then for the overlay permission on launch. If the overlay permission is missing when a popup is needed, `OverlayPermissionHelper` posts a notification that links to the grant screen. It opens the settings screen directly only when notifications are disabled.

**Languages = IME subtypes.** Languages are declared as `<subtype>` entries in `res/xml/method.xml`. Each `imeSubtypeLocale` string is looked up in `AlternativesProvider.localeToAltKeys`, which points to the maps in `mapping/Alternatives.kt`. A locale with no map is treated as the special `"full"` subtype (`ImeUtils.DEFAULT_LOCALE`), which merges every language's alternatives. To add a language:
1. add a subtype in `method.xml`,
2. add its label string in `values/` and `values-it/`,
3. add a map in `Alternatives` and register it in `localeToAltKeys`.

`AlternativesProvider` caches the subtype list for the lifetime of the process.

`SettingsActivity` is the launcher activity. It only links to the system screens for enabling the IME and its subtypes, and shows which language is currently selected.

User-facing strings have English (`values/`) and Italian (`values-it/`) versions; keep both in sync.
