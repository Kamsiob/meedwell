# Contributing to Meedwell

Meedwell is maintained by one person. Issues and pull requests are welcome, and response times vary. Everything gets read; not everything gets a reply.

## How the implementation is written

The specifications in this repository are authoritative, and the code is written by a coding agent working from them. That is why `MASTER_SPEC.md` and `DESIGN.md` are treated as binding documents rather than as notes, and why a change to behaviour or appearance belongs in those documents in the same commit as the code. A pull request that changes behaviour without updating the specification will be asked to update it.

## Reporting a bug

Open an issue using the bug template. The fields it asks for are the ones that make a bug reproducible: what you did, what happened, what you expected, your Android version and device, the Meedwell version from the About screen, and whether you were connected to Bandcamp or using local files only.

Please do not include your Bandcamp credentials in an issue. Nothing about a bug report requires them.

## Reporting a security or privacy problem

Do not open an issue. See `SECURITY.md`.

## Proposing a change

Open an issue first and describe the problem you want solved rather than the solution you have in mind. Meedwell has a deliberately narrow scope and several features are absent on purpose rather than by oversight; the What's Ahead screen in the app and the pinned roadmap issue list both what is being considered and what is not planned, with the reason in each case.

## Setting up a development environment

You need a recent Android Studio, a JDK matching the version in the Gradle configuration, and either a physical device with developer options enabled or an emulator.

```
git clone https://github.com/kamsiob/meedwell.git
cd meedwell
./gradlew assembleDebug
```

To run the tests, including the instrumented set:

```
./gradlew test
./gradlew connectedAndroidTest
```

Both must compile and pass before a pull request is considered. A test source set that does not compile is worse than no tests, because it looks like coverage while providing none.

You do not need a Bandcamp account to build the app or to work on anything in local files only mode. You do need one to work on anything touching the API.

## Code and commit conventions

The project is Kotlin with Jetpack Compose. `:core` holds pure Kotlin with no Android dependencies, enforced by the build; `:app` holds everything Android. Logic that could ever run on another platform belongs in `:core`.

Commit messages use a short prefix, an imperative summary, a body where the reasoning is not obvious, and the issue number:

```
fix: keep the queue sheet's order after a process death (#42)
```

Prefixes in use: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `chore`. The specific convention matters far less than following it without exception.

Commits are signed. A log where every message follows one shape and every commit is verified reads as discipline; a mixture reads as whoever happened to be typing.

## What will and will not be accepted

Accepted: bug fixes, accessibility improvements, translations, performance work, and anything that makes an honest limit clearer.

Not accepted: anything that adds analytics, telemetry, tracking, an account system, an advertisement, a subscription, or a paywall. Anything that scrapes a website rather than using a documented API. Anything that adds an engagement mechanic such as a streak, a badge, or a notification designed to pull someone back into the app. Anything that makes a claim in the interface the software cannot actually deliver.

These are not preferences that a good enough implementation could overcome. They are the reason the app exists.

## Licence

Contributions are made under the AGPL-3.0, the same licence the project carries.
