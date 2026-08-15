# tools

Pipelines, generators, and scripts this project needs. Each is runnable with one command, and that command is documented here. Nothing in this folder ships inside the app.

## bandcamp-friday-manifest

**Status: to be built in the phase that implements the Bandcamp Friday line.**

Bandcamp Friday dates are deliberately **not hardcoded**. Hardcoding them means the feature silently expires and starts lying to users the moment the list runs out. Instead this generator produces a versioned JSON manifest that the app fetches, using the same pattern already proven in Bearings content updates, Local AI Hub model installs, and Kam AI packs.

What it must do:

1. Take the known Bandcamp Friday dates as input, midnight to midnight Pacific.
2. Emit a versioned JSON manifest listing each date.
3. Compute a SHA-256 hash of the manifest and include it in the release.
4. Publish the manifest as a GitHub release asset, which has no bandwidth limit, so distribution costs nothing at any scale.

What the app must do with it: verify the hash before use, treat the fetch as user-initiated network activity described plainly, and **degrade gracefully when the list ends**, meaning the gold dot line simply stops appearing rather than showing a wrong date or an error.

Known dates remaining in 2026: 7 August, 4 September, 2 October, 6 November, 4 December.

## Adding a tool here

Any new script gets a section in this file with its one-command usage before it is considered done. A tool nobody can run from its documentation is not finished.
