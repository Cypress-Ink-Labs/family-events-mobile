# android dev scripts

Quick wrappers around `adb` + `./gradlew` for the FamilyEvents Android app.
All scripts auto-bootstrap `JAVA_HOME` / `ANDROID_HOME` and put `adb` on PATH
via `with-android-env.sh`.

## Scripts

| Script | Purpose |
|---|---|
| `install.sh [--launch]` | Uninstall + `./gradlew :app:installDebug`. Pass `--launch` to start after. |
| `launch.sh` | Force-stop + start MainActivity. Keeps app data. |
| `clear.sh` | `pm clear` + launch. Fresh DB / signed-out, no rebuild. |
| `log.sh [--crash\|--grep PAT\|--wait]` | Pid-scoped logcat. `--crash` filters errors, `--wait` waits for process. |
| `dev.sh` | Uninstall → install → launch → tail logs. |

## Env vars

- `PKG` — package id (default `com.familyevents.app`).
- `ACT` — activity component (default `$PKG/com.familyevents.app.MainActivity`).
- `ADB_DEVICE` — target a specific device (e.g. `emulator-5554`) when multiple
  are attached. Forwarded as `adb -s $ADB_DEVICE …`.

## Examples

```bash
# fresh build, install, launch, tail logs
./scripts/dev.sh

# rebuild & install only
./scripts/install.sh

# install + launch
./scripts/install.sh --launch

# wipe data, relaunch, watch for crashes
./scripts/clear.sh
./scripts/log.sh --crash

# target physical device while emulator is also up
ADB_DEVICE=R5CT... ./scripts/launch.sh

# tail with custom filter
./scripts/log.sh --grep "Supabase|Room|EventDetail"
```
