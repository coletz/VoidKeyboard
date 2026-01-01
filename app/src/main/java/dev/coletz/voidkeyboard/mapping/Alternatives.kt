package dev.coletz.voidkeyboard.mapping

import dev.coletz.voidkeyboard.AltKeyCharacters

object Alternatives {
    val it_IT = mapOf(
        'a' to AltKeyCharacters(listOf('à', 'á')),
        'e' to AltKeyCharacters(listOf('è', 'é')),
        'i' to AltKeyCharacters(listOf('ì', 'í')),
        'o' to AltKeyCharacters(listOf('ò', 'ó')),
        'u' to AltKeyCharacters(listOf('ù', 'ú')),
        'n' to AltKeyCharacters(listOf('ñ')),
        'c' to AltKeyCharacters(listOf('ç')),
    )

    val es_ES = mapOf(
        'a' to AltKeyCharacters(listOf('á', 'ª')),
        'e' to AltKeyCharacters(listOf('é')),
        'i' to AltKeyCharacters(listOf('í')),
        'o' to AltKeyCharacters(listOf('ó', 'º')),
        'u' to AltKeyCharacters(listOf('ú', 'ü')),
        'n' to AltKeyCharacters(listOf('ñ')),
        'c' to AltKeyCharacters(listOf('ç')),
        '?' to AltKeyCharacters(listOf('¿')),
        '!' to AltKeyCharacters(listOf('¡'))
    )

    val es_EXT = mapOf(
        // A: Accento acuto (ES), grave (CAT) e ordinale
        'a' to AltKeyCharacters(listOf('á', 'à', 'ª')),

        // E: Accento acuto (ES/GL), grave (CAT)
        'e' to AltKeyCharacters(listOf('é', 'è')),

        // I: Accento acuto (ES), dieresi (CAT, es. "veïna")
        'i' to AltKeyCharacters(listOf('í', 'ï')),

        // O: Accento acuto (ES), grave (CAT), ordinale
        'o' to AltKeyCharacters(listOf('ó', 'ò', 'º')),

        // U: Accento acuto (ES), dieresi (ES/CAT)
        'u' to AltKeyCharacters(listOf('ú', 'ü')),

        // N: La eñe è la regina indiscussa qui
        'n' to AltKeyCharacters(listOf('ñ')),

        // C: Cedilla (CAT, usata anche in francese/portoghese)
        'c' to AltKeyCharacters(listOf('ç')),

        // L: Fondamentale per il Catalano "l·l" (ela geminada).
        // Si inserisce il "punt volat" (·) tenendo premuta la L.
        'l' to AltKeyCharacters(listOf('·')),

        // Punteggiatura invertita
        '?' to AltKeyCharacters(listOf('¿')),
        '!' to AltKeyCharacters(listOf('¡'))
    )
}