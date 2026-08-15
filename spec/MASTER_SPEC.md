# Meedwell by Kamsiob: master specification

**Precedence.** This document, `DESIGN.md`, `DECISIONS.md`, and the open GitHub issues are the current source of truth for this project. Anything in an older prompt, an earlier conversation, or a superseded document that conflicts with these is superseded. This is a living document: it is updated with every commit so it always describes the app as it currently is and as it is intended to be. Superseded instructions are corrected in place, never left beside their replacements, and anything still pending is marked pending rather than described as built.

---

## 1. What Meedwell is and who it is for

A free, open source (AGPL-3.0), zero-telemetry Android music player built for Bandcamp's Subsonic API, which also plays local files. Kotlin. It would be the first purpose-built Android Bandcamp client: Bandcamp officially lists only Amperfy on iOS, Feishin, and Submariner on Mac as supported clients.

Positioning: **for people who buy their music.** The player where your music is actually yours.

The mission, woven through the app's own copy: subscriptions squeeze the people who listen and starve the people who make; Bandcamp proves another model works; Meedwell exists to make owning music feel better than renting it.

It is for someone who already buys music, on purpose, and wants a player that treats those purchases as possessions rather than as another catalogue to browse. The anti-Symfonium point is concrete rather than rhetorical: Symfonium encrypts its offline cache, so its downloads are hostage to the app. Meedwell writes real files to the public Music folder that survive uninstall and open in any player.

Name history, so it is not relitigated: "Kamp" was killed because kamp.fm exists, a desktop-only Mac and Windows Bandcamp player with near-identical positioning, which also validates the concept. "Krate" is taken twice. "Siob" (the back half of "Kamsiob") was clear but was retired by the owner because it tells a stranger nothing and cannot be spelled from hearing. **"Meedwell"** is the final name: *meed* is Old English, attested before 900 and appearing in Beowulf, meaning an earned reward or a fitting recompense, the wage honored; *well* is how it is paid. The name is the mission in one word: the earned reward, honored well. The compound is unclaimed anywhere (no app, no repository, no company). Adjacency recorded for honesty: "meed Loyalty" is an operating customer-loyalty company using the bare root in a different category; the compound does not collide with it, but search results for the root alone reach them first. Spelling is **Meedwell**, never "Meadwell", which is a different word.

Legal posture: "Bandcamp" is used nominatively only. The About screen and the store listing carry "Not affiliated with or endorsed by Bandcamp." No scraping of anything, anywhere, for any reason. The only network peer is Bandcamp's own API server, plus URLs handed to the user's browser or share sheet.

## 2. Values, non-negotiable

Zero data collection by design. Everything local. No tracking, no analytics beyond unavoidable platform defaults such as Play install counts.

No paywalls, no subscriptions, no accounts of Meedwell's own, no logins, no ads.

Network access only for what genuinely requires it, and only for that. Streaming and syncing the user's collection requires Bandcamp's API. Nothing else phones anywhere.

Monetisation is an optional donate link only. The visible label is always "Support this work", linking to https://buymeacoffee.com/kamsiob. Never coffee or caffeine cliches, never framing that anchors support to small amounts, never anything reading as begging or pressure.

AGPL-3.0. Any bundled content carries its own license alongside, correctly attributed.

Honest limits are a feature, stated in the interface and in all public copy.

Legal and terms of service compliance, applied as a filter before building rather than a caveat afterward.

No dark patterns. No engagement mechanics, streaks, badges, or nagging notifications.

Named "Meedwell by Kamsiob" in the About screen, the README title, and store listings. "Meedwell" alone is fine inside the interface.

## 3. The Bandcamp Subsonic API: what is known

**Verified against the live account on 15 August 2026.** What follows is what the server actually does, not what the protocol says. The full evidence is in `API-VERIFICATION.md` and `DECISIONS.md`.

