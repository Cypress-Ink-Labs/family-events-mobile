#!/usr/bin/env bash
# Tail logcat scoped to the running app pid.
# Args:
#   --crash       Crash-only (FATAL/AndroidRuntime/E)
#   --grep PAT    Filter via case-insensitive ripgrep/grep pattern
#   --wait        Wait for app process if not running yet
# Default: full pid-scoped logcat, follow.
set -euo pipefail
. "$(dirname "$0")/_common.sh"

require_device

MODE=full
PATTERN=""
WAIT=0
while [ $# -gt 0 ]; do
  case "$1" in
    --crash|-c) MODE=crash ;;
    --grep|-g) shift; PATTERN="${1:-}" ;;
    --wait|-w) WAIT=1 ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
  shift
done

pid=""
get_pid() { adb_cmd shell pidof -s "$PKG" | tr -d '\r'; }

pid=$(get_pid || true)
if [ -z "$pid" ] && [ "$WAIT" -eq 1 ]; then
  echo "[log] waiting for $PKG to start..."
  while [ -z "$pid" ]; do
    sleep 0.5
    pid=$(get_pid || true)
  done
fi

if [ -n "$pid" ]; then
  echo "[log] following pid=$pid"
  if [ "$MODE" = "crash" ]; then
    adb_cmd logcat --pid="$pid" *:E
  elif [ -n "$PATTERN" ]; then
    adb_cmd logcat --pid="$pid" | grep -iE --line-buffered "$PATTERN"
  else
    adb_cmd logcat --pid="$pid"
  fi
else
  echo "[log] $PKG not running. Falling back to tag/keyword filter." >&2
  if [ "$MODE" = "crash" ]; then
    adb_cmd logcat *:E | grep -iE --line-buffered "androidruntime|fatal|$PKG"
  else
    adb_cmd logcat | grep -iE --line-buffered "${PATTERN:-familyevents|fatal|androidruntime}"
  fi
fi
