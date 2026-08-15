# Issue seed

Open these as real GitHub issues in Phase 0, before writing feature code, using the `gh` CLI. They are known now, so opening them later would be pretending they were discovered later.

**Conventions.** Use GitHub's issue **type** field for the kind of work, never a type label. Types used here: `bug`, `feature`, `task`, `documentation`, `initiative`. Labels carry **area** and **release-blocking status** only: `area:api`, `area:playback`, `area:downloads`, `area:library`, `area:ui`, `area:data`, `area:platform`, `area:a11y`, `area:release`, plus `blocker` and `blocked`. Add `good first issue` and `help wanted` honestly where they genuinely fit.

Every issue body carries: what and why in two or three sentences, acceptance criteria in checkable terms, and how to verify. Parents hold intent and overall criteria; children hold their own. Never nest more than two levels and never create a parent with a single child.

Two of these are **release blockers** rather than ordinary work, labeled `blocker`: the download tier decision, because public copy depends on it, and the export round trip, because shipping export without tested restore risks user data.

---

## Initiatives, each a parent

**I1. Verify the Bandcamp Subsonic API and record the results.** Type initiative, `area:api`, `blocker`. Children: one per section of `API-VERIFICATION.md`. Acceptance: every table filled, results block pasted into `DECISIONS.md`, one raw response saved per endpoint outside the repository with paths recorded, and the download tier decided with its evidence.

**I2. Local files, tags, and the merge.** Type initiative, `area:library`. Children: folder scanning and the local folders setting; tag handling; merge on connect; merge in reverse for files downloaded before install; local-only mode as a complete surface. Acceptance: a user who never connects an account sees no sync language anywhere, and a user who connects later sees no duplicates in either direction.

**I3. Export, restore, and automatic backup.** Type initiative, `area:data`, `blocker`. Children: format version one with its written schema; export; atomic restore; automatic backup through the Storage Access Framework plus WorkManager; the round-trip equality test. Acceptance: the equality test passes field by field after export, wipe and import, and restore is verified onto a fresh install, onto an install with existing data, and onto a weaker emulator profile.

**I4. Accessibility floor.** Type initiative, `area:a11y`. Children: waveform seek semantics; the 200 percent font scale audit; screen reader labels on every icon-only control; touch target audit; contrast verification of every pair in both themes. Acceptance: the whole app is operable with TalkBack alone, including seeking, and no layout breaks at 200 percent with enlarged display size.

---

## Tasks and features

**Tolerant deserialisation for every API response.** Type task, `area:api`, `blocker`. Every numeric field accepts integer or float; every field tolerates null and absence; unknown fields ignored. Acceptance: a unit test suite of deliberately malformed and float-bearing payloads, including real saved responses, all parsing without error.

**Auth, with the 401 investigated.** Type feature, `area:api`. Token and salt, or apiKey per verification. Plaintext passwords refused regardless of whether the server accepts them. Acceptance: a wrong password produces the Connection trouble screen with the real error code and the last successful sync time.

**Sync, incremental and resumable.** Type feature, `area:api`. Paged requests, resumable after process death, honoring whatever pacing verification established. Acceptance: a sync of the owner's real collection completes, and one killed halfway resumes without duplicating or losing anything.

**Album art without the album-list URL.** Type task, `area:api`. Field reports say the album list omits cover art URLs while the artists endpoint carries them. Acceptance: covers appear throughout the shelf regardless, with the resolution path recorded in `DECISIONS.md`.

**Shelf: Albums, Artists, Genres as sibling views.** Type feature, `area:library`. Scope filters live in the sort menu. Acceptance: all three views populate from the real account, and the A to Z index is built once and cached rather than recomputed on scroll.

**Media3 playback service with gapless.** Type feature, `area:playback`. `MediaLibraryService`, notification and lock screen controls, the system card treatment from `DESIGN.md` section 9. Acceptance: gapless verified on a continuous-mix album, and the lock screen shows correct art, metadata and seek position.

