package dev.coletz.voidkeyboard

import android.content.ComponentName
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import dev.coletz.voidkeyboard.mapping.AlternativesProvider

object ImeUtils {
    const val DEFAULT_LOCALE = "full"

    fun getSelectedLocale(context: Context): String {
        val currentSubtype = context.inputMethodManager.currentInputMethodSubtype
        val currentLocale = currentSubtype?.locale ?: context.resources.configuration.locales[0].toString()
        val availableAltKeys = AlternativesProvider.getAvailableLanguages(context)
        return if (availableAltKeys.any { it.locale == currentLocale }) {
            currentLocale
        } else {
            DEFAULT_LOCALE
        }
    }

    fun getSelectedImeInfo(context: Context): String {
        val currentImePackage = getCurrentImePackage(context) ?: return context.getString(R.string.ime_unknown)
        if (currentImePackage != context.packageName) {
            val otherAppLocale = context.inputMethodManager.currentInputMethodSubtype
            return context.getString(
                R.string.other_app_selected_locale,
                otherAppLocale?.locale ?: "",
                currentImePackage
            )
        }
        val currentSubtype = context.inputMethodManager.currentInputMethodSubtype
        val currentLocale = currentSubtype?.locale ?: context.resources.configuration.locales[0].toString()
        val languages = AlternativesProvider.getAvailableLanguages(context)
        val displayName = languages.firstOrNull { it.locale == currentLocale }?.displayName
            ?: languages.firstOrNull()?.displayName
            ?: currentLocale

        return displayName
    }

    private fun getCurrentImePackage(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return context.inputMethodManager.currentInputMethodInfo?.packageName
        }
        // getCurrentInputMethodInfo() is API 34, before that the current IME id is in the secure settings
        val imeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?: return null
        return ComponentName.unflattenFromString(imeId)?.packageName
    }
}

val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