- Open beta opened 16 July 2026. The server address a user pastes is `https://bandcamp.com/api/subsonic`, and **every call goes to `.../api/subsonic/rest/<endpoint>`**. Users generate credentials under Fan Settings, then Subsonic: the server address, a long generated username, and a long generated password.
- **The API exposes only the user's own collection.** There is no store or catalogue search. This shapes the entire Search screen and is the single most important constraint on the app's scope.
- Auth is token and salt: `t` is MD5 of password concatenated with salt, `s` is the salt. Send `v=1.16.1`, `c=Meedwell`, `f=json`. `getOpenSubsonicExtensions` returns an **empty** array, so there is no `apiKey` path and no extensions.
- **`ping` does not enforce authentication.** It returns ok for a wrong password. Credentials are validated against `getArtists` instead. This is a trap that will look like a simplification opportunity to a future session; it is not.
- **A failed login is HTTP 500 with an empty body**, never a Subsonic error code. There are no codes 40 through 44 to map. This is the cause of the unexplained 401 in the field reports.
- **`{"error":true,"error_message":"bad version"}` means the route does not exist**, not that the version is wrong. It is not a `subsonic-response` envelope.
- **There is no `download` endpoint.** Streaming is MP3 V0 and the transcoding parameters are ignored. This settles the tier decision at C; see section 6.
- **Playlists are read-only.** `getPlaylists` exists; `createPlaylist`, `updatePlaylist`, `deletePlaylist` and `getPlaylist` do not. Lists cannot sync to the Bandcamp collection, and the earlier claim that they could is withdrawn.
- **Loved is one-way.** `star` works. `unstar` is broken server side and answers in XML while ignoring `f=json`.
- `getArtists` and `getGenres` are present and carry the Artists and Genres browse views. `getStarred` carries Loved; `getStarred2` is absent.
- **`getAlbumInfo2` is absent, so liner notes do not exist** and that section is not built.
- Field reports that did **not** reproduce: durations are integers here, not floats, and `getAlbumList2` does include `coverArt`. Tolerant parsing is built regardless, because the beta changes underneath us and a three-album account proves little. Two real data bugs were found instead: `getStarred` returns `idDir` where the schema says `isDir`, and `genres` arrays contain duplicates.
- **No response carries a Bandcamp page URL** for an artist or album. The only URL anywhere is `artistImageUrl`. So "Their Bandcamp page" and all share text use a constructed `bandcamp.com/search?q=NAME&item_type=b` deep link.
- `getCoverArt` ignores its `size` parameter and always returns the same 700x700 JPEG, so the app resizes locally.
- No rate limiting observed and no caching headers returned. Sync is sequential with no artificial delay and no parallel fan-out, decided deliberately.

`API-VERIFICATION.md` holds the completed protocol and the filled-in result tables.

## 4. Technical stack

- Kotlin, Jetpack Compose, Material 3 with a fully custom theme implementing the `DESIGN.md` tokens, bundled fonts, single activity.
- Verify the current Play target API requirement and set accordingly. As of the last check, API 36 was required from 31 August 2026.
- **Media3 via `MediaLibraryService`**, which gives notification and lock screen controls and makes Android Auto nearly free later. `media3-datasource-okhttp`. Native FLAC. Gapless playback.
- Two Gradle modules, `:core` with no Android dependencies and `:app`, per `MASTER_PROMPT.md` section 4. The no-Android-dependency rule in `:core` is enforced by not applying the Android plugin, so a violation fails the build.
- Room over a single SQLite database, with an append-only `play_event` table powering the Forgotten Shelf, History, and stats, all on device. **Not encrypted**, deliberately, per `MASTER_PROMPT.md` section 7: the database is a catalogue and a play log, and it must stay exportable as plain portable SQLite for a future desktop or web build.
- **Credentials in `EncryptedSharedPreferences` only.** Never in the database, never in an export, never in a log, never in a crash report.
- Downloads written as real files to `Music/Meedwell/Artist/Album` via `MediaStore.Audio` inserts using `RELATIVE_PATH` and `IS_PENDING`. `READ_MEDIA_AUDIO` is needed to see the app's own files again after a reinstall.
- Crash handling: ACRA in local-only mode. The crash sheet shows the full report and the user reads it before choosing to share it manually. Embed the stacktrace in `EXTRA_TEXT` to work around ACRA's attachment bug. Nothing auto-sends, ever.
- Ambient colour: `androidx.palette` plus `material-color-utilities`.
- Waveform amplitude comes from a custom `AudioProcessor` tap inside Meedwell's own Media3 pipeline, which needs zero permissions. **Never the `Visualizer` API,** which requires `RECORD_AUDIO`. Downloaded files get a full envelope precomputed once and stored locally.
- Bandcamp Friday dates are fetched as versioned JSON from a public GitHub release asset with SHA-256 verification, using the same pattern as Bearings content updates and Local AI Hub model installs. Remaining 2026 dates, midnight to midnight Pacific: 7 August, 4 September, 2 October, 6 November, 4 December. If the file cannot be reached the feature is simply absent, and the gold dot never appears on a wrong day. Do not hardcode the list into the binary.
- Play Data Safety: "no data collected, no data shared" is defensible, since Bandcamp traffic falls under the ephemeral service-provider exemption. Store category: Music and Audio.

