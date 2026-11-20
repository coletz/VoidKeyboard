package dev.coletz.voidkeyboard.mapping

/**
 * What a device's hardware keyboard sends. Each product flavor provides an
 * `object DeviceKeyboard : DeviceKeyboardLayout` in its own source set (app/src/<flavor>/java).
 */
interface DeviceKeyboardLayout {
    /** Primary character and the one typed while ALT is held, for each hardware keycode */
    val keyMappings: Map<Int, KeyMapping>

    /** Keycode of the dedicated voice input key, null if the device has none */
    val microphoneKeyCode: Int?

    /**
     * Name of the input device (as in `adb shell getevent -lp`) that reports touches on the
     * keyboard surface as a SOURCE_TOUCHPAD, null if the keyboard is not touch sensitive
     */
    val touchKeyboardDeviceName: String?
}
