# Bundled fonts

Meedwell bundles its fonts rather than fetching them, so the interface looks the
same offline and nothing is requested from a font server at runtime. Fetching a
font at runtime would be a network call this app does not make.

| Font | Files | Licence |
|---|---|---|
| Instrument Sans | `app/src/main/res/font/instrument_sans_variable.ttf` | SIL Open Font License 1.1 |
| Instrument Serif | `app/src/main/res/font/instrument_serif_regular.ttf`, `instrument_serif_italic.ttf` | SIL Open Font License 1.1 |

Both are Copyright 2022 The Instrument Sans Project Authors. The full licence
text is in `OFL-InstrumentSans-InstrumentSerif.txt` beside this file.
