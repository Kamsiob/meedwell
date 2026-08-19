# Surroundings

A field recording playing under the music. One hundred and eleven of them,
three inside the app and the rest published as a GitHub release.

**The measurement authority for the card is
`reference/meedwell-surroundings-card.html`.** For every other Surroundings
surface it is `reference/meedwell-screen-grid-CURRENT.html`. Where prose here
and either file disagree, the file wins and this document is corrected.

---

## The surfaces

Seven, and the seventh is new.

| # | Surface | Where | Grid screen |
|---|---|---|---|
| 1 | The library | Surroundings tab | 13 |
| 2 | A group | inside the library | 14 |
| 3 | Recording detail | a sheet over the library | 15 |
| 4 | Storage | inside the library | 16 |
| 5 | The player's right page | player spread | 09 |
| 6 | Downloading | Surroundings tab, in progress | 25 |
| 7 | **The floating card** | Shelf, Search, Lists, More | card file |

Surroundings is a **first-class destination**, one of four tabs. It was buried
under More, which made a headline feature something you had to already know
about. The width came from folding Lists into the Shelf's own view switcher as
"Shelves".

---

## 7. The floating card

**This supersedes any earlier entry saying a floating pill control was
considered and rejected.** That entry is void. This card is the approved form
and it is built.

### When it exists

Only while a sound is playing. Nothing floats by default. It slides in when a
sound starts and leaves when it stops, so it is always a consequence of
something the listener did rather than an interruption.

**There is no setting to disable it.** It only exists while a sound plays, and
swiping it away stops the sound, so there is nothing to opt out of. Do not add
one.

### Where it appears

Shelf, Search, Lists and More.

**Not on the player spread.** The Surroundings page there already holds a full
volume control and the library, so the card would duplicate it. Suppressed
whenever the player is open, and on the Surroundings tab itself.

### The transparency, precisely

The detail most likely to go wrong.

| | Daylight | Lamplight |
|---|---|---|
| Background | `rgba(246,244,236,.88)` | `rgba(18,22,15,.88)` |
| Border | `1px rgba(28,36,32,.16)` | `1px rgba(239,238,230,.16)` |
| Shadow | `0 10dp 26dp -14dp` at 42% ink | same at 80% black |
| Corner | 16dp | 16dp |

The background is the **current theme's own ground colour** at 88 percent, not
white and not grey. A translucent white over warm paper goes chalky, which is
why it is tinted with the ground rather than lightened.

**No backdrop blur. No frosted glass. No gradient. No Material scrim or dialog
surface.** Content behind must ghost through faintly rather than being obscured.
A blur here would read as glassmorphism, which is exactly the generic look being
removed from this build.

### Layout and behaviour

Inset **14dp from each edge**, so it reads as a separate floating layer rather
than a second full-width bar. It sits above the music mini player.

Collapsed is roughly **62dp**: the sound's name, a volume line, and pp / ff at
the ends. Tapped, it expands downward to roughly **210dp**.

**It expands downward from a fixed anchor.** The top edge and the volume line do
not move, so a finger already resting on the volume is not displaced. Only the
height animates.

**The list is not a browser.** Only recordings already on the phone, **four at
most**, sorted by most recently used with the playing one first and marked. Not
scrollable. "All recordings" at the foot opens the Surroundings tab. If it
listed downloadable recordings it would become a second Surroundings tab living
in a corner.

### Gestures

- tap anywhere on the card to expand or collapse
- drag along the volume line to set level
- drag the card down to fold it, up to open it. Neither stops the sound
- stop the sound from the named row at the foot of the opened card
- long press does nothing

### It never blocks content

Any list or grid behind it takes bottom padding equal to the card height plus
the mini player height, so the last row is always reachable. That padding is
recomputed when the card appears, expands, collapses or disappears.

### Accessibility

The expanded and collapsed state is announced. The volume is a slider whose
value is described **in plain words** rather than as pp and ff, since those mean
nothing read aloud. Every row is 44dp minimum. At 200 percent font scale the
card grows rather than truncating.

---

## The audio, and why it is arithmetic

From the library's own `REQUIREMENTS.md`, all of it proved in `:core` tests
rather than by ear.

**A 2 to 3 second loop crossfade is mandatory.** Every file has only a 50 ms
fade at each edge on purpose, so a hard cut from end to start joins two
unrelated points in a waveform and clicks. The fade is **power preserving**,
`cos` and `sin` of one quarter turn, because a linear crossfade of two
uncorrelated signals dips about 3 dB in the middle and is heard once per loop.

**Playback gain of 0 to +18 dB with a true-peak limiter.** ExoPlayer's own
volume cannot exceed 1, so the gain lives in a float audio processor with the
limiter directly behind it. The limiter reconstructs the signal at four times
the sample rate through a windowed-sinc polyphase filter, as ITU-R BS.1770
specifies, because a signal whose samples are all legal can still clip between
them.

**Loop-point compensation** ramps the incoming loop by `head − tail` across the
fade, which turns a step of up to 7 dB into a swell.

---

## The hard rule on attribution

**A manifest entry missing any attribution field is invalid and its recording is
never offered**, however good the audio is. So is one with no checksum, which
cannot be verified, and one with no size, which cannot be consented to.

Enforced in exactly one place, `Downloads.offerable`, so no future screen can
route around it. Every credit shown anywhere is generated from `manifest.json`
rather than typed, so it cannot drift, and the uploader's own extra conditions
are reproduced word for word.

---

## Downloads

Three granularities, each stating its cost before it is tapped: one recording, a
group, or the whole library. The figure is always what is genuinely missing
rather than the catalog total.

Wi-Fi only is on by default and checked immediately before each transfer, not
once at the top of a batch. Nothing is installed unverified: a partial is
written under a temporary name, its SHA-256 checked, and only then renamed into
place, which is atomic.

**Updates are never automatic.** Only "Check for new recordings" ever fetches a
new manifest. Nothing checks on a timer, on launch, or in the background.
