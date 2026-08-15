package com.kamsiob.meedwell.core.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The round trip, field by field.
 *
 * `MASTER_SPEC.md` section 8 is explicit that **equality after export, wipe and
 * import is the gate**, not whether the import completes. An import that
 * finishes and quietly loses a field is the failure this is here to catch,
 * because nobody discovers it until they go looking for something that is gone.
 *
 * So the fixture is deliberately awkward rather than tidy: unicode, an empty
 * string, a very long string, zero, a negative, ordering that must survive, and
 * a track whose file is not where it was.
 */
class BackupRoundTripTest {

    private fun awkward(): BackupFile = BackupFile(
        writtenBy = "Meedwell 1.0",
        writtenAt = 1_755_000_000,
        plays = listOf(
            BackupPlay("t:1", "a:1", playedAt = 1_700_000_000, playedSeconds = 245, completed = true),
            // Zero seconds and not completed: somebody who skipped immediately.
            // A real row that a naive "skip the empties" pass would drop.
            BackupPlay("t:2", "a:1", playedAt = 1_700_000_100, playedSeconds = 0, completed = false),
            BackupPlay("t:3", "a:2", playedAt = 1_700_000_200, playedSeconds = 3_600, completed = true),
        ),
        lovedTracks = listOf("t:1", "t:3"),
        lovedAlbums = listOf("a:2"),
        resumePoints = listOf(
            BackupResume("t:3", positionSeconds = 1_805),
            // A long piece barely started. Zero is a real position, not "none".
            BackupResume("t:9", positionSeconds = 0),
        ),
        lists = listOf(
            BackupList(
                id = "l:1",
                // Unicode, an em dash, and an apostrophe, because a file that
                // survives ASCII and mangles this is a file that fails on real
                // record titles.
                name = "Rørt — Sølvi's mixtape 🌧",
                createdAt = 1_700_000_000,
                updatedAt = 1_700_000_500,
                // Order is the whole point of a list.
                trackIds = listOf("t:3", "t:1", "t:2"),
            ),
            BackupList(id = "l:2", name = "", createdAt = 0, updatedAt = 0, trackIds = emptyList()),
        ),
        localFiles = listOf(
            BackupLocalFile("t:1", "/storage/emulated/0/Music/Artist/Album/01 Track.flac"),
            BackupLocalFile("t:2", ""),
            BackupLocalFile("t:3", "/storage/emulated/0/Music/" + "very-long-folder-name/".repeat(20) + "x.mp3"),
        ),
        watchedFolders = listOf(
            BackupFolder("content://com.android.externalstorage.documents/tree/primary%3AMusic", "Music", 1_700_000_000),
        ),
        settings = BackupSettings(
            theme = "Light",
            shelfGrid = false,
            gapless = false,
            rememberLongTrackPosition = false,
            wifiOnlyDownloads = false,
            surroundingsVolume = 0.37f,
        ),
    )

    /** The gate. Everything that went in comes back, field for field. */
    @Test
    fun `an awkward export survives the round trip exactly`() {
        val original = awkward()
        val text = BackupReader.write(original)
        val result = BackupReader.read(text)

        assertThat(result).isInstanceOf(BackupReader.Result.Ok::class.java)
        val restored = (result as BackupReader.Result.Ok).file

        assertThat(restored).isEqualTo(original)
        assertThat(result.unknownSections).isEmpty()
    }

    /** Order is data. A list whose tracks come back shuffled is a broken list. */
    @Test
    fun `list order survives`() {
        val restored = roundTrip(awkward())
        assertThat(restored.lists.first().trackIds).containsExactly("t:3", "t:1", "t:2").inOrder()
    }

    @Test
    fun `unicode survives`() {
        val restored = roundTrip(awkward())
        assertThat(restored.lists.first().name).isEqualTo("Rørt — Sølvi's mixtape 🌧")
    }

    @Test
    fun `zero and empty are preserved rather than treated as absent`() {
        val restored = roundTrip(awkward())
        assertThat(restored.plays[1].playedSeconds).isEqualTo(0)
        assertThat(restored.plays[1].completed).isFalse()
        assertThat(restored.resumePoints[1].positionSeconds).isEqualTo(0)
        assertThat(restored.lists[1].name).isEmpty()
        assertThat(restored.localFiles[1].path).isEmpty()
    }

