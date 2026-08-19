# Privacy policy: Meedwell by Kamsiob

**Effective date:** to be set on the day the app is first published, and matched exactly to the canonical hosted version at kamsiob.com. This file must mirror that hosted version word for word. The About screen links to the hosted version rather than to a copy, so the two cannot drift apart.

---

## What Meedwell collects

Nothing.

There is no analytics, no telemetry, no advertising identifier, no crash reporting that sends anything anywhere, and no account with Kamsiob or with anyone else. Meedwell has no server. There is nowhere for your data to go.

## What stays on your phone

Your listening history, which powers the forgotten shelf and the waveforms. Your lists and hearts, in the cases where they are local rather than held in your Bandcamp account. Your settings. Your resume positions on long pieces. Your downloaded music files.

All of it sits in one database file and one music folder on your phone. You can export all of it to a file you keep, and you can erase your listening history at any time from Settings.

## Your Bandcamp credentials

If you connect a Bandcamp account, the username and password Bandcamp generates for you are stored on your phone using Android's encrypted preferences. They are never written into the app's database, never included in an export or a backup file, never written into a crash report, and never logged. They are sent only to Bandcamp, only to authenticate you, over an encrypted connection.

## Network traffic

Two servers, both only when you ask.

**Bandcamp's**, to stream and sync the music you own. That traffic is governed by Bandcamp's own privacy policy, not by this one.

**GitHub's**, for two things. The Surroundings ambience library, whose list of recordings and audio files are published as public release assets, and one small public file listing Bandcamp Friday dates. Those requests carry nothing about you beyond what any web request necessarily reveals to the server serving the file. Nothing is fetched from GitHub until you ask for a recording.

Nothing else. No other server is contacted for any reason.

## Where you are

Meedwell never asks for your location and has no way to find it. The app does not request the location permission, so Android would refuse it even if the code tried.

The day line on the shelf, and the sun and moon on it, run on your phone's own clock and time zone plus two times you can set yourself in Settings. They are not sunrise and sunset calculated from where you are standing, because working those out would need a latitude, and asking for one to draw a line is not a trade worth making.

## Notifications

Downloading Surroundings recordings runs as a foreground service, which Android requires to show a notification while it works. That notification names the recording being fetched and how far along it is, and it exists so that work happening in the background is visible and stoppable rather than hidden. Playback posts the usual media notification with the track and its controls.

Both are drawn on your phone by Android. Nothing about them is sent anywhere.

Android asks you to allow notifications before either can appear. Refusing costs you the shade controls and the download progress, and nothing else: the music still plays and the downloads still finish.

## What Meedwell asks Android for

The whole list, which you can check against the manifest in the source:

- Internet access, for the two servers above.
- Network state, to tell Wi-Fi from mobile data, so that the Wi-Fi-only download setting can be honored.
- Foreground service, for playback and for downloads.
- Notifications, for the two notifications above.
- Wake lock, so playback and downloads are not cut off when the screen goes off.

There is no location permission, no microphone, no camera, no contacts, no calendar, and no permission to read the media on your phone.

You will also find one permission in the manifest that Meedwell did not write: Android's own media library adds a private one so that its playback components can talk to each other inside the app. It grants access to nothing outside Meedwell, and it is named here so that reading the manifest holds no surprises.

## Sharing and links out

Sharing hands a plain Bandcamp link to Android's own share sheet. Links open in your browser. Meedwell fetches nothing along the way and learns nothing about what you shared or where you sent it.

## Downloaded files

Downloads are written as ordinary files to your phone's public Music folder. They belong to you, any music player can read them, and they survive uninstalling Meedwell. Meedwell does not encrypt them, hide them, or tie them to itself.

## Children

Meedwell is not directed at children and collects nothing from anyone.

## Changes to this policy

If this policy ever changes, the effective date above changes with it, and the version in the app's repository is updated in the same commit as the hosted version.

## Verifying any of this

Every line of Meedwell's source code is public at https://github.com/kamsiob. The claims above are checkable rather than promises.

## Contact

hello@kamsiob.com
