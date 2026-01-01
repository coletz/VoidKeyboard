package dev.coletz.voidkeyboard

import android.util.Log

private const val TAG = "VoidKeyboard"

fun Any?.log() {
    Log.d(TAG, this?.toString() ?: "[NULL]")
}