Do not trust any library or framework version named here as current. Check actual current releases and recommended integration paths before integrating, and record what was chosen and why.

## 5. Feature scope, version one

**Connect.** A guided three-step flow for the Bandcamp Subsonic credentials, address prefilled, paste chips on each field, a reveal eye on the password, and an "Open Bandcamp for me" link to the settings page where the credentials are generated. Beta-honest copy throughout.

**The Shelf.** A merged, album-first shelf of the Bandcamp collection and local files. Albums, Artists and Genres are three sibling first-class views, switched at the top; the Downloaded and Local scope filters live in the sort menu rather than competing with the view switcher. Grid and list layouts, toggled beside search. List rows carry 48dp art, artist, year, format, and a downloaded dot, at a 56dp minimum height. An A to Z fast scroller appears on scroll, with its index built once and cached rather than recomputed. Sort by recent, artist, purchase date, most played, plus the scope filters. A labelled tab bar: Shelf, Search, Lists, More.

**Newest arrival card** at the top of the shelf: the complete cover beside its caption, never underneath it.

**Playback.** Media3, gapless. Streams are MP3 V0, which is what Bandcamp's API serves, and no copy anywhere implies otherwise. Queue as a glass bottom sheet with drag reorder, swipe to remove, honestly random shuffle, and Save as list which writes the queue to a list on this phone. Now playing shows the complete cover above a palette-drawn wash, with the live waveform below it as the scrubber: played bars burn white, drag anywhere to seek. Sleep timer with a visible countdown when running. Long-track resume for pieces over twenty minutes, surfacing as "Resume from 22:40" on track rows, with a Settings toggle. The sleep timer offers "when the track ends" alongside fixed durations, and a running timer can be extended by fifteen minutes in one tap.

**Playback resilience, added in the July 2026 design review.** These are version-one requirements, not polish. Audio focus is handled completely: playback pauses when headphones disconnect (the becoming-noisy broadcast), yields to calls and other apps taking focus, ducks for transient interruptions such as navigation prompts, and resumes only after a transient loss, never after a permanent one. Media buttons on wired and Bluetooth headsets work for play, pause, and skip. Meedwell never auto-plays on Bluetooth connection: a car or speaker grabbing the session unprompted is the opposite of restraint. The queue, the current track, and the playback position survive process death and reboot; reopening the app lands on the same queue, paused where it left off, with no spinner and no re-sync required first. Swiping the now-playing cover left or right skips forward or back; tap still opens the artwork viewer, and the two gestures never conflict.

**Platform decisions, same review.** Version one is portrait-first: every screen survives rotation without losing state (the hostile path already tests this), but no landscape-specific layouts are designed, and tablet or large-screen layouts are deferred to Being Considered with that stated as the reason. Version one ships in English only, stated plainly in the store listing rather than discovered; all user-facing strings are externalized from the first commit so translation later is an addition, not a rewrite.

**The action sheet.** Long-press anywhere a track or album lives opens the same eight verbs, app-wide, in this order: Play next, Add to queue, Add to a list, Love, Download, View artwork, Go to artist, Share.

