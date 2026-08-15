# Decisions

The running log. Every judgment call made without asking, every tradeoff, every measured figure, every deviation from the specification and why, and every discovered constraint. This is what makes autonomous work auditable. Append dated entries; never rewrite history here, and correct the specification documents instead when something changes.

The BLOCKED section at the bottom lists anything only the owner can resolve.

---

## Decided before the build started

These were settled in design and research and are recorded here so a future session does not relitigate them. Each is closed unless new evidence arrives.

**AGPL-3.0, donate-only, zero data collection.** Standing across every Kamsiob app.

**Two modules, `:core` with no Android dependencies and `:app`.** A Linux desktop version is likely later and a web version is possible. The boundary is drawn now while it is free rather than retrofitted. Full Kotlin Multiplatform was considered and deliberately not adopted for version one: the module boundary delivers most of the portability benefit at a fraction of the cost, and a pure-Kotlin `:core` can become a multiplatform module later without rewriting its logic.

**The SQLite schema plus the versioned export format are the public data contract from version one.** The app is AGPL so the schema is public anyway. Documenting it deliberately in `ARCHITECTURE.md` is what makes a future desktop or web build able to interoperate.

**No SQLCipher, and the database is not encrypted.** This departs from the standing template, which specifies an encrypted database with a Keystore-held key. That is right for an app holding personal records and wrong here: this database holds an album catalogue and a play log, and encrypting it would break the portability contract above in order to protect data that is not sensitive. Credentials are the one genuinely sensitive item and they never go in the database at all: they live in `EncryptedSharedPreferences`, are never logged, never exported, and never written to a crash report. The Privacy screen states this plainly.

**The export file is not encrypted, and says what it contains.** Follows from the above, since it carries no credentials.

**Signing follows Play App Signing with an upload key only,** and the GitHub release asset is the Google-signed universal APK downloaded from Play Console. This supersedes the older two-key approach still present in the standing template. One signature serves both channels so users move between them without uninstalling. Consequence: every release goes to Play first, then GitHub. No wording anywhere may say the two builds are signed differently.

**The adaptive-scrim legibility law was retired and replaced.** Any system that measures artwork and darkens it has a worst case. The replacement has none: artwork and words never share pixels. The full rule is in `DESIGN.md` section 5.

**The light-mode gold was corrected from `#9A6F1E` to `#8A6215`.** The original measured 4.06:1 on paper `#F5F3ED` and fails WCAG AA for a 14sp label. The replacement measures 4.93:1. Measured, not estimated.

**The mark is flat rustic copper `#AE6738`.** An earlier three-dimensional version was explicitly rejected, and the Siob-era circle-on-flat-line construction was retired with the name. The colour was deepened from `#C97E52` to read less orange and more like old pipe and pennies. The current construction is a coin at rest in a shallow open cradle: the meed, held well, which is the app's whole argument in two shapes. Full rules in `DESIGN.md` section 7.

**The Meedwell mark is never used as a placeholder for missing album art.** The mark's one job is to never be confused with artwork, and borrowing it for a cover would undo that.

**No scraping, anywhere, for anything.** The only network peer is Bandcamp's own API server, plus URLs handed to the browser or share sheet. This is a compliance filter applied before designing rather than a caveat after.

**Never the `Visualizer` API for waveform data,** because it requires microphone permission. A custom `AudioProcessor` tap in Meedwell's own Media3 pipeline needs none.

**Bandcamp Friday dates are fetched from a versioned manifest rather than hardcoded,** reusing the pattern already proven in Bearings and Local AI Hub, so the feature cannot silently expire.

**The name is Meedwell, decided 27 July 2026, replacing Siob.** *Meed* is Old English, attested before 900, in Beowulf: an earned reward, a fitting recompense. *Well* is how it is paid. Checked clear of apps, repositories, and companies as a compound; the bare root is used by "meed Loyalty", an unrelated customer-loyalty company, recorded here as adjacency rather than collision. Spelling is Meedwell, never Meadwell. Client string `c=Meedwell`, download folder `Music/Meedwell`. Siob-era names in file paths, code, or copy are defects.

