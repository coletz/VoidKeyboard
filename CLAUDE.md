# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

VoidKeyboard is an Android input method (IME) for devices with a **physical hardware keyboard**. It has no on-screen QWERTY layout. It works by intercepting hardware key events in `InputMethodService.onKeyDown/onKeyUp`. The only on-screen UI is a symbols keyboard that the SYM key toggles, plus overlay popups.

Single module `:app`, package `dev.coletz.voidkeyboard`, Kotlin, no Compose and no DI. Debug builds use the application ID suffix `.dev`, so they install alongside release builds.

There is one flavor dimension, `device`, with two product flavors:
- `minimalphone` (the default) adds the suffix `.minimalphone`, so its ID is `dev.coletz.voidkeyboard.minimalphone` (`.minimalphone.dev` for debug builds).
- `blackberry` adds the suffix `.blackberry`, so its ID is `dev.coletz.voidkeyboard.blackberry` (`.blackberry.dev` for debug builds).

Code or resources specific to one flavor go in `app/src/minimalphone/` or `app/src/blackberry/`. A class defined per flavor must exist in every flavor's source set and not in `main`. Android Studio only indexes the selected build variant, so `./gradlew assembleDebug` is the way to check that every flavor still compiles.

The hardware keyboard layout is per flavor: each one has an `object DeviceKeyboard` in `mapping/` that implements the `DeviceKeyboardLayout` interface from `main` (key → char mappings and the optional microphone keycode).

## Commands

```bash
./gradlew assembleMinimalphoneDebug   # build debug APK for one flavor (also: assembleBlackberryDebug)
./gradlew installMinimalphoneDebug    # install dev.coletz.voidkeyboard.minimalphone.dev on a connected device
./gradlew installBlackberryDebug      # install dev.coletz.voidkeyboard.blackberry.dev
./gradlew assembleDebug               # debug APKs for every flavor
./gradlew assembleMinimalphoneRelease # minified release build (also: assembleBlackberryRelease)
./gradlew lint                   # Android lint
```

AGP 8.13 / Gradle 8.13 don't run on very new JDKs. If the default `java` is too new, set `JAVA_HOME` to JDK 17 or 21.

```bash
adb logcat -s VoidKeyboard       # all app logs (see Logger.kt)
```

There are no tests yet: `app/src/test` and `app/src/androidTest` are empty. `app/release/` (APK and baseline profiles) is committed, so a release build shows up as modified files in git.

## Architecture

**`ImeService`** holds nearly all of the logic, as one state machine over hardware keys:
- **Character input**: `KeyCodeMapper` uses the flavor's `DeviceKeyboard.keyMappings` to map Android keycodes to a primary char and a secondary char (the secondary one is used while ALT is held). The service commits text itself and does not let the framework handle letter keys.
- **Capitalization**: `shouldCapitalize()` combines `ShiftState` (OFF / HELD / CAPS_LOCK; a double-tap within 500ms locks it) with `InputConnection.getCursorCapsMode()`. Auto-caps therefore happens only where the editor asks for it (`textCapSentences`/`Words`/`Characters`).
- **Long-press alternatives**: on key down the base char is committed right away and the key is tracked with `event.startTracking()`. When `onKeyLongPress` fires, the committed char is deleted and `AlternativesDialog` opens. While it is open, a key that maps to a digit 1–9 via ALT picks that alternative and BACK just closes the popup. Any other key closes it and is then processed normally. `trackedKeys` stops repeats and stray key-ups during this flow: `CONSUMED` marks keys whose key-up must not reach the app.
- **SYM key**: it is both a toggle and a modifier. A tap (down then up with no other key) toggles the on-screen `R.xml.sym_keyboard`, using the deprecated `KeyboardView`. SYM held with another key is used as a shortcut: T/F/G/H = arrows, R/Y = Home/End, W = select word, Space = IME picker. Any other key is sent with Ctrl held. `SymKeyState.UsedAsModifier` keeps the toggle from firing on release.
- **Voice input**: the key at `DeviceKeyboard.microphoneKeyCode` (if the device has one) starts `SpeechRecognizer` (it does not fire while ALT is held), and pressing it again stops it. If the RECORD_AUDIO permission is missing, it is requested through `MicrophonePermissionActivity`, a transparent activity, because an IME service cannot request permissions directly.

Some devices send non-standard keycodes, for example the Minimal Phone's 666 (emoji) and 667 (microphone). They are private constants of that flavor's `DeviceKeyboard`.

**Popups** (`AlternativesDialog`, `VoiceInputDialog`) are `Dialog`s built in code inside a `TYPE_APPLICATION_OVERLAY` window context. They need the `SYSTEM_ALERT_WINDOW` permission. `SettingsActivity` asks for notifications and then for the overlay permission on launch. If the overlay permission is missing when a popup is needed, `OverlayPermissionHelper` posts a notification that links to the grant screen. It opens the settings screen directly only when notifications are disabled.

**Languages = IME subtypes.** Languages are declared as `<subtype>` entries in `res/xml/method.xml`. Each `imeSubtypeLocale` string is looked up in `AlternativesProvider.localeToAltKeys`, which points to the maps in `mapping/Alternatives.kt`. A locale with no map is treated as the special `"full"` subtype (`ImeUtils.DEFAULT_LOCALE`), which merges every language's alternatives. To add a language:
1. add a subtype in `method.xml`,
2. add its label string in `values/` and `values-it/`,
3. add a map in `Alternatives` and register it in `localeToAltKeys`.

`AlternativesProvider` caches the subtype list for the lifetime of the process.

`SettingsActivity` is the launcher activity. It only links to the system screens for enabling the IME and its subtypes, and shows which language is currently selected.

User-facing strings have English (`values/`) and Italian (`values-it/`) versions; keep both in sync.
