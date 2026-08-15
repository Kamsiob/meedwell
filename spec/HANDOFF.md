# Handoff

The resume document. Kept current at all times so a session with no memory of any previous conversation can pick up cleanly after a disconnection, a crash, context exhaustion, or a gap of weeks.

**Read this file in full at the start of every session.** That is its purpose. Then search the other documents for the sections relevant to the current item rather than loading them whole.

Structure: state of play, next step, and the remaining work inventory at the top; the longer historical record below. Prune superseded detail rather than letting it grow without bound.

---

## State of play

Nothing built yet. This is the seeded handoff shipped with the build kit, and the first session replaces this section with reality.

The design is complete and locked at 46 screens in `reference/meedwell-screen-grid-final.html`. The functional specification is complete in `MASTER_SPEC.md`. Research is complete and summarised in `reference/meedwell-research-handoff.md`, which is the long-form background document; `MASTER_SPEC.md` is the authoritative version of anything they both cover.

## Next concrete step

Phase 0 in `MASTER_SPEC.md` section 11: initialise git, create the public repository, copy in `repo-seed/`, commit the specification and design documents, open the seeded issues from `ISSUES-SEED.md`, set up the project board, scaffold the two modules, and prove the app launches.

Then, before any feature work, run `API-VERIFICATION.md` in full and record the results in `DECISIONS.md`. Tolerant deserialisation is the first code written.

## Remaining work inventory

Every item below is **not started**. As work proceeds, mark each verified, unverified, partial, not started, skipped, or blocked, and describe partial items precisely enough to resume mid-task.

- Phase 0, repository and verification
- Phase 1, the working core
- Phase 2, ownership and downloads
- Phase 3, the collection
- Phase 4, local files
- Phase 5, platform surfaces
- Phase 6, files and backup
- Phase 7, hardening and release

The issue tracker is the item-level inventory once Phase 0 has opened the seeded issues. Until then `ISSUES-SEED.md` is.

## What has been tried and did not work

Nothing yet. This section is the most valuable part of the document once work begins, because it is the knowledge most likely to be lost and most wastefully repeated. Record every approach that failed or was rejected, what happened, and whether it should never be tried again or might be worth revisiting under specific different circumstances, naming those circumstances.

## Measurements

Nothing yet. Record real numbers rather than impressions: collection size, sync duration, download throughput, app size, cold start time, memory during a large sync, and the timing figures from the large-library emulator test.

## Environment and toolchain notes

The development machine runs Bazzite, an immutable Fedora Atomic system with a read-only `/usr`, so installers that write to system directories or call `useradd` will fail. Install into the home directory, a virtual environment, or a container, and record which was used and how to invoke it.

A physical Android device connects over ADB. Device rules are in `MASTER_SPEC.md` section 10 and are strict: touch nothing on the phone except this app, one copy only, in-place upgrades, destructive tests on an emulator, and never capture the screen unless Meedwell is in the foreground.

Reinstalling a debug build can silently clear system role selections. Development-only annoyance; note the restore commands here when encountered.

## Decisions a future session might reverse

See `DECISIONS.md`, "Decided before the build started". The ones most likely to look wrong without their reasoning: the unencrypted database, the unencrypted export, and the retirement of the adaptive-scrim legibility law. Read the reasoning before changing any of them.

## Waiting on the owner

See the BLOCKED section of `DECISIONS.md`. Currently nothing beyond the known manual Play Console steps and the one-time ADB authorisation.

## Open questions and unverified assumptions

Everything in `API-VERIFICATION.md`. Nothing about the live API should be treated as known until that document is filled in.