**The mark was redrawn for the rename, same date.** The circle-on-flat-line construction retired with Siob. Current construction: a copper coin at rest in a shallow open cradle, the meed held well. Rules in `DESIGN.md` section 7. Still two shapes, still flat, still `#AE6738` on `#16121C`.

**July 2026 design review adopted five requirements and two platform decisions.** A structured multi-lens review (platform engineer, target collector, accessibility, first-run, FOSS-skeptic perspectives; simulated review passes, not live user research, recorded as such honestly) found the specification silent on daily-use fundamentals. Adopted as v1 requirements: complete audio-focus handling including becoming-noisy and no auto-play on Bluetooth connect; queue and position persistence across process death and reboot; sleep timer finish-the-track and extend; now-playing cover swipe; pre-order shelf state gated on API verification. Platform decisions: portrait-first with rotation surviving state loss-free, tablet layouts deferred and stated; English-only at launch, stated in the listing, strings externalized from commit one. Considered and deliberately not adopted to protect scope: crossfade (harms album intent, gapless is the point), ReplayGain (Being Considered), recent-searches history, home screen continue-listening card (engagement-adjacent). F-Droid recorded in Being Considered with its real constraint: it requires reproducible builds, which this project deliberately does not pursue.

**Artifact audit against template section A1, 27 July 2026.** Four required artifacts were missing from the kit and are now seeded as skeletons: `ARCHITECTURE.md`, `CHANGELOG.md`, `tools/` and `store-assets/`. `tools/` matters more than it looks: the spec requires Bandcamp Friday dates to come from a versioned manifest rather than hardcoded values, and no generator for that manifest had been specified anywhere, so the feature had no path to existing. Two artifacts remain deliberately unwritten, `README.md` and `LAUNCH.md`, both recorded as decisions in `ARTIFACT-CHECKLIST.md` rather than left as apparent gaps: the README because the sequencing rule forbids public copy before API verification, and LAUNCH.md because which manual steps remain is only knowable after automation runs. Added `ARTIFACT-CHECKLIST.md` so this question is checkable in future without a manual audit.

**Stale cross-reference defect, found in the same audit and fixed.** Three places cited `MASTER_PROMPT.md` section 12 for the signing and database-encryption departures. The prompt has seven sections; the departures section is section 7. The references were left behind by an earlier renumbering and would have sent a session hunting for signing instructions in a section that does not exist. Corrected in `MASTER_SPEC.md`. The separate citation of `MASTER_SPEC.md` section 12 in `ISSUES-SEED.md` was verified correct and left alone.

**Restore replaces rather than merges.** Merging two divergent listening histories is genuinely ambiguous, so the app states the behaviour plainly instead of guessing.

---

## Decided during the build

Add dated entries below as work proceeds. Include the API verification results block as the first one.

### 15 August 2026: API verification results, run against the live account

Raw responses saved outside the repository at `~/.kamsiob-secrets/meedwell-api-responses/`, one file per endpoint. Credentials live at `~/.kamsiob-secrets/meedwell-subsonic.env`, mode 600, never in the repository.

**The base path is `/rest/`, and this was not obvious.** The server address the user pastes is `https://bandcamp.com/api/subsonic`, exactly as the Connect screen prefills it, but every call goes to `https://bandcamp.com/api/subsonic/rest/<endpoint>`. Calling the endpoint directly under the server address returns `bad version` and looks like a protocol mismatch. This cost the first twenty minutes of verification and is recorded so it costs nobody else any. Appending `/rest/` is standard Subsonic client behaviour, so the prefilled address stays as designed.

**`bad version` is Bandcamp's response to an unknown route, not a version problem.** Proven by calling `definitelyNotAnEndpoint`, which returns the same body. The body is `{"error":true,"error_message":"bad version"}` and is **not** a `subsonic-response` envelope. Every protocol version from 1.8.0 to 1.16.1 produces it on a wrong path, and 1.16.1 works on a right one. Treat this exact body as "endpoint absent" rather than as an error to show a user.

