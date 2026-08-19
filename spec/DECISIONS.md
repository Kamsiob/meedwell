# Decisions

The running log. Every judgment call made without asking, every tradeoff, every measured figure, every deviation from the specification and why, and every discovered constraint. This is what makes autonomous work auditable. Append dated entries; never rewrite history here, and correct the specification documents instead when something changes.

The BLOCKED section at the bottom lists anything only the owner can resolve.

---

## Decided before the build started

These were settled in design and research and are recorded here so a future session does not relitigate them. Each is closed unless new evidence arrives.

**AGPL-3.0, donate-only, zero data collection.** Standing across every Kamsiob app.

**Two modules, `:core` with no Android dependencies and `:app`.** A Linux desktop version is likely later and a web version is possible. The boundary is drawn now while it is free rather than retrofitted. Full Kotlin Multiplatform was considered and deliberately not adopted for version one: the module boundary delivers most of the portability benefit at a fraction of the cost, and a pure-Kotlin `:core` can become a multiplatform module later without rewriting its logic.

**The SQLite schema plus the versioned export format are the public data contract from version one.** The app is AGPL so the schema is public anyway. Documenting it deliberately in `ARCHITECTURE.md` is what makes a future desktop or web build able to interoperate.

**No SQLCipher, and the database is not encrypted.** This departs from the standing template, which specifies an encrypted database with a Keystore-held key. That is right for an app holding personal records and wrong here: this database holds an album catalog and a play log, and encrypting it would break the portability contract above in order to protect data that is not sensitive. Credentials are the one genuinely sensitive item and they never go in the database at all: they live in `EncryptedSharedPreferences`, are never logged, never exported, and never written to a crash report. The Privacy screen states this plainly.

**The export file is not encrypted, and says what it contains.** Follows from the above, since it carries no credentials.

**Signing follows Play App Signing with an upload key only,** and the GitHub release asset is the Google-signed universal APK downloaded from Play Console. This supersedes the older two-key approach still present in the standing template. One signature serves both channels so users move between them without uninstalling. Consequence: every release goes to Play first, then GitHub. No wording anywhere may say the two builds are signed differently.

**The adaptive-scrim legibility law was retired and replaced.** Any system that measures artwork and darkens it has a worst case. The replacement has none: artwork and words never share pixels. The full rule is in `DESIGN.md` section 5.

**The light-mode gold was corrected from `#9A6F1E` to `#8A6215`.** The original measured 4.06:1 on paper `#F5F3ED` and fails WCAG AA for a 14sp label. The replacement measures 4.93:1. Measured, not estimated.

**The mark is flat rustic copper `#AE6738`.** An earlier three-dimensional version was explicitly rejected, and the Siob-era circle-on-flat-line construction was retired with the name. The color was deepened from `#C97E52` to read less orange and more like old pipe and pennies. The current construction is a coin at rest in a shallow open cradle: the meed, held well, which is the app's whole argument in two shapes. Full rules in `DESIGN.md` section 7.

**The Meedwell mark is never used as a placeholder for missing album art.** The mark's one job is to never be confused with artwork, and borrowing it for a cover would undo that.

**No scraping, anywhere, for anything.** The only network peer is Bandcamp's own API server, plus URLs handed to the browser or share sheet. This is a compliance filter applied before designing rather than a caveat after.

**Never the `Visualizer` API for waveform data,** because it requires microphone permission. A custom `AudioProcessor` tap in Meedwell's own Media3 pipeline needs none.

**Bandcamp Friday dates are fetched from a versioned manifest rather than hardcoded,** reusing the pattern already proven in Bearings and Local AI Hub, so the feature cannot silently expire.

**The name is Meedwell, decided 27 July 2026, replacing Siob.** *Meed* is Old English, attested before 900, in Beowulf: an earned reward, a fitting recompense. *Well* is how it is paid. Checked clear of apps, repositories, and companies as a compound; the bare root is used by "meed Loyalty", an unrelated customer-loyalty company, recorded here as adjacency rather than collision. Spelling is Meedwell, never Meadwell. Client string `c=Meedwell`, download folder `Music/Meedwell`. Siob-era names in file paths, code, or copy are defects.

**The mark was redrawn for the rename, same date.** The circle-on-flat-line construction retired with Siob. Current construction: a copper coin at rest in a shallow open cradle, the meed held well. Rules in `DESIGN.md` section 7. Still two shapes, still flat, still `#AE6738` on `#16121C`.

