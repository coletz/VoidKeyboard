package dev.coletz.voidkeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import dev.coletz.voidkeyboard.mapping.AlternativesProvider
import dev.coletz.voidkeyboard.mapping.KeyCodeMapper

@Suppress("DEPRECATION")
class ImeService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private enum class KeyState { TRACKING, LONG_PRESS_HANDLED }

    private var shiftState = ShiftState.OFF
    private var lastShiftPressTime = 0L
    private var symKeyState: SymKeyState = SymKeyState.Up
    private var symKeyHeld = false
    private var symKeyboardVisible = false
    private var altKeyDown = false

    private val trackedKeys = mutableMapOf<Int, KeyState>()

    private var alternativesDialog: AlternativesDialog? = null
    private var currentAlternatives: List<Char>? = null
    private var keyboardView: KeyboardView? = null
    private var cursorAnchorInfo: CursorAnchorInfo? = null

    private fun getCurrentAlternativesMap(): Map<Char, AltKeyCharacters> {
        return AlternativesProvider.getAltKeysMap(this, ImeUtils.getSelectedLocale(this))
    }

    override fun onCreateInputView(): android.view.View {
        val view = KeyboardView(this, null).apply {
            keyboard = Keyboard(this@ImeService, R.xml.sym_keyboard)
            setOnKeyboardActionListener(this@ImeService)
        }
        keyboardView = view
        return view
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return super.onEvaluateInputViewShown() || symKeyboardVisible
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        symKeyboardVisible = false
    }

    override fun onStartInput(
        attribute: android.view.inputmethod.EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInput(attribute, restarting)
        resetState()
        currentInputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(info)
        cursorAnchorInfo = info
    }

    private fun resetState() {
        shiftState = ShiftState.OFF
        trackedKeys.clear()
        symKeyState = SymKeyState.Up
        if (symKeyboardVisible) {
            symKeyboardVisible = false
            requestHideSelf(0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event ?: return super.onKeyDown(keyCode, event)
        "onKeyDown: keyCode=$keyCode (${KeyEvent.keyCodeToString(keyCode)})".log()

        val keyState = trackedKeys[keyCode]
        if (keyState == KeyState.LONG_PRESS_HANDLED) {
            return true
        }
        if (keyState == KeyState.TRACKING && event.repeatCount > 0) {
            return true
        }

        if (alternativesDialog?.isShowing() == true) {
            if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
                altKeyDown = true
                return true
            }
            val index = KeyCodeMapper.toChar(keyCode, useSecondary = altKeyDown)?.digitToIntOrNull()
            if (index != null && index in 1..9) {
                currentAlternatives?.getOrNull(index - 1)?.let { commitCharacter(it) }
            }
            dismissAlternativesPopup()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (symKeyboardVisible) {
                symKeyboardVisible = false
                requestHideSelf(0)
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_SYM) {
            "SYM key DOWN detected".log()
            symKeyState = SymKeyState.Down
            symKeyHeld = true
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
            altKeyDown = true
            return true
        }

        if (symKeyHeld) {
            symKeyState = SymKeyState.UsedAsModifier
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                inputMethodManager.showInputMethodPicker()
            } else {
                sendCtrlKey(keyCode)
            }
            return true
        }

        if (isShiftKey(keyCode)) {
            handleShiftDown()
            return super.onKeyDown(keyCode, event)
        }

        val char = KeyCodeMapper.toChar(keyCode, useSecondary = altKeyDown)?.lowercaseChar()
        if (char != null && event.repeatCount == 0) {
            // Track for long press only if character has alternatives
            if (!altKeyDown && getCurrentAlternativesMap().containsKey(char)) {
                trackedKeys[keyCode] = KeyState.TRACKING
                event.startTracking()
            }
            commitCharacter(char)
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun isShiftKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SYM) {
            "SYM key UP detected, state: $symKeyState".log()
            if (symKeyState == SymKeyState.Down) {
                toggleSymKeyboard()
            }
            symKeyState = SymKeyState.Up
            symKeyHeld = false
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT) {
            altKeyDown = false
            return true
        }

        if (isShiftKey(keyCode)) {
            handleShiftUp()
            return true
        }

        // Consume key up events when SYM is held
        if (symKeyHeld) {
            return true
        }

        val keyState = trackedKeys.remove(keyCode)

        return when (keyState) {
            KeyState.TRACKING -> true
            KeyState.LONG_PRESS_HANDLED -> true
            null -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        val keyState = trackedKeys[keyCode]
        if (keyState != KeyState.TRACKING) {
            return super.onKeyLongPress(keyCode, event)
        }

        val char = KeyCodeMapper.toChar(keyCode, useSecondary = altKeyDown)?.lowercaseChar()
            ?: return super.onKeyLongPress(keyCode, event)
        val alternatives = getCurrentAlternativesMap()[char]
            ?: return super.onKeyLongPress(keyCode, event)

        trackedKeys[keyCode] = KeyState.LONG_PRESS_HANDLED

        // Delete the character that was committed on key down
        currentInputConnection?.deleteSurroundingText(1, 0)

        showAlternativesPopup(alternatives)
        return true
    }

    private fun sendCtrlKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON))
    }

    private fun handleShiftDown() {
        val now = System.currentTimeMillis()
        val isDoubleTap = now - lastShiftPressTime < 500

        shiftState = when (shiftState) {
            ShiftState.OFF -> {
                if (isDoubleTap) ShiftState.CAPS_LOCK else ShiftState.HELD
            }
            ShiftState.HELD -> ShiftState.HELD // Already held, ignore repeat
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftPressTime = now
    }

    private fun handleShiftUp() {
        if (shiftState == ShiftState.HELD) {
            shiftState = ShiftState.OFF
        }
    }

    private fun toggleSymKeyboard() {
        "toggleSymKeyboard called, current visible: $symKeyboardVisible".log()
        symKeyboardVisible = !symKeyboardVisible
        if (symKeyboardVisible) {
            requestShowSelf(0)
        } else {
            requestHideSelf(0)
        }
    }

    private fun showAlternativesPopup(alternatives: AltKeyCharacters) {
        val cursorX = cursorAnchorInfo?.insertionMarkerHorizontal?.toInt()
        val alternativeChars = if (shouldCapitalize()) alternatives.uppercase else alternatives.lowercase
        currentAlternatives = alternativeChars
        alternativesDialog = AlternativesDialog(
            context = this,
            alternatives = alternativeChars,
            cursorX = cursorX,
            onCharacterSelected = { char -> commitCharacter(char) }
        ).also { it.show() }
    }

    private fun dismissAlternativesPopup() {
        alternativesDialog?.dismiss()
        alternativesDialog = null
        currentAlternatives = null
    }

    private fun commitCharacter(char: Char) {
        val ic = currentInputConnection ?: return
        val charToCommit = if (shouldCapitalize()) char.uppercaseChar() else char
        ic.commitText(charToCommit.toString(), 1)
    }

    private fun shouldCapitalize(): Boolean {
        if (!isAutoCapitalizationAllowed()) return shiftState != ShiftState.OFF
        if (shiftState != ShiftState.OFF) return true

        val ic = currentInputConnection ?: return false
        val textBefore = ic.getTextBeforeCursor(2, 0) ?: return true

        if (textBefore.isEmpty()) return true // No character before, start of the input
        if (textBefore.isBlank()) return false // There are 1/2 spaces, this is not the start

        val lastChar = textBefore.lastOrNull() ?: return true
        if (lastChar.isWhitespace() && textBefore.length >= 2) {
            val charBeforeSpace = textBefore[textBefore.length - 2]
            return charBeforeSpace in listOf('.', '!', '?')
        }
        return false
    }

    private fun isAutoCapitalizationAllowed(): Boolean {
        val editorInfo = currentInputEditorInfo ?: return true
        val inputType = editorInfo.inputType and InputType.TYPE_MASK_CLASS
        val variation = editorInfo.inputType and InputType.TYPE_MASK_VARIATION

        if (inputType != InputType.TYPE_CLASS_TEXT) return true

        return variation !in listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        )
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_DONE -> ic.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
            else -> commitCharacter(primaryCode.toChar())
        }
    }

    override fun onText(text: CharSequence?) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    override fun onDestroy() {
        dismissAlternativesPopup()
        super.onDestroy()
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)

        val langDisplayName = AlternativesProvider.getAvailableLanguages(this)
            .firstOrNull { it.locale == newSubtype?.locale }
            ?.displayName
            ?: return

        Toast.makeText(this, getString(R.string.ime_changed_toast, langDisplayName), Toast.LENGTH_SHORT).show()
    }
}
