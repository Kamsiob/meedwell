# Architecture

**Status: partial, and honest about it.** Sections describing code that exists are written. Sections describing code that does not exist yet are marked pending rather than deleted, so a gap reads as pending work instead of an omission.

**Previously:** This file is seeded with its required structure and the decisions already made. Claude Code fills each section as the corresponding code is written, and updates it whenever structure or a significant integration changes. Empty sections are marked pending rather than deleted, so a gap reads as pending work instead of an omission.

This document is for someone who wants to understand or modify the software, including a future session with no memory of building it.

---

## Module boundary

Two modules, decided before the build started:

- **`:core`** holds the domain logic, the Subsonic client, the data layer, and the export and import format. It has **no Android dependencies**. This is enforced, not aspirational: a dependency check in CI fails the build if an Android artifact appears on `:core`'s classpath.
- **`:app`** holds everything Android: the Compose interface, the Media3 service, the platform integrations, and the permission flows.

Why: a Linux desktop version is likely later and a web version is possible. The boundary is drawn now while it is free rather than retrofitted. Full Kotlin Multiplatform was considered and deliberately not adopted for version one; a pure-Kotlin `:core` can become a multiplatform module later without rewriting its logic.

## Components and responsibilities

### Built

**`SubsonicClient`, in `:core`.** Owns building every API call, the token and salt auth, and turning a raw HTTP result into a meaning. It deliberately does **not** own the socket: `:core` has no HTTP dependency and must not gain one. It describes a request through `SubsonicHttpEngine`, which `:app` implements with OkHttp. That seam is the same one a future Linux desktop or web build would implement with whatever its platform offers.

Two things it knows that are not obvious. First, `validateCredentials()` calls `getArtists` rather than `ping`, because `ping` returns ok for a wrong password. Second, it carries a `VERIFIED_ABSENT` set naming the endpoints Bandcamp does not implement, so a capability that cannot exist is absent from the interface rather than an error in it.

**The tolerant parser, in `:core`.** `Tolerant.kt` holds the scalar readers, `SubsonicDto.kt` the wire shapes, `SubsonicOutcome.kt` the five distinguishable failure shapes. It owns absorbing a beta that does not match its own schema. It does **not** own deciding what any of it means to the app; mapping to domain models is a separate layer, still pending.

The five failure shapes matter enough to name here, because telling them apart is the difference between an honest error screen and a shrug: a rejected login is HTTP 500 with an empty body; an absent route is a non-envelope JSON body; `unstar` answers in XML while ignoring `f=json`; a real Subsonic error arrives in a proper envelope with a code; and the transport can simply fail.

**The design system, in `:app`.** `ui/theme/` owns the tokens from `DESIGN.md`, and the rules that are arithmetic are asserted in `DesignRulesTest` rather than trusted: contrast for every token pair in both themes, and the mark's construction rule that the coin rests on the cradle touching it.

**The manifest audit, in `app/build.gradle.kts`.** Owns making the permission promise mechanical. It runs after every assemble and fails the build on anything not explicitly justified.

### Pending

The sync engine, the library repository, the local folder scanner and matcher that Tier C made load bearing, the playback service, the waveform tap, and the export and import layer. As each is built, record what it owns, what it deliberately does not own, and which module it lives in.

## The data contract

**This is the section that outlives the app, so it is written with the most care.**

One SQLite database, accessed through Room. The schema plus the versioned export format together are the app's public data contract from version one. The app is AGPL-3.0 so the schema is public regardless; documenting it deliberately is what lets a future desktop or web build interoperate rather than reverse engineer.

Record here, as they are built: every table and column with its meaning, the `play_event` append-only table's exact shape, the schema version history with each migration, and the export format's version-one field list. When the export format version increments, the old version's field list stays documented here rather than being replaced.

**Not encrypted, deliberately.** See `MASTER_PROMPT.md` section 7 and `DECISIONS.md`. The database holds an album catalogue and a play log. Credentials never enter it; they live in `EncryptedSharedPreferences` alone, are never logged, never exported, and never written to a crash report.

## Threading and lifecycle

Pending. Record: which dispatcher owns database work, how the playback service's lifecycle relates to the interface's, what survives process death and how it is restored (queue, current track, position, in-flight downloads), and what happens to each long operation under Doze.

## Where the real constraints come from

Pending, but three are known before any code exists:

- **Bandcamp's API returns only the user's own collection.** There is no store or catalogue search. This is the single largest constraint on the app's scope and it shapes Search entirely.
- **Bandcamp's API is in open beta and does not match its own schema.** Tolerant parsing is a structural requirement, not defensive coding, and it was implemented before any feature. Verified 15 August 2026: the reported float durations did not reproduce on the test account, but two other data bugs did, and the beta changes underneath us.
- **Bandcamp's API will not release purchased files.** Verified exhaustively on 15 August 2026: there is no `download` endpoint in any form, and `stream` returns MP3 V0 while ignoring every transcoding parameter. This is the largest single constraint on the product after the collection-only limit. It moves the entire ownership story onto local folder scanning and matching, which is therefore a version one requirement rather than a convenience.
- **Owned files live in the open**, which is the app's central differentiator and also means they can vanish underneath it at any time. Reconciliation is therefore a normal code path, not an error path.
- **`ping` does not enforce authentication**, and a failed login is a bare HTTP 500 with an empty body rather than a Subsonic error code. Both shape the Connect flow and the Connection trouble screen.

Add discovered constraints here as verification and building reveal them, with the measurement or observation that established each.
