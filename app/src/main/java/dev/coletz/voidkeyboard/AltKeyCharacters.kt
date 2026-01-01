package dev.coletz.voidkeyboard

data class AltKeyCharacters(
    val lowercase: List<Char>
) {
    val uppercase: List<Char> = lowercase.map { it.uppercaseChar() }
}