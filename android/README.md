# Family Events Android

Native Kotlin/Jetpack Compose consumer app for Family Events.

## Scope

- Consumer-only: Plan, Explore, Saved, Event Detail, Auth, Profile.
- Admin surfaces are intentionally absent and guarded by workspace tests.
- Local emulator Supabase default: `http://10.0.2.2:55321`.

## Commands

Run from `android/` — either the pnpm scripts (which wrap
`scripts/with-android-env.sh` to locate JDK 17 + the SDK) or gradle directly:

```bash
pnpm run check     # or: ./gradlew check
pnpm run test      # or: ./gradlew test
pnpm run build     # or: ./gradlew assembleDebug
pnpm run lint      # or: ./gradlew lint
```

## Environment

Debug builds default to local Supabase. Release builds fail configuration when:

- `SUPABASE_URL` is empty
- `SUPABASE_ANON_KEY` is empty

Optional:

- `MAP_STYLE_URL`
- `ANDROID_GOOGLE_WEB_CLIENT_ID`

## Architecture

Modules:

- `app`: activity, shell, root navigation, deep links
- `core`: identifiers, errors, env, endpoint policy, dates
- `data`: Supabase client, DTOs, Room cache, repository contracts
- `designsystem`: generated tokens, theme, Compose components
- `auth`, `plan`, `explore`, `saved`, `eventdetail`: consumer features
- `platform`: Android intents, app links, shortcuts, notification hooks
