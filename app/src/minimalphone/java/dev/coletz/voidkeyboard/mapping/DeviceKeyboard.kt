package dev.coletz.voidkeyboard.mapping

import android.view.KeyEvent

object DeviceKeyboard : DeviceKeyboardLayout {

    // Non-standard keycodes sent by the Minimal Phone keyboard
    private const val KEYCODE_EMOJI = 666
    private const val KEYCODE_MICROPHONE = 667

    override val microphoneKeyCode: Int? = KEYCODE_MICROPHONE

    override val keyMappings: Map<Int, KeyMapping> = mapOf(
        KeyEvent.KEYCODE_A to KeyMapping('a', '@'),
        KeyEvent.KEYCODE_B to KeyMapping('b', '%'),
        KeyEvent.KEYCODE_C to KeyMapping('c', '9'),
        KeyEvent.KEYCODE_D to KeyMapping('d', '5'),
        KeyEvent.KEYCODE_E to KeyMapping('e', '2'),
        KeyEvent.KEYCODE_F to KeyMapping('f', '6'),
        KeyEvent.KEYCODE_G to KeyMapping('g', '='),
        KeyEvent.KEYCODE_H to KeyMapping('h', ':'),
        KeyEvent.KEYCODE_I to KeyMapping('i', '!'),
        KeyEvent.KEYCODE_J to KeyMapping('j', ';'),
        KeyEvent.KEYCODE_K to KeyMapping('k', '\''),
        KeyEvent.KEYCODE_L to KeyMapping('l', '"'),
        KeyEvent.KEYCODE_M to KeyMapping('m', ','),
        KeyEvent.KEYCODE_N to KeyMapping('n', '?'),
        KeyEvent.KEYCODE_O to KeyMapping('o', '#'),
        KeyEvent.KEYCODE_P to KeyMapping('p', '$'),
        KeyEvent.KEYCODE_Q to KeyMapping('q', '&'),
        KeyEvent.KEYCODE_R to KeyMapping('r', '3'),
        KeyEvent.KEYCODE_S to KeyMapping('s', '4'),
        KeyEvent.KEYCODE_T to KeyMapping('t', '_'),
        KeyEvent.KEYCODE_U to KeyMapping('u', '+'),
        KeyEvent.KEYCODE_V to KeyMapping('v', '*'),
        KeyEvent.KEYCODE_W to KeyMapping('w', '1'),
        KeyEvent.KEYCODE_X to KeyMapping('x', '8'),
        KeyEvent.KEYCODE_Y to KeyMapping('y', '-'),
        KeyEvent.KEYCODE_Z to KeyMapping('z', '7'),
        KEYCODE_EMOJI to KeyMapping('0', '0'),
        KEYCODE_MICROPHONE to KeyMapping('.', '.')
    )
}
