package dev.coletz.voidkeyboard

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Gestures on a touch sensitive hardware keyboard (BlackBerry KEYone / KEY2).
 *
 * The keyboard surface is a separate input device with SOURCE_TOUCHPAD. A touchpad isn't a pointer
 * source, so the app's ViewRootImpl hands its events to the IME (onGenericMotionEvent) before the
 * app sees them. BlackBerry's framework converts the events nobody consumes into mouse wheel
 * scrolls (ViewRootImpl.SyntheticTouchKeypadHandler): that's why typing scrolls lists, and
 * consuming the events is all it takes to stop it.
 *
 * - flick left: delete the word before the cursor
 * - double tap (touching without pressing keys): toggle cursor mode, where dragging moves the cursor.
 *   Pressing a key other than a modifier also leaves cursor mode, and the key is typed.
 *
 * Touches that overlap a key press are typing, never a gesture.
 */
class TouchKeyboardGestures(
    private val deviceName: String,
    private val settings: TouchKeyboardSettings,
    private val listener: Listener
) {

    interface Listener {
        fun onDeleteWord()

        /** One cursor step, as a DPAD keycode */
        fun onMoveCursor(keyCode: Int)

        fun onCursorModeChanged(enabled: Boolean)
    }

    var cursorMode = false
        private set

    private var deviceId = NO_DEVICE
    private var unitsPerMm = DEFAULT_UNITS_PER_MM

    private var tracking = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var maxDistance = 0f
    // A second finger or a key press during the touch: the user is typing
    private var typing = false

    // Point the cursor steps are measured from
    private var refX = 0f
    private var refY = 0f

    private var lastKeyTime = Long.MIN_VALUE / 2
    private var lastTapUpTime = NO_TAP

    fun isFromTouchKeyboard(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_TOUCHPAD)) return false
        if (event.deviceId == deviceId) return true
        val device = event.device ?: return false
        if (device.name != deviceName) return false
        deviceId = device.id
        device.getMotionRange(MotionEvent.AXIS_X)?.resolution?.takeIf { it > 0f }?.let { unitsPerMm = it }
        "Touch keyboard found: id=$deviceId, units/mm=$unitsPerMm".log()
        return true
    }

    fun onMotionEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (cursorMode) {
                    // The newest finger drives the cursor, e.g. the other thumb while one holds SHIFT
                    trackPointer(event, event.actionIndex)
                } else {
                    typing = true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    val remaining = (0 until event.pointerCount).firstOrNull { it != event.actionIndex }
                    if (remaining != null && cursorMode) trackPointer(event, remaining) else tracking = false
                }
            }
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP -> onUp(event)
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
    }

    /** Every physical key down: the touch it belongs to is typing, not a gesture */
    fun onKeyDown(eventTime: Long, isModifier: Boolean) {
        lastKeyTime = eventTime
        lastTapUpTime = NO_TAP
        // In cursor mode this only rules out a tap: the other finger can keep dragging
        typing = true
        // Modifiers stay usable in cursor mode: SHIFT + drag selects
        if (!isModifier) setCursorMode(false)
    }

    fun reset() {
        tracking = false
        lastTapUpTime = NO_TAP
        setCursorMode(false)
    }

    private fun onDown(event: MotionEvent) {
        tracking = true
        trackPointer(event, 0)
        downTime = event.eventTime
        downX = refX
        downY = refY
        maxDistance = 0f
        typing = event.eventTime - lastKeyTime < KEY_SUPPRESSION_MS
    }

    private fun trackPointer(event: MotionEvent, index: Int) {
        activePointerId = event.getPointerId(index)
        refX = event.getX(index)
        refY = event.getY(index)
    }

    private fun onMove(event: MotionEvent) {
        if (!tracking) return
        val index = event.findPointerIndex(activePointerId)
        if (index < 0) return
        val x = event.getX(index)
        val y = event.getY(index)
        maxDistance = maxOf(maxDistance, hypot(x - downX, y - downY))
        if (cursorMode) stepCursor(x, y)
    }

    private fun onUp(event: MotionEvent) {
        if (!tracking) return
        tracking = false
        val index = event.findPointerIndex(activePointerId)
        if (typing || index < 0) {
            lastTapUpTime = NO_TAP
            return
        }

        val dxMm = (event.getX(index) - downX) / unitsPerMm
        val dyMm = (event.getY(index) - downY) / unitsPerMm
        val duration = event.eventTime - downTime
        "Touch keyboard gesture: dx=%.1fmm dy=%.1fmm max=%.1fmm in %dms".format(
            dxMm, dyMm, maxDistance / unitsPerMm, duration
        ).log()

        val isTap = maxDistance / unitsPerMm <= TAP_SLOP_MM && duration <= TAP_MAX_MS
        val isFlickLeft = dxMm <= -FLICK_MIN_MM && abs(dyMm) < abs(dxMm) * FLICK_MAX_SLOPE && duration <= FLICK_MAX_MS
        when {
            isTap -> onTap(event.eventTime)
            // In cursor mode every drag moves the cursor
            isFlickLeft && !cursorMode && settings.swipeToDeleteEnabled -> {
                "Flick left: delete word".log()
                lastTapUpTime = NO_TAP
                listener.onDeleteWord()
            }
            else -> lastTapUpTime = NO_TAP
        }
    }

    private fun onTap(upTime: Long) {
        if (lastTapUpTime != NO_TAP && downTime - lastTapUpTime <= DOUBLE_TAP_MS) {
            lastTapUpTime = NO_TAP
            // Turning it off always works, even if the option was disabled while cursor mode was on
            if (cursorMode || settings.cursorModeEnabled) {
                "Double tap: cursor mode ${if (cursorMode) "off" else "on"}".log()
                setCursorMode(!cursorMode)
            }
        } else {
            lastTapUpTime = upTime
        }
    }

    private fun stepCursor(x: Float, y: Float) {
        val stepX = CURSOR_STEP_X_MM * unitsPerMm
        val stepY = CURSOR_STEP_Y_MM * unitsPerMm
        while (true) {
            val stepsX = (x - refX) / stepX
            val stepsY = (y - refY) / stepY
            when {
                abs(stepsX) >= 1f && abs(stepsX) >= abs(stepsY) -> {
                    val sign = if (stepsX > 0) 1 else -1
                    listener.onMoveCursor(if (sign > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT)
                    refX += sign * stepX
                    // Vertical drift during a horizontal drag must not jump lines
                    refY = y
                }
                abs(stepsY) >= 1f -> {
                    val sign = if (stepsY > 0) 1 else -1
                    listener.onMoveCursor(if (sign > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP)
                    refY += sign * stepY
                    refX = x
                }
                else -> return
            }
        }
    }

    private fun setCursorMode(enabled: Boolean) {
        if (cursorMode == enabled) return
        cursorMode = enabled
        listener.onCursorModeChanged(enabled)
    }

    private companion object {
        const val NO_DEVICE = Int.MIN_VALUE
        const val NO_TAP = -1L

        // touch_keypad on the KEYone reports 21 units/mm
        const val DEFAULT_UNITS_PER_MM = 21f

        // Same as the stock BlackBerry keyboard (config_default_CKB_swipe_gesture_suppression_timeout)
        const val KEY_SUPPRESSION_MS = 350L

        const val FLICK_MIN_MM = 7f
        const val FLICK_MAX_MS = 500L
        // Max |dy| / |dx| of a horizontal flick
        const val FLICK_MAX_SLOPE = 0.6f

        const val TAP_SLOP_MM = 1.5f
        const val TAP_MAX_MS = 250L
        const val DOUBLE_TAP_MS = 350L

        // The keyboard is ~51mm wide and ~15mm tall
        const val CURSOR_STEP_X_MM = 3f
        const val CURSOR_STEP_Y_MM = 5f
    }
}
