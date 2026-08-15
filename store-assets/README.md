# store-assets

Play Store listing images, generated from the design system rather than mocked up by hand, so they cannot drift from what the app actually looks like.

**Status: generated in Phase 7, not before.** Screenshots must come from the built app running on a real device, per the standing screenshot rule. Store assets built from mockups would show an app that does not exist.

## What goes here

- **Phone screenshots**, captured from the running app on the target device via ADB, in both themes, at the required Play dimensions. Same set of screens as the README screenshots so the two never disagree.
- **Feature graphic**, 1024 by 500, built from the design tokens in `DESIGN.md`. The mark is a coin at rest in a shallow copper cradle; the construction rules in `DESIGN.md` section 7 apply here exactly as they do in the app.
- **App icon**, exported at every size Play requires, in the default Rustic Copper finish.

## Rules

Every screenshot caption follows the copy voice in `DESIGN.md` and carries no em dashes.

No screenshot shows a capability that verification did not confirm. If the download tier turned out to be B or C, no screenshot may imply A.

Recapture the full set before every release, not just the screens that changed, so the set never mixes versions.

The demo album in every screenshot is "Copper Lines by The Long Static", chosen so the mark is never confused with album artwork.
