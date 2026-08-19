# Handoff

The resume document. Kept current at all times so a session with no memory of any previous conversation can pick up cleanly after a disconnection, a crash, context exhaustion, or a gap of weeks.

**Read this file in full at the start of every session.** That is its purpose. Then search the other documents for the sections relevant to the current item rather than loading them whole.

---

## State of play

**Last updated: 19 August 2026, after the store listing went up.**

**The application id is now `io.github.kamsiob.meedwell`** and the Play listing
is live in the console: title, both descriptions, icon, feature graphic and
eight screenshots, all pushed through the Publisher API. The listing lives in
`store/listing/en-US/` as plain files and `store/push-listing.py` sends it, so
a store update is a text edit and one command. `store/check-chain.py` verifies
the service account chain. `LAUNCH.md` holds the owner's remaining console
work, which is the content declarations, the privacy policy URL and the release
rollout. The bundle is `Meedwell-1.0.0.aab` in the project root.

**Last updated: 16 August 2026, third entry, after the visual round.**

Since the completion round: the collapsed Surroundings card is one row with
its volume as a rule on its bottom edge (about 66dp, down from 104), numeral
columns size themselves from the highest numeral so XVIII never wraps, a
horizontal swipe on the Shelf steps the upper view switcher instead of the
bottom tabs, and a visual round put one motion grammar across the app: page
turns between tabs and shelf views, staves that rule themselves on, a
breathing copper halo on the day sun, a loupe on the alphabet rail, and press
give on both floating cards. All in DECISIONS.md, 16 August second entry.
Device verification note: HealthTrail's dev loop on the tester steals focus
every few seconds; the workaround is short device-side scripts via the
launcher intent (monkey), never `am start`, which stacks a fresh activity and
resets navigation.

