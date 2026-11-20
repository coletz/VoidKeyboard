package dev.coletz.voidkeyboard

import android.content.Context
import androidx.core.content.edit

/**
 * User options for the touch sensitive keyboard gestures. ImeService and SettingsActivity run in the same
 * process and share the SharedPreferences instance, so a change applies to the next gesture.
 */
class TouchKeyboardSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Double tap on the keyboard toggles cursor mode */
    var cursorModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CURSOR_MODE, true)
        set(value) = prefs.edit { putBoolean(KEY_CURSOR_MODE, value) }

    /** Flick left on the keyboard deletes the word before the cursor */
    var swipeToDeleteEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_TO_DELETE, true)
        set(value) = prefs.edit { putBoolean(KEY_SWIPE_TO_DELETE, value) }

    private companion object {
        const val PREFS_NAME = "touch_keyboard"
        const val KEY_CURSOR_MODE = "cursor_mode_enabled"
        const val KEY_SWIPE_TO_DELETE = "swipe_to_delete_enabled"
    }
}
