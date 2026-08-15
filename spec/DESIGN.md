# Meedwell by Kamsiob: design specification

**This document is binding.** Where code and this document disagree, this document wins. It is a living document, updated with every commit. The visual reference is `reference/meedwell-screen-grid-final.html`, 46 screens: 2 mark screens, 38 dark app screens, 4 light reference screens, and 2 system surfaces. Open it and look at it. No written description substitutes for seeing it.

Earlier grids v1 through v10, and the 30-screen and 37-screen versions, are superseded.

---

## 1. The idea the design is built on

Meedwell is for people who buy their music, so the design treats artwork as a possession rather than as decoration. Two consequences run through everything: covers are shown whole and never written on, and gold is reserved for the single moment where money reaches the people who made the music.

Structure is carried by hairlines and space rather than boxes and cards. Warmth rather than neon. Nothing gamer-coded, nothing that reads as a streaming service.

## 2. Themes and colour tokens

**Dark is the default.**

| Token | Dark | Light |
|---|---|---|
| Background | `#0B0B0E` warm near-black | `#F5F3ED` warm paper |
| Primary text | `#F4F3F6` | `#17151D` |
| Secondary text | `#A7A5B1` | `#33303B` to `#44414C` |
| Tertiary floor | `#8B8993` | `#56525E` |
| Hairline border | `rgba(255,255,255,.08)` | `rgba(30,28,38,.11)` |
| Surface panel | `rgba(255,255,255,.05)` | `rgba(30,28,38,.04)` |
| Gold | `#E7C171` | `#8A6215` |
| Copper, the mark | `#AE6738` | `#AE6738` |

**The light-mode ink floor is law.** Nothing lighter than slate `#56525E` on paper, ever. This was corrected twice in design and is not open for reinterpretation. Measured contrast on paper: `#17151D` 16.3:1, `#33303B` 11.6:1, `#44414C` 9.0:1, `#56525E` 6.9:1.

**Gold was corrected for accessibility.** The light gold was `#9A6F1E`, which measures 4.06:1 on paper and fails AA for a 14sp label. It is now `#8A6215`, which measures 4.93:1. Dark gold `#E7C171` on `#0B0B0E` is 11.5:1. Any new colour pair introduced anywhere gets measured before it ships.

**Ambient glow washes,** dark theme: soft radial fields in violet `rgba(112,84,150,.26)`, teal `rgba(58,122,116,.24)`, rose `rgba(150,74,102,.22)` and ember `rgba(168,104,66,.22)`, drifting slowly over roughly 16 seconds, gated behind the reduced-motion setting. At half opacity in light theme.

**Never** pure black or pure white backgrounds. Colour is never the only carrier of meaning.

## 3. The gold rule

Gold means money reaching makers, and nothing else. Exactly two uses in the entire app:

1. The Bandcamp Friday dot on the shelf.
2. The "Support this work" button, as a gold hairline pill with a small gold dot and a faint glow.

The support button appears at the bottom of **Settings** and of **About**, and in both places a short value block leads into it. Settings: "Free no matter what. Nothing held back, nothing unlocked later. One person carries it." About, the fuller version: "Free no matter what. Nothing held back, nothing unlocked later. Built and carried by one person. If software made this way matters to you, there's a place to stand behind it. Either way, it's yours."

The label is always "Support this work". Never a coffee or caffeine cliche, never a visible "buy me a coffee", never framing that anchors support to a small amount, never anything reading as begging or pressure. It links to https://buymeacoffee.com/kamsiob. The terms are stated before any invitation is made, and no ask is made at all.

## 4. Typography

**Instrument Sans** for the interface. **Instrument Serif italic** for the voice lines, which are the app's quiet editorial moments: "148 albums, 62 of them living here", "On your shelf since June 2023", "Bought, loved, and quietly waiting". Also used for the "yours" provenance markers and for the letters in a missing-cover placeholder.

Tabular numerals on every time, duration, size and count.

Both fonts are bundled, not fetched.

**The scale, in sp.** The visual reference is a 330px mock of roughly a 412dp screen, so mock pixels multiply by about 1.25 to reach sp:

- Caps eyebrow: 12sp, weight 600, letter-spacing 2.2, uppercase
- Secondary and metadata: 14sp
- Body and list row titles: 16sp to 17sp
- Section heading: 32sp, weight 700, tight tracking
- Large heading: 42sp to 50sp, weight 700, tracking to -2

**Nothing in the app is smaller than 12sp.** Every screen must survive 200 percent font scale and enlarged display size. The album screen, Settings and the action sheet are the known pressure points.

## 5. The legibility law

This supersedes the earlier adaptive-scrim law completely. The scrim approach was retired because it was the problem: any system that measures artwork and darkens it has a worst case. This one has none.

**Artwork and words never share pixels.**

- Artwork is always shown **complete**: never cropped, never faded, never scrimmed, never written on.
- Text lives only on theme surface, past a hard edge.
- The album screen shows the full square cover edge to edge, with the toolbar above it on surface and text beginning only past the art's bottom edge. On scroll the cover collapses into a hairline toolbar carrying a 30dp thumb, the title, Play, and the action menu.
- The shelf's newest-arrival card places the whole cover **beside** its caption rather than underneath it.
- A **full-screen artwork viewer** sits one tap behind every cover in the app. It is themeless by design: the complete art on near-black in both light and dark, no text over it, pinch to zoom, tap to close.

**The single sanctioned text-over-colour moment** is the now-playing wash: a palette-derived colour field, which is not the artwork, clamped below a brightness ceiling so white always passes on any album in either theme. The complete cover sits above it.

**Net effect: legibility no longer depends on the artwork at all, in either theme.** No measuring, no adaptive anything, no worst case.

## 6. The missing-cover rule

The legibility law makes this load-bearing rather than cosmetic: if words may never sit on art, absent art must not leave a hole where words are forbidden.

A missing cover is drawn as **surface, never as a fake image**: a surface panel with a hairline, carrying the title's own first letters in Instrument Serif italic at secondary ink.

Never a grey box. Never a generic music-note icon. **Never the Meedwell mark**, because borrowing the mark for album art blurs the one thing it must never be confused with.

On the album screen a missing cover means the art region is **omitted entirely** rather than held open, so the screen opens on the title.

## 7. The mark

Flat. A rustic copper `#AE6738` coin resting in a shallow copper cradle, an open arc, on flat near-black `#16121C`, in a rounded-square icon frame at 22.5 percent corner radius. The cradle is a stroke, the coin is a fill; the pairing keeps the mark two shapes.

No gradients, no glow, no dimension. Earlier versions, a three-dimensional treatment and a circle-on-flat-line construction from the Siob era, were both retired; the flat-line construction is superseded by the cradle.

It reads as a record in an open sleeve, a coin in a waiting palm, the pan of an honest scale, and above all as the name itself: **the meed, held well**. The earned reward received and kept, the way money should come to rest with the people who made the music.

**Construction rules:** the coin rests at the lowest point of the cradle, touching it, never sunk into it and never floating above it. The cradle's arc is shallow, its ends rising level with each other and stopping short of the frame so the arc breathes. Nothing else is in frame. One-colour stroke variants exist for both themes. The cradle stroke scales with the mark: hairline at favicon sizes, deliberate at icon sizes, never heavier than the coin's radius reads.

Wordmark lockup: mark plus "Meedwell" in Instrument Sans 700, tight tracking.

The mark keeps three privileges no artwork shares: the rounded-square frame, flat copper on flat black, and empty field around two shapes. Covers live in sharp-cornered squares full of their own life.

**App icon finishes,** user-pickable in Settings through Android's activity-alias technique, which switches instantly, offline, at no cost:

- Rustic Copper, default: `#AE6738` on `#16121C`
- Dusk: `#8B84AE` on `#12121B`
- Moss: `#7C8F5E` on `#11150D`
- Ink: `#F4F3F6` on `#0B0B0E`
- Paper: `#17151D` on `#F5F3ED`

The coin stays at rest in its cradle in every finish.

Demo and screenshot album is "Copper Lines by The Long Static", rose artwork with diagonal etched lines, chosen specifically so the mark is never confused with album art.

## 8. The waveform

In-app surfaces, meaning now playing, the mini player and any future widget, show **real amplitude**: played bars white, remainder dim, drag anywhere to seek. The mini player's waveform stills when paused. Reduced motion gets the static envelope.

Downloaded files show a precomputed full-track portrait, computed once on the phone. Streams draw themselves live as they arrive.

The data comes from Meedwell's own decoder through a custom `AudioProcessor` tap, needs no permissions, and never leaves the phone. **Never the `Visualizer` API,** which requires microphone permission.

System cards, meaning lock screen and notification shade, keep Android's own progress; those pixels belong to the operating system.

