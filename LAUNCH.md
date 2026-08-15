# Launching Meedwell

Everything that has to happen on Google Play, written for somebody who does not
write code. Each step says what to do, where to click, and what to paste.

The app itself is finished and built. Nothing in this document requires editing
a file or running a command unless it says so.

---

## Before you start

You need a **Google Play Console account**. It costs 25 US dollars once, ever.
Sign up at [play.google.com/console](https://play.google.com/console) with the
Google account you want to own this app permanently. That account cannot be
changed later without transferring the whole app, so use one you will keep.

---

## Step 1. The file you upload

The file is:

```
app/build/outputs/bundle/release/app-release.aab
```

It is about 19 MB. If it is missing, or you want to rebuild it, run this from
the project folder:

```
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
./gradlew :app:bundleRelease
```

**About the signing key.** There is an upload key already made, sitting in
`~/.meedwell-signing/meedwell-upload.jks`, and the password is in
`keystore.properties` in the project folder. Neither of those is in the
repository and neither ever should be.

**Back up that folder somewhere safe now**, before you upload anything. A copy
on a memory card or in a password manager is enough. If you lose it you can ask
Google to reset the upload key, which takes a few days; it is not fatal, but it
is an annoying week.

---

## Step 2. Create the app

In Play Console, **Create app**.

| Field | What to put |
|---|---|
| App name | Meedwell |
| Default language | English (United States) |
| App or game | App |
| Free or paid | **Free** |

Free cannot be changed to paid later. Meedwell is free forever, so this is
correct and permanent.

---

## Step 3. The listing

**Short description**, 80 characters maximum:

> Your Bandcamp collection and your own files, on one shelf. Free, no tracking.

**Full description:**

> Meedwell is for people who buy their music.
>
> Your Bandcamp collection and the music files already on your phone live on one
> shelf. If you own a record twice, the file you own plays. Point Meedwell at a
> folder and everything in it joins the shelf as what it is: plain files, that
> any player can read, that outlive this app.
>
> The forgotten shelf finds records you bought, meant to listen to, and never
> did. It works that out on your phone from your own listening, with no
> algorithm and no feed, and nothing about it leaves the device.
>
> Surroundings puts a field recording under the music. Rain on leaves, a
> fireplace, a rainforest at night, a train at a station. Three come with the
> app and another hundred and eight can be downloaded, and they loop for hours
> without the loop ever announcing itself.
>
> Meedwell collects nothing. No analytics, no telemetry, no identifiers, no
> account with us to make. It talks to Bandcamp to sync and stream what you own,
> and to GitHub to fetch ambience, and only when you ask it to.
>
> Some honest limits. Bandcamp's API streams your collection but will not hand
> over the files, so Meedwell cannot download your purchases: download them the
> way you always have and point the app at the folder. Adding a heart works and
> reaches your account; removing one is broken on Bandcamp's side, and the app
> says so rather than failing quietly. Making your own lists is not built yet.
>
> Free forever, with nothing held back and nothing unlocked later. Open source
> under the AGPL.

**Category:** Music & Audio.

**Tags:** music player, offline music, audio.

**Contact email:** whichever address you want public. It is shown on the store
listing, so use one you do not mind publishing.

---

## Step 4. Graphics

The icon and the banner are already made, in the `store/` folder:

| Asset | File | Size |
|---|---|---|
| App icon | `store/icon-512.png` | 512 x 512 |
| Feature graphic | `store/feature-1024x500.png` | 1024 x 500 |
| Phone screenshots | `store/screenshots/` | 1080 x 2400 |

Both images are drawn by `tools/make-store-art.py` from the same numbers as the
app's own icon, so they cannot drift away from the real mark. If you ever change
the mark, run that script again rather than editing the PNGs.

The screenshots are, in order: the shelf, now playing, Surroundings, export and
restore, and the privacy screen. The last one is worth keeping; almost no music
app can show that page.

---

## Step 5. The questionnaires

Play asks a series of questions. Meedwell's answers are unusually simple.

**Data safety.** This is the one most apps get wrong. Answer:

- Does your app collect or share any of the required user data types? **No.**
- Is all of the user data collected by your app encrypted in transit? Not
  applicable, since none is collected.
- Do you provide a way for users to request that their data is deleted? Not
  applicable, since none is collected. There is an erase control in the app for
  the data that lives on their own phone.

**Content rating.** Complete the questionnaire honestly. Meedwell has no
violence, no user-generated content, no ads, no purchases, and no location. It
will come back rated for everyone.

**Ads:** No, this app contains no ads.

**Target audience:** 13 and over. Not designed for children.

**News app:** No.

**Government app:** No.

**Financial features:** None.

**Privacy policy URL.** Play requires one even when nothing is collected. Use:

```
https://github.com/Kamsiob/meedwell/blob/main/spec/PRIVACY.md
```

---

## Step 6. Play App Signing

When you upload the first bundle, Play offers **Play App Signing**. Accept it.

This is the decision already recorded in `DECISIONS.md` and the build is set up
for it: Google holds the app signing key, you hold only the upload key. It means
the Play install and any GitHub release install carry the same signature, so
somebody can move between them without uninstalling and losing their data.

**Consequence, and it matters:** every release goes to Play first. The APK you
put on GitHub is the Google-signed one you download from Play Console
afterward, never the one built here. Nothing anywhere should say the two builds
are signed differently, because they are not.

---

## Step 7. Release

Start with **Internal testing**, not production. Add your own email as a tester,
install from the link Play gives you, and use the app for a day. An internal
release goes live in minutes rather than days, so it is the cheap way to catch
anything.

When you are happy, promote that same build to **Production**. First review
usually takes a few days.

---

## Step 8. After it is live

1. Download the signed APK from Play Console under **App bundle explorer**,
   pick your version, **Downloads**, then the signed universal APK.
2. Put that APK on a GitHub release in the `meedwell` repository, so people who
   avoid Play can install the identical build.
3. Write the release notes in `CHANGELOG.md`, in plain language, about what a
   person would notice.

---

## Still open

Things that are genuinely undecided or waiting on you, kept short. The full list
with reasoning is in `spec/DECISIONS.md` under BLOCKED.

- **Two hearts on your Bandcamp account cannot be removed by the app.** One
  track and one album got starred while verifying that starring works, and
  Bandcamp's unstar endpoint is broken. Remove them on the Bandcamp website if
  you want them gone.
- **The upload keystore is not backed up yet.** Only you can do that. See step 1.
That is the whole list. Everything else is done.
