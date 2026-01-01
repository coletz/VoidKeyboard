package dev.coletz.voidkeyboard.mapping

import dev.coletz.voidkeyboard.AltKeyCharacters

data class LanguageOption(
    val locale: String,
    val displayName: String,
    val mapping: Map<Char, AltKeyCharacters>?
)