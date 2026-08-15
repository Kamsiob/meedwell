# Architecture

**Status: skeleton.** This file is seeded with its required structure and the decisions already made. Claude Code fills each section as the corresponding code is written, and updates it whenever structure or a significant integration changes. Empty sections are marked pending rather than deleted, so a gap reads as pending work instead of an omission.

This document is for someone who wants to understand or modify the software, including a future session with no memory of building it.

---

## Module boundary

Two modules, decided before the build started:

- **`:core`** holds the domain logic, the Subsonic client, the data layer, and the export and import format. It has **no Android dependencies**. This is enforced, not aspirational: a dependency check in CI fails the build if an Android artifact appears on `:core`'s classpath.
- **`:app`** holds everything Android: the Compose interface, the Media3 service, the platform integrations, and the permission flows.

Why: a Linux desktop version is likely later and a web version is possible. The boundary is drawn now while it is free rather than retrofitted. Full Kotlin Multiplatform was considered and deliberately not adopted for version one; a pure-Kotlin `:core` can become a multiplatform module later without rewriting its logic.

## Components and responsibilities

Pending. As each is built, record: what it owns, what it deliberately does not own, and which module it lives in. Expected entries include the Subsonic client and its tolerant parser, the sync engine, the library repository, the download manager and its foreground service, the playback service, the waveform tap, and the export and import layer.

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
- **Bandcamp's API is in open beta and returns durations as floats** where the schema says integer. Tolerant parsing is a structural requirement, not defensive coding, and it is implemented before any feature.
- **Downloads are real files in the public Music folder**, which is the app's central differentiator and also means files can vanish underneath it at any time. Reconciliation is therefore a normal code path, not an error path.

Add discovered constraints here as verification and building reveal them, with the measurement or observation that established each.