**The waveform, live and precomputed.** Type feature, `area:playback`. Custom `AudioProcessor` tap, never the `Visualizer` API. Acceptance: no microphone permission in the merged manifest, live drawing on a stream, precomputed envelope on a downloaded file, static envelope under reduced motion, and full TalkBack seek support.

**Queue sheet with Save as list.** Type feature, `area:playback`. Drag reorder, swipe remove, honest shuffle. Acceptance: Save as list creates a playlist visible in the Bandcamp account.

**The action sheet, eight verbs, everywhere.** Type feature, `area:ui`. Acceptance: identical verbs and order from every surface a track or album appears on, one shared component rather than per-screen variants.

**Downloads in three sizes, with a foreground service.** Type feature, `area:downloads`. Correct Android 14 or later service type. Real files to `Music/Meedwell/Artist/Album` through `MediaStore`. Acceptance: a multi-gigabyte run survives backgrounding, Doze and process death, and the files are readable by another player and survive uninstall.

**Reconciliation of missing files.** Type feature, `area:downloads`. Detect files removed or unmounted outside the app, mark those albums not downloaded, change nothing else. Acceptance: deleting files in another app on an emulator produces the missing-files screen with correct counts and no data loss elsewhere.

**Storage exhaustion.** Type feature, `area:downloads`. Acceptance: on a deliberately filled emulator the run stops cleanly, keeps completed tracks, states the space needed, and leaves nothing half written.

**One track that will not play.** Type feature, `area:playback`. Acceptance: playback continues past the failure, the row is marked and retryable in place, and the album is not interrupted.

**Search, collection only, with the browser handoff.** Type feature, `area:library`. Acceptance: results cover albums, tracks and artists, and the deep link opens Bandcamp's site with the correct `item_type`.

**Playlists with two-way sync.** Type feature, `area:library`. Acceptance: a playlist created in Meedwell appears in the Bandcamp account and the reverse, with edits surviving a round trip in both directions.

**Loved through star and unstar.** Type feature, `area:library`. Acceptance: a heart set in Meedwell appears in the Bandcamp account, and one set on Bandcamp appears in Meedwell after sync.

**History from the play log.** Type feature, `area:library`. Acceptance: grouped by day, reads the same `play_event` table as the Forgotten Shelf, and Erase listening history genuinely empties it.

**Forgotten Shelf.** Type feature, `area:library`. Computed on device. Acceptance: the categories match hand-checked play history, and no network call is made to produce it.

**Artwork viewer.** Type feature, `area:ui`. Acceptance: reachable from the album header, now playing and the action sheet; identical near-black presentation in both themes; no text over art; pinch to zoom works.

**Album screen and its collapsed state.** Type feature, `area:ui`. Full square cover, hard edge, then text; collapse to a hairline toolbar with a 30dp thumb on scroll. Acceptance: no text over art at any point in the scroll transition, in either theme.

**Missing-cover placeholder.** Type feature, `area:ui`. Surface panel with serif letters. Acceptance: no gray boxes, no music-note icons, the mark never used as cover art, and the album screen omits the art region entirely rather than holding it open.

**Empty states, all of them.** Type feature, `area:ui`. Zero purchases, zero local files, zero search results, an empty list, an empty queue. Acceptance: each written in the app voice as an invitation, none scolding.

**Bandcamp Friday from a versioned manifest.** Type feature, `area:platform`. Public GitHub release asset, SHA-256 verified, feature absent if unreachable. Acceptance: the gold dot appears only on a real date, and an unreachable or tampered manifest produces absence rather than a wrong date.

**App icon finishes through activity aliases.** Type feature, `area:platform`. Five finishes. Acceptance: switching is instant, needs no network, and survives a reboot.

**Permission explainers before the system dialogs.** Type feature, `area:platform`. Notifications and media audio, each in Meedwell's voice. Acceptance: declining either leaves the app working with exactly one capability missing, stated plainly and reversible in Settings.

**Share through the system sheet only.** Type feature, `area:platform`. Acceptance: plain Bandcamp link text, and a network trace showing Meedwell fetched nothing to build it.