**July 2026 design review adopted five requirements and two platform decisions.** A structured multi-lens review (platform engineer, target collector, accessibility, first-run, FOSS-skeptic perspectives; simulated review passes, not live user research, recorded as such honestly) found the specification silent on daily-use fundamentals. Adopted as v1 requirements: complete audio-focus handling including becoming-noisy and no auto-play on Bluetooth connect; queue and position persistence across process death and reboot; sleep timer finish-the-track and extend; now-playing cover swipe; pre-order shelf state gated on API verification. Platform decisions: portrait-first with rotation surviving state loss-free, tablet layouts deferred and stated; English-only at launch, stated in the listing, strings externalized from commit one. Considered and deliberately not adopted to protect scope: crossfade (harms album intent, gapless is the point), ReplayGain (Being Considered), recent-searches history, home screen continue-listening card (engagement-adjacent). F-Droid recorded in Being Considered with its real constraint: it requires reproducible builds, which this project deliberately does not pursue.

**Artifact audit against template section A1, 27 July 2026.** Four required artifacts were missing from the kit and are now seeded as skeletons: `ARCHITECTURE.md`, `CHANGELOG.md`, `tools/` and `store-assets/`. `tools/` matters more than it looks: the spec requires Bandcamp Friday dates to come from a versioned manifest rather than hardcoded values, and no generator for that manifest had been specified anywhere, so the feature had no path to existing. Two artifacts remain deliberately unwritten, `README.md` and `LAUNCH.md`, both recorded as decisions in `ARTIFACT-CHECKLIST.md` rather than left as apparent gaps: the README because the sequencing rule forbids public copy before API verification, and LAUNCH.md because which manual steps remain is only knowable after automation runs. Added `ARTIFACT-CHECKLIST.md` so this question is checkable in future without a manual audit.

**Stale cross-reference defect, found in the same audit and fixed.** Three places cited `MASTER_PROMPT.md` section 12 for the signing and database-encryption departures. The prompt has seven sections; the departures section is section 7. The references were left behind by an earlier renumbering and would have sent a session hunting for signing instructions in a section that does not exist. Corrected in `MASTER_SPEC.md`. The separate citation of `MASTER_SPEC.md` section 12 in `ISSUES-SEED.md` was verified correct and left alone.

**Restore replaces rather than merges.** Merging two divergent listening histories is genuinely ambiguous, so the app states the behavior plainly instead of guessing.

---

## Decided during the build

Add dated entries below as work proceeds. Include the API verification results block as the first one.

### 15 August 2026: "Surroundings", an ambient sound library, parked rather than adopted

The owner has a prepared library of ambient sound files on the desktop: roughly 30 GB across nine categories (water and weather, rainforest, water bodies, wind, fire, forest and countryside, human spaces, transit, mechanical), with an attribution file and a manifest already generated. He described them as being for "Surroundings, the background audio feature".

Nothing in `MASTER_SPEC.md`, `DESIGN.md` or the 46 screen reference mentions ambient sounds, and Meedwell's positioning is a deliberately narrow scope where absences are decisions rather than gaps. Adopting this silently would be the largest change to what the app is since the name was chosen.

**Parked, on the owner's instruction, pending details from him.** Not built, not designed, and `MASTER_SPEC.md` is not modified. Recorded here so the asset library and the intent are not lost, and so a future session does not rediscover a 30 GB folder with no idea what it was for.

Worth noting for whenever it is picked up, because it shapes the answer: 30 GB cannot ship inside an app bundle, so wherever this lands it needs a delivery story, and Meedwell's current story is that the only server it ever talks to is Bandcamp's.

### 15 August 2026: issue types are not available, so type labels are used

`MASTER_SPEC.md` section 12 says to use GitHub's issue **type** field rather than a type label. That field only exists on repositories owned by an organization, and `Kamsiob` is a user account, so the API returns 404 for issue types on this repository.

Decided: `type:` prefixed labels carry the kind of work, and area plus blocking labels are unchanged. This is the reversible option: if the repository ever moves under the B7 Collective organization that the Play Console account already uses, the labels convert to real types and nothing else changes. `MASTER_SPEC.md` is corrected to say so rather than describing a convention the tracker cannot follow.

### 15 August 2026: minSdk is 29, set by a feature rather than by taste

`MediaStore` writes using `RELATIVE_PATH` and `IS_PENDING` are how owned files land in the public Music folder, and neither exists before API 29. Tier C makes local files the whole ownership story, so this is not a corner of the app that could be dropped to reach older phones.

Compiled against 37, targeting 36. Play requires API 36 from 31 August 2026, verified against the live requirements page on 15 August 2026 rather than taken from the specification's note.

### 15 August 2026: the build needs a JDK 21 daemon, and no path is committed

AGP 9.3.1 does not run on the machine's default JDK 26. `JAVA_HOME` is pointed at the JDK 21 installed alongside it before invoking Gradle. Deliberately **not** pinned in `gradle.properties`, because that file is committed and a machine specific path would break CI and every other machine. The development machine's path is recorded in `HANDOFF.md`, which is where machine specific facts belong.

Also discovered: AGP 9 brings its own Kotlin support, and applying `org.jetbrains.kotlin.android` alongside it fails the build outright. The Compose compiler plugin is still applied and still tracks the Kotlin version exactly.

