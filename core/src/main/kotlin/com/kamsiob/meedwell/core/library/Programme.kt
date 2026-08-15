package com.kamsiob.meedwell.core.library

/**
 * Setting a record as a programme rather than as a track list.
 *
 * The design sets an album the way a concert programme is set: centred titling,
 * a plate line, and movements numbered in Roman numerals with a tempo marking
 * beside them. The player carries the same idea in one line, "andante · IV of
 * IX · mp", where dynamics replace numbers wherever a number would otherwise
 * do.
 *
 * **Two of those three parts are real and one is not, so only two are shown.**
 *
 *  - "IV of IX" is the track's position in its record. Real, and known.
 *  - "andante" is a tempo marking. Real *when the person who made the record
 *    put it in the title*, which for this kind of music is often. Read from the
 *    title rather than guessed.
 *  - "mp" is a dynamic marking. There is no way to know it. A file carries no
 *    such tag and measuring loudness would produce a number about the master,
 *    not a marking the composer wrote.
 *
 * Printing a plausible "mp" beside somebody's music would be inventing
 * metadata about their record and setting it in the same type as the parts that
 * are true. So it is absent, and the line is shorter on most records than the
 * grid's example. That is the grid's example being a piece that happens to
 * carry one.
 */
object Programme {

    /**
     * The tempo markings worth recognizing, longest first so "allegretto" is
     * not matched as "allegro".
     *
     * Italian, because that is what is printed on the scores this app is for.
     * The list is deliberately short: these are the markings that actually turn
     * up in the titles of chamber, piano and score releases, not a dictionary.
     */
    private val TEMPO_MARKS = listOf(
        "larghissimo", "adagissimo", "allegrissimo", "prestissimo",
        "moderato", "allegretto", "andantino", "larghetto", "sostenuto",
        "maestoso", "grazioso", "cantabile", "tranquillo",
        "adagio", "andante", "allegro", "lentissimo", "vivace", "presto",
        "largo", "lento", "grave", "molto", "assai",
    )

    /**
     * A tempo marking found in a title, or null.
     *
     * Matched on whole words only, so a record called "Andante Records" or a
     * piece called "Largo Winch" does not acquire a tempo. Returned in the
     * lower case it is printed in.
     */
    fun tempoIn(title: String): String? {
        if (title.isBlank()) return null
        val words = title.lowercase()
            .map { if (it.isLetter()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() }
            .toSet()
        return TEMPO_MARKS.firstOrNull { it in words }
    }

    /**
     * "IV of IX", or null when the position is not known.
     *
     * Both numbers have to be real. A track with no number, or a record whose
     * length is not known, gets nothing rather than "I of I", which would be a
     * confident statement about a record the app has not finished reading.
     */
    fun movement(trackNumber: Int, trackCount: Int): String? {
        if (trackNumber <= 0 || trackCount <= 0 || trackNumber > trackCount) return null
        return "${roman(trackNumber)} of ${roman(trackCount)}"
    }

    /**
     * The whole line, with only the parts that are true.
     *
     * Joined with the grid's separator. Empty when nothing is known, so a
     * caller can omit the line entirely rather than print a lone bullet.
     */
    fun line(title: String, trackNumber: Int, trackCount: Int): String =
        listOfNotNull(tempoIn(title), movement(trackNumber, trackCount)).joinToString(" · ")

    /**
     * Roman numerals, which is how movements are numbered on a programme.
     *
     * Only up to a few hundred, because a record with more than a few hundred
     * movements is not a record. Above the table it falls back to the Arabic
     * number rather than producing a wall of Ms.
     */
    fun roman(value: Int): String {
        if (value !in 1..399) return value.toString()
        val table = listOf(
            100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
            10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
        )
        var left = value
        return buildString {
            for ((number, numeral) in table) {
                while (left >= number) {
                    append(numeral)
                    left -= number
                }
            }
        }
    }
}
