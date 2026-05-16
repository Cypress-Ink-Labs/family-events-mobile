# Snapshot testing for FEDesignSystem primitives

## What we snapshot
- Each primitive (EventCard, FavoriteButton, future StarRating) gets 3 baselines per scenario:
  - `light` — default appearance
  - `dark` — `userInterfaceStyle: .dark`
  - `xxl-type` — `accessibilityExtraExtraLarge` content size category, light mode

## When a test fails
1. Run `pnpm run test:app` — failure output points at the mismatched PNG.
2. Inspect the diff (the failure message includes a path to the generated image plus the reference).
3. If the diff is INTENTIONAL (you changed the primitive on purpose):
   - Flip `isRecording = true` in the affected test class's `setUp()`.
   - Re-run tests; baselines regenerate.
   - Flip `isRecording = false` and commit the new PNGs.
4. If the diff is a REGRESSION, fix the primitive instead.

## Adding a new primitive
1. Create `Sources/FEDesignSystem/<Primitive>.swift`.
2. Append a `<Primitive>SnapshotTests` class to the existing `<Primitive>Tests.swift` (or create one) gated by `#if os(iOS) && canImport(UIKit)`.
3. Call `assertSnapshotVariants(of: yourView)`.
4. First test run generates baselines under `__Snapshots__/`.
5. Commit the baselines along with the new primitive.

## Why fixed canvas, not device frame
The harness uses `layout: .fixed(width: 390, height: 700)` to keep baselines stable across Xcode versions and iPhone simulators. Per-device snapshots drift between Xcode releases due to anti-aliasing kernel changes; a fixed canvas pins width independently and surfaces layout regressions without false positives.

## CI considerations
Pin both Xcode and the iOS simulator version in CI. As of writing: Xcode 16.x targeting iPhone 17 simulator. Document any bump in `apps/ios/package.json`'s `test:app` script.
