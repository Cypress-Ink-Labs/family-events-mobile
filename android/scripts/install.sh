#!/usr/bin/env bash
# Uninstall any prior install + build + install :app debug APK.
# Use when signing key changed or you want a clean install. Add --launch to
# auto-start the app after install.
set -euo pipefail
. "$(dirname "$0")/_common.sh"

require_device

LAUNCH=0
for arg in "$@"; do
  case "$arg" in
    --launch|-l) LAUNCH=1 ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

echo "[install] uninstalling $PKG (ignored if absent)..."
adb_cmd uninstall "$PKG" >/dev/null 2>&1 || true

echo "[install] :app:installDebug..."
(cd "$ANDROID_ROOT" && ./gradlew :app:installDebug)

if [ "$LAUNCH" -eq 1 ]; then
  exec "$(dirname "$0")/launch.sh"
fi
