package dev.coletz.voidkeyboard

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
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
        val currentImeInfo = context.inputMethodManager.currentInputMethodInfo ?: return context.getString(R.string.ime_unknown)
        if (currentImeInfo.packageName != context.packageName) {
            val otherAppLocale = context.inputMethodManager.currentInputMethodSubtype
            return context.getString(
                R.string.other_app_selected_locale,
                otherAppLocale?.locale ?: "",
                currentImeInfo.packageName
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
}

val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

