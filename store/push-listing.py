#!/usr/bin/env python3
"""
Push the Meedwell store listing to Google Play.

The listing lives in files under `store/listing/en-US/` and this reads them, so
updating the store is editing a text file and running one command rather than
filling in a web form and hoping it matches what the repository says.

**Everything is validated locally first.** The Play API rejects an oversized
description or a wrongly sized image with errors that name neither the field
nor the file, and it does it halfway through an edit, so a run can leave the
listing part written. Every check that can be done on this machine is done
before the first network call, and nothing is uploaded unless all of them pass.

**One edit, one commit.** Insert, update the text, upload the images, commit.
If anything fails the edit is deleted, which discards every change in it, so a
failed run leaves the live listing exactly as it was.

What this deliberately does not do, because the Play API has no method for it:
create the app, upload the first bundle, or fill in the content rating, ads,
app access, target audience or data safety forms. Those are web console work.

Run:  python3 store/push-listing.py [--dry-run]
"""

import os
import struct
import sys

KEY = os.path.expanduser("~/.kamsiob-secrets/play-service-account.json")
PKG = "io.github.kamsiob.meedwell"
LANG = "en-US"
HERE = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.join(HERE, "listing", LANG)
GFX = os.path.join(BASE, "graphics")

# Play's published limits, in one place so a change is a one line edit.
LIMITS = {"title": 30, "short-description": 80, "full-description": 4000}
ICON = (512, 512)
FEATURE = (1024, 500)
SHOT_MIN, SHOT_MAX = 320, 3840
MAX_BYTES = 8 * 1024 * 1024
MIN_SHOTS, MAX_SHOTS = 2, 8


def png_size(path):
    """Width, height and color type, read from the PNG header itself.

    Read here rather than shelled out to ImageMagick so this script has no
    dependency beyond the Google client: a validator that cannot run is a
    validator that gets skipped.
    """
    with open(path, "rb") as f:
        head = f.read(26)
    if head[:8] != b"\x89PNG\r\n\x1a\n" or head[12:16] != b"IHDR":
        raise ValueError(f"{os.path.basename(path)} is not a PNG")
    width, height = struct.unpack(">II", head[16:24])
    depth, color_type = head[24], head[25]
    return width, height, depth, color_type


def read_text(name):
    path = os.path.join(BASE, f"{name}.txt")
    if not os.path.exists(path):
        return None, f"missing {os.path.relpath(path, HERE)}"
    text = open(path, encoding="utf-8").read().strip()
    if not text:
        return None, f"{name}.txt is empty"
    if len(text) > LIMITS[name]:
        return None, f"{name} is {len(text)} characters, the limit is {LIMITS[name]}"
    # The project's own standing rule, checked here because the store is the one
    # place a stray em dash cannot be quietly fixed later.
    if "—" in text or "–" in text:
        return None, f"{name} contains an em or en dash"
    return text, None


def validate():
    """Everything checkable without the network. Returns (values, problems)."""
    values, problems = {}, []

    for name in LIMITS:
        text, err = read_text(name)
        if err:
            problems.append(err)
        else:
            values[name] = text
            print(f"  ok  {name}: {len(text)}/{LIMITS[name]} characters")

    def check_image(path, expect=None, label=""):
        if not os.path.exists(path):
            problems.append(f"missing {os.path.relpath(path, HERE)}")
            return None
        try:
            w, h, depth, color = png_size(path)
        except ValueError as e:
            problems.append(str(e))
            return None
        size = os.path.getsize(path)
        name = os.path.basename(path)
        if size > MAX_BYTES:
            problems.append(f"{name} is {size/1e6:.1f} MB, the limit is 8 MB")
        if expect and (w, h) != expect:
            problems.append(f"{name} is {w}x{h}, Play requires {expect[0]}x{expect[1]}")
        if depth != 8:
            problems.append(f"{name} is {depth} bits per channel, Play expects 8")
        print(f"  ok  {label or name}: {w}x{h}, {size/1024:.0f} KB")
        return (w, h)

    icon = os.path.join(GFX, "icon.png")
    if check_image(icon, ICON, "icon"):
        # The icon is the one asset Play specifies as 32 bit, meaning RGBA.
        _, _, _, color_type = png_size(icon)
        if color_type != 6:
            problems.append("icon.png has no alpha channel, Play requires a 32 bit PNG")
    check_image(os.path.join(GFX, "feature-graphic.png"), FEATURE, "feature graphic")

    shot_dir = os.path.join(GFX, "phone")
    shots = sorted(
        os.path.join(shot_dir, f) for f in os.listdir(shot_dir) if f.lower().endswith(".png")
    ) if os.path.isdir(shot_dir) else []
    if not MIN_SHOTS <= len(shots) <= MAX_SHOTS:
        problems.append(f"{len(shots)} phone screenshots, Play allows {MIN_SHOTS} to {MAX_SHOTS}")
    for path in shots:
        got = check_image(path, None, f"phone/{os.path.basename(path)}")
        if got:
            w, h = got
            lo, hi = min(w, h), max(w, h)
            if lo < SHOT_MIN or hi > SHOT_MAX:
                problems.append(
                    f"{os.path.basename(path)} is {w}x{h}, each side must be "
                    f"{SHOT_MIN} to {SHOT_MAX} px"
                )
            if hi > lo * 2:
                problems.append(
                    f"{os.path.basename(path)} is {w}x{h}, the long side may not exceed "
                    f"twice the short side"
                )
    values["shots"] = shots
    return values, problems