**Auth scheme: token plus salt, `t=MD5(password+salt)`, `s=salt`.** Confirmed working. `getOpenSubsonicExtensions` returns an **empty** extensions array, so no OpenSubsonic extension is available and no `apiKey` path exists. Plaintext `p=` is refused by the server, and Meedwell refuses it regardless.

**Authentication is not enforced on `ping`, which is a real trap.** `ping` returns `status: ok` for a wrong password. Credential validation must therefore call a data endpoint, and Meedwell uses `getArtists`. A future session that "simplifies" the Connect flow back to `ping` would ship a screen that accepts any password.

**A failed login is HTTP 500 with an empty body,** not Subsonic error code 40. Verified for wrong password, wrong username, missing auth parameters, and plaintext `p=`. This is almost certainly the cause of the unexplained 401 in the field reports: Bandcamp's auth failure path returns a bare server error and each client renders it differently. **Consequence for the interface:** the Connection trouble screen in `DESIGN.md` shows `error 40 · wrong username or password`, and no such code is ever returned. The screen now shows what actually happened. `DESIGN.md` is corrected rather than the code bent to match it.

**Genuine Subsonic errors do honour `f=json`.** A bad album id returns `{"code":70,"message":"not found"}` inside a proper envelope. So there are three distinct failure shapes to parse, not one.

**`unstar` is broken, and it answers in XML.** Every form tried, `id=`, `songId=`, and `unstar.view`, returns `status="failed"` with `code="0" message="unknown error"` as **XML, ignoring `f=json` entirely**. `star` works. This means a parser that assumes JSON crashes on the one call most likely to be made twice in a row. It also means Loved is one-way against Bandcamp today: a heart can be set and cannot be removed through the API.

**Download tier: C. There is no download endpoint.** This is the decision that gates the marquee feature and the public copy, so it was tested to exhaustion rather than once: `download`, `download.view`, `Download`, by track id and by album id, all return the unknown-route body. `stream` always redirects to a `mp3-v0` asset regardless of `format=raw`, `format=flac`, or `maxBitRate`, so the transcoding parameters are accepted and ignored. The API's own metadata agrees: every track reports `suffix: mp3`, `contentType: audio/mpeg`, `bitRate: 256`. Bandcamp's Subsonic API streams MP3 V0 and does not release purchased files at all.

Consequences, all of them already designed for, per `MASTER_SPEC.md` section 6:

- The "Your files" fallback screen, screen 26 in the visual reference, replaces the marquee Downloads screen.
- Local folder scanning and matching becomes a **version one requirement**, not a Phase 4 convenience.
- The ownership claim survives unchanged, because it was always about portable files rather than about who fetches them. Only who fetches them changes.
- No copy anywhere may say Meedwell downloads from Bandcamp. It says plainly that the API streams but does not release files, and points the user at Bandcamp to download them the way they always have.
- Streaming quality wording is fixed to what was measured: MP3 V0. Never "lossless", never "best quality Bandcamp provides", which was Tier A wording.

**Playlists are read-only, which the specification did not anticipate.** `getPlaylists` exists and returns an empty list. `createPlaylist`, `updatePlaylist`, `deletePlaylist` and even `getPlaylist` are all absent. **Save queue as a list cannot write back to the Bandcamp collection**, and `MASTER_SPEC.md` section 5 claimed it could on the strength of Bandcamp's own documentation. Lists in Meedwell are therefore local to the phone, labelled as such, in exactly the way local-only mode already labels them. The "Edits here appear in your Bandcamp collection too" line is removed rather than shown and broken.

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

---

## BLOCKED

Anything only the owner can resolve. Each entry states exactly what he needs to do, in plain steps, with no code and no jargon. Summarise this list at the end of every session.

- Nothing yet.

### Known manual steps, expected rather than blocking

These cannot be automated and are not failures. Collect them in `LAUNCH.md` as plain numbered clicks rather than raising them one at a time.

1. Create the app entry in Play Console.
2. Upload the very first bundle through the Play Console web interface, since the API cannot manage releases until one bundle exists.
3. Complete the IARC content rating questionnaire, which has no API.
4. Complete the ads declaration and app access instructions, both likely manual.
5. Approve the one-time ADB authorisation prompt on the phone.
