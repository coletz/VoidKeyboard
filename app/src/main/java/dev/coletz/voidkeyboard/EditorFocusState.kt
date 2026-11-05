package dev.coletz.voidkeyboard

// Written by ImeService and read by KeyNavigationService, both on the main thread of the same process
object EditorFocusState {
    // False when VoidKeyboard is not the current IME: the focus state is then unknown
    var imeRunning = false
    var editorFocused = false
}
