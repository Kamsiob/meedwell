package com.kamsiob.meedwell.core.surroundings

/**
 * What the app is allowed to offer, and what it costs to take it.
 *
 * Pure decisions, kept out of the Android layer so the rules that matter most
 * can be tested without a device: which recordings may be offered at all, and
 * what a download will actually cost before anybody agrees to it.
 */
object Downloads {

    /**
     * The only way a recording becomes downloadable.
     *
     * **The hard rule, enforced in one place.** A manifest entry missing any
     * attribution field is invalid and its recording is never offered, however
     * good the audio is. It is also dropped if it has no checksum, because a
     * file that cannot be verified cannot be installed, and no size, because a
     * download whose cost cannot be stated cannot be consented to.
     *
     * Everything the interface shows comes through here. Nothing else in the
     * app is permitted to build a download from a raw manifest entry, so a
     * future screen cannot accidentally route around the rule.
     */
    fun offerable(manifest: SurroundingsManifest): List<SurroundingsSound> =
        manifest.sounds.filter { it.isOfferable }

    /**
     * Recordings a pack would actually deliver.
     *
     * A pack archive is fetched whole, so a pack containing anything invalid
     * cannot be offered as a pack: taking it would install a file the app is
     * not allowed to ship. Rather than silently delivering fewer recordings
     * than the pack claims, such a pack is offered sound by sound instead.
     */
    fun packIsWhole(manifest: SurroundingsManifest, packId: String): Boolean {
        val inPack = manifest.sounds.filter { it.pack == packId }
        return inPack.isNotEmpty() && inPack.all { it.isOfferable }
    }

    /**
     * What a set of recordings will cost to fetch, in bytes.
     *
     * Always computed from what is genuinely missing, never from the catalog
     * total. Somebody who already has eight of a pack's nine recordings is told
     * the cost of the ninth.
     */
    fun bytesToFetch(sounds: List<SurroundingsSound>, alreadyHave: Set<String>): Long =
        sounds.filter { it.isOfferable && it.id !in alreadyHave }.sumOf { it.fileSizeBytes }

    /**
     * A size a person can judge, in the units a phone shows them.
     *
     * Megabytes below a gigabyte, one decimal place, and never "1024 MB". This
     * is the number somebody agrees to before anything is fetched, so it is
     * rounded the way a download manager rounds rather than the way a
     * spreadsheet does.
     */
    fun humanSize(bytes: Long): String {
        if (bytes <= 0) return "nothing to fetch"
        val mib = bytes.toDouble() / (1024 * 1024)
        return when {
            mib < 0.1 -> "under 0.1 MB"
            mib < 1000 -> "%.1f MB".format(mib)
            else -> "%.2f GB".format(mib / 1024)
        }
    }

    /**
     * How long a set of recordings runs, in words.
     *
     * Hours and minutes, because these are long-form beds: several run past
     * half an hour and the whole library is measured in hours.
     */
    fun humanDuration(seconds: Long): String {
        if (seconds <= 0) return "no time at all"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours == 0L && minutes == 0L -> "under a minute"
            hours == 0L -> "$minutes min"
            minutes == 0L -> if (hours == 1L) "1 hour" else "$hours hours"
            else -> if (hours == 1L) "1 hour $minutes min" else "$hours hours $minutes min"
        }
    }

    /**
     * The sentence shown before anything is fetched.
     *
     * Size first, because that is the thing being consented to. Count and
     * running time follow, because "48.2 MB" alone does not say whether it is
     * worth it.
     */
    fun costLine(sounds: List<SurroundingsSound>, alreadyHave: Set<String>): String {
        val missing = sounds.filter { it.isOfferable && it.id !in alreadyHave }
        if (missing.isEmpty()) return "You already have all of these."
        val size = humanSize(missing.sumOf { it.fileSizeBytes })
        val time = humanDuration(missing.sumOf { it.durationSeconds })
        val count = if (missing.size == 1) "1 recording" else "${missing.size} recordings"
        return "$size · $count · $time of sound"
    }
}

/**
 * Whether this recording can be offered for download at all.
 *
 * See `Downloads.offerable` for why each condition is here. Kept on the model
 * so no caller can construct a downloadable recording without it.
 */
val SurroundingsSound.isOfferable: Boolean
    get() = attribution.isComplete &&
        sha256.isNotBlank() &&
        fileSizeBytes > 0 &&
        filename.isNotBlank()
