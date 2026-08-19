# Launch: what is done, and whose click each step is

The store listing is **live in the console**, pushed through the Publisher API:
title, short and full descriptions, icon, feature graphic and all eight phone
screenshots. It reads from plain files in `store/listing/en-US/`, so changing
the store later is editing a text file and running one command.

The package name is `io.github.kamsiob.meedwell`, matching the console entry.
The bundle in the project root declares it, verified with aapt2.

## Then, in order

1. **The listing.** Done. Rerun `python3 store/push-listing.py` any time to
   push changes, or `--dry-run` to validate without sending. It checks every
   character count, image dimension, bit depth and file size locally first,
   then writes everything in a single edit and commits. A failure discards the
   whole edit and prints the real error rather than retrying.

2. **App content declarations.** Owner, web console, none of these have an API:
   - Content rating: complete the IARC questionnaire. Music player, no user
     generated content, no ads.
   - Ads: this app contains no ads.
   - App access: all functionality is available without special access.
     Reviewers without a Bandcamp account still get local file playback and the
     whole Surroundings library, which is worth saying in the notes.
   - Target audience: adults. No child directed content.
   - Data safety: no data collected, no data shared. Everything stays on the
     device.
   - Government apps, financial features, health apps: all no.

3. **Privacy policy URL.** Owner. Host `spec/PRIVACY.md` at kamsiob.com, set its
   effective date to the publish date, and paste the URL into the listing.

4. **Store settings.** Owner. Category Music and Audio, contact email
   hello@kamsiob.com, website https://kamsiob.com.

5. **Production release.** Owner. Release, Production, Create new release,
   upload `Meedwell-1.0.0.aab`, accept Play App Signing, add release notes,
   Review release, then roll out. Play requires this first bundle to go through
   the console; later ones can go through the API.

## Not automatable, for the record

The Play Developer API has no method to create an app, upload the first bundle
for a new app, or complete any of the content declarations in step 2. Those are
web console work by definition, not gaps in the tooling here.
