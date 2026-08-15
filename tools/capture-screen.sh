#!/usr/bin/env bash
#
# Captures a screenshot from the connected device, and refuses unless Meedwell
# is genuinely in the foreground.
#
# The check is mechanical rather than a matter of timing, and that is the whole
# point of this script existing. A mistimed capture puts whatever else was on
# the owner's phone into a public repository, and no amount of care at the call
# site prevents that reliably. See MASTER_SPEC.md section 10.
#
# Usage:
#   tools/capture-screen.sh <output-path.png>
#
set -euo pipefail

PACKAGE="com.kamsiob.meedwell"
OUT="${1:?usage: capture-screen.sh <output-path.png>}"

command -v adb >/dev/null || { echo "adb is not on PATH"; exit 1; }

DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}' | wc -l)
if [ "$DEVICES" -eq 0 ]; then
  echo "No device is connected and authorised."
  exit 1
fi
if [ "$DEVICES" -gt 1 ]; then
  echo "More than one device is connected. Disconnect the others so the capture cannot go to the wrong one."
  exit 1
fi

# The gate. Ask the window manager what is actually focused right now, and stop
# if it is anything other than Meedwell.
FOCUS=$(adb shell dumpsys activity activities 2>/dev/null | grep -m1 'topResumedActivity' || true)
if [ -z "$FOCUS" ]; then
  FOCUS=$(adb shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' || true)
fi

if ! printf '%s' "$FOCUS" | grep -q "$PACKAGE"; then
  echo "Refusing to capture: $PACKAGE is not in the foreground."
  echo "Focused instead: ${FOCUS:-nothing reported}"
  exit 1
fi

mkdir -p "$(dirname "$OUT")"
adb exec-out screencap -p > "$OUT"

if [ ! -s "$OUT" ]; then
  echo "The capture came back empty."
  rm -f "$OUT"
  exit 1
fi

echo "Captured $PACKAGE to $OUT"
