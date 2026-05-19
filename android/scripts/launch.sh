#!/usr/bin/env bash
# Force-stop then start MainActivity. Keeps app data (DB, prefs) intact.
set -euo pipefail
. "$(dirname "$0")/_common.sh"

require_device
adb_cmd shell am force-stop "$PKG"
adb_cmd shell am start -n "$ACT" >/dev/null
echo "[launch] started $ACT"