**Your files, which is Tier C.** Verification found no download endpoint, so the marquee Downloads screen does not exist and the already-designed fallback, screen 26, takes its place. Meedwell says plainly that Bandcamp's API streams the collection but does not release the files, links out to Bandcamp where the user downloads them the way they always have, then watches folders and matches what arrives back onto the shelf as owned, with per-album "8 of 10 tracks found" honesty. The ownership claim is untouched, because it was always about portable files rather than about who fetches them.

Consequence: **local folder scanning and matching is a version one requirement**, not a Phase 4 convenience, and it moves forward in the phase plan accordingly.

**Search.** The user's own collection, split into albums, tracks and artists, with "Search all of Bandcamp for 'x'" at the bottom as a browser deep link using `https://bandcamp.com/search?q=QUERY` and the `item_type` parameter, where `b` is artists, `a` is albums and `t` is tracks. Nothing about the search leaves the phone except that deliberate handoff to the browser.

**Lists, which are local.** Full create, read, update and delete, stored in Meedwell's own database. Bandcamp's API is read-only for playlists: it will show any playlist the account already has, and it offers no way to create, edit or delete one. So lists live on this phone and say so, in exactly the wording local-only mode already uses. Any playlist the account does have appears alongside them, marked as coming from Bandcamp and not editable here. "Save as list" saves the queue to the phone, and the screen never claims the edit reached Bandcamp.

**Loved, which is one-way.** Built on `star`, which works, and `getStarred`, which carries the screen. `unstar` is broken server side and returns an error, so a heart set through Meedwell can be set and not removed. The interface states this at the moment it matters rather than offering a control that silently fails, and points the user at Bandcamp's website to remove a heart. If Bandcamp fixes `unstar`, the control becomes symmetric and the honest-limit line disappears.

**History.** A recently-played screen grouped by day, read straight from the existing `play_event` table. Erasable in Settings.

**Artist pages.** Their albums, with owned ones marked "yours" in serif italic, in-rotation figures from local history, and a prominent "Their Bandcamp page" link with copy noting the money goes to them and Meedwell takes no cut.

**Sharing.** `ACTION_SEND` to Android's own share sheet only, plain Bandcamp link text, zero in-app fetching. Covered explicitly on the Privacy screen under "Sharing and outside links".

**Rediscovery: the Forgotten Shelf.** Computed entirely on device from play history: never played, played twice ever, quiet for fourteen months. No algorithm, no feed, nothing sent anywhere, and the copy says so.

**Artwork viewer.** One tap behind every cover in the app, from the album header, now playing, and the action sheet's View artwork row. Themeless by design: the complete art on near-black in both themes, no text over it, pinch to zoom.

**Transparency screens.** Privacy, as five plain questions and answers. What's Ahead, split into Being Considered, which includes in-app Bandcamp store search waiting on their API, an equalizer, Android Auto, other Subsonic servers, tablet and large-screen layouts, languages beyond English, and F-Droid distribution, which names its real constraint: F-Droid inclusion requires reproducible builds, which this project deliberately does not pursue, and Not Planned, which includes accounts, ads, telemetry and subscriptions framed as decisions rather than gaps, and no in-app store because buying happens on Bandcamp. About, carrying the mission copy, the links, the non-affiliation line, the support framing and the gold button.

**Settings.** Theme with dark as default plus light and system; App icon with five finishes; Shelf view; gapless toggle; long-track resume toggle; Library covering local music folders and wifi-only downloads; Your data covering Export and restore and Erase listening history; then the support value block and the gold Support this work button at the bottom.

**App icon finishes,** switched instantly through Android's activity-alias technique, which costs nothing and needs no network: Rustic Copper as default, Dusk, Moss, Ink, Paper.

Deferred to 1.1 and later: equalizer, casting, Android Auto, multiple servers, opt-in bring-your-own Last.fm scrobbling, a home screen widget, and a yearly shelf-in-review recap. Never: social features, an in-app store, accounts, telemetry.

## 6. The download endpoint decision, resolved: Tier C

**Decided 15 August 2026 on the evidence. There is no download endpoint.** `download` was tested as `download`, `download.view` and `Download`, by track id and by album id, and every form returned Bandcamp's unknown-route body. `stream` always yields MP3 V0 and ignores `format=raw`, `format=flac` and `maxBitRate`. The API's own metadata agrees, reporting every track as `suffix: mp3` at 256 kbps. Full evidence in `DECISIONS.md`.

