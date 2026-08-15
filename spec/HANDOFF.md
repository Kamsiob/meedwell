# Handoff

The resume document. Kept current at all times so a session with no memory of any previous conversation can pick up cleanly after a disconnection, a crash, context exhaustion, or a gap of weeks.

**Read this file in full at the start of every session.** That is its purpose. Then search the other documents for the sections relevant to the current item rather than loading them whole.

---

## State of play

**Last updated: 15 August 2026.**

Phase 0 is complete. The repository is live at https://github.com/Kamsiob/meedwell, public, with 48 issues open and a first signed commit. The app builds, installs on the Pixel 8, and launches to the Welcome screen with the real design tokens and both bundled fonts.

**API verification is done, and it changed the product.** This is the most important thing on this page. The full evidence is in `DECISIONS.md` under 15 August 2026 and in `API-VERIFICATION.md`, whose tables are filled in. The three findings that changed what gets built:

1. **Tier C. There is no download endpoint.** Tested exhaustively: every casing, by track id and by album id. `stream` returns MP3 V0 and ignores every transcoding parameter. So the marquee Downloads screen does not exist, the already-designed "Your files" fallback replaces it, and **local folder scanning is now a version one requirement carrying the app's entire ownership story**.
2. **Playlists are read-only.** `createPlaylist`, `updatePlaylist`, `deletePlaylist` and `getPlaylist` are all absent. Lists live on this phone and say so. The two-way sync claim is withdrawn.
3. **`unstar` is broken** and answers in XML while ignoring `f=json`. A heart can be added and not removed. Stated in the interface rather than left to fail silently.

Three more that will bite anyone who forgets them:

- **`ping` does not enforce authentication.** It returns ok for a wrong password. Credentials are validated against `getArtists`. This will look like an obvious simplification to a future session. It is not one.
- **A failed login is HTTP 500 with an empty body**, not error code 40. That is the unexplained 401 from the field reports.
- **`{"error":true,"error_message":"bad version"}` means the route does not exist**, not that the version is wrong. Every version from 1.8.0 to 1.16.1 returns it on a bad path.

## Next concrete step

**Phase 1, the working core.** Start with the Connect flow, issue #6's screen half, then sync (#7), then the shelf (#9).

Before writing the shelf's sort menu, read issue #48: three of the API's list types return empty, so the specified "most played" sort has to be computed on device instead.

## Remaining work inventory

- **Phase 0, repository and verification: complete and verified.** Repository created, `repo-seed` copied in, 48 issues opened, both modules scaffolded, design tokens implemented, tolerant parsing written and tested, smoke test passing on the device.
- **Phase 1, the working core: not started.**
- **Phase 2, ownership: not started.** Reshaped by Tier C into the "Your files" surface (#14) rather than a download manager. There is no download queue, no foreground download service and no storage-exhaustion path, because nothing is being fetched.
- **Phase 3, the collection: not started.**
- **Phase 4, local files: not started.** Note that Tier C promoted folder scanning itself into Phase 2; what remains here is the tag and merge intelligence on top.
- **Phase 5, platform surfaces: not started.**
- **Phase 6, files and backup: not started.**
- **Phase 7, hardening and release: not started.**

The issue tracker is the item-level inventory from here on.

## What has been tried and did not work

- **Calling endpoints directly under the server address.** `https://bandcamp.com/api/subsonic/ping` returns `bad version`, which reads like a protocol mismatch and is actually a wrong path. The real base is `.../api/subsonic/rest/`. This cost the first twenty minutes of verification. The address the user pastes stays as designed; the client appends `/rest/`.
- **Tuning the protocol version to fix `bad version`.** Every value from 1.8.0 to 1.16.1 returns it on a bad path. The version was never the problem. Do not go down this road again.
- **Applying `org.jetbrains.kotlin.android` alongside AGP 9.** Fails the build outright: AGP 9 brings its own Kotlin support. The Compose compiler plugin is still applied and still tracks the Kotlin version exactly.
- **Drawing the mark's cradle with `arcTo` and a rect built from the arc's ends and its lowest point.** The ellipse's lowest point landed below the canvas, the arc was clipped, and the coin floated well clear of it. It looked wrong on the device before anything caught it. Now drawn as two quadratics meeting at the bottom centre, so the join *is* the lowest point, and there is a unit test asserting the coin touches. Worth revisiting only with the test in place.
- **Capturing script-level values inside a Gradle `doLast`.** Breaks the configuration cache, which slows every build. Capture them as locals in the task registration block instead.
- **Truth's `assertWithMessage` with `%.2f`.** It takes `%s` placeholders only. Format the number before passing it.

## Measurements

Real figures, not impressions.

- **The owner's collection: 3 albums, 60 tracks, 3.5 hours.** Far too small to validate any large-library work, so the synthetic emulator test (#33) carries that load alone and cannot be skipped on the strength of a fast real sync.
- **API response times: roughly 200 ms** for metadata calls. 20 rapid sequential calls averaged 105 ms with zero failures and no rate limiting.
- **Cover art: 700x700 JPEG, about 174 KB.** The `size` parameter is ignored, so the app resizes locally.
- **Tests: 23 in `:core`, 9 in `:app`.** All passing.
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
- **Type labels rather than GitHub issue types.** Issue types need an organisation account and this repository is under a user account.
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
