# Play listing: rules, and why this file holds no copy yet

**There is deliberately no draft listing in this file.** Writing it now would break the sequencing rule in `MASTER_SPEC.md` section 3 and `API-VERIFICATION.md`: several of the listing's central claims depend on API answers nobody has yet, chiefly whether downloads yield original files, transcoded files, or nothing at all. A listing written before verification is a promise the software might not keep, and correcting a live store listing afterwards is worse than waiting a day.

Write the listing in Phase 7, from the verified reality, using these rules.

## Before writing a single line

Research current AI-slop tells in language, then deliberately avoid them. This is a standing rule and it comes first, not after a draft exists. The usual offenders in app store copy: triads of adjectives, "seamlessly", "effortlessly", "unlock", "elevate", "reimagined", rhetorical questions as openers, and sentences that could describe any app in the category.

Then read the What's Ahead and Privacy screens in the built app. The listing must not claim anything those screens contradict.

## Hard requirements

**No em dashes.** Anywhere. Title, short description, full description, release notes, screenshot captions.

**"Not affiliated with or endorsed by Bandcamp"** appears in the full description. "Bandcamp" is used nominatively only, as the name of a service the app works with, never in a way implying partnership or endorsement.

**State English-only plainly.** Version one ships in English. Say so in the listing rather than letting a user in another locale discover it. Translation is in Being Considered, not promised.

**No lossless streaming claim** unless verification actually found lossless streaming, which is unlikely. Whatever the stream format turns out to be, either name it accurately or do not raise the subject.

**The download claim matches the tier that was decided.** Tier A may say original quality. Tier B names the real format and bitrate plainly. Tier C does not claim in-app downloads at all, and instead describes recognizing the files a user downloads from Bandcamp themselves.

**Every capability claim is traceable to something verified in the built software.** If a sentence cannot be pointed at a working feature on the device, it does not ship.

**Honest limits appear in the listing, not only in the app.** The most important one: the API exposes only the user's own collection, so there is no store or catalog search inside Meedwell. Say that plainly rather than letting someone install it expecting a Bandcamp shopping app. It filters out the wrong users before they leave a one-star review, which is worth more than the installs it costs.

**Category:** Music and Audio. **Not** a category implying a store or a social product.

**Data Safety:** no data collected, no data shared. This must remain literally true in the built software, and the declaration is uploaded through the Play API as CSV where the API permits it.

**Support label:** if the listing mentions supporting the work at all, the words are "Support this work". Never a coffee or caffeine reference, never a framing that anchors support to a small amount, never anything reading as an appeal.

## Assets, generated from the design system

Generate rather than hand-assemble, so the store and the app cannot drift:

- **Icon**, the Rustic Copper finish, from the same source as the launcher icon.
- **Feature graphic**, using the design tokens. The mark and the wordmark on the warm near-black, with nothing else in frame. No screenshot collage, no device frames, no gradient, no floating UI fragments.
- **Screenshots**, captured from the running app on the device per `DESIGN.md` section 13, never mockups and never from the reference grid. Both themes represented. Suggested set, in order: the shelf, an album, now playing, downloads, and the privacy screen, because leading with the shelf shows what the app is and ending on privacy shows what it is not.
- Captions on screenshots, if used at all, are short and factual.

## Release notes

Derived from the issues the milestone contained, not written from memory. Plain language, no marketing.

**Never write that the Play build and the GitHub build are signed differently, or that one must be uninstalled before installing the other.** Both channels carry the same Google-signed build under Play App Signing, so a user moves between them freely with their data intact. Any such wording from an older approach is wrong.

## What goes in LAUNCH.md instead of here

`LAUNCH.md` is produced in Phase 7 and contains only the owner's remaining manual clicks, in plain numbered steps, with no code and no explanation of things already done: creating the app entry, uploading the first bundle through the web interface, the IARC questionnaire, the ads declaration, and the app access instructions. Everything else is automated through the Play API and should not appear there.
