#!/usr/bin/env bash
# One-shot dev loop: uninstall + build + install + launch + tail logs.
# Hit Ctrl+C to stop log tailing; app keeps running on device.
set -euo pipefail
HERE="$(dirname "$0")"
"$HERE/install.sh" --launch
exec "$HERE/log.sh" --wait