    /** Every setting, including the ones whose default is the opposite. */
    @Test
    fun `settings survive including the non-default ones`() {
        val restored = roundTrip(awkward())
        assertThat(restored.settings.theme).isEqualTo("Light")
        assertThat(restored.settings.shelfGrid).isFalse()
        assertThat(restored.settings.gapless).isFalse()
        assertThat(restored.settings.rememberLongTrackPosition).isFalse()
        assertThat(restored.settings.wifiOnlyDownloads).isFalse()
        assertThat(restored.settings.surroundingsVolume).isWithin(1e-6f).of(0.37f)
    }

    // ---------- Refusing rather than half working ----------

    @Test
    fun `an empty file is refused`() {
        assertThat(BackupReader.read("")).isInstanceOf(BackupReader.Result.Unreadable::class.java)
    }

    @Test
    fun `something that is not JSON is refused`() {
        val result = BackupReader.read("this is my grocery list")
        assertThat(result).isInstanceOf(BackupReader.Result.Unreadable::class.java)
    }

    @Test
    fun `JSON that is not an export is refused`() {
        val result = BackupReader.read("""{"hello":"world"}""")
        assertThat((result as BackupReader.Result.Unreadable).message).contains("no format version")
    }

    /**
     * The important refusal. A file from a later version must not be half read:
     * the fields this version happens to recognize would come back and
     * everything else would vanish, which is a silent partial restore.
     */
    @Test
    fun `a file from a newer app is refused rather than partly read`() {
        val future = BackupReader.write(awkward()).replace("\"format_version\": 1", "\"format_version\": 99")
        val result = BackupReader.read(future)
        assertThat(result).isInstanceOf(BackupReader.Result.Unreadable::class.java)
        assertThat((result as BackupReader.Result.Unreadable).message).contains("newer version")
        assertThat(result.message).contains("Nothing has been changed")
    }

    /**
     * Sections this version has never heard of are named back rather than
     * dropped, so the app can say what it did not understand instead of
     * claiming a clean import.
     */
    @Test
    fun `unknown sections are reported, not silently ignored`() {
        val withExtra = BackupReader.write(awkward())
            .replaceFirst("{", """{ "smart_lists": [], "listening_streaks": {},""")
        val result = BackupReader.read(withExtra) as BackupReader.Result.Ok
        assertThat(result.unknownSections).containsExactly("smart_lists", "listening_streaks")
        // And what it did understand still came back whole.
        assertThat(result.file.plays).hasSize(3)
    }

    /** A file with nothing in it but a version is valid and restores nothing. */
    @Test
    fun `an empty but valid export is readable`() {
        val result = BackupReader.read("""{"format_version": 1}""")
        assertThat(result).isInstanceOf(BackupReader.Result.Ok::class.java)
        assertThat((result as BackupReader.Result.Ok).file.plays).isEmpty()
    }

    /** The file says what it holds and what it does not, in the file itself. */
    @Test
    fun `the note travels with the file`() {
        val text = BackupReader.write(BackupFile())
        assertThat(text).contains("does not hold the music itself")
        assertThat(text).contains("does not hold your Bandcamp credentials")
    }

    /**
     * The guarantee that makes an export safe to email to yourself.
     *
     * Checked against the written bytes rather than against intent, and against
     * **field names** specifically: the note says in plain words that the file
     * holds no credentials, so searching for the word would only ever find that
     * sentence. What must not exist is somewhere for one to live.
     */
    @Test
    fun `no credential has a field to live in`() {
        val text = BackupReader.write(awkward()).lowercase()
        val forbiddenKeys = listOf(
            "\"password\":", "\"token\":", "\"salt\":", "\"username\":",
            "\"credential\":", "\"server\":", "\"auth\":",
        )
        forbiddenKeys.forEach { assertThat(text).doesNotContain(it) }
    }

    /**
     * The same guarantee from the other direction: the format has no field for
     * a credential at all, so no future caller can put one in by accident.
     */
    @Test
    fun `the format itself has nowhere to put a credential`() {
        val everyFieldName = BackupReader.write(
            BackupFile(
                plays = listOf(BackupPlay()),
                resumePoints = listOf(BackupResume()),
                lists = listOf(BackupList()),
                localFiles = listOf(BackupLocalFile()),
                watchedFolders = listOf(BackupFolder()),
            )
        ).lowercase()
        listOf("password", "token", "salt", "username", "credential").forEach { word ->
            // Allowed inside the note, which is prose; never as a key.
            assertThat(everyFieldName).doesNotContain("\"$word\"")
        }
    }

    private fun roundTrip(file: BackupFile): BackupFile =
        (BackupReader.read(BackupReader.write(file)) as BackupReader.Result.Ok).file
}
