package dev.coletz.voidkeyboard.mapping

object KeyCodeMapper {

    fun toChar(keyCode: Int, useSecondary: Boolean = false): Char? {
        val mapping = DeviceKeyboard.keyMappings[keyCode] ?: return null
        return if (useSecondary) mapping.secondary else mapping.primary
    }
}