**Accessibility:** the scrubber is a custom drag control and is invisible to TalkBack without explicit seek semantics: a slider role, spoken current position and duration, and increment and decrement actions. It is the signature interaction and therefore the most likely accessibility failure in the app. The static envelope already required for reduced motion doubles as the accessible representation.

## 9. System media cards

Flat palette-tinted ground rather than a busy blur. Art at 56 to 64dp. Title and artist. A knobless 2dp hairline progress line with times at the ends. Previous, pause and next with generous spacing. The small copper mark in the top right corner.

Meedwell's job on these surfaces is perfect art, correct metadata, an accurate seek position, and the mark. Nothing else.

## 10. Motion

Exactly two spring personalities: a standard damped spring for everything by default, and an expressive spring with slight overshoot reserved for a small number of signature moments. Three durations, used consistently.

The ambient washes drift over roughly 16 seconds. Waiting tiles during first sync shimmer in the incoming album's own colour, never grey.

Respect the system reduced-motion setting everywhere: no drift, no shimmer, and a static waveform envelope.

## 11. Copy voice

Plain, warm, honest, first-person-adjacent. "A report was saved, here, and only here."

Write like a person explaining something to a friend across a table. Plain words, short sentences, contractions welcome. No exclamation points. No hype words. No fear language. If a sentence could appear in a generic tech advertisement, rewrite it.

**No em dashes anywhere,** in any user-facing copy, documentation, README, commit message or store text. Use commas, periods or colons.

Buttons say exactly what they do, and an action keeps the same name through its whole flow. Interface labels use plain nouns.

Errors explain what happened and offer a way forward. They never apologise theatrically and never go vague. An empty screen is an invitation to act, never a scolding.

**Honest limits appear in the interface** at the moment they matter, not buried in a help screen.

**Three limits verification made real, and where each one is stated.** These are not hypothetical any more, and the interface carries them plainly rather than letting a control fail quietly:

- **Bandcamp does not release files through its API.** Stated on the "Your files" screen, screen 26, which replaces the marquee Downloads screen entirely.
- **Lists cannot reach the Bandcamp collection.** The Lists screen says lists live on this phone. The line "Edits here appear in your Bandcamp collection too" on the playlist screen is **removed**, and "Kept in step with your Bandcamp collection" under the Lists heading is replaced with wording that is true.
- **A heart can be set and not removed.** `star` works and `unstar` is broken on Bandcamp's side. The heart states this where it sits, and points at Bandcamp's website for removal.

**The Connection trouble screen shows a bare server error, because that is what arrives.** Screen 31 in the visual reference shows `error 40 · wrong username or password`. No such code is ever returned: a failed login is HTTP 500 with an empty body. The screen keeps its shape, its reassurance and its three ways forward, and the "What happened" block now reads what actually happened:

```
server error 500 · no reason given
last successful sync · today, 14:02
```

with the explanatory line above it saying that Bandcamp answers a rejected login this way, that it usually means the password was regenerated, and that nothing on the shelf is lost.

**Collector provenance surfaces** in the voice lines: "On your shelf since March 2025", label names, purchase-format honesty, and the "yours" markers in serif italic.

Before writing any store copy, README or website page, research current AI-slop tells in both language and visual design, then deliberately avoid them. Do that research first, not after.

## 12. Accessibility floor

- Contrast meeting WCAG AA in both themes, measured rather than eyeballed. Any new colour pair gets computed before it ships.
- Minimum touch target 48dp. The existing 56dp list row minimum satisfies this; the per-track download circles and the A to Z rail need explicit checking, since both are small by design.
- Complete screen reader labels on every control, including the many icon-only ones: view toggle, sort, share, sleep timer, shuffle, repeat, heart.
- The waveform scrubber per section 8.
- Dynamic type respected to 200 percent without breaking layouts.
- Reduced motion respected everywhere.
- Colour never the only carrier of meaning: the downloaded dot, the Bandcamp Friday dot and the "yours" marker all pair with text or shape.
- Visible focus states throughout.

## 13. Screenshots

Capture from the running app on the device, never from a mockup or this reference file, and only when Meedwell is in the foreground, enforced mechanically in the capture script.

Capture in both themes, keeping the same set of screens represented so the README's story stays coherent. Store them under a predictable directory with stable filenames so replacing one is mechanical.

Whenever a screen changes materially, meaning its layout, controls, colours or copy, recapture the affected screenshots in the same pass as the change. Do not defer and do not batch, because a later pass never comes and the drift compounds. Before any release, recapture the full set regardless of what changed.
