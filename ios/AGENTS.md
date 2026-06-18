# ios

Scope: native SwiftUI consumer iOS app.

## Tooling

If using XcodeBuildMCP, use the installed XcodeBuildMCP skill before calling XcodeBuildMCP tools.

`ios/project.yml` is the XcodeGen source of truth for project structure. Do not hand-edit generated Xcode project structure unless the task explicitly requires investigating generated output.

## Commands

Run from `ios/`:

```bash
pnpm run generate         # xcodegen generate
pnpm run test:packages
pnpm run test:app
pnpm run test             # test:packages + test:app
```

This standalone repo has no root pnpm workspace — run the scripts from `ios/`.

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
ios/Packages/FEDesignSystem/Sources/FEDesignSystem/Generated/Tokens.swift
```

Tokens are generated from the external `@cypress-ink-labs/design-system` package
and synced by the `sync-tokens` GitHub Actions workflow. Token changes are made
upstream in the design-system package, not here.

## Verification

Verify iOS changes from `ios/`:

```bash
pnpm run test
```

There is no cross-platform aggregate verify task in this standalone repo; run each
platform's tests directly.