### 15 August 2026: MD5 is the one JVM-only call in `:core`

Subsonic auth requires `t=MD5(password+salt)`. `java.security.MessageDigest` is used for it, isolated in a single function in `SubsonicClient.kt` with a comment saying so. MD5 here is a protocol requirement and not a security choice; the transport is HTTPS and nothing relies on the digest being strong.

Considered and rejected: hand writing MD5 in pure Kotlin to keep `:core` free of the JVM entirely. That would trade a well tested platform primitive for roughly a hundred lines of bit twiddling in order to save one function's worth of work during a multiplatform conversion that may never happen. If that conversion does happen, this is the only function in the module that needs a platform implementation.

### 15 August 2026: Meedwell does not take part in autofill

Found by using the app on the device rather than by review. After a successful connect, Android offered to save the Bandcamp credentials to Google Password Manager, which would copy them to Google's cloud. The screen immediately above that dialog says "Stored only on this phone, encrypted".

Both cannot be true, and the copy is the promise, so the platform behavior is what changes: `importantForAutofill` is set to `NO_EXCLUDE_DESCENDANTS` on the window, app-wide.

Considered: leaving it, on the grounds that the user chooses whether to accept the dialog. Rejected, because the app would then be printing a claim on screen that the platform contradicts one tap later, and "honest limits stated at the moment they matter" cuts both ways. A user who keeps credentials in a password manager is not cut off: Bandcamp generates them and keeps them on its own settings page, and the Paste chips exist precisely so they can be pasted from wherever the user keeps them.

### 15 August 2026: the keyboard is told what the credential fields are

Also found on the device. The password field used a visual transformation but no `KeyboardType.Password`, so the IME treated it as ordinary prose and printed it in the suggestion strip in plain sight, as well as learning it.

Both credential fields now declare their keyboard type with autocorrect and capitalisation off. The username needs it too: Bandcamp's generated username is a long uppercase token that autocorrect will happily mangle.

### 15 August 2026: WorkManager removed until the feature that needs it exists

It was added ahead of automatic backup in Phase 6, and the manifest audit immediately failed the build because it brought in `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED`. Shipping two permissions for months before the feature that justifies them is exactly what that audit exists to stop, so the dependency came out and returns with the feature.

`WAKE_LOCK` remains, from Media3's ExoPlayer, and is allowlisted with its justification: a music player that stops when the screen turns off is broken.

### 15 August 2026: API verification results, run against the live account

Raw responses saved outside the repository at `~/.kamsiob-secrets/meedwell-api-responses/`, one file per endpoint. Credentials live at `~/.kamsiob-secrets/meedwell-subsonic.env`, mode 600, never in the repository.

**The base path is `/rest/`, and this was not obvious.** The server address the user pastes is `https://bandcamp.com/api/subsonic`, exactly as the Connect screen prefills it, but every call goes to `https://bandcamp.com/api/subsonic/rest/<endpoint>`. Calling the endpoint directly under the server address returns `bad version` and looks like a protocol mismatch. This cost the first twenty minutes of verification and is recorded so it costs nobody else any. Appending `/rest/` is standard Subsonic client behavior, so the prefilled address stays as designed.

**`bad version` is Bandcamp's response to an unknown route, not a version problem.** Proven by calling `definitelyNotAnEndpoint`, which returns the same body. The body is `{"error":true,"error_message":"bad version"}` and is **not** a `subsonic-response` envelope. Every protocol version from 1.8.0 to 1.16.1 produces it on a wrong path, and 1.16.1 works on a right one. Treat this exact body as "endpoint absent" rather than as an error to show a user.

**Auth scheme: token plus salt, `t=MD5(password+salt)`, `s=salt`.** Confirmed working. `getOpenSubsonicExtensions` returns an **empty** extensions array, so no OpenSubsonic extension is available and no `apiKey` path exists. Plaintext `p=` is refused by the server, and Meedwell refuses it regardless.

**Authentication is not enforced on `ping`, which is a real trap.** `ping` returns `status: ok` for a wrong password. Credential validation must therefore call a data endpoint, and Meedwell uses `getArtists`. A future session that "simplifies" the Connect flow back to `ping` would ship a screen that accepts any password.

**A failed login is HTTP 500 with an empty body,** not Subsonic error code 40. Verified for wrong password, wrong username, missing auth parameters, and plaintext `p=`. This is almost certainly the cause of the unexplained 401 in the field reports: Bandcamp's auth failure path returns a bare server error and each client renders it differently. **Consequence for the interface:** the Connection trouble screen in `DESIGN.md` shows `error 40 · wrong username or password`, and no such code is ever returned. The screen now shows what actually happened. `DESIGN.md` is corrected rather than the code bent to match it.

**Genuine Subsonic errors do honor `f=json`.** A bad album id returns `{"code":70,"message":"not found"}` inside a proper envelope. So there are three distinct failure shapes to parse, not one.

