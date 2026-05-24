# apps/ios

Scope: native SwiftUI consumer iOS app.

## Tooling

If using XcodeBuildMCP, use the installed XcodeBuildMCP skill before calling XcodeBuildMCP tools.

`apps/ios/project.yml` is the XcodeGen source of truth for project structure. Do not hand-edit generated Xcode project structure unless the task explicitly requires investigating generated output.

## Commands

Run from repo root:

```bash
pnpm run ios:generate
pnpm run ios:test
```

Run from `apps/ios` when needed:

```bash
pnpm run test:packages
pnpm run test:app
```

## Boundaries

- Consumer-only unless explicitly approved.
- Admin endpoints stay blocked by endpoint policy tests.
- `FECore` owns domain primitives and pure helpers.
- `FEData` owns Supabase adapters, DTOs, mappers, repositories, cache, and platform data services.
- `FEAuth` owns auth UI/session workflows and auth-specific Supabase calls.
- `FEDesignSystem` owns SwiftUI primitives and generated design tokens.
- Feature packages consume `FECore`, `FEData` contracts/fakes, and `FEDesignSystem`.
- Feature packages must not import Supabase, CoreLocation, WeatherKit, or SwiftData directly.

## Generated Tokens

Do not hand-edit:

```text
apps/ios/Packages/FEDesignSystem/Sources/FEDesignSystem/Generated/Tokens.swift
```

Change `packages/design-system/tokens/tokens.json`, then run:

```bash
pnpm --filter @family-events/design-system build
```

## Verification

For iOS-only changes:

```bash
pnpm run verify:ios
```

For shared contract or design-token changes, also run:

```bash
pnpm run verify:web
pnpm run verify:android
```
