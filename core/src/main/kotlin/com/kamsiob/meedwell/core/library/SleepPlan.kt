package com.kamsiob.meedwell.core.library

/**
 * The sleep timer's arithmetic.
 *
 * Kept out of Android so the one thing that must not be wrong, when it actually
 * stops, can be proved without a device or a two hour wait.
 *
 * **The fade is disclosed with its real clock time.** A timer that says "45
 * minutes" and starts fading at 44 has lied by a minute, and the person it lied
 * to was trying to fall asleep. So the screen states when the fade begins, and
 * that figure comes from here.
 */
object SleepPlan {

    /**
     * How long the fade to silence lasts.
     *
     * Ninety seconds. Long enough that it is never noticed as an event, short
     * enough that it does not eat a meaningful part of a fifteen minute timer.
     */
    const val FADE_SECONDS = 90L

    /** The presets the screen offers, in minutes. */
    // Up to three hours, because this player's whole audience listens to
    // long-form work. Sixty minutes as a ceiling was shorter than a Bruckner.
    val PRESETS = listOf(15, 30, 45, 60, 90, 120, 180)

    /** The slider's range, in minutes: five minutes to three hours. */
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 180

    /**
     * The gain to apply at a given moment, 1 down to 0.
     *
     * Linear in amplitude across the fade rather than in decibels, because a
     * decibel-linear fade spends most of its length nearly silent and the last
     * thing anybody hears is an abrupt disappearance.
     */
    fun gainAt(secondsRemaining: Long): Float = when {
        secondsRemaining <= 0 -> 0f
        secondsRemaining >= FADE_SECONDS -> 1f
        else -> secondsRemaining.toFloat() / FADE_SECONDS
    }

    /** Whether the fade has started. */
    fun isFading(secondsRemaining: Long): Boolean =
        secondsRemaining in 1..FADE_SECONDS

    /** The countdown, as a clock. Never negative, never "0:-3". */
    fun countdown(secondsRemaining: Long): String {
        val s = secondsRemaining.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * When the fade begins, as minutes and seconds from now.
     *
     * Returns null when the timer is shorter than the fade itself, in which
     * case the whole thing is a fade and saying "fades at 0:00" would be noise.
     */
    fun fadeBeginsIn(secondsRemaining: Long): Long? =
        (secondsRemaining - FADE_SECONDS).takeIf { it > 0 }
}
