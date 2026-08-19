package com.kamsiob.meedwell.core.library

/**
 * Tone, named by musical umbrella rather than by genre.
 *
 * The list says more by what it omits than any wording could: there is no rock,
 * no electronic, no bass anything. This app is for chamber works, solo piano,
 * Celtic strings, ambient, film and game scores, and the tone control is voiced
 * for those instruments and rooms.
 *
 * ## Every voicing is cuts only
 *
 * Not one band is ever boosted. Three reasons, and all of them are the same
 * reason in different clothes:
 *
 *  - **Nothing can clip.** A boost adds gain that was not in the master, and on
 *    a loud passage it runs out of headroom. A cut cannot.
 *  - **It is what an engineer would do.** Taking a little out of the mud is a
 *    correction; adding a shelf is a taste.
 *  - **There are no sliders**, so nothing can be pushed somewhere daft. That is
 *    a decision rather than an omission, and the screen says so.
 *
 * The curves are shallow on purpose. The largest cut anywhere is 3 dB, which is
 * a hand on the mix rather than a hand over it.
 */
enum class Voicing(
    val label: String,
    val note: String,
    /** Gains in dB at 40, 160, 800, 3k and 12k Hz. Never above zero. */
    val curve: List<Double>,
) {
    Orchestral(
        "Orchestral & Scores",
        "Symphony, opera, film and game scores",
        // A little out of the low-mid mud where a big room piles up, and a
        // touch off the upper mids where massed strings get glassy.
        listOf(-1.0, -2.0, -1.5, -0.5, 0.0),
    ),

    Strings(
        "Strings & Traditional",
        "Celtic harp and fiddle, quartets, early music",
        // Fiddle and harp both sit hard in the 2 to 4k band, which is exactly
        // where the ear is most sensitive.
        listOf(-2.0, -1.0, 0.0, -2.5, -0.5),
    ),

    Piano(
        "Piano & Keys",
        "Solo piano, harpsichord, organ",
        // Boxiness in a close-miked piano lives around 200 to 400.
        listOf(-1.5, -2.5, -1.0, -0.5, 0.0),
    ),

    Ambient(
        "Ambient & Nature",
        "Drone, minimalism, field recordings",
        // Rumble at the bottom and tape or air hiss at the top, both of which
        // are the room rather than the music.
        listOf(-3.0, -1.0, 0.0, 0.0, -1.5),
    ),

    AsRecorded(
        "As Recorded",
        "Nothing applied. What the engineer left",
        listOf(0.0, 0.0, 0.0, 0.0, 0.0),
    );

    /** Whether this voicing does anything at all. */
    val isFlat: Boolean get() = curve.all { it == 0.0 }

    companion object {
        /** The frequencies the curve is defined at, in Hz. */
        val FREQUENCIES = listOf(40, 160, 800, 3_000, 12_000)

        fun byName(name: String?): Voicing =
            entries.firstOrNull { it.name == name } ?: AsRecorded

        /**
         * The gain this voicing wants at an arbitrary frequency, in dB.
         *
         * A phone's equalizer has whatever bands its chip felt like offering,
         * usually five but sometimes ten and rarely at the frequencies above.
         * So the curve is defined once and **interpolated onto the device's own
         * bands** rather than assuming a layout. Interpolation is linear in the
         * log of frequency, because that is how hearing spaces them and how the
         * curve was drawn.
         */
        fun gainAt(curve: List<Double>, hz: Int): Double {
            if (curve.isEmpty()) return 0.0
            if (hz <= FREQUENCIES.first()) return curve.first()
            if (hz >= FREQUENCIES.last()) return curve.last()

            for (i in 0 until FREQUENCIES.lastIndex) {
                val low = FREQUENCIES[i]
                val high = FREQUENCIES[i + 1]
                if (hz in low..high) {
                    val t = (ln(hz) - ln(low)) / (ln(high) - ln(low))
                    return curve[i] + (curve[i + 1] - curve[i]) * t
                }
            }
            return curve.last()
        }

        private fun ln(v: Int): Double = kotlin.math.ln(v.toDouble())
    }
}