**Crash sheet, local only.** Type feature, `area:platform`. ACRA in local mode, stacktrace in `EXTRA_TEXT`. Acceptance: a deliberately triggered crash produces a readable report that goes nowhere until the user sends it.

**Local-only mode audit.** Type task, `area:library`. Acceptance: a full pass through every screen with no account connected, finding no sync language, no dead controls and no empty account-dependent sections.

**Manifest permission audit after every dependency addition.** Type task, `area:platform`. Acceptance: the merged manifest reviewed and every permission justifiable in one sentence, recorded in `DECISIONS.md`.

**Large library on an emulator.** Type task, `area:library`. Synthetic library of several thousand albums. Acceptance: sync, scroll, search and the A to Z rail all remain usable, with real timing figures recorded.

**CI compiling every test source set.** Type task, `area:release`. Including instrumented sets not built by default. Acceptance: a deliberately broken instrumented test fails the build rather than passing silently.

**Release workflow with artifact provenance.** Type task, `area:release`. Keyless attestation, plus two or three lines in the README on how a user verifies it. Acceptance: a release artifact verifies against its provenance following only the README's instructions.

**Signing and the release path.** Type task, `area:release`. Play App Signing, upload key only, Google-signed universal APK from Play Console published as the GitHub asset. Acceptance: the same signature on both channels, verified by installing one over the other without uninstalling, and no wording anywhere saying they differ.

**LAUNCH.md.** Type documentation, `area:release`. Acceptance: it contains only the owner's remaining clicks, in plain numbered steps, and nothing that has already been automated.

**The cold read test.** Type task, `area:release`. Acceptance: run before release with findings recorded and fixed, per `MASTER_SPEC.md` section 12.

**Store listing and README, written only after verification.** Type documentation, `area:release`, `blocked` until I1 closes. Acceptance: every capability claim traceable to something verified in the built software, no em dashes, AI-slop research done first, and the non-affiliation line present.

**PRIVACY.md mirroring the hosted policy.** Type documentation. Acceptance: word for word identical to the canonical hosted version with the same effective date, and the About screen links to the hosted one rather than a second copy.

**A pinned roadmap issue.** Type documentation. Acceptance: it includes the deliberate exclusions, matching the Not Planned screen, so the same question is answered in both places identically.

## Added in the July 2026 design review

**Audio focus, becoming-noisy, and media buttons.** Type feature, `area:playback`. Release blocker. Acceptance: unplugging headphones pauses playback; an incoming call pauses and a transient loss resumes; navigation prompts duck rather than pause; wired and Bluetooth media buttons control play, pause, and skip; Bluetooth connection never starts playback on its own.

**Queue and position persistence.** Type feature, `area:playback`. Release blocker. Acceptance: kill the process mid-track, reboot the phone, reopen the app: the same queue is present, the same track is current, the position matches within a second, playback is paused, and nothing re-syncs before the queue is usable.

**Sleep timer: finish the track, extend by fifteen.** Type feature, `area:playback`. Acceptance: "when the track ends" appears alongside fixed durations; a running timer shows its countdown and extends by fifteen minutes in one tap; ending at a track boundary never clips audio.

**Now-playing cover swipe.** Type feature, `area:playback`. Acceptance: horizontal swipe on the cover skips forward or back with the standard motion spring; tap still opens the artwork viewer; TalkBack users have equivalent named actions; reduced motion swaps the slide for a crossfade.

**Pre-orders on the shelf.** Type feature, `area:library`, `blocked` until API verification answers how pre-orders are returned. Acceptance: an unreleased purchase appears on the shelf visibly distinct, with its release date where the format line usually sits, is excluded from Download everything until playable, and joins the shelf normally on release without duplicate entries.

**A to Z rail accessibility.** Type task, `area:library`. Acceptance: the fast scroller is reachable and operable with TalkBack and switch access via an equivalent jump-to-letter action, and its touch target meets 48dp despite being visually slim.
