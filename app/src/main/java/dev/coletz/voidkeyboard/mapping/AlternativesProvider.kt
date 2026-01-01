package dev.coletz.voidkeyboard.mapping

import android.content.Context
import dev.coletz.voidkeyboard.AltKeyCharacters
import dev.coletz.voidkeyboard.inputMethodManager

object AlternativesProvider {

    private val localeToAltKeys: Map<String, Map<Char, AltKeyCharacters>> = mapOf(
        "it_IT" to Alternatives.it_IT,
        "es_ES" to Alternatives.es_ES,
        "es_EXT" to Alternatives.es_EXT,
    )

    private var cachedLanguages: List<LanguageOption>? = null

    fun getAvailableLanguages(context: Context): List<LanguageOption> {
        cachedLanguages?.let { return it }

        val imm = context.inputMethodManager
        val imePackage = context.packageName

        val languages = imm.inputMethodList
            .find { it.packageName == imePackage }
            ?.let { (0 until it.subtypeCount).map(it::getSubtypeAt) }
            ?.map { subtype ->
                val locale = subtype.locale
                val displayName = subtype.getDisplayName(
                    context,
                    context.packageName,
                    context.applicationInfo
                ).toString()
                val mapping = localeToAltKeys[locale]
                LanguageOption(locale, displayName, mapping)
            }
            ?: emptyList()

        return languages.also { cachedLanguages = it }
    }

    private fun getDefaultAltKeys(context: Context): Map<Char, AltKeyCharacters> {
        return getAvailableLanguages(context)
            .mapNotNull { it.mapping }
            .flatMap { it.entries }
            .groupBy({ it.key }, { it.value.lowercase })
            .mapValues { (_, charLists) ->
                AltKeyCharacters(charLists.flatten().distinct())
            }
    }

    fun getAltKeysMap(context: Context, locale: String?): Map<Char, AltKeyCharacters> {
        val languages = getAvailableLanguages(context)
        return languages.firstOrNull { locale == it.locale }?.mapping
            ?: getDefaultAltKeys(context)
    }
}