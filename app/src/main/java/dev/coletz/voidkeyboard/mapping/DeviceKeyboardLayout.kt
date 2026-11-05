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
}