**`unstar` is broken, and it answers in XML.** Every form tried, `id=`, `songId=`, and `unstar.view`, returns `status="failed"` with `code="0" message="unknown error"` as **XML, ignoring `f=json` entirely**. `star` works. This means a parser that assumes JSON crashes on the one call most likely to be made twice in a row. It also means Loved is one-way against Bandcamp today: a heart can be set and cannot be removed through the API.

**Download tier: C. There is no download endpoint.** This is the decision that gates the marquee feature and the public copy, so it was tested to exhaustion rather than once: `download`, `download.view`, `Download`, by track id and by album id, all return the unknown-route body. `stream` always redirects to a `mp3-v0` asset regardless of `format=raw`, `format=flac`, or `maxBitRate`, so the transcoding parameters are accepted and ignored. The API's own metadata agrees: every track reports `suffix: mp3`, `contentType: audio/mpeg`, `bitRate: 256`. Bandcamp's Subsonic API streams MP3 V0 and does not release purchased files at all.

Consequences, all of them already designed for, per `MASTER_SPEC.md` section 6:

- The "Your files" fallback screen, screen 26 in the visual reference, replaces the marquee Downloads screen.
- Local folder scanning and matching becomes a **version one requirement**, not a Phase 4 convenience.
- The ownership claim survives unchanged, because it was always about portable files rather than about who fetches them. Only who fetches them changes.
- No copy anywhere may say Meedwell downloads from Bandcamp. It says plainly that the API streams but does not release files, and points the user at Bandcamp to download them the way they always have.
- Streaming quality wording is fixed to what was measured: MP3 V0. Never "lossless", never "best quality Bandcamp provides", which was Tier A wording.

**Playlists are read-only, which the specification did not anticipate.** `getPlaylists` exists and returns an empty list. `createPlaylist`, `updatePlaylist`, `deletePlaylist` and even `getPlaylist` are all absent. **Save queue as a list cannot write back to the Bandcamp collection**, and `MASTER_SPEC.md` section 5 claimed it could on the strength of Bandcamp's own documentation. Lists in Meedwell are therefore local to the phone, labeled as such, in exactly the way local-only mode already labels them. The "Edits here appear in your Bandcamp collection too" line is removed rather than shown and broken.

**Endpoints confirmed present:** `ping`, `getOpenSubsonicExtensions`, `getLicense`, `getMusicFolders`, `getGenres`, `getArtists`, `getArtist`, `getAlbum`, `getAlbumList2`, `search3`, `getCoverArt`, `stream`, `star`, `getStarred`, `getPlaylists`, `scrobble`.

**Endpoints confirmed absent:** `download`, `getAlbumInfo2`, `getArtistInfo2`, `getStarred2`, `getScanStatus`, `getNowPlaying`, `getRandomSongs`, `createPlaylist`, `updatePlaylist`, `deletePlaylist`, `getPlaylist`.

**Liner notes do not exist.** `getAlbumInfo2` is absent, so the album screen's liner notes section is not built. This was already conditional in the specification.

**`getAlbumList2` includes `coverArt` on every album.** The field report saying the album list omits cover art did **not** reproduce. The planned workaround is unnecessary and the issue is closed as not reproducing rather than left open.

**No float fields, no null fields, in any response from this account.** The reported float durations did not reproduce: `duration`, `track`, `year`, `size`, `bitRate`, `songCount` all came back as integers. Tolerant parsing is still built exactly as specified, because absence of evidence on a three-album collection is not evidence of absence, and the beta changes under us.

**Real beta bugs found in the data itself,** both handled by tolerant parsing: `getStarred` returns a song object with the key **`idDir`** where the schema says `isDir`, and album `genres` arrays contain duplicates, for example `soundtrack` twice on one album.

**Fields absent that the app expected:** no `discNumber` anywhere, so multi-disc handling has no API side and applies only to local files; no `albumArtist`, so the compilation rule also applies only to local files; no `path`. Multi-artist albums are represented as one album artist with per-track artists differing, confirmed on "Medieval Times", where three distinct track artists sit under one album artist.

**IDs are prefixed and stable across calls:** `b:` band, `a:` album, `t:` track, `ca:` cover art, `ci:` artist image. Stability across sessions is assumed from this consistency but has only been observed within one day, and is recorded as an assumption rather than a verified fact.

**Bandcamp page URLs: none, except an artist image.** The only URL field anywhere in any response is `artistImageUrl`, pointing at `f4.bcbits.com`. No artist or album page URL is returned. "Their Bandcamp page" and all share text therefore use the constructed `bandcamp.com/search?q=NAME&item_type=b` deep link, which is the already-specified fallback.

**`getCoverArt` ignores its `size` parameter.** It redirects to a `bcbits.com` asset and returns the same 700x700 JPEG at 174 KB whether or not `size=300` is sent. The app resizes locally and caches, since asking the server for a thumbnail achieves nothing.

