# Meedwell by Kamsiob · Knowledge Transfer Handoff

Purpose of this document: complete context transfer to a new chat whose job is to write the master Claude Code build prompt. Everything decided, researched, and designed for Meedwell lives here. The final visual reference is the file `meedwell-screen-grid-final.html` (30 screens), which should be uploaded alongside this document.

---

## 1. What Meedwell is

Meedwell is a free, open source (AGPLv3), zero-telemetry Android music player built for Bandcamp's new OpenSubsonic API, which also plays local files. Kotlin. It would be the first purpose-built Android Bandcamp client; Bandcamp officially lists only Amperfy (iOS), Feishin, and Submariner (Mac) as supported clients.

Tagline and positioning: "for people who buy their music." The player where your music is actually yours. Mission (Karim's words, woven through the app copy): subscriptions squeeze listeners and starve artists; Bandcamp proves another model works; Meedwell exists to make owning music feel better than renting it.

Follows every Kamsiob standing rule: zero data collection, everything local, no accounts, no ads, no subscriptions, donate-only (Buy Me a Coffee behind "Support this work" buttons), GitHub-only tooling, vibe-coded entirely by Claude Code, kamsiob-project-template.md conventions apply in full (HANDOFF.md, living docs, GitHub issues discipline, versioning by Claude, one-copy rule, data portability A7c, signed commits, CI, artifact provenance, project board, etc.).

Name checks done: "Kamp" killed (kamp.fm exists, a desktop-only Mac/Windows Bandcamp player with identical positioning, which also validates the concept). "Krate" taken twice. "Siob" was clear but retired by the owner in July 2026: it tells a stranger nothing and cannot be spelled from hearing. Final name: "Meedwell". *Meed* is Old English (before 900, in Beowulf): an earned reward, a fitting recompense; *well* is how it is paid. The compound checked clear of apps, repositories, and companies; the bare root is used by "meed Loyalty," an unrelated loyalty-platform company, recorded as adjacency, not collision. Spelling is Meedwell, never Meadwell.

Legal posture: "Bandcamp" used nominatively only; the About screen and store listing carry "Not affiliated with or endorsed by Bandcamp." No scraping anywhere; the only network peer is Bandcamp's own API server, plus URLs opened in the user's browser.

## 2. The Bandcamp API context

- Bandcamp launched Subsonic API open beta July 16, 2026. Server URL: https://bandcamp.com/api/subsonic. Users get generated credentials under Fan Settings → Subsonic: the server address, a long generated username (fan_xxxx style), and a long (~24+ char) generated password. The Connect screen shows all three, address prefilled, with Paste chips and a password reveal eye.
- The API exposes ONLY the user's own collection. No store/catalog search. This shapes Search (own collection only, with a browser deep link "Search all of Bandcamp for 'x'" using https://bandcamp.com/search?q=QUERY with item_type parameter: b for artists, a for albums, t for tracks) and the roadmap entry promising in-app store search the day Bandcamp opens it.
- Known beta quirks from field reports: durations returned as FLOATS (breaks strict parsers; tolerant kotlinx.serialization is the single highest-priority engineering requirement), album-list JSON omits cover art URL (artists endpoint has it), getSimilarSongs absent, Symfonium needed a "compatibility mode" for sync, one Strawberry user hit an unexplained 401.
- Playlists created/edited via the API sync back to the user's Bandcamp collection (official). Save-queue-as-playlist rides the same endpoint.
- getArtists and getGenres are first-class in the protocol and carry the Artists and Genres browse views whole. star/unstar is what Loved is built on: hearts sync both ways and live in the account, not the app.
- Subsonic auth: u + token/salt (t=MD5(password+salt), s=salt) or OpenSubsonic apiKey; send v=1.16.1, c=Meedwell, f=json; probe getOpenSubsonicExtensions first; error codes 40-44 mapped to friendly states.
- Liner notes: the protocol supports getAlbumInfo2 with a notes field. Whether Bandcamp populates it is UNVERIFIED. Build behind a capability check: the "Liner notes" section on the album screen exists only if data comes back.
- Bandcamp page URLs for artists/albums: NO standard Subsonic field exists. Verify whether Bandcamp returns a nonstandard URL field; fallback is constructing a bandcamp.com/search deep link from the artist name. This feeds both "View on Bandcamp" buttons and the share text.

### Open questions to verify empirically against the live API (this is build task one)

HARD SEQUENCING RULE: no store listing copy, README, website page, or release note may be written before verification is done and recorded in DECISIONS.md. Several of the app's public claims (chiefly the "real files you own" claim) depend on answers nobody has yet. Writing the copy first and correcting it later is how a project ends up shipping a promise it cannot keep.

Exact accepted auth scheme and the 401 cause; full endpoint matrix (search3, getCoverArt, download, star, scrobble, deletePlaylist, getAlbumInfo2); stream format/bitrate (likely MP3; do NOT promise lossless streaming); whether the download endpoint yields original FLAC; which numeric fields are floats; rate limits; pre-order handling; nonstandard URL fields; liner notes presence.

## 3. Technical stack (researched and decided)

- Kotlin, Jetpack Compose, Material 3, target API 36 (required Aug 31, 2026).
- Media3 1.10.x via MediaLibraryService (gives notification/lock screen media controls and makes Android Auto nearly free later); media3-datasource-okhttp; native FLAC; gapless playback.
- Downloads written as real files to the public Music folder: Music/Meedwell/Artist/Album, via MediaStore.Audio inserts with RELATIVE_PATH + IS_PENDING. Files survive uninstall and are readable by any player (the anti-Symfonium differentiator; Symfonium encrypts its offline cache). READ_MEDIA_AUDIO needed to re-see own files after reinstall.
- Room, single DB, append-only play_event table (powers Forgotten Shelf rediscovery and stats, all on-device).
- Credentials in EncryptedSharedPreferences.
- Crash handling: ACRA in local-only mode; crash sheet shows the full report, user reads before manually sharing (stacktrace embedded in EXTRA_TEXT to dodge ACRA's attachment bug). Nothing auto-sends.
- Ambient color: androidx.palette + material-color-utilities.
- Play Data Safety: "No data collected/shared" defensible (Bandcamp traffic falls under the ephemeral service-provider exemption). Category: Music & Audio.
- Bandcamp Friday 2026 remaining dates to hardcode (midnight-midnight Pacific): Aug 7, Sep 4, Oct 2, Nov 6, Dec 4. The Shelf shows a quiet gold-dot line on those days linking out to Bandcamp. Degrade gracefully after the list ends (feature simply doesn't appear until an update adds dates).
- Waveform (see design section for rules): amplitude via a custom AudioProcessor tap in Meedwell's own Media3 pipeline (zero permissions; NEVER the Visualizer API, which needs RECORD_AUDIO). Downloaded files get a precomputed full envelope stored locally. System media cards cannot be repainted; they keep Android's own progress.

## 4. V1 feature scope (final)

Core: connect via Bandcamp Subsonic credentials (guided 3-step flow, "Open Bandcamp for me" link); merged album-first Shelf of Bandcamp collection + local files; grid AND list views (toggle beside search; list rows 48px art, artist, year, format, downloaded dot, 56px min height); Albums, Artists, and Genres as sibling first-class views of the shelf (the Downloaded and Local scope filters moved into the sort menu to make room); A-to-Z fast scroller appearing on scroll; sort menu (recent, artist, purchase date, most played, plus the scope filters); labeled tab bar (Shelf, Search, Lists, More); a local music folders setting (Library section of Settings) for adding folders beyond Music/.

Action sheet: long-press anywhere a track or album lives opens the same eight verbs app-wide: Play next, Add to queue, Add to a list, Love, Download, View artwork, Go to artist, Share.

Loved: built on Subsonic star/unstar, syncing both ways, so hearts belong to the account and outlive the phone and the app. Heart in the now-playing secondary row and the action sheet; Loved pinned at the top of Lists.

History: a recently-played screen read straight from the append-only play_event table that already powers the Forgotten Shelf. Grouped by day, erasable in Settings.

Playback: Media3 gapless; queue as glass bottom sheet (drag reorder, swipe remove, honest shuffle, Save as list writing back to Bandcamp); now playing with the complete cover above the wash and the live waveform scrubber below (drag to seek); sleep timer (moon icon, shows countdown when running); long-track resume (pieces over 20 min remember position, "Resume from 22:40" on track rows; a Settings toggle).

Downloads: three sizes of ownership: "Download everything · about X GB" (the marquee button, wifi-only respected), per-album, per-track. Real files to Music/Meedwell. Downloads screen shows in-flight (album progress with pause, single tracks), on-phone list with sizes and paths.

Search: own collection (albums, tracks, artists) + "Search all of Bandcamp for 'x' ↗" browser deep link at the bottom.

Playlists: full CRUD syncing both ways with Bandcamp.

Artist pages: their albums (owned marked "yours" in serif italic), in-rotation stats from local history, prominent "Their Bandcamp page ↗" with copy noting money goes to them and Meedwell takes no cut.

Sharing: ACTION_SEND to Android's native share sheet only, plain Bandcamp link text, zero in-app fetching. Covered explicitly on the Privacy screen ("Sharing and outside links").

Rediscovery: Forgotten Shelf, computed entirely on-device from play history ("never played," "quiet for 14 months").

States designed, not discovered: First Sync (playable-immediately, beta-honest copy), Offline (quiet banner, shelf becomes what's downloaded, no spinner), Connection Trouble (shows real error code + last successful sync + "Get fresh credentials ↗" + "Stay offline"), Crash sheet (local report, read-before-send).

Transparency screens: Privacy (five plain Q&As, "collects nothing," GitHub link), What's Ahead (Being Considered: in-app Bandcamp store search waiting on their API, equalizer, Android Auto, other Subsonic servers; Not Planned: accounts/ads/telemetry/subscriptions "not gaps, decisions," and no in-app store because buying happens on Bandcamp), About (mission copy, links: GitHub, YouTube, kamsiob.com, hello@kamsiob.com, non-affiliation line, support framing copy + gold button).

Liner notes: album screen section behind capability check.

Settings: Theme (dark default, light, system), App icon (five finishes), Shelf view, gapless toggle, long-track resume toggle, Library (local music folders, wifi-only downloads), Export everything, Erase listening history; then the support value block ("Free no matter what. Nothing held back, nothing unlocked later. One person carries it.") above the gold Support button. The same value block leads into the button on About.

Deferred to v1.1+: EQ, casting, Android Auto, multi-server, Last.fm scrobbling (opt-in BYO), home screen widget, yearly "shelf in review" recap. Never: social features, in-app store, accounts, telemetry.

## 4b. The download endpoint decision tree (resolve on day one, before any public copy)

"Real files to Music/Meedwell, readable by any player, they outlive this app" is the marquee differentiator, the anti-Symfonium argument, and roughly half the positioning. It rests entirely on an endpoint that has not been tested. Verification therefore has three possible outcomes and all three have a decided response, so a bad answer costs a day instead of a rewrite:

- TIER A, original purchased quality (FLAC or whatever the purchase was). Everything in the grid and all copy stands as written. Downloads screen keeps the marquee "Download everything · about X GB" button.
- TIER B, transcoded only (for example MP3 at some bitrate). Downloads still happen, still land as real portable files, still outlive the app. Only one line of copy changes: "best quality Bandcamp provides" becomes the actual format and bitrate, named plainly, on the Downloads screen and in the store listing. The differentiator survives untouched because it was always about file ownership and portability, never about lossless.
- TIER C, no usable download endpoint at all. The marquee screen is replaced by the fallback screen already designed in the grid ("Your files"): Meedwell says plainly that the API streams but does not release files, links out to Bandcamp where the user downloads them the way they always have, then watches folders and matches what arrives back onto the shelf as owned. The ownership claim survives because portable files are still the outcome; only who fetches them changes. Local folder scanning and matching becomes a v1 requirement rather than a convenience.

Record the outcome in DECISIONS.md before writing a word of public copy. Also verify and record: whether the endpoint respects the wifi-only setting, whether it rate limits a 9.8 GB run, and what it returns for pre-orders and unreleased items.

## 4c. Local files only mode, fully specified

This was one unlabeled button on the welcome screen with nothing behind it. It is a second product and needs stating, because a user who never connects an account must not meet sync language the app cannot honor.

In local-only mode: the Shelf reads local files only, with its own voice line ("31 albums on this phone, no account involved") and Folder as an available sort. Artists and Genres are built from file tags. Lists work, stored in the app's own database, labeled as living on this phone. Loved works the same way, local only, and the "synced with your Bandcamp account" line is absent rather than shown and broken. Search covers local files, and keeps the Bandcamp browser deep link, since that is only a URL handed to the browser and is genuinely useful. Forgotten Shelf and History work unchanged, since both read the local play log. Downloads is replaced by the local folders screen. Connect Bandcamp stays permanently reachable from More and from Settings.

Tag handling, which local-only mode makes load-bearing: read albumArtist before artist so compilations do not shatter into one album per track; respect disc numbers; group loose singles under a plainly labeled bucket rather than inventing album names; treat a folder with no usable tags as its folder name.

Merge rule when a user connects later: match on artist plus album plus track, never duplicate, prefer the local file for playback, and mark the album as owned. Also the reverse case, which is likelier than it sounds: a user who downloaded from Bandcamp's website before installing Meedwell should find those files recognized and merged, not duplicated alongside the streamed copies.

## 4d. Export and restore, the full round trip

Settings previously offered Export everything with no way back in. That is half a round trip and violates the standing data portability rule (A7c), which requires that restore be as easy as backup, equally tested, and verified onto a fresh install, onto an install with existing data, and onto a less capable device.

The export file carries: listening history, lists and hearts made on this phone, resume points on long pieces, every setting, and the download manifest with file paths. It does not carry the audio, and the app says so plainly, because the audio is already the user's, sitting in Music where any app can read it. The manifest exists so a restore can re-find those files rather than re-fetch them.

Restore replaces app data in one atomic operation after an explicit confirmation, never a half import, and never silently drops data a version does not recognize (it says what it did not understand). Replace rather than merge is the deliberate choice, since merging two divergent listening histories is genuinely ambiguous; state that in the interface rather than guessing. Format is versioned from release one. An optional automatic backup writes to the same user-chosen folder, triggered by accumulated change rather than elapsed time, with a quiet "last backed up" line that never nags, per the standing backup prompting rules.

## 4e. States that were missing, now designed

- FILES THAT WENT MISSING. The property that makes downloads real also makes them deletable in the Files app, and cards unmount. Meedwell reconciles rather than trusting its own database: detect absent files, mark those albums as not downloaded, change nothing else, and offer either a re-download or leaving them as streaming. Framed in-app as the honest cost of real files, not as a bug.
- STORAGE EXHAUSTION. A download that cannot finish stops cleanly, keeps the tracks that landed, states how much room it needed, and leaves nothing half written.
- LONG DOWNLOADS SURVIVING THE SYSTEM. A 9.8 GB run needs a foreground service with the correct Android 14+ service type declared, plus resume across process death and Doze. Not a nicety: without it the marquee button silently fails on most phones.
- ONE TRACK THAT WILL NOT PLAY. Designed as an inline state on the album and queue: a quiet banner, the failed row marked and retryable in place, and playback moving on rather than stopping. The old grid designed sync failure and skipped playback failure, which is the one the listener actually notices, especially on a beta API.
- EMPTY STATES. Zero purchases, zero local files, zero search results, and an empty list each get copy. The shelf version names both ways to fill it and never scolds.
- ENORMOUS COLLECTIONS. Five thousand albums needs incremental and resumable sync, paged requests, art loaded lazily with a bounded disk cache, the A to Z index built once and cached rather than recomputed on scroll, and a stated expectation for first sync duration. Test with a synthetic large library on an emulator, per the destructive-testing rule.
- PERMISSIONS. Two, both narrow, both explained in Meedwell's own words before Android's dialog appears: notifications (only so the player appears in the shade and on the lock screen, and never used for anything else) and music and audio files (so Meedwell can see its own downloads and existing local music). Declining either leaves the app working with exactly one capability missing, stated plainly, reversible in Settings. An app whose whole pitch is restraint cannot let a bare system dialog speak first.

## 5. Design system (locked; meedwell-screen-grid-final.html is the reference)

Fonts: Instrument Sans (UI) + Instrument Serif italic for the "voice" lines (the app's quiet editorial moments). Tabular numerals for all times.

The legibility law, rewritten (supersedes the adaptive-scrim law entirely): artwork is always shown complete: never cropped, never faded, never scrimmed, never written on. Text lives only on theme surface, past a hard edge. Album screens show the full square cover edge to edge with the toolbar above it on surface; it collapses into the toolbar on scroll. The Shelf's newest-arrival card puts the whole cover beside its caption instead of under it. A full-screen artwork viewer sits one tap behind every cover in the app (album header, now playing, action sheet); the viewer is themeless by design: the complete art on near-black in both modes, no text on it, pinch to zoom. The single sanctioned text-over-color moment is the now-playing wash: a palette-derived color field (not the artwork) clamped below a brightness ceiling so white always passes, on any album, in either theme. Net effect: legibility no longer depends on the artwork at all, in either mode.

The missing-cover rule, which the new law makes load-bearing rather than cosmetic: if words may never sit on art, then absent art must not leave a hole where words are forbidden. A missing cover is drawn as surface, never as a fake image: a surface panel with a hairline carrying the title's own first letters in Instrument Serif italic at secondary ink. Never a gray box, never a generic music-note icon, and never the Meedwell mark, since borrowing the mark for album art would blur the one thing it must never be confused with (see screen 02). On the album screen a missing cover means the art region is omitted entirely rather than held open, so the screen opens on the title.

Dark theme (default): warm near-black #0B0B0E, text #F4F3F6, secondary #A7A5B1, hairline borders rgba(white,.08), soft radial "glow" washes (violet/teal/rose/ember) drifting slowly (16s, reduced-motion gated). No boxes; hairlines and space do the structure.

Light theme: warm paper #F5F3ED, primary ink #17151D, secondary #33303B-#44414C, tertiary floor #56525E (NOTHING lighter than slate on paper; this was corrected twice, treat as law), borders rgba(30,28,38,.11), ink pill buttons, frosted-paper glass, dark covers get a hairline edge.

The legibility law (absolute): text over artwork is ALWAYS white on an adaptive scrim; the app measures the region behind the text and deepens the scrim until contrast passes, falling back to a solid panel drawn from the album's darkest palette color for the palest covers. Now-playing ambient washes are palette-generated but clamped below a brightness ceiling so white always passes. In light mode the artwork region ends at a hard edge with pure paper before any ink text begins ("ink never sits on art, white never sits on paper, the transition zone belongs to nobody"). Top of album art keeps a dark scrim band so white nav icons survive any cover.

Gold rule: gold means money reaching makers, nothing else. Exactly two uses: the Bandcamp Friday dot and the "Support this work" buttons (gold hairline pill, small gold dot, faint glow; #E7C171 on dark, old gold #8A6215 on light, corrected from #9A6F1E for AA). Support button appears at the bottom of Settings AND About, and in both places a short value block leads into it: on Settings, "Free no matter what. Nothing held back, nothing unlocked later. One person carries it."; on About, the fuller version ending "...there's a place to stand behind it. Either way, it's yours." The terms are stated before any invitation is made. Label is always "Support this work" (standing rule: no coffee cliches, no begging), linking to https://buymeacoffee.com/kamsiob.

The mark (logo, final): FLAT. A rustic copper (#AE6738, less orange, more old pipe and pennies) circle resting tangent on a copper line, on flat near-black #16121C, rounded-square icon frame (22.5% radius). No gradients, no glow, no dimension (earlier 3D-ish version explicitly rejected). Reads as a record on the shelf, a sun on the horizon, and above all a point of balance: weight settled where it belongs, the way money should come to rest with the people who made the music. Construction rule: circle tangent on the line, never overlapping, never floating; line breathes at both ends; nothing else in frame. One-color stroke variants for both themes. Wordmark lockup: mark + "Meedwell" Instrument Sans 700 tight.

App icon finishes: the icon is user-pickable in Settings, five finishes via Android's activity-alias technique (instant swap, offline, free): Rustic Copper (default, #AE6738 on #16121C), Dusk (#8B84AE on #12121B), Moss (#7C8F5E on #11150D), Ink (#F4F3F6 on #0B0B0E), Paper (#17151D on #F5F3ED). The coin stays at rest in its cradle in every finish. Demo/screenshot album is "Copper Lines by The Long Static" (rose art with diagonal etched lines) specifically so the mark is never confused with album art; the mark alone wears the rounded frame.

Waveform rule: in-app surfaces (now playing, mini player, future widget) show real amplitude: played bars white, remainder dim, drag to seek; mini player waveform stills when paused; reduced-motion gets the static envelope. Downloaded files show the precomputed full-track portrait. System cards (lock screen, shade) keep Android's own progress; Meedwell's job there is perfect art, metadata, seek position, and the copper mark.

System media cards (Media3): flat palette-tinted ground (not busy blur), 56-64px art, title/artist, knobless 2px hairline progress with times at the ends, prev/pause/next with generous spacing, small copper mark top-right.

Copy voice: plain, warm, honest, first-person-adjacent ("A report was saved, here, and only here"). Honest limits stated in-UI. No em dashes anywhere in user-facing copy (standing rule). Collector provenance surfaces: "On your shelf since March 2025," label names, purchase-format honesty ("yours" markers in serif italic).

### Accessibility floor (part of the design system, not a later pass)

- CONTRAST, corrected: the light-mode gold was #9A6F1E, which computes to 4.06:1 on paper #F5F3ED and fails AA for the 14px support label. It is now old gold #8A6215, which computes to 4.93:1. Dark-mode gold #E7C171 on #0B0B0E is 11.5:1. The light ink scale all passes comfortably (#17151D 16.3:1, #33303B 11.6:1, #44414C 9.0:1, #56525E 6.9:1 on paper), which is what the "nothing lighter than slate" law was protecting. Any new color pair gets measured before it ships, not eyeballed.
- THE WAVEFORM SCRUBBER is a custom drag control and is therefore invisible to TalkBack unless built with explicit seek semantics: a slider role, spoken current position and duration, and keyboard or switch-accessible increment and decrement. This is the single most likely accessibility failure in the app, because it is also the signature interaction. The static envelope already required for reduced-motion doubles as the accessible representation.
- TYPE SCALE IN REAL UNITS: the grid is a 330px mock of roughly a 412dp screen, so mock pixels multiply by about 1.25 to reach sp. The scale is therefore caps 12sp, secondary 14sp, body and row titles 16sp to 17sp, section heads 32sp, large heads 42sp to 50sp. Nothing in the app is smaller than 12sp. Every screen must be tested at 200% font scale and with display size enlarged, with the album screen, Settings, and the action sheet as the known pressure points.
- TOUCH TARGETS: 48dp minimum everywhere, which the existing 56px row minimum already satisfies; the per-track download circles and the A to Z rail need explicit checking since both are small by design.
- Reduced motion is already honored for the drifting washes, the shimmer, and the live waveform. Screen reader labels are required on every icon-only control, of which the app has many (view toggle, sort, share, sleep timer).

## 6. Process requirements for the master prompt

- First build task: empirical verification of the open API questions against a live Bandcamp Subsonic account, results recorded in DECISIONS.md, with the tolerant-parser requirement implemented before anything else.
- All kamsiob-project-template.md rules apply: public GitHub repo from the first prompt matching existing repo patterns, MASTER_SPEC.md + DESIGN.md + DECISIONS.md + HANDOFF.md living documents with precedence statement, GitHub issues as specifications with the full label/type/board/milestone model, semantic versioning chosen by Claude Code with one-line reasoning, one copy of the app on the machine, in-place upgrades only, destructive tests on emulator, data portability round-trip standard (A7c) for export/import, CI compiling all test source sets, signed commits, release artifact provenance, screenshots recaptured from the running app.
- Android publishing: B7 Collective org Play Console, developer name "Kamsiob," Play App Signing Option A (upload bundle to Play first, then publish the Google-signed universal APK as the GitHub release asset; never a second signing key), Play API automation via kamsiob@kamsiob-503213.iam.gserviceaccount.com where the API allows, manual steps acknowledged (app creation, first upload, IARC).
- AI-slop check before writing any store copy, README, or website page (standing rule).

### Issues to open on day one, against the spec rather than discovered later
Per the standing rule that issues are specifications opened at the moment of discovery, these are known now and should exist before code does, each with acceptance criteria in checkable terms: tolerant parser for float durations and unexpected nulls; download endpoint verification and the Tier A/B/C decision; local-only mode as a complete surface; tag handling (albumArtist, disc numbers, loose singles, untagged folders); collection merge without duplication in both directions; export format v1 plus atomic restore with a field-by-field round-trip assertion test; missing-file reconciliation; storage exhaustion; foreground service with Android 14+ service type for long downloads; per-track playback failure; empty states; large-library sync, paging, and cached A to Z index; notification and media permission flows with in-app explanations; waveform seek semantics for TalkBack; 200% font scale audit; contrast audit of any new color pair; Bandcamp Friday dates as versioned JSON with hash verification and graceful absence; missing-cover placeholder; album header collapse behavior.

Two of these are release blockers rather than ordinary work, and should be labeled that way: the download endpoint decision (because public copy depends on it) and the restore round trip (because shipping export without tested restore risks user data).

## 7. Where things stand

Design: complete and locked (meedwell-screen-grid-final.html, 46 screens: 2 mark screens, 38 dark app screens, 4 light reference screens, 2 system surfaces). Added in the first pass: Artists, Genres, the artwork viewer, the action sheet, Loved, History, the app icon picker, the rebuilt album screens in both themes, the rebuilt Shelf newest-arrival card, the cover on now playing, Save as list on the queue, and the support value blocks. Added in the gap pass: album scrolled (collapsed header), the missing-cover rule, local files only, nothing here yet, the download fallback, files that went missing, one track that will not play, export and restore, and the permissions explainer. Earlier grids v1-v10 are superseded.

Known deliberate omissions from the grid, specified in text above rather than drawn, and safe to build from the spec: light-theme versions of the states and secondary screens (the four light reference screens establish the mechanism and the tokens do the rest), the queue's own failure and empty states, and the large-library first-sync progress detail.
Research: complete (API, stack, publishing, market; summarized above).
Next step: write the master Claude Code build prompt in a fresh chat using this document plus the final grid as inputs.


## 8. Design review round, July 2026 (post-rename)

Method, stated honestly: a structured review by simulated perspectives, not live user research. Five lenses were run against the full spec and all 46 screens: an Android media-app engineer, a daily Bandcamp collector, an accessibility auditor, a first-run specialist, and a skeptical FOSS user. Findings below; adoption decisions and rejections are logged in DECISIONS.md.

**What the review caught that the spec missed.** The spec designed every failure state except the ones a music player hits hourly: it said nothing about audio focus, so unplugging headphones would have kept playing out loud; nothing about queue persistence, so a process death would have emptied the queue the hostile-path tests kill; nothing about Bluetooth behavior, so a car connection could auto-play unprompted, the exact opposite of the restraint pitch. All three are now v1 requirements with acceptance criteria in ISSUES-SEED.md, the first two as release blockers.

**Collector lens.** Pre-orders are the one purchase type the shelf had no state for; a collector who pre-orders sees their money vanish until release day. Now specified as a distinct shelf state gated on API verification. Sleep timer gained finish-the-track and extend-by-fifteen, the two behaviors listeners actually reach for.

**Accessibility lens.** The A to Z rail was flagged for touch targets but had no TalkBack story; it now requires an equivalent jump-to-letter action. The new cover-swipe gesture ships with named TalkBack actions and a reduced-motion crossfade from day one rather than retrofitted.

**FOSS lens.** F-Droid was the unanswered question every FOSS-minded user asks first. It now appears in Being Considered naming its real constraint: F-Droid requires reproducible builds, which this project deliberately does not pursue. An honest no beats silence.

**Platform decisions.** Portrait-first, rotation survives without state loss, tablet layouts deferred and stated. English-only at launch, stated in the listing, strings externalized from the first commit.

**Considered and rejected, to protect scope:** crossfade (gapless is the point; crossfade harms album intent), ReplayGain (Being Considered), recent-search history, and a continue-listening card on the Shelf (engagement-adjacent; queue restore already covers the honest version of it).
