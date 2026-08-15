# Working on Meedwell

Read `spec/HANDOFF.md` in full first. It is the resume document and it is kept
current, so it tells you the real state of the project rather than the intended
one.

## Standing writing rules

These apply to everything the project produces: interface strings, every
document in `spec/`, `DECISIONS.md`, `HANDOFF.md`, code comments, KDoc, commit
messages, issue text, the README and store copy. They are not re-litigated per
file.

**1. American English.** Standing order from the owner, given 15 August 2026.

Write: color, behavior, license, normalize, organization, catalog, canceled,
gray, toward, math.

Not their British forms. (Deliberately not spelled out here: the spell-sweep in
`tools/` would rewrite the counter-examples and turn this rule into "color, not
color", which is exactly what happened the first time.)

Three things are deliberately **not** converted, because changing them would be
wrong rather than inconsistent:

- **Verbatim quotations**, above all the nine attribution conditions in the
  Surroundings library. Those are a recordist's own words.
- **Proper nouns and official names**, such as "Creative Commons Attribution
  4.0 International".
- **Existing data field names** in the Surroundings `manifest.json`
  (`license_name`, `license_version`, `license_url`, `license_short`,
  `license_tier`). They are a published, hash-verified data contract produced by
  the owner's own pipeline. Renaming them would fork that schema.

**2. No em dashes.** Anywhere. Not in copy, documentation, commit messages,
store text or comments meant for humans. Use commas, periods or colons. This
rule predates the American English one and is recorded in `DESIGN.md` section 11.

**3. The voice.** Plain, warm, honest. Write like a person explaining something
to a friend across a table. Short sentences, contractions welcome. No
exclamation points, no hype words, no fear language. Buttons say exactly what
they do and keep the same name through a whole flow. Errors explain what
happened and offer a way forward. An empty screen is an invitation, never a
scolding. Honest limits appear in the interface at the moment they matter.

## Binding documents

`spec/DESIGN.md` wins over code on anything visual or written.
`spec/MASTER_SPEC.md` wins on anything functional.
`spec/DECISIONS.md` records why things are the way they are; read it before
reversing something that looks wrong.

## The build

AGP 9 brings its own Kotlin support, so `org.jetbrains.kotlin.android` must not
be applied. The Gradle daemon needs a JDK 21:

```
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
export ANDROID_HOME=/var/home/Kamsiob/Android/Sdk
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

Credentials live at `~/.kamsiob-secrets/`, never in the repository. A pre-commit
hook refuses anything credential-shaped; enable it in a fresh clone with
`git config core.hooksPath .githooks`.
