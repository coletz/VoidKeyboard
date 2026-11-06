package dev.coletz.voidkeyboard

sealed class SymKeyState {
    data object Up : SymKeyState()
    data object Down : SymKeyState()
    data object UsedAsModifier : SymKeyState()
}

enum class ModifierState { OFF, HELD, LOCKED }

/**
 * A modifier key (SHIFT, ALT) that applies while held and locks with a double tap.
 * Only a tap counts toward the double tap: a press used together with another key doesn't,
 * so typing quickly with the modifier held never locks it by accident.
 */
class LockableModifier {

    var state = ModifierState.OFF
        private set

    val isActive: Boolean get() = state != ModifierState.OFF
    val isLocked: Boolean get() = state == ModifierState.LOCKED

    private var lastTapTime = 0L
    private var usedWithOtherKey = false

    fun onDown(now: Long) {
        state = when (state) {
            ModifierState.LOCKED -> ModifierState.OFF
            ModifierState.HELD -> ModifierState.HELD
            ModifierState.OFF -> {
                usedWithOtherKey = false
                if (now - lastTapTime < DOUBLE_TAP_TIMEOUT_MS) ModifierState.LOCKED else ModifierState.HELD
            }
        }
        if (state != ModifierState.HELD) lastTapTime = 0L
    }

    fun onUp(now: Long) {
        if (state == ModifierState.HELD) {
            state = ModifierState.OFF
            lastTapTime = if (usedWithOtherKey) 0L else now
        }
    }

    /** Any other key: it marks the modifier as used and breaks a pending double tap */
    fun onOtherKeyDown() {
        usedWithOtherKey = true
        lastTapTime = 0L
    }

    fun reset() {
        state = ModifierState.OFF
        lastTapTime = 0L
        usedWithOtherKey = false
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT_MS = 500L
    }
}
