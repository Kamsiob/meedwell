# API verification protocol

**Status: complete. Run 15 August 2026 against the live account.** The full results block is in `DECISIONS.md` under that date and is the authoritative copy. The tables below are filled in with what the server actually returned.

Server: `https://bandcamp.com/api/subsonic`, and **every call goes to `https://bandcamp.com/api/subsonic/rest/<endpoint>`**. Credentials come from the owner's Bandcamp Fan Settings under Subsonic and live at `~/.kamsiob-secrets/meedwell-subsonic.env`, outside the repository, mode 600. They never enter the repository, a log, or a commit.

Always send `v=1.16.1`, `c=Meedwell`, `f=json`.

**Method.** Each endpoint was called and its raw JSON saved to `~/.kamsiob-secrets/meedwell-api-responses/`, one file per endpoint, outside the repository. What is recorded below is what the response actually contained, not what the schema says it should.

**Re-running this.** The harness used is a short shell script that reads the credentials file, builds the token and salt, and saves each response. It is not committed, because it exists to be thrown away and rewritten against whatever the beta looks like next time.

---

## 0. The finding that comes before all the others

`{"error":true,"error_message":"bad version"}` is **Bandcamp's response to an unknown route**, not a protocol version complaint. Proven by calling a deliberately invented endpoint name and getting the identical body. It is not a `subsonic-response` envelope.

Reading it as a version problem sends you tuning `v=` through every value from 1.8.0 to 1.16.1, all of which return it, and none of which is the cause. The cause is the missing `/rest/` path segment, or an endpoint Bandcamp has not implemented.

## 1. Authentication

| Question | Answer | Notes |
|---|---|---|
| Does token plus salt auth work? `t=MD5(password+salt)`, `s=salt` | **Yes.** This is what Meedwell uses | The only scheme available |
| Does an OpenSubsonic `apiKey` work? | **No.** `getOpenSubsonicExtensions` returns an empty array | No extensions are offered at all |
| Which does Bandcamp prefer or document? | Token and salt, by elimination | |
| Does plaintext `p=` work, and should it be refused anyway? | **It does not work.** Returns HTTP 500. Refused by Meedwell regardless | |
| Reproduce the reported unexplained 401 if possible | **Cause found.** A failed login returns **HTTP 500 with an empty body**, never a Subsonic error code. Different clients surface that bare server error differently, which is where the 401 report comes from | |
| What does the error body look like for a wrong password? | **There is no body.** HTTP 500, `content-length: 0` | Feeds the Connection trouble screen, which had to be rewritten. See section 8 |

**The trap worth naming twice: `ping` does not enforce authentication.** It returns `status: ok` for a wrong password. Credential validation must call a data endpoint. Meedwell uses `getArtists`.

## 2. Endpoint matrix

| Endpoint | Exists | Returns what the app needs | Notes |
|---|---|---|---|
| `ping` | Yes | Server identity only | **Does not validate credentials.** `type: BandcampServer`, `serverVersion: 1.0`, `openSubsonic: true` |
| `getOpenSubsonicExtensions` | Yes | Yes | Extensions array is **empty** |
| `getLicense` | Yes | `valid: true` | |
| `getMusicFolders` | Yes | One folder, id 1, "Collection" | |
| `getAlbumList2` | Yes | Yes, **including `coverArt`** | The field report about missing cover art did **not** reproduce. `type=frequent`, `recent` and `highest` return empty; `newest`, `alphabeticalByArtist`, `random` and `starred` work. `offset` paging works |
| `getAlbum` | Yes | Yes, with the full song list | |
| `getArtists` | Yes | Yes, indexed, with `coverArt` and `artistImageUrl` | Carries the Artists view |
| `getArtist` | Yes | Yes, with the artist's albums | |
| `getGenres` | Yes | Yes, with `songCount` and `albumCount` | Carries the Genres view |
| `getAlbumInfo2` | **No** | | **Liner notes do not exist.** That section is not built |
| `getArtistInfo2` | **No** | | |
| `search3` | Yes | Albums, tracks and artists | Searches the user's own collection only, as expected |
| `getCoverArt` | Yes | 700x700 JPEG via redirect to `bcbits.com` | **The `size` parameter is ignored.** Same bytes with or without it, so the app resizes locally |
| `stream` | Yes | 302 to a `bcbits.com` asset | Always `mp3-v0`. See section 3 |
| `download` | **No** | | **The decision. See section 4** |
| `star` | Yes | Works for tracks and albums | |
| `unstar` | **Broken** | Returns `status="failed"`, `code="0"`, **as XML, ignoring `f=json`** | Every form tried. Loved is one-way against Bandcamp today |
| `getStarred` | Yes | Yes | Carries the Loved screen |
| `getStarred2` | **No** | | Use `getStarred` |
| `getPlaylists` | Yes | Empty list | Read only |
| `getPlaylist` | **No** | | |
| `createPlaylist` | **No** | | **Lists cannot sync to Bandcamp.** See section 7 |
| `updatePlaylist` | **No** | | |
| `deletePlaylist` | **No** | | |
| `scrobble` | Yes | Returns ok | Play history stays local regardless |
| `getScanStatus` | **No** | | |
| `getNowPlaying` | **No** | | |
| `getRandomSongs` | **No** | | |

## 3. Streaming