So **Tier C is what gets built**, and the fallback screen was already designed. The three tiers are kept below because a beta can change: if Bandcamp ships a download endpoint later, this is the decision to revisit and the copy that changes with it.

**Tier A, original purchased quality**, FLAC or whatever the purchase format was. Everything in the design and all copy stands as written. The Downloads screen keeps the marquee "Download everything, about X GB" button.

**Tier B, transcoded only**, for example MP3 at some bitrate. Downloads still happen, still land as real portable files, still outlive the app. Exactly one line of copy changes: "best quality Bandcamp provides" becomes the actual format and bitrate, named plainly, on the Downloads screen and in the store listing. The differentiator is untouched, because it was always about ownership and portability rather than about lossless.

**Tier C, no usable download endpoint.** The marquee screen is replaced by the fallback already designed in the grid, titled "Your files": Meedwell says plainly that the API streams but does not release files, links out to Bandcamp where the user downloads them the way they always have, then watches folders and matches what arrives back onto the shelf as owned. The ownership claim survives because portable files are still the outcome; only who fetches them changes. Local folder scanning and matching becomes a version one requirement rather than a convenience.

Also verify and record: whether the endpoint respects the wifi-only setting, whether it rate limits a run of several gigabytes, and what it returns for pre-orders and unreleased items.

## 7. Local files only mode, fully specified

This is a second product, not a fallback, and it needs stating because a user who never connects an account must never meet sync language the app cannot honour.

The Shelf reads local files only, with its own voice line, "31 albums on this phone, no account involved", and Folder available as a sort. Artists and Genres are built from file tags. Lists work, stored in the app's own database, labelled as living on this phone. Loved works the same way, local only, and the "synced with your Bandcamp account" line is **absent** rather than shown and broken. Search covers local files and keeps the Bandcamp browser deep link, since that is only a URL handed to the browser and is genuinely useful. Forgotten Shelf and History work unchanged, both reading the local play log. Downloads is replaced by the local folders screen. Connect Bandcamp stays permanently reachable from More and from Settings.

**Tag handling,** which local-only mode makes load-bearing: read `albumArtist` before `artist` so compilations do not shatter into one album per track; respect disc numbers; group loose singles under a plainly labelled bucket rather than inventing album names; treat a folder with no usable tags as its folder name.

**Merge rule when an account is connected later:** match on artist plus album plus track, never duplicate, prefer the local file for playback, mark the album as owned. The reverse case is likelier than it sounds and must work too: someone who downloaded from Bandcamp's website before installing Meedwell should find those files recognised and merged, not duplicated alongside the streamed copies.

## 8. Export and restore, the full round trip

Per the standing data portability rules, restore must be as easy as backup, equally tested, and verified onto a fresh install, onto an install that already has data, and onto a less capable device.

**The export file carries** listening history, lists and hearts made on this phone, resume points on long pieces, every setting, and the download manifest with file paths. **It does not carry** the audio, and the app says so plainly, because the audio is already the user's, sitting in `Music` where any app can read it. The manifest exists so a restore can re-find those files rather than re-fetch them. It carries no credentials.

**Restore** replaces app data in one atomic operation after an explicit confirmation. Never a half import. Never silently drops data a version does not recognise: it says what it did not understand. Replace rather than merge is the deliberate choice, because merging two divergent listening histories is genuinely ambiguous, and the interface states that rather than guessing.

**Format is versioned from release one.** The file says version one, so a version one file imported into a much later app can be migrated cleanly.

**Round trip equality testing** is the gate, not whether the import completes. Populate the app with a realistic spread of every data type including the awkward cases: archived and pinned state, completion flags, ordering, timestamps, relationships between records, empty and edge values, unicode and very long text. Export, wipe completely, import, then assert equality field by field.

**Automatic backup:** Android supports genuine automated local backup to a user-chosen folder through the Storage Access Framework's persistent permission plus WorkManager scheduling, with no cloud involved. Offer it, triggered by accumulated change rather than elapsed time. Keep a quiet permanent "last backed up" line that never nags, and never use system notifications for it.