**The completion round is done and shipped.** Everything remaining from the
three design-panel reports and the motion review was implemented, verified on
the tester device, and delivered to the owner as `Meedwell-1.0.0.apk` in the
project root (the matching AAB sits beside it, both signed and verified,
neither ever committed). The full list of what changed and why lives in
`DECISIONS.md` under "16 August 2026: the completion round". The last three
fixes, made after device screenshots: the Surroundings card fill is now fully
opaque (0.98 still let bright album art ghost through on the shelf grid), the
grid tile no longer prints the artist twice (`ProvenanceLine` carries only the
collector's mark now; the plate line above it owns the artist), and the grid
gained its plate marks plus press-ink feedback on every row through
`combinedClickableCompat`.

What remains before Play submission is release logistics, not product work:
store art and screenshots, the launch documents, a full functional pass on the
owner's main phone, and multi-screen-size verification.

Phase 0 is complete, Phase 1 is complete, and Phase 2 is largely complete. The repository is live at https://github.com/Kamsiob/meedwell, public, with 48 issues open and a first signed commit. The app builds, installs on the Pixel 8, and launches to the Welcome screen with the real design tokens and both bundled fonts.

**API verification is done, and it changed the product.** This is the most important thing on this page. The full evidence is in `DECISIONS.md` under 15 August 2026 and in `API-VERIFICATION.md`, whose tables are filled in. The three findings that changed what gets built:

1. **Tier C. There is no download endpoint.** Tested exhaustively: every casing, by track id and by album id. `stream` returns MP3 V0 and ignores every transcoding parameter. So the marquee Downloads screen does not exist, the already-designed "Your files" fallback replaces it, and **local folder scanning is now a version one requirement carrying the app's entire ownership story**.
2. **Playlists are read-only.** `createPlaylist`, `updatePlaylist`, `deletePlaylist` and `getPlaylist` are all absent. Lists live on this phone and say so. The two-way sync claim is withdrawn.
3. **`unstar` is broken** and answers in XML while ignoring `f=json`. A heart can be added and not removed. Stated in the interface rather than left to fail silently.

Three more that will bite anyone who forgets them:

- **`ping` does not enforce authentication.** It returns ok for a wrong password. Credentials are validated against `getArtists`. This will look like an obvious simplification to a future session. It is not one.
- **A failed login is HTTP 500 with an empty body**, not error code 40. That is the unexplained 401 from the field reports.
- **`{"error":true,"error_message":"bad version"}` means the route does not exist**, not that the version is wrong. Every version from 1.8.0 to 1.16.1 returns it on a bad path.

## What works right now, verified on the Pixel

Every item here was exercised on the device against the real account, not only compiled.

- **Connect, sync and the shelf.** Three albums, 60 tracks, artists and genres arrive and render with cover art. Albums, Artists and Genres are sibling views; grid and list both work.
- **Playback.** Media3 through a `MediaLibraryService`. Audio focus granted, `AudioTrack` live, media button session registered, gapless advance between tracks confirmed. Queue and position are written on every meaningful change.
- **Album screen**, including the collapsing toolbar and the legibility law: cover complete, hard edge, text only past it.
- **Now playing**, with the palette-derived wash clamped so white passes. The clamp is swept across 216 colors in a unit test.
- **Artwork viewer**, themeless by construction.
- **Mini player** with the waveform, which stills when paused.
- **Search**, done locally, with the Bandcamp browser handoff.
- **More, Privacy, What's ahead, About, Settings**, all with copy checked against what the software actually does.
- **Your files**, the Tier C surface, with the local scanner, matching, and reconciliation.
- **The play log**, which History, the Forgotten Shelf and most-played all read.

## Next concrete step

**Phase 3, the rest of the collection.** In rough order of value:

1. History and the Forgotten Shelf. Both read `play_event`, which is now being written, so both are mostly a screen each.
2. The action sheet (#13), which every surface needs and which several screens currently have a `TODO` comment where it belongs.
3. The queue sheet (#12), reachable from now playing.
4. Lists (#18) and Loved (#19), both local, both with their stated limits.
5. Artist pages.

Then Phase 5 platform surfaces (#26 through #30), Phase 6 export and restore (#3, a release blocker), and Phase 7.

**Do not skip #49.** A missing migration currently crash-loops the app with no way out. The no-destructive-migration decision is right; its handling is not, and a released user hitting it would lose everything.

## Remaining work inventory

- **Phase 0, repository and verification: complete and verified.** Repository created, `repo-seed` copied in, 48 issues opened, both modules scaffolded, design tokens implemented, tolerant parsing written and tested, smoke test passing on the device.
- **Phase 1, the working core: complete and device-verified.** See the list above.
- **Phase 2, ownership: largely complete.** Reshaped by Tier C into the "Your files" surface (#14). Watched folders, scanning, tag reading with `albumArtist` preferred, matching, local-only albums and reconciliation are all built. **Not yet verified end to end on the device with real files**, because that needs a folder of Bandcamp downloads on the phone to test against; the matching rules themselves have unit coverage.
- **Phase 3, the collection: partly done.** Search is built and verified. History, the Forgotten Shelf, Lists, Loved, artist pages, the action sheet and the queue sheet remain.
- **Phase 4, local files: not started.** Note that Tier C promoted folder scanning itself into Phase 2; what remains here is the tag and merge intelligence on top.
- **Phase 5, platform surfaces: not started.**
- **Phase 6, files and backup: not started.**
- **Phase 7, hardening and release: not started.**

The issue tracker is the item-level inventory from here on.

## What has been tried and did not work

- **Calling endpoints directly under the server address.** `https://bandcamp.com/api/subsonic/ping` returns `bad version`, which reads like a protocol mismatch and is actually a wrong path. The real base is `.../api/subsonic/rest/`. This cost the first twenty minutes of verification. The address the user pastes stays as designed; the client appends `/rest/`.
- **Tuning the protocol version to fix `bad version`.** Every value from 1.8.0 to 1.16.1 returns it on a bad path. The version was never the problem. Do not go down this road again.
- **Applying `org.jetbrains.kotlin.android` alongside AGP 9.** Fails the build outright: AGP 9 brings its own Kotlin support. The Compose compiler plugin is still applied and still tracks the Kotlin version exactly.
- **Drawing the mark's cradle with `arcTo` and a rect built from the arc's ends and its lowest point.** The ellipse's lowest point landed below the canvas, the arc was clipped, and the coin floated well clear of it. It looked wrong on the device before anything caught it. Now drawn as two quadratics meeting at the bottom center, so the join *is* the lowest point, and there is a unit test asserting the coin touches. Worth revisiting only with the test in place.
- **Capturing script-level values inside a Gradle `doLast`.** Breaks the configuration cache, which slows every build. Capture them as locals in the task registration block instead.
- **Truth's `assertWithMessage` with `%.2f`.** It takes `%s` placeholders only. Format the number before passing it.
- **Applying `padding` after a size modifier in Compose.** It shrinks the content inside the size rather than adding around it. The waveform rendered as a row of dots for this reason: a 38dp box with 18dp of padding left 20dp of canvas. Padding goes before the size.
- **A corner radius taken from bar width alone.** Short bars became circles. Cap the radius against height too.
- **Assuming a library merges a permission you rely on.** `FOREGROUND_SERVICE` was on the audit allowlist but never declared, and the service crashed the moment it went foreground. The audit fails on permissions that should not be there; it cannot notice one that should.
- **Adding a database column without bumping the schema version.** Room refuses to open the old database and the app crash-loops. Correct behavior, badly handled; see #49.

## Measurements

Real figures, not impressions.

- **The owner's collection: 3 albums, 60 tracks, 3.5 hours.** Far too small to validate any large-library work, so the synthetic emulator test (#33) carries that load alone and cannot be skipped on the strength of a fast real sync.
- **API response times: roughly 200 ms** for metadata calls. 20 rapid sequential calls averaged 105 ms with zero failures and no rate limiting.
- **Cover art: 700x700 JPEG, about 174 KB.** The `size` parameter is ignored, so the app resizes locally.
- **Tests: 60 in `:core`, 14 in `:app`.** All passing. The `:app` suite includes the contrast measurements for every token pair, the mark's construction rule, and the wash clamp swept over the color cube.
- Still to measure: sync duration on a large library, app size, cold start, memory during a large sync.

## Environment and toolchain notes

The development machine runs Bazzite, an immutable Fedora Atomic system with a read-only `/usr`, so installers that write to system directories will fail. Install into the home directory, a virtual environment, or a container.

**The build needs a JDK 21 Gradle daemon.** The machine's default JDK is 26 and AGP 9.3.1 does not run on it. No path is pinned in `gradle.properties`, because that file is committed. Before invoking Gradle:

```
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
export ANDROID_HOME=/var/home/Kamsiob/Android/Sdk
```

**Credentials and secrets live at `~/.kamsiob-secrets/`, outside the repository:**

- `meedwell-subsonic.env`, mode 600, the Bandcamp Subsonic credentials.
- `meedwell-api-responses/`, the raw responses saved during verification, one per endpoint. They hold the owner's real collection and are never committed. To run the parser against them: `MEEDWELL_API_RESPONSES=~/.kamsiob-secrets/meedwell-api-responses ./gradlew :core:test`.
- `play-service-account.json`, for Play automation.
- No Meedwell upload keystore exists yet. It is generated in Phase 7.

A pre-commit hook in `.githooks/` refuses any commit carrying credential-shaped content. Enable it in a fresh clone with `git config core.hooksPath .githooks`.

The device is a Pixel 8 on Android 17, API 37. Device rules in `MASTER_SPEC.md` section 10 are strict: touch nothing on the phone except this app, one copy only, in-place upgrades, destructive tests on an emulator, and never capture the screen unless Meedwell is in the foreground. `tools/capture-screen.sh` enforces that last one mechanically and refuses otherwise.

## Decisions a future session might reverse

See `DECISIONS.md`. The ones most likely to look wrong without their reasoning:

- **Validating credentials with `getArtists` rather than `ping`.** Looks like an obvious simplification. `ping` accepts any password.
- **The unencrypted database and unencrypted export.** Deliberate, and the reason is portability rather than laziness.
- **The retirement of the adaptive-scrim legibility law.**
- **Type labels rather than GitHub issue types.** Issue types need an organization account and this repository is under a user account.
- **Keeping `unstar` in the client although it always fails.** The app has to be able to explain why a heart will not come off.

## Waiting on the owner

See the BLOCKED section of `DECISIONS.md`.

- **Two starred items cannot be removed.** Verification had to star something to test `star`, and `unstar` is broken server side. One track, "Wolf Blood", and the album "The Celtic Collection II" are now starred on the real Bandcamp account and can only be unstarred on Bandcamp's website.
- **A Bandcamp pre-order would unblock issue #46.** The collection contains none, so how pre-orders are represented could not be verified.
- The known manual Play Console steps, collected in `LAUNCH.md` at Phase 7 rather than raised one at a time.

## Open questions and unverified assumptions

- **ID stability over time.** IDs are prefixed and consistent within a day (`b:` band, `a:` album, `t:` track, `ca:` cover art, `ci:` artist image). Everything in the local database depends on them being stable across weeks. This is an assumption, not a verified fact. If albums start duplicating after a gap, look here first.
- **How pre-orders are represented.** Unverifiable without one in the collection.
- **Whether float durations ever appear.** They did not on this account. The field reports exist, the beta changes, and tolerant parsing is built regardless.
- **Whether `unstar` and the playlist write endpoints arrive later.** The client is written so that each starts working without other changes if they do.