| Question | Answer | Notes |
|---|---|---|
| What format and bitrate does `stream` return? | **MP3 V0.** The redirect target path is literally `/stream/<hash>/mp3-v0/<id>` | Track metadata agrees: `suffix: mp3`, `contentType: audio/mpeg`, `bitRate: 256`. **Never promise lossless** |
| Are there transcoding parameters, and are they honored? | Accepted and **ignored.** `format=raw`, `format=flac` and `maxBitRate=320` all return the same `mp3-v0` asset | |
| Does it support range requests and seeking? | **Yes.** A `Range` header returns 206 with a correct `content-range` | Gapless and seeking have what they need |
| Behavior on a track that will not stream | Not reproduced on this collection; all 60 tracks resolved | The "one track that will not play" state is still built, because a beta will produce it eventually |

## 4. Downloads, the decision that gates public copy

| Question | Answer | Notes |
|---|---|---|
| Does a `download` endpoint exist and work? | **No.** Returns the unknown-route body | Tested as `download`, `download.view`, `Download`, by track id and by album id |
| Does it yield the original purchased file, for example FLAC? | Not applicable | |
| If transcoded, to what format and bitrate exactly? | Not applicable | |
| Does it respect a wifi-only client decision? | Not applicable | Wifi-only remains a client side setting for streaming |
| Does it rate limit a run of several gigabytes? | Not applicable | |
| What happens with pre-orders and unreleased items? | Could not be tested; the collection holds none | Recorded as unverified. The pre-order shelf issue stays blocked |
| What does it return for something with no purchasable file? | Not applicable | |

**Tier decided: C.** No usable download endpoint. Recorded in `DECISIONS.md` with the evidence. The "Your files" fallback screen replaces the marquee Downloads screen, and local folder scanning becomes a version one requirement.

## 5. Data shape and parsing

| Question | Answer | Notes |
|---|---|---|
| Which numeric fields come back as floats rather than integers? | **None on this account.** `duration`, `track`, `year`, `size`, `bitRate`, `songCount` were all integers | The reported float durations did not reproduce. Tolerant parsing is built anyway |
| Which documented fields are absent entirely? | `discNumber`, `albumArtist`, `path` | So multi-disc and compilation rules apply to local files only |
| Which fields are null where the schema implies a value? | **None found** | |
| Are there nonstandard fields Bandcamp adds? | `artistImageUrl`, and an OpenSubsonic style `genres` array alongside the plain `genre` string | |
| Are IDs stable across sessions and syncs? | Prefixed and consistent within a day: `b:` band, `a:` album, `t:` track, `ca:` cover art, `ci:` artist image. **Stability across longer periods is assumed, not verified** | Recorded as an assumption |
| How are compilations and multi-artist albums represented? | One album artist, with per-track `artist` differing. Confirmed on "Medieval Times": three distinct track artists under one album artist | |
| How are multi-disc releases represented? | **No `discNumber` field exists**, so they are not represented | |
| Total collection size and response times | **3 albums, 60 tracks, 3.5 hours.** Metadata calls about 200 ms | Far too small to validate large-library work. The synthetic emulator test carries that alone |

**Two real beta bugs found in the data,** both absorbed by tolerant parsing: `getStarred` returns a song object keyed **`idDir`** instead of `isDir`, and album `genres` arrays contain duplicate entries.

## 6. Bandcamp page URLs

| Question | Answer | Notes |
|---|---|---|
| Does any response carry a real Bandcamp page URL for an artist or album? | **No.** The only URL anywhere is `artistImageUrl`, an image on `f4.bcbits.com` | Checked against the raw JSON, not the parsed model |
| If not, does a constructed `bandcamp.com/search?q=NAME&item_type=b` deep link land usefully? | Yes, and it is what the app uses | Feeds "Their Bandcamp page" and all share text |

## 7. Rate limits and etiquette

| Question | Answer | Notes |
|---|---|---|
| Any documented or observed rate limit | **None observed.** 20 rapid sequential calls, zero failures, 105 ms average | |
| Any required or advisable request pacing during a full sync | **Sequential, no artificial delay, no parallel fan-out.** Decided deliberately | Being an unremarkable client of an open beta is correct and in the app's interest |
| Does the API return caching headers worth honoring | **No.** No `cache-control`, `etag` or `last-modified` on metadata calls | Local caching is entirely Meedwell's problem |

## 8. What verification changed in the specification

Recorded here because these are the places where the built app now differs from what the documents said before 15 August 2026.

1. **Downloads become Tier C.** `MASTER_SPEC.md` section 5 rewritten. The marquee "Download everything" button does not exist.
2. **Playlists are local only.** `createPlaylist` and friends are absent, so "Save as list" cannot write back to Bandcamp and the two-way sync claim is withdrawn.
3. **Liner notes are not built.** `getAlbumInfo2` is absent.
4. **The Connection trouble screen shows a bare server error, not `error 40`.** No Subsonic auth error code is ever returned. `DESIGN.md` corrected.
5. **Loved is one-way.** `star` works, `unstar` is broken server side. The interface says so rather than offering a control that silently fails.
6. **Streaming quality wording is fixed at MP3 V0.** No copy may imply lossless streaming.
7. **The album-list cover art workaround is unnecessary** and that issue closes as not reproducing.