## 9. States, designed rather than discovered

- **First sync.** Playable immediately, beta-honest copy, waiting tiles shimmering in the incoming album's own colour rather than grey.
- **Offline.** A quiet banner, the shelf becomes what is downloaded, no spinner and no apology.
- **Connection trouble.** The real error code, the last successful sync time, a "Get fresh credentials" link, and a "Stay offline for now" option.
- **Nothing here yet.** Connected with an empty collection. Names both ways to fill it and never scolds. Zero local files, zero search results and an empty list each get their own copy.
- **Files that went missing.** The property making downloads real also makes them deletable in the Files app, and cards unmount. Meedwell reconciles rather than trusting its own database: detect absent files, mark those albums not downloaded, change nothing else, offer either a re-download or leaving them as streaming. Framed as the honest cost of real files, not as a bug.
- **Storage exhaustion.** A download that cannot finish stops cleanly, keeps the tracks that landed, states how much room it needed, leaves nothing half written.
- **One track that will not play.** A quiet banner, the failed row marked and retryable in place, and playback moving on rather than stopping. On a beta API this will happen, and it is the failure the listener actually notices.
- **Enormous collections.** Incremental and resumable sync, paged requests, art loaded lazily with a bounded disk cache, the A to Z index cached, and a stated expectation for first sync duration. Test with a synthetic large library on an emulator.
- **Permissions.** Two, both narrow, both explained in Meedwell's own words before Android's dialog appears: notifications, only so the player appears in the shade and on the lock screen and never used for anything else; and music and audio files, so Meedwell can see its own downloads and existing local music. Declining either leaves the app working with exactly one capability missing, stated plainly and reversible in Settings.
- **Crash.** The local report shown in full, read before send, nothing auto-sent.

## 10. Testing protocol

Testing is continuous, not a final phase. Each phase has a gate that must pass before the next begins: unit tests for logic, instrumented tests for behaviour, and a manual pass on the real device.

**Every test suite must actually compile and run,** including instrumented sets that are not part of the default build and therefore rot silently. Verify at the start of every session and after any change to a shared interface. Where a suite has known failures caused by the toolchain rather than by defects, document the exact command that separates real failures from that noise, record it in `DECISIONS.md` and `HANDOFF.md`, and use it every time.

Walk complete user journeys end to end across feature boundaries, not only individual functions. Run a regression sweep of all previous phases after each new phase. Every bug fixed gets a regression test.

**User testing scripts,** at minimum: a first-time user connecting and playing something; a heavy daily user exercising downloads, lists and search; a local-files-only user who never connects an account; and a hostile path.

**The hostile path must include:** no network, no storage space, denied permissions, process death at every stage of every long operation, a download interrupted mid-album, files deleted underneath the app, corrupt and truncated files, rotation and resizing on every screen, rapid repeated input, very long inputs, very long sessions, and low memory.

**Device rules.** A physical device is connected over ADB for installs, instrumented tests and screenshots. Touch nothing else on the phone, ever. Exactly one copy of the app exists on the device at all times and it is the current build; never install a second, parallel, older or differently named copy for any reason, including as a way to protect data during a test, and always update in place rather than uninstalling. Destructive and data-affecting tests, including schema migrations, wipes, storage exhaustion and corruption handling, go on an emulator; if one genuinely cannot, say so and ask first, and pull the app's data off as a safety copy. Never capture the screen unless Meedwell is in the foreground, enforced mechanically in the capture script rather than by timing, because a mistimed capture can put personal content into a public repository.

**Accessibility verification** is a gate, not a pass at the end. The waveform scrubber is a custom drag control and is invisible to TalkBack unless built with explicit seek semantics: a slider role, spoken position and duration, and increment and decrement actions. It is the app's signature interaction and therefore its most likely accessibility failure. Every screen tested at 200 percent font scale and with display size enlarged, with the album screen, Settings and the action sheet as the known pressure points. Touch targets 48dp minimum. Contrast measured rather than eyeballed for any new colour pair.

## 11. Phase plan

