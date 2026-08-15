# Master build prompt: Meedwell by Kamsiob

Paste this whole file as the first message to Claude Code, launched in the project folder. Everything it references is in this folder.

---

You are building **Meedwell by Kamsiob**, a free and open source Android music player for people who buy their music: a client for Bandcamp's Subsonic API that also plays local files, from scratch to a release-ready state.

Read this entire prompt before writing any code. Then read, in this order: `spec/MASTER_SPEC.md`, `spec/DESIGN.md`, `spec/API-VERIFICATION.md`, `spec/ISSUES-SEED.md`, `spec/DECISIONS.md`, `spec/HANDOFF.md`. Open `reference/meedwell-screen-grid-final.html` in a browser and look at all 46 screens; it is the binding visual reference and no written description substitutes for seeing it.

`reference/kamsiob-project-template.md` is the standing convention document for every Kamsiob project. It applies in full. Where this prompt and the template differ, this prompt wins, and every such difference is listed in section 12 below with its reason, so you do not need to guess.

This is an unattended run. Work continuously through the phases in order without stopping for approval.

## 1. What the app is, in one paragraph

Subscriptions squeeze the people who listen and starve the people who make. Bandcamp proves it can work another way. Meedwell exists to make owning music feel better than renting it ever did. It plays the user's own Bandcamp collection over Bandcamp's Subsonic API, plays local files on the phone, and downloads purchased music as real files into the public Music folder where any player can read them and where they outlive the app. It collects nothing, has no accounts of its own, no ads, no subscriptions, and no telemetry. It would be the first purpose-built Android Bandcamp client.

## 2. The rules that outrank everything else

**Legal and terms of service compliance is a filter applied before building, never a caveat added after.** Bandcamp's own API is the only server the app talks to. No scraping, ever, of anything, including for artwork or artist pages. "Bandcamp" is used nominatively only, and the About screen and store listing carry "Not affiliated with or endorsed by Bandcamp." If any feature turns out to need a non-compliant route, rule the feature out as specified and record it rather than architecting around the restriction.

**Zero data collection by design.** Nothing leaves the phone except requests to Bandcamp's API server and URLs handed to the user's own browser or share sheet. No analytics, no crash auto-reporting, no identifiers. The Play Data Safety declaration is "no data collected, no data shared" and it must remain literally true in the built software.

**No em dashes** in any user-facing copy, documentation, README, commit message, store text, or code comment intended for humans.

**Honest limits are a feature.** Every constraint the API imposes gets stated plainly in the interface at the moment it matters. Never imply the app can do something it cannot, and never compare it favourably to products it cannot match.

**No dark patterns.** No streaks, no badges, no engagement mechanics, no nagging notifications, nothing engineered to pull the user back. Deleting one's own data is easy.

**The owner does not write code and does not intend to learn.** Every step must be completable by you: setup, build, test, fix, release, maintenance. Never leave a step that requires him to write, debug, or read code. If something cannot realiztically be built and maintained that way, say so plainly before starting it.

## 3. Build task one, before any feature work

The Bandcamp Subsonic API entered open beta on 16 July 2026 and several of its behaviors are unverified. `spec/API-VERIFICATION.md` is a protocol with an empty results table. Complete it against the live account first, record every answer in `DECISIONS.md`, and only then build.

Two of its answers change what gets built rather than merely informing it:

- **The download endpoint** decides whether the app's marquee feature exists as designed. `MASTER_SPEC.md` section 6 defines three outcomes, Tier A, Tier B and Tier C, each with its own already-designed screen and copy. Pick the tier the evidence supports, record it, build that one.
- **Float durations.** Field reports say durations come back as floats where the Subsonic schema says integer. A strict parser breaks on the first album. Tolerant deserialisation is therefore the first code written, before any UI, and it applies to every numeric field rather than only the ones currently known to misbehave.

**No public copy of any kind is written before verification is recorded.** Not the store listing, not the README feature list, not release notes, not the website page. Several public claims depend on answers nobody has yet, and writing them first is how a project ships a promise it cannot keep.

## 4. Architecture, with the later platforms in mind

Android is the only target now. A Linux desktop version is likely later and a web version is possible, so the boundary that would make those feasible gets drawn now, while it is free, rather than retrofitted.

Two Gradle modules:

- **`:core`**, pure Kotlin with no Android dependencies at all, enforced by having no Android plugin applied so a violation fails the build rather than being noticed later. It holds the domain models, the Subsonic client and its tolerant parsing, authentication, sync and merge logic, the shelf and search and rediscovery logic, the play-history model, and the export and import format with its versioning and validation. This module is the part a future desktop or web build reuses.
- **`:app`**, the Android application: Compose UI, Media3 playback service, downloads, MediaStore writes, platform surfaces, and the SQLite driver implementation behind an interface `:core` defines.

