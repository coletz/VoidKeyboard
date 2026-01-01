package dev.coletz.voidkeyboard

sealed class SymKeyState {
    data object Up : SymKeyState()
    data object Down : SymKeyState()
    data object UsedAsModifier : SymKeyState()
}

enum class ShiftState { OFF, HELD, CAPS_LOCK }