**Phase 0, repository and verification.** Initialise git; create the public GitHub repository under the kamsiob account with the gh CLI, matching the description style, topics and conventions of the other kamsiob repositories; copy in `repo-seed/`; commit the specification and design documents, the visual reference, and the .gitignore in the first commit; open the seeded issues; set up the project board; scaffold the two modules with the design tokens implemented; prove the app launches with a smoke test. Then run `API-VERIFICATION.md` in full and record every answer in `DECISIONS.md`, including the download tier decision. Implement tolerant parsing before anything else.

**Phase 1, the working core.** Connect flow, sync, the shelf in both layouts with all three views, the album screen, playback through Media3 with the queue, now playing with the waveform, and the mini player. To the point where a person could use it daily. Every screen implemented to `DESIGN.md` including motion, empty states and error states.

**Phase 2, ownership.** Verification decided Tier C, so this phase is the "Your files" surface rather than a download manager: watched folders, the local scan that feeds them, matching what arrives back onto the shelf with honest per-album counts, reconciliation when files move or vanish, and the precomputed waveform envelope for files that are present. There is no download queue, no foreground download service and no storage-exhaustion path, because nothing is being fetched.

**Phase 3, the collection.** Search, local lists, Loved as a one-way heart with its stated limit, History, artist pages, the action sheet, and the Forgotten Shelf.

**Phase 4, local files.** Tag handling, the merge rules in both directions, and local-only mode as a complete surface. Note that Tier C promoted folder scanning itself into Phase 2, since the app's ownership story now runs entirely through it; what remains here is the tag and merge intelligence on top.

**Phase 5, platform surfaces.** Share targets, the Bandcamp Friday manifest fetch, app icon aliases, and the permission explainers. Each with the minimum permissions required, and the merged manifest audited after every dependency addition, since libraries introduce permissions silently.

**Phase 6, files and backup.** Export, restore, automatic backup, and the round-trip equality test.

**Phase 7, hardening and release.** Full self-review against `DESIGN.md` screen by screen in both themes; the complete user testing protocol on the device and on emulator profiles representing weaker hardware; accessibility verification; real ADB screenshots for the README recaptured in full; store assets generated from the design system; the cold read test on the repository; version selection with one line of reasoning; the signed bundle and the release, following the corrected signing and distribution path in `MASTER_PROMPT.md` section 7; and `LAUNCH.md` listing the owner's exact remaining clicks in plain numbered steps and nothing else.

## 12. Process rules

**Autonomy.** Execute every phase in order without pausing for approval. Decide judgment calls, prefer the simpler and more reversible option, log them, continue. Anything genuinely requiring the owner goes under BLOCKED in `DECISIONS.md` with exactly what he needs to do; skip it and keep building everything that does not depend on it.

**Do not end a turn while work remains.** The most common failure in a long unattended run is ending a turn because a task felt complete. Finishing an item is the trigger to begin the next one in the same turn. If unsure what comes next, take the next item from the remaining work inventory in `HANDOFF.md`. When a duration is set, check the real system clock with a `date` command after each item rather than estimating elapsed time.

**If build work runs out before the time does,** spend the remainder on user acceptance testing per section 10. Finding no problems is not a reason to stop; look harder in whatever was tested least.

**Context discipline.** Do not load whole documents into context. Read `HANDOFF.md` in full once at the start of a session, since that is its purpose, then search the other documents for the sections relevant to the current item. Scan issue titles and states rather than reading every comment, and open an individual issue only when about to work on it.

**Living documents.** With every commit, update `MASTER_SPEC.md`, `DESIGN.md`, `HANDOFF.md`, `DECISIONS.md`, `README.md`, `ARCHITECTURE.md`, `CHANGELOG.md` at each release, and `ARTIFACT-CHECKLIST.md` when an artifact's status changes, so they describe the app as it currently is. This is part of the definition of done for every change, not periodic cleanup.

**Handoff discipline.** The owner should never have to ask for a handoff, because the repository should already be resumable at any moment. Update and commit `HANDOFF.md` whenever any item is finished or partially finished, at every commit, before any pause, when available context starts running low rather than after, whenever something fails or is rejected while the details are fresh, and whenever a decision is made that a future session might reverse. Keep it accurate rather than optimistic: overstating completion is worse than admitting something is half-finished, because the next session builds on top of it and the error compounds.