def push(values):
    from google.oauth2 import service_account
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload

    creds = service_account.Credentials.from_service_account_file(
        KEY, scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
    edits = svc.edits()

    try:
        eid = edits.insert(packageName=PKG, body={}).execute()["id"]
    except Exception as e:
        if "Package not found" in str(e):
            sys.exit(
                f"\nBLOCKED: Play has no package named {PKG}.\n"
                "The app entry exists in the console, but no package name is bound to\n"
                "it yet, and every Publisher API call is addressed by package name.\n"
                "See LAUNCH.md, step 1, for the clicks that bind it."
            )
        raise
    print(f"\nedit {eid} opened")

    try:
        edits.listings().update(
            packageName=PKG,
            editId=eid,
            language=LANG,
            body={
                "language": LANG,
                "title": values["title"],
                "shortDescription": values["short-description"],
                "fullDescription": values["full-description"],
            },
        ).execute()
        print("  listing text written")

        for image_type, path in (
            ("icon", os.path.join(GFX, "icon.png")),
            ("featureGraphic", os.path.join(GFX, "feature-graphic.png")),
        ):
            edits.images().deleteall(
                packageName=PKG, editId=eid, language=LANG, imageType=image_type
            ).execute()
            edits.images().upload(
                packageName=PKG, editId=eid, language=LANG, imageType=image_type,
                media_body=MediaFileUpload(path, mimetype="image/png"),
            ).execute()
            print(f"  {image_type} uploaded")

        edits.images().deleteall(
            packageName=PKG, editId=eid, language=LANG, imageType="phoneScreenshots"
        ).execute()
        for path in values["shots"]:
            edits.images().upload(
                packageName=PKG, editId=eid, language=LANG, imageType="phoneScreenshots",
                media_body=MediaFileUpload(path, mimetype="image/png"),
            ).execute()
            print(f"  screenshot {os.path.basename(path)} uploaded")

        edits.validate(packageName=PKG, editId=eid).execute()
        edits.commit(packageName=PKG, editId=eid).execute()
        print("\ncommitted. The listing is live in the console.")
    except Exception as e:
        try:
            edits.delete(packageName=PKG, editId=eid).execute()
            print("\nedit discarded, the live listing is unchanged")
        except Exception as cleanup:
            print(f"\nedit could not be discarded: {cleanup}")
        # The real error, not a retry. A commit that fails twice for the same
        # reason has told you the reason once already.
        print(f"\nFAILED: {e}")
        sys.exit(1)


if __name__ == "__main__":
    print(f"validating {os.path.relpath(BASE, HERE)}")
    values, problems = validate()
    if problems:
        print("\nnot pushing. Fix these first:")
        for p in problems:
            print(f"  - {p}")
        sys.exit(1)
    print("\nall local checks passed")
    if "--dry-run" in sys.argv:
        print("dry run, nothing sent")
    else:
        push(values)
