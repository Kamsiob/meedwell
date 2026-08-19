# Architecture

What the pieces are, where the line between them runs, and what a second
platform has to supply to reuse this.

`DECISIONS.md` has promised this document since before the build started and it
did not exist. It is written now because a Linux build is planned, and the
question that build will ask first is "how much of this can I keep".

---

## 1. Two modules, and the line between them

**`:core` is pure Kotlin. It has no Android dependency of any kind**, and that
is enforced by its Gradle file rather than by good intentions. Everything in it
compiles and runs on a JVM, which is the whole reason a desktop build is
cheap rather than a rewrite.

What lives there today:

| Area | What it is |
| --- | --- |
| `subsonic/` | The Bandcamp Subsonic client: request building, salted token auth, the DTOs, the mapper into app models, and `Tolerant` decoding |
| `backup/` | The export format and its reader. The versioned data contract |
| `library/` | Sorting keys, fuzzy matching, album-artist resolution, programme lines, sleep plans, tone curves |
| `surroundings/` | The manifest, credits, loop mixing, true-peak limiting |
| `model/` | The library models the interface renders |

**`:app` is the Android shell.** It supplies the platform things `:core`
deliberately does not know about: Room, the credential store, preferences, the
OkHttp engine, the media session, the Storage Access Framework, and all of the
Compose interface.

The test for whether something belongs in `:core`: could a desktop build use it
unchanged? If yes it goes there, even when only Android calls it today.

---

## 2. The ports a second platform must fill

These are already interfaces, so a Linux build implements them rather than
forking anything.

**`SubsonicHttpEngine`** (`core/subsonic/SubsonicClient.kt`) is the only way
`:core` reaches the network. Android supplies `OkHttpSubsonicEngine`; a desktop
build supplies its own and everything above it, including the salted-token auth
and every tolerant parser, is reused as is.

Three things are **not** ports yet and would need writing on any new platform,
because each is genuinely platform-shaped:

- **Storage.** Room is Android's. The schema below is the contract; the driver
  is not. Any SQLite binding that can create these tables is fine.
- **Credentials.** Android uses `EncryptedSharedPreferences`. A desktop build
  should use whatever that desktop's secret service is. Credentials are never in
  the database and never in an export, so this port stands alone.
- **Settings.** Trivial key-value. The keys are in `SettingsStore`.

---

## 3. The database is a public contract

The database is **deliberately not encrypted**, and `DECISIONS.md` records why:
it holds a catalog and a play log, and encrypting it would break portability to
protect data that is not sensitive. The consequence is that the schema is part
of the contract, not an implementation detail. The tables another build must
understand are `album`, `track`, `artist`, `play_event`, `playlist`,
`playlist_track` and `watched_folder`.

**Identifiers.** Bandcamp IDs arrive prefixed and are used verbatim: `b:` band,
`a:` album, `t:` track, `ca:` cover art, `ci:` artist image. They are stable
across calls. Local files are keyed by their own identifiers rather than by
Bandcamp's, so the two sources never collide in one table.

---

## 4. The export format is the wire format

`core/backup/BackupFormat.kt` is the versioned, documented, human-readable
carrier for everything a person owns in this app. It exists for export and
restore today, and it is also what any future sync should speak, for one
reason: a format that is already written, already read, already tested and
already shipped will not drift the way a second sync-only format would.

Rules it already follows, all of which sync depends on:

- **`format_version` is the first field and is checked before anything else.**
- **Every field has a default**, so a file written by an older or newer build
  reads rather than refuses. There is a test for exactly this.
- **Unknown sections are preserved as "not understood" and reported**, rather
  than silently dropped.
- **Credentials are never written and never read.** An export can be mailed to
  yourself without a second thought.

---

## 5. Sync semantics, per entity

This is the part a Linux build will actually need, so it is written down before
anything is built against it. **Nothing here is implemented yet**; see section 6.

Every entity below already carries what a merge needs, which is the point.

| Entity | Natural key across devices | How two copies merge |
| --- | --- | --- |
| Play events | `(track_id, played_at)` | **Union.** The table is append-only and every row is timestamped, so the union of two histories is the true history. Duplicates are identical rows and collapse on the key |
| Loved tracks and albums | the id | **Union.** Loving is one-way in this app already, because Bandcamp's `unstar` is broken server side |
| Resume points | `track_id` | **Latest wins**, by the play event that produced it |
| Lists | `id`, with `updated_at` | **Last write wins per list.** Lists are small and edited deliberately, so a per-list resolution is honest where a per-track merge would invent an order nobody chose |
| Watched folders | `uri` | **Device-local. Never synced.** A path from another machine is meaningless and a folder grant is not transferable |
| Settings | key | **Device-local by default.** A theme or a tone voicing is a property of the device it is being heard on |

Two notes that matter more than they look:

**The `play_event` primary key is an autogenerated local integer and must never
be synced.** It is meaningless off the device that made it, which is exactly why
`BackupPlay` does not carry it. The natural key is `(track_id, played_at)`.

**A merge must be idempotent.** Importing the same file twice has to leave the
same state as importing it once, which the keys above give for free provided the
implementation keys on them rather than on row identity.

---

## 6. What sync does not have yet

Stated plainly so nobody builds against an assumption:

**Restore is replace, not merge.** It is deliberate and the confirmation says so
in words before anything is touched: merging two divergent listening histories
is genuinely ambiguous, and guessing at it would produce a history that is
neither. It runs in a single database transaction and rolls back entirely on any
failure.

So a sync feature needs one genuinely new thing: a merge path alongside the
existing replace path, following the table in section 5. The data is ready for
it. The function is not written.

---

## 7. Threading and lifecycle, briefly

`:core` is suspend-function based and has no opinion about dispatchers, so a
desktop build schedules it however it likes. `:app` confines database and
network work to `Dispatchers.IO` and keeps the player on the main thread because
Media3 requires it.