**Range requests work.** A `Range` header returns 206 with a correct `content-range`, so seeking and gapless playback have what they need.

**No caching headers on metadata calls.** No `cache-control`, no `etag`, no `last-modified`. Local caching is entirely Meedwell's problem.

**No rate limiting observed** at 20 rapid sequential calls, averaging 105 ms each with no failures. Pacing decision, recorded deliberately: sync issues requests **sequentially with no artificial delay**, and no parallel fan-out. Being an unremarkable client of a service in open beta is both correct and in the app's interest, and sequential is already fast enough at these response times.

**Real collection figures, for the large-library expectations:** 3 albums, 60 tracks, 3.5 hours. Metadata calls return in roughly 200 ms. This account is far too small to validate the large-library work, so the synthetic several-thousand-album emulator test in `ISSUES-SEED.md` carries that load alone and cannot be skipped on the strength of a fast real sync.

**Account state changed during verification, and it could not be reverted.** Verifying `star` required starring something. One track, "Wolf Blood" from The Celtic Collection II, and one album, The Celtic Collection II, were starred. `unstar` is broken server side, so neither could be undone through the API. They can be unstarred on the Bandcamp website. Recorded here rather than quietly left, because it is a change made to the owner's real account.

### 19 August 2026: Android Auto, and what the car is allowed to see

Built for 1.5. The expensive decision was made in Phase 1 and was correct:
`PlaybackService` has always been a `MediaLibraryService` rather than a
`MediaSessionService`, so this is a browse tree rather than a rewrite.

**The car never sees this app's design.** Auto asks for a tree and draws its
own templates. Nothing about staves, contours or the serif italic exists there,
and pretending otherwise would be the wrong thing to optimize. What can be
chosen is the shape of the tree, and the constraint that matters is not visual:
how many taps it takes to start music while driving.

**Recent is the first tab** because a car overwhelmingly wants the record you
were already playing. Albums, Composers and Lists mirror the shelf. Genres are
left out: they are the library's idea of order rather than the listener's, and
a browse tab has to earn its place at a steering wheel.

**Surroundings is deliberately absent from the car.** The bed is a second sound
layered under the first at a level set by dragging, which is a two control
interaction, and driving is the one place that is genuinely a bad idea. If it
ever appears it should be plain playable items with no layering.

**The tree is served entirely from the local database.** A browse tree that
waits on Bandcamp is empty in a parking garage, which is exactly where somebody
is when they plug in.

**`onAddMediaItems` is the callback that makes a tap produce sound**, and its
absence is invisible: browse items carry an id and no uri, so without it the
car accepts the tap and plays nothing, silently. It resolves a record to its
whole programme, and reuses `Track.toMediaItem`, so the rule that a local file
beats a stream has one implementation rather than two.

**`onPlaybackResumption` is still not implemented**, deliberately, and this is
the release where somebody would be most tempted to add it. Implementing it is
what makes an app start playing on its own when a car connects, which is the
single most common way a player annoys people.

**Verified without a car.** `MediaTreeBrowseTest` connects a real
`MediaBrowser` to the service and walks the tree exactly as Auto does, seeding
one record first so the assertions are not vacuous on an empty shelf, which is
how the first version of it passed while proving nothing. The test was then
checked by breaking album resolution on purpose and confirming it went red. The
Desktop Head Unit is installed for the chrome, but the chrome is Google's code;
this is the part that is ours.

### 19 August 2026: the application id is io.github.kamsiob.meedwell

The Play Console entry was created under `io.github.kamsiob.meedwell`, the
reverse domain of the repository that publishes this app and the form F-Droid
expects, while the build had shipped `com.kamsiob.meedwell` since Phase 0.

**The build moved, not the console.** A package name is permanent from the
first published release, and the console entry already held the listing. An
upload whose id does not match the entry is rejected outright, so this was a
release blocker rather than a preference.

`namespace` deliberately stays `com.kamsiob.meedwell`. It is only the Kotlin
package that the generated `R` and `BuildConfig` land in, no user or store ever
sees it, and changing it would rewrite the package line of every source file in
both modules to change nothing observable. The two being different is normal
Android and is now commented at both sites.