**Issues as specifications.** Open an issue for every bug, feature and enhancement, including ones discovered rather than reported, at the moment of discovery. Use GitHub's issue **type** field rather than a type label. Labels carry area and release-blocking status only. Acceptance criteria in checkable terms. Real working notes as progress happens, not only a closing comment. Issue numbers referenced in commit messages. Close only when genuinely done and device-verified, and reopen anything closed prematurely.

**Board.** One project board with a single-select Status field, automation configured before populating, work in progress genuinely limited, and every blocked item naming its blocker. Keep it current during active development. Going quiet during a genuine pause is not staleness; on a public repository it honestly signals the project is not currently being worked on.

**Branches and pull requests** for substantive work, each referencing its issue, with a passing integration run and a note on what was tested. Going forward only, never by rewriting history.

**Signed commits** using SSH signing with vigilant mode on the account. Going forward only.

**Commit message convention** documented in `CONTRIBUTING.md` and followed without exception. Consistency matters more than which convention.

**Versioning.** Semantic versioning chosen by you: bug fixes bump the third number, backward-compatible features the second, breaking changes the first. State the number and the reasoning in one line. The owner does not track version numbers.

**Secrets never enter the repository.** It is public. The Play service account key and the upload keystore live outside it or in a directory covered by the .gitignore written in the first commit. If a credential file is found near the project at the start of a run, move it somewhere protected, reference it by path, and record where it went.

**One copy only,** on the machine and on the device. Delete or overwrite previous builds so only the latest exists.

**AI-slop check** before writing any store copy, README, or website page: research current tells in both language and visual design first, then deliberately avoid them.

**The cold read test** before any release. Open the repository as a stranger: is it obvious what this is, what state it is in, and what is next? Open five issues at random including old ones: does each state its situation, its significance, and how to verify completion? Trace three recently closed issues to the commits that resolved them. Does every blocked item name its blocker? Does the milestone percentage match what `HANDOFF.md` says? Do the badges show real state? Read the README as a stranger and verify every factual claim against the built software, recording any unverifiable claim as a finding.

## 13. Play Console and distribution

The Play Console account is an organisation account under the legal entity B7 Collective, public developer name **Kamsiob**, so B7 Collective is visible only if a user opens the about-the-developer view. The organisation account was chosen deliberately because it avoids the closed-testing requirement personal accounts face.

Play automation uses the service account `kamsiob@kamsiob-503213.iam.gserviceaccount.com` in Google Cloud project `kamsiob-503213`, with the Android Publisher API enabled and admin rights in the Play Console. Its JSON key is provided outside the repository and must never be committed. The older service account under the owner's personal Google account is decommissioned and obsolete.

Identity verification for the Android developer verification mandate is already complete under B7 Collective, and new apps auto-register their package name when created in Play Console, so nothing is needed there. Context for why it matters: unregistered package names stop being installable on certified Android devices from September 2026 in selected countries and globally in 2027.

**Signing and the release path follow `MASTER_PROMPT.md` section 7,** which supersedes the older approach: Play App Signing, an upload key only, and the Google-signed universal APK downloaded from Play Console and published as the GitHub release asset so both channels share one signature. Never write that the two builds are signed differently or that one must be uninstalled first.

Known Play constraints that cannot be automated, and should not be fought: the app entry must be created manually in the Console, the very first bundle must be uploaded through the web interface before the API can manage releases, the IARC content rating questionnaire has no API, and the ads declaration and app access instructions are likely manual too. `LAUNCH.md` lists the owner's exact remaining clicks in plain numbered steps and nothing else.

Store category is Music and Audio. Data Safety is "no data collected, no data shared".

## 14. Open questions and deliberate omissions

Open, to be resolved by verification and recorded: everything in `API-VERIFICATION.md`.

Deliberately omitted from the visual reference and to be built from this specification: light-theme versions of the states and secondary screens, since the four light reference screens establish the mechanism and the tokens do the rest; the queue's own failure and empty states; and the large-library first-sync progress detail.
