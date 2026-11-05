package dev.coletz.voidkeyboard

import android.util.Log

private const val TAG = "VoidKeyboard"

fun Any?.log() {
    if (BuildConfig.DEBUG) {
        Log.d(TAG, this?.toString() ?: "[NULL]")
    }
}