The single SQLite schema and the versioned documented export format together are the app's public data contract from version one, exactly as they are for Health Trail. The app is AGPL-3.0 so the schema is public anyway; document it in `ARCHITECTURE.md` deliberately rather than leaving it to be reverse engineered. Any future desktop or web build interoperates by reading that format, and file-level export and import compatibility is the bridge, not sync.

If direct device-to-device sync is ever built it will be direct only, over the local network or a Tailscale-style connection, never a cloud relay. Not in scope now. Do not build seams for it beyond the export format.

Record in `DECISIONS.md` that full Kotlin Multiplatform was considered and deliberately not adopted for version one: the module boundary delivers most of the portability benefit at a fraction of the cost, and a `:core` module with no Android dependencies can be converted to a multiplatform module later without rewriting its logic.

## 5. Everything else

`spec/MASTER_SPEC.md` carries the complete functional specification, the phase plan, the testing protocol, and the process rules. `spec/DESIGN.md` carries the binding visual and copy specification. Commit both unchanged in the first commit and keep them current with every commit after that. Where code and `DESIGN.md` disagree, `DESIGN.md` wins.

Copy `repo-seed/` into the repository root in the first commit. It holds the license, the .gitignore, the community and contribution documents, the issue and pull request templates, the funding file, and working CI and release workflows. They are starting points held to the same standard as anything else: read them, correct anything wrong for this project, and keep them current.

## 6. Begin

`ARTIFACT-CHECKLIST.md` lists every artifact the standing template requires and where each one is. Four are seeded as skeletons and are yours to complete as the work reaches them: `ARCHITECTURE.md` (fill the component, threading, and lifecycle sections as you build them, and write the schema and export format into the data contract section as they stabilise), `CHANGELOG.md` (first entry at first release), `tools/` (build the Bandcamp Friday manifest generator when you implement that line, since those dates are deliberately not hardcoded), and `store-assets/` (Phase 7 only, generated from the built app). Two artifacts are deliberately deferred and must not be written early: `README.md` and `LAUNCH.md`, both Phase 7, for the reasons in the checklist. Update the checklist's rows as statuses change; it is a living artifact like the rest.

Start at Phase 0 in `MASTER_SPEC.md` section 11 and run continuously. Commit and push tested increments. Open issues from `spec/ISSUES-SEED.md` as real GitHub issues in Phase 0 and work the tracker properly from then on. Update `HANDOFF.md`, `DECISIONS.md`, `MASTER_SPEC.md`, `DESIGN.md` and `README.md` with every commit so a session with no memory can resume from the repository alone.

When a judgment call arises, decide it, prefer the simpler and more reversible option, log it in `DECISIONS.md`, and continue. Never stop to ask. Anything only the owner can do goes under BLOCKED in `DECISIONS.md` with exactly what he needs to do, then you skip it and keep building everything that does not depend on it.

Do not end a turn while work remains.

## 7. Differences from the standing template, each deliberate

These override `reference/kamsiob-project-template.md` for this project. The reasons are recorded so a future session does not "fix" them back.

**Signing and distribution, replacing template section C5.** The template still carries the older two-key approach and its release-note wording about the Play build and the GitHub build being signed differently. That approach is superseded and must not be used. Instead: enrol in Play App Signing, generate an **upload key only**, store it outside the repository, and tell the owner plainly in `LAUNCH.md` where it lives and that it needs backing up, noting that it is recoverable because Google holds the actual app signing key. Upload the bundle to Play, wait for processing, download the Google-signed universal APK from Play Console under Test and release, then latest releases and bundles, then the bundle's Downloads tab, and publish that exact file as the GitHub release asset. One signature serves both channels, so a user can move between the Play install and the GitHub install without uninstalling or losing data. Every release therefore goes Play first, then GitHub; the GitHub asset cannot exist before the bundle reaches Play. Any wording anywhere in the project saying the two builds are signed differently or that one must be uninstalled first is wrong and must not be written. This same correction is pending in the template itself and the owner will apply it there separately.

**No SQLCipher, and no encryption of the music database.** The template specifies an encrypted database with a Keystore-held key, which is right for an app holding personal records and wrong here. This database holds a catalog of albums and a play history, and it must stay readable by a plain SQLite export that a future desktop or web build can import. Encrypting it would break the portability contract in section 4 to protect data that is not sensitive. Credentials are the exception and they never go in the database: the Bandcamp username and password live in `EncryptedSharedPreferences` and nowhere else, are never logged, never exported, and never written to any crash report. Record this reasoning in `DECISIONS.md` and state it plainly on the Privacy screen.

**Export is not encrypted, and says so.** Follows from the above. The export file carries no credentials, which is the only thing in the app worth protecting, so it is a plain portable file and the interface says what is in it.

**No on-device inference,** so template section C7 does not apply. Delete it from consideration rather than adapting it.

**A local-only mode that never mentions accounts.** The template assumes one coherent app. Meedwell has a second shape, described in `MASTER_SPEC.md` section 7, and the rule there is absolute: a user who never connects an account must never meet sync language the app cannot honor.
