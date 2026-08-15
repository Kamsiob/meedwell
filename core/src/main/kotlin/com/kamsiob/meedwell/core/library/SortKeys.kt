package com.kamsiob.meedwell.core.library

/**
 * Sort keys and the A to Z index.
 *
 * Pure logic, so it lives in `:core` and is tested without a device. The
 * orchestration that reads and writes the database lives in `:app`; only the
 * rules live here. That split is what "the module boundary delivers most of the
 * portability benefit" actually means in practice.
 */
object SortKeys {

    /**
     * Leading articles, stripped so "The Long Static" files under L.
     *
     * English only, matching what the app ships in version one. Bandcamp's
     * `getArtists` returns an `ignoredArticles` field which is **empty** on
     * Bandcamp, so the server offers no help here and the client decides.
     */
    private val ARTICLES = listOf("the ", "a ", "an ")

    /**
     * Turns a display name into something sortable.
     *
     * Lowercased, leading article removed, punctuation that a person would not
     * count dropped from the front. The result is stored on the row rather than
     * computed at query time, because the A to Z rail builds its index once and
     * must agree exactly with the order the list is in.
     */
    fun sortKey(name: String): String {
        // Drop the punctuation a person ignores when alphabetising, everywhere
        // rather than only at the front. Stripping it only at the front files
        // "(Sandy) Alex G" under S correctly but then sorts it as
        // "sandy) alex g", which lands it in the wrong place among its
        // neighbors. Letters, digits and single spaces are what remain.
        val cleaned = buildString {
            var lastWasSpace = true
            for (ch in name.trim().lowercase()) {
                when {
                    ch.isLetterOrDigit() -> {
                        append(ch); lastWasSpace = false
                    }
                    !lastWasSpace -> {
                        append(' '); lastWasSpace = true
                    }
                }
            }
        }.trim()

        var s = cleaned
        for (article in ARTICLES) {
            if (s.startsWith(article)) {
                s = s.removePrefix(article).trimStart()
                break
            }
        }
        // A name made entirely of punctuation, such as "!!!", would otherwise
        // become empty and file somewhere unfindable. Fall back to the name
        // itself so it at least sorts consistently.
        return s.ifEmpty { name.trim().lowercase() }
    }

    /**
     * The letter a row files under in the fast scroller.
     *
     * Anything that is not a Latin letter files under "#", which is the
     * honest answer: inventing a letter for a title in another script would put
     * it somewhere a person could never find it.
     */
    fun indexLetter(sortKey: String): String {
        val first = sortKey.firstOrNull() ?: return "#"
        return if (first in 'a'..'z') first.uppercase() else "#"
    }

    /**
     * Builds the A to Z index once, from the list in the order it is displayed.
     *
     * Returns the letters present and the position each one starts at. Built
     * once and cached rather than recomputed on scroll, which is what keeps the
     * rail usable on a large library.
     */
    fun buildIndex(sortKeys: List<String>): List<IndexEntry> {
        val entries = mutableListOf<IndexEntry>()
        var last: String? = null
        sortKeys.forEachIndexed { position, key ->
            val letter = indexLetter(key)
            if (letter != last) {
                entries += IndexEntry(letter, position)
                last = letter
            }
        }
        return entries
    }

    data class IndexEntry(val letter: String, val position: Int)
}
