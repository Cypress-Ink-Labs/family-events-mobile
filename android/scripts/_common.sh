#!/usr/bin/env bash
# Shared env for android dev scripts. Source this, do not exec.
# `with-android-env.sh` ends with `exec "$@"` so we cannot source it; instead
# we replicate its detection logic inline.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
export ANDROID_ROOT

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/jlink" ]; then
  if [ -x /usr/libexec/java_home ] && JAVA_HOME_CANDIDATE=$(/usr/libexec/java_home -v 17 2>/dev/null); then
    export JAVA_HOME="$JAVA_HOME_CANDIDATE"
  elif [ -x /usr/libexec/java_home ] && JAVA_HOME_CANDIDATE=$(/usr/libexec/java_home 2>/dev/null); then
    export JAVA_HOME="$JAVA_HOME_CANDIDATE"
  elif [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  elif [ -d /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
    export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  fi
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

PKG="${PKG:-com.familyevents.app}"
ACT="${ACT:-$PKG/com.familyevents.app.MainActivity}"
ADB="${ADB:-adb}"

# Optional: ADB_DEVICE=emulator-5554 to target a specific device.
adb_cmd() {
  if [ -n "${ADB_DEVICE:-}" ]; then
    "$ADB" -s "$ADB_DEVICE" "$@"
  else
    "$ADB" "$@"
  fi
}

require_device() {
  local count
  count=$(adb_cmd devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
  if [ "$count" -eq 0 ]; then
    echo "no adb device attached. start emulator or plug device." >&2
    exit 1
  fi
}