Two consequences worth knowing. The `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
that androidx.core adds is namespaced to the **application id**, so the manifest
audit's allowlist had to move with it or every build would fail the audit.
And a phone carrying the old build treats this as a different app: it installs
alongside rather than over, so the old one should be uninstalled by hand.

### 19 August 2026: the store title is "Meedwell: Bandcamp Player"

**Owner's instruction, overriding MASTER_SPEC section 2**, which says store
listings carry "Meedwell by Kamsiob". That name tells a stranger scrolling Play
nothing about what the app does, and the store title is the one line of copy
that has to earn a tap from somebody who has never heard of either word. 25 of
the 30 characters, so it is not truncated on any surface.

**The trademark exposure is real and is being taken deliberately.** Play's
metadata policy forbids using another company's brand in a way that misleads
about affiliation, and a title beginning with a product name and ending in
"Bandcamp Player" is a stronger claim than the nominative use that section 1
permits everywhere else in the app. Mitigations already in place: the full
description carries "Not affiliated with or endorsed by Bandcamp", the app
never uses Bandcamp's logo or wordmark art, and nothing in the interface
implies a partnership. If the listing is rejected or a trademark complaint
arrives, the fallback is "Meedwell: Player for Bandcamp" and then "Meedwell:
Music You Own", in that order, rather than relitigating the whole name.

### 16 August 2026, second entry: the card shrinks, the pages turn

**The Surroundings card is one row with its level on its bottom edge.** The
owner's verdict on the three-storey collapsed card was that it took up way too
much space, and the clever way out was the one the mini player had already
found: re-home the control instead of dropping it. The volume is now a thin
rule along the card's bottom edge, the contour's own dot riding it, tiny
speakers at the ends, an invisible 40dp reach overlapping the row padding. So
the two cards are mirrored twins, the music's rule on the mini player's top
edge and the room's rule on the bed card's bottom edge. The grab handle went
with the height; drag, tap and chevron all still work, and the full level line
returns when the card is open. Collapsed height fell from about 104dp to about
66dp.

**Numeral columns are sized from the highest numeral.** XVIII wrapped in a
fixed 30dp column. `numeralColumnWidth` picks one width per programme from the
largest track number, so every row shares one margin and no numeral ever
breaks.

**On the Shelf, a swipe crosses the switcher, not the tab bar.** The owner's
call: the row a person on that screen is choosing between is Albums,
Composers, Shelves, Lists. ShelfScreen owns the horizontal swipe now and the
tab-body swipe is suppressed there; the other tabs keep it.

**The visual round, one grammar.** Crossing bottom tabs and crossing shelf
views are both small page turns now, arriving 42px from the side you traveled
toward on the Settle curve, same as the player spread. Section-head staves
rule themselves on left to right, each line a breath behind the one above, the
contour's own gesture made ambient. The day sun warms the paper around it, a
copper halo breathing over ten seconds, the one glow in the app because it is
the one literal light; still and dimmer at night, still under reduced motion.
The alphabet rail grew a loupe: the letter under a finger swells half again in
moss with its neighbors leaning in. And both floating cards give 1.5 percent
under a press on the standard spring, because a surface that answers a finger
reads as a thing and one that does not reads as a picture of a thing. Every
one of these snaps or stills under reduced motion.

### 16 August 2026: the completion round

Everything remaining from the three panel reports and the motion review was
implemented in one sitting, at the owner's instruction to finish it all.

The signatures: the contour now writes itself onto the staff once per piece,
carries wet ink at the nib as it plays, and answers a finger by swelling its dot
and lifting the remainder. The cover skips on a drag, with the rubber-band as
its own affordance, closing a version-one requirement of MASTER_SPEC. The queue
became the printed bill of grid 11: Roman numerals when the queue is one whole
record, tempo marks read from titles, a duration column, pieces-and-minutes
remaining in the head, drag to reorder, swipe to remove, and the engraved sprig
closing the sheet. The day line's moon is now the real moon, computed from the
lunation with no location asked, and the sun advances with the clock instead of
freezing at composition. The page edge crosses and widens as the leaf turns.

The truths: the Forgotten Shelf finally says why ("Last heard in March", "Never
played. Shelved July 2026") from facts its own query had always computed and
thrown away. The grid's section head names the order it is actually in. The
Composers view carries time held, never play counts, per the grid's caption.
Search results and the Loved screen show their artwork. Empty states became
invitations with the serif and the plate. The About screen's serif thesis was
restored over the wordmark. Albums and playlists gained aligned duration
columns and ghost-pill shuffles; the artwork viewer gained double-tap zoom and
lost its borrowed ✕ glyph.

### 16 August 2026: the design panel's rulings, and the owner's overrides

A three-seat internal panel reviewed the whole app: a listener's advocate, an
art director, and a contrarian arguing for subtraction. Most of their consensus
was implemented the same day. Three of their hills were overruled by the owner,
and those overrides are recorded here so they are not "fixed" back later.

**The mini player is a card, against the grid.** The grid draws `.mini` as a
full-bleed bar with a top hairline and 34px art. The owner ordered a separated,
rounded card with larger art, type and play mark, twice and explicitly. The
divergence is deliberate. What was kept from the art director's ruling is the
recipe: every card in the app now shares 20dp corners, a hairline edge at 16
percent ink, and ranked shadows, with the mini player forward at 10dp and the
floating Surroundings card behind it at 6dp, so depth says which is the record
and which is the room.

**The category rail on the player's Surroundings page stays.** The contrarian
argued a flat list was enough. The owner asked for categories by name. The
rail's real defects were fixed instead: its underline animates rather than
teleporting, and a right-swipe on the first category turns the page back to the
music instead of dying silently.

**The staggered arrival stays.** It replays on every page turn, which the
contrarian correctly counted as a cost. The owner asked twice for parts arriving
in order, in so many words. The beats are 38ms apart and the whole build is over
inside 380ms.

The rest of the panel's consensus shipped: the Loved screen got a door, a third
way through onboarding for somebody with no account and no files, the sleep
timer returns to the player and reaches three hours, automatic sync failures
stopped raising a modal sheet over the app, the Connect button says why it is
refusing, history rows play the piece rather than track one of its album, ten
invisible panels became real recesses, seventeen bare section labels became the
one ruled section head, and around two hundred lines of dead code from earlier
iterations were removed.

### 15 August 2026: grid pixels multiply by 1.25 to reach sp, lengths stay 1:1

Two binding documents disagreed and `DECISIONS.md` recorded neither, so this
settles it. `DESIGN.md` section 9 says the 330px mock multiplies by about 1.25 to
reach sp and that nothing is smaller than 12sp. `Type.kt` had reversed that to
one to one, calling the multiplier a quiet reinterpretation.

The reversal was wrong, and measurably so. The mock's 286px content column maps
to a 411dp device's 367dp column, a factor of about 1.28, so setting type at 1:1
rendered the entire app at roughly 80 percent of the specified scale. Four
tokens, `.plate`, `.tabLabel`, `.section` and `.meta`, sat under the 12sp floor
`DESIGN.md` sets.

That is the mechanical reason the app read as flat everywhere rather than only on
one screen. A 13.5sp heading over 13.5sp body has no hierarchy available to it,
and no amount of spacing or rhythm buys it back.

The owner arbitrated in favor of `DESIGN.md`, which `CLAUDE.md` already says wins
on anything visual. Type is now scaled by 1.25 and `.plate` is rounded up to the
12sp floor rather than left at 11.9 by a rounding error.

**Lengths are not scaled.** The gutter is 22dp because that figure was given
directly rather than derived from the mock. Only type moves.

The caution that came with the ruling: presence should come from the serif
dynamics line and the staff rather than from bigger bold sans. Growing the type
is the correction, not the design.

### 15 August 2026: stopping a Surroundings bed is a named row, not a swipe

The spec said "swipe the card down to stop the sound and dismiss the card", and
the card reference said the same. Both are now amended: a drag folds the card,
and stopping is a named row at the foot of the opened card.

The owner reported losing beds to stray thumbs, repeatedly. The cause was not
taste, it was arithmetic. `detectVerticalDragGestures` reports the distance since
the last pointer event, not the distance travelled, and the test was 26 pixels,
which is roughly one touch slop. So the first move of any downward flick cleared
it. The gesture also fired mid-drag rather than on release, so the sound was gone
while the finger was still down with no way to abort, and the card never followed
the finger, so nothing on screen said a gesture was underway. The card sits in the
thumb arc above the mini player.

Worst of all, the rows for changing the bed are children of that same gesture, so
reaching in to swap the recording was the motion most likely to kill it.

A confirmation dialog was considered and rejected. This design has no filled
containers and the voice does not nag. The rule taken instead is that a
destructive action gets a name and a place, and gestures are reserved for things
that can be undone by doing them again. Folding a card qualifies. Ending the
sound does not.

The opt out argument in the old spec still holds, it just moved: there is still no
setting to hide the card, because stopping the sound is still the whole opt out.
It is now one named row rather than a gesture nobody could aim.

### 15 August 2026, building Tone and auditing Settings

**`READ_MEDIA_AUDIO` is no longer declared.** This reverses `MASTER_SPEC.md` section 4 and its "two permissions" statement in section 15, both of which are now corrected there.

It was declared from the start and used exactly nowhere. Local music reaches the shelf entirely through the Storage Access Framework: `LocalScanner` takes a persistable grant on the folder the user picks and reads it with `DocumentFile`, which needs no runtime permission. The permission's original purpose was the `MediaStore` download path in section 4, and that path was never built and cannot be, because verification found Bandcamp exposes no download endpoint.

It was found by building the Permissions section of Settings and noticing the row read "Not allowed. Local folders cannot be read" on a phone whose local folders read perfectly well. An app arguing for restraint cannot ask for a media permission it never exercises, and it removes a Play data-safety declaration that had nothing behind it. Removing an ungranted permission cannot regress the SAF path, which never consulted it.

**The notification permission is now actually requested.** It was declared and never asked for, so on Android 13 and up there was no playback notification at all: no lock screen controls, no shade player, no way to pause without reopening the app. It is asked for at the first note rather than at launch, once per install, and never again if refused. Verified on the Pixel 8: the dialog appears on first play and the transport notification follows.

**Settings said downloads go to `Music/Meedwell`. They do not.** The grid's screen 19 says so and it would have been a plainly false sentence on the screen whose whole purpose is being checkable. `SurroundingsStore` puts recordings in `filesDir` on purpose, documented there, so they stay out of other apps' media scanners and leave when the app does. The row now says that instead. Counted as a grid divergence, resolved in favor of the code because the code's reason is the better one.

**Two settings were missing from the export format.** `voicing`, added earlier the same day with Tone, and `resume_queue_on_opening`. `SettingsStore` carries a standing note that every setting must reach the export, and both had slipped it. Added, with a round-trip test and a test that an export written before either key existed still reads, defaulting `voicing` to `AsRecorded` rather than inventing a curve.

**Onboarding was one screen where the grid specifies three, and the one screen made a promise the app cannot keep.** Grid 01 the declaration, 02 two ways in, and 03 the tone disclosure are now all built, in that order.

The screen that stood there was a generic welcome: a tagline, a body line, and two doors. Its body copy said the collection was "yours to keep as real files on your phone", which is false. Verification found Bandcamp offers no download endpoint at all, so the very first sentence anybody read was a promise the app could not honor.

What the grid has instead is a declaration made once, plainly, with a second button that **refuses nobody**: somebody who listens to something else is told what this was built for and waved straight through. That is a position rather than a gate, and it is the thing the old screen had no way of saying.

Three consequences worth recording:

- **`hasChosenPath` is now set at the end of onboarding, not the start.** Marking it on the first tap meant somebody who backed out halfway reopened onto a shelf they had never agreed to set up.
- **The engraved plate exists.** `SeedHeadPlate` draws the grid's own paths, so it takes its color from the theme instead of shipping as an asset. It is the first of the plates the design puts at rest points.
- **The mark is drawn bare inside the app.** The dark rounded square is the launcher icon's frame, and it had been carried onto the Welcome and About screens, where it read as the one card in a design that has none.

**The default voicing is `Orchestral`, not `AsRecorded`.** This follows grid 03, which exists precisely because a default that alters playback has to be declared rather than discovered. The disclosure and the opt-out sit on the same screen, so nobody meets it without being told. Recorded because the safer looking default, applying nothing, is the one being deliberately not taken.

**Pull to refresh existed only as a comment.** `MeedwellViewModel.refresh` has been documented as "pull to refresh, and the manual override" since the sync work, and the gesture was never built: the only manual sync was a row inside Settings. It is on the shelf now, disabled when local-only, because a spinner that resolves to no change is the app pretending to work.

Its indicator is the mark rather than a spinner: pulling lifts the coin out of its cradle, and while the sync runs the coin rolls side to side along the **real cradle parabola**, computed from the same constants `Mark.kt` draws with. A first attempt drew the cradle unconditionally, so a finished sync left a bare copper arc under the title with no coin in it, looking like a rendering fault. One alpha now covers the whole mark.

**The tone curve is drawn with zero on the middle staff line, and the top half is left permanently empty.** The grid's own sketch on screen 17 runs the curve above centre, which contradicts the caption directly beneath it about cuts rather than boosts. `Voicing` cannot produce a boost, so drawing one would be the single dishonest mark on the screen. The scale puts the deepest permitted cut, 3 dB, exactly on the bottom line: the curve can never leave the staff, and the empty upper half is the drawing making the same point the copy makes.

---

## BLOCKED

Anything only the owner can resolve. Each entry states exactly what he needs to do, in plain steps, with no code and no jargon. Summarize this list at the end of every session.

1. **Two starred items on the real Bandcamp account cannot be removed by the app.** Verifying that `star` works required starring something, and `unstar` turned out to be broken on Bandcamp's side. One track, "Wolf Blood", and one album, "The Celtic Collection II", are now starred. **What to do:** if you want them unstarred, remove the hearts on the Bandcamp website. Nothing in the app can do it until Bandcamp fixes the endpoint.

2. **A Bandcamp pre-order would unblock issue #46.** The collection contains none, so how the API represents an unreleased purchase could not be verified, and building the pre-order shelf state would mean guessing at a data shape. **What to do:** nothing, unless you happen to pre-order something. If you do, say so, and that issue unblocks.

3. **The Bandcamp Friday manifest needs a home before issue #26 can finish.** The dates are fetched from a public GitHub release asset with a hash check rather than hardcoded. The generator will be written into `tools/`, but publishing the asset is a repository release step. **What to do:** nothing yet. Raised here so it is not a surprise later.

### Known manual steps, expected rather than blocking

These cannot be automated and are not failures. Collect them in `LAUNCH.md` as plain numbered clicks rather than raising them one at a time.

1. Create the app entry in Play Console.
2. Upload the very first bundle through the Play Console web interface, since the API cannot manage releases until one bundle exists.
3. Complete the IARC content rating questionnaire, which has no API.
4. Complete the ads declaration and app access instructions, both likely manual.
5. Approve the one-time ADB authorization prompt on the phone.
