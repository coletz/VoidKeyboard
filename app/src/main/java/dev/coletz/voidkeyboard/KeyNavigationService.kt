package dev.coletz.voidkeyboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

// Fraction of the window height left free above and below the swipe, away from the shade and home gestures
private const val SWIPE_MARGIN = 0.2f
private const val DRAG_DURATION_MS = 200L
private const val HOLD_DURATION_MS = 100L

// SPACE scrolls down and SHIFT+SPACE scrolls up when no editor is focused, by swiping on the active window.
// Key events reach this service before the IME, so EditorFocusState keeps it away from editors.
class KeyNavigationService : AccessibilityService() {

    private var symKeyHeld = false
    // The SPACE down was consumed, so its repeats and key up must be consumed too
    private var spaceConsumed = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SYM) {
            symKeyHeld = event.action == KeyEvent.ACTION_DOWN
            return false
        }
        if (event.keyCode != KeyEvent.KEYCODE_SPACE) return false

        if (event.action == KeyEvent.ACTION_UP) {
            return spaceConsumed.also { spaceConsumed = false }
        }
        if (event.repeatCount > 0) return spaceConsumed

        spaceConsumed = shouldScroll(event) && scroll(down = !event.isShiftPressed)
        return spaceConsumed
    }

    private fun shouldScroll(event: KeyEvent): Boolean {
        if (!EditorFocusState.imeRunning || EditorFocusState.editorFocused) return false
        // SYM+SPACE opens the IME picker, other modifiers are left to the app
        return !symKeyHeld && !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed
    }

    // Returns false when there is no active window, so SPACE still reaches the app
    private fun scroll(down: Boolean): Boolean {
        val window = rootInActiveWindow ?: return false
        val bounds = Rect().also(window::getBoundsInScreen)
        val x = bounds.exactCenterX()
        val top = bounds.top + bounds.height() * SWIPE_MARGIN
        val bottom = bounds.bottom - bounds.height() * SWIPE_MARGIN
        // Scrolling down means dragging the content up
        val (startY, endY) = if (down) bottom to top else top to bottom
        "scroll ${if (down) "down" else "up"}: swipe from $startY to $endY".log()

        val drag = GestureDescription.StrokeDescription(
            Path().apply { moveTo(x, startY); lineTo(x, endY) },
            0,
            DRAG_DURATION_MS,
            true
        )
        dispatchGesture(GestureDescription.Builder().addStroke(drag).build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // Holding still before lifting zeroes the velocity, so the list stops where the finger stops instead of flinging
                val hold = drag.continueStroke(
                    Path().apply { moveTo(x, endY); lineTo(x, endY + 1) },
                    0,
                    HOLD_DURATION_MS,
                    false
                )
                dispatchGesture(GestureDescription.Builder().addStroke(hold).build(), null, null)
            }
        }, null)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
