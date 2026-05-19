#!/usr/bin/env bash
# Wipe app data (signed-out, fresh DB) then launch.
# Equivalent to fresh install without rebuilding the APK.
set -euo pipefail
. "$(dirname "$0")/_common.sh"

require_device
echo "[clear] pm clear $PKG..."
adb_cmd shell pm clear "$PKG" >/dev/null
adb_cmd shell am start -n "$ACT" >/dev/null
echo "[clear] cleared + launched."
