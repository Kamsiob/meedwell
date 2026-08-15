# Artifact checklist: template A1 mapped to this kit

Every file `reference/kamsiob-project-template.md` section A1 requires, and where it is. This exists so the question "is anything missing?" has a checkable answer instead of needing an audit, and so a deliberate deferral cannot be mistaken for an omission.

**Seeded** means it is in this kit and gets copied into the repository in the first commit. **Skeleton** means the structure and known content are seeded and Claude Code fills the rest as the code is written. **Build-time** means it correctly does not exist yet, with the reason stated.

| Required artifact | Status | Location |
|---|---|---|
| MASTER_SPEC.md | Live | `spec/MASTER_SPEC.md`. Rewritten 15 Aug 2026 where verification contradicted it |
| DESIGN.md | Live | `spec/DESIGN.md`. Connection trouble copy corrected; three verified limits added |
| HANDOFF.md | Live | `spec/HANDOFF.md`. Replaced with real state at the end of Phase 0 |
| DECISIONS.md | Live | `spec/DECISIONS.md`. Carries the API verification results block |
| PRIVACY.md | Seeded | `spec/PRIVACY.md` |
| LICENSE | Seeded | `repo-seed/LICENSE` |
| .gitignore | Seeded | `repo-seed/.gitignore` |
| CONTRIBUTING.md | Seeded | `repo-seed/CONTRIBUTING.md` |
| SECURITY.md | Seeded | `repo-seed/SECURITY.md` |
| CODE_OF_CONDUCT.md | Seeded | `repo-seed/CODE_OF_CONDUCT.md` |
| .github/ISSUE_TEMPLATE/ | Seeded | `repo-seed/.github/ISSUE_TEMPLATE/` (bug, feature or change, config) |
| .github/PULL_REQUEST_TEMPLATE.md | Seeded | `repo-seed/.github/PULL_REQUEST_TEMPLATE.md` |
| .github/workflows/ | Seeded | `repo-seed/.github/workflows/` (ci.yml, release.yml with provenance) |
| .github/FUNDING.yml | Seeded | `repo-seed/.github/FUNDING.yml` |
| **ARCHITECTURE.md** | **Partial** | `repo-seed/ARCHITECTURE.md`. Module boundary, data contract intent, and the three known constraints are written. Component, threading, and lifecycle sections are marked pending and filled as built. |
| **CHANGELOG.md** | **Skeleton** | `repo-seed/CHANGELOG.md`. Rules and an Unreleased section. First real entry written at release. |
| **tools/** | **Partial** | `tools/`. `capture-screen.sh` is written and in use. The Bandcamp Friday manifest generator is still pending, tracked as issue #26. |
| **store-assets/** | **Skeleton** | `repo-seed/store-assets/README.md`. Rules only. Assets are generated in Phase 7 from the built app, never from mockups. |
| README.md | Build-time, now unblocked | Phase 7. The sequencing rule that deferred it is satisfied: API verification was recorded on 15 August 2026. Tracked as issue #39. |
| LAUNCH.md | Build-time, correct | Phase 7. Tracked as issue #37. |

## The two deliberate deferrals, recorded as decisions

**README.md is not seeded, and this is deliberate rather than an oversight.** The hard sequencing rule in `MASTER_SPEC.md` section 3 and `MASTER_PROMPT.md` section 3 forbids writing any public copy before API verification is recorded, because the README's central capability claim, that downloads are real files you own, depends on an endpoint nobody has tested. A seeded README would either state that claim before it is verified or ship as a placeholder that the cold read test would flag. It is written in Phase 7 from verified reality. The template requires the file to exist in the finished repository, and it will; it does not require it to exist before the facts do.

**LAUNCH.md is not seeded** because it lists only the owner's remaining manual clicks, and which steps remain is determined by what the Play API turned out to automate. Seeding it would mean guessing, and a launch document that lists already-automated steps is worse than none.

## Defect found and fixed during this audit

Three cross-references pointed at `MASTER_PROMPT.md` section 12. That section does not exist; the differences-from-template section is **section 7**. The prompt was renumbered at some point and the references were left stale, which would have sent a session looking for signing instructions to a section that is not there. Corrected in `spec/MASTER_SPEC.md` at three places. The citation of `MASTER_SPEC.md` section 12 in `ISSUES-SEED.md` is correct and was left alone; that section really is the process rules.

## Keeping this file honest

This checklist is itself a living artifact under the A1b rule. When an artifact moves from skeleton to complete, update its row. If any artifact is ever deliberately dropped for this project, change its row to state that and record the reasoning in `DECISIONS.md`, per the template's rule that a departure must read as a decision rather than an omission. A checklist that claims something exists when it does not is worse than no checklist.
