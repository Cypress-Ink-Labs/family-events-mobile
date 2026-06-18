# Plan 013: Design spike — Android Shortcuts / App Actions parity with iOS App Intents

> **Executor instructions**: This is a **design spike**, not an implementation.
> The deliverable is a written design document — do NOT add app features or new
> dependencies. Investigate, then write `docs/android-app-intents-design.md`. If
> "STOP conditions" occur, stop and report. When done, update the status row in
> `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- ios/Packages/FEAppIntents`
> If the iOS intents changed since this plan was written, re-read them.

## Status

- **Priority**: P3
- **Effort**: M (investigation + writing; no production code)
- **Risk**: LOW (produces a doc only)
- **Depends on**: none
- **Category**: direction (feature parity / spike)
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

iOS ships a full App Intents surface (`ios/Packages/FEAppIntents/`) — Siri /
Shortcuts entry points for searching events, opening an event, saving, rating,
adding comments, and adding to calendar. Android has **no equivalent**: no App
Actions, no `ShortcutManager` dynamic shortcuts, no intent-backed voice surface.
This is a real platform asymmetry, but unlike notification preferences it is
*not* obviously worth building — Android's App Intents/App Actions story differs
from iOS, demand is unproven, and the effort is large. So the right next step is a
spike that defines the API surface, maps each iOS intent to its best Android
mechanism, and surfaces open questions — letting the maintainer decide before any
build. Do not build it speculatively.

## Current state

`ios/Packages/FEAppIntents/Sources/FEAppIntents/AppIntentsRegistry.swift` (420
lines) registers ~13 intents. The **consumer** ones (the only candidates for
Android — admin intents are server-gated and out of scope for the consumer app):
search events, open event, save event, add event to calendar, rate event, add
comment. Each is a thin `AppIntent` whose `perform()` routes into the app.

Android has deep links already (`android/platform/.../` handles app links /
intents; `DeepLinkPolicy` validates them) — these are the natural substrate for
Android shortcuts/actions to target. There is currently no `shortcuts.xml`, no
`<capability>` App Actions wiring, and no `ShortcutManagerCompat` usage (confirm
with grep during the spike).

## Commands you will need

| Purpose | Command | Expected |
|---------|---------|----------|
| Read iOS intents | (Read `ios/Packages/FEAppIntents/Sources/FEAppIntents/AppIntentsRegistry.swift`) | enumerate consumer intents |
| Find Android shortcut wiring | `grep -rn "shortcuts\|ShortcutManager\|<capability\|app-actions" android` | likely none (confirms the gap) |
| Find deep-link entry points | `grep -rn "familyevents://\|DeepLink\|app-links\|<data android:scheme" android` | the existing intent substrate to reuse |

## Scope

**In scope** (create ONE file):
- `docs/android-app-intents-design.md` — the design document.

**Out of scope** (do NOT do):
- Any production code, manifest changes, new modules, or dependencies.
- Building any shortcut or App Action.
- iOS changes.

## Git workflow

- Branch: `advisor/013-android-intents-spike`
- Commit style: `docs(android): design spike for Shortcuts/App Actions parity`
- Do NOT push unless instructed.

## Steps

### Step 1: Inventory the iOS consumer intents

Read `AppIntentsRegistry.swift` and list each **consumer** intent: its name,
parameters, and what `perform()` does (which screen/route it drives). Exclude
admin intents (note that they exist and are excluded, and why).

### Step 2: Inventory Android's existing intent substrate

Use the greps above to document: the deep-link schemes/hosts the app already
handles, where they are validated (`DeepLinkPolicy`), and confirm there is no
existing shortcut/App-Actions wiring.

### Step 3: Map each iOS intent → Android mechanism

For each consumer intent, choose and justify the Android counterpart:
- **Static/dynamic shortcuts** (`ShortcutManagerCompat`, `shortcuts.xml`) for
  launcher long-press actions (e.g. "Search events", "Saved").
- **App Actions / built-in intents** (`shortcuts.xml` `<capability>`, Google
  Assistant) for voice — note the BII availability and that this requires a
  Play Console + actions.xml review path.
- **Deep-link targeting**: every shortcut should resolve to an existing validated
  deep link, reusing `DeepLinkPolicy` — no new routing logic.
Produce a table: iOS intent | Android mechanism | target deep link | min API | notes.

### Step 4: Open questions, effort, and recommendation

Document: which intents are worth doing first (launcher shortcuts are cheapest and
highest-certainty; Assistant App Actions are heavier and review-gated), the
minSdk implications (repo `minSdk = 24`; `ShortcutManagerCompat` covers back to
API 25 dynamic / 26 pinned — confirm), required dependencies if any, and a coarse
effort estimate per tier. End with a clear recommendation and the open questions a
human must answer before a build plan is written.

### Step 5: Write the document

Write `docs/android-app-intents-design.md` containing Steps 1-4. Keep it concise
and decision-oriented.

**Verify**: `ls docs/android-app-intents-design.md` → listed; the file contains
the iOS-intent inventory, the mapping table, and a recommendation section.

## Test plan

- None — this is a document. Verification is that the file exists and contains the
  four required sections (iOS inventory, Android substrate, mapping table, open
  questions + recommendation).

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `docs/android-app-intents-design.md` exists
- [ ] It enumerates the iOS consumer intents (admin excluded, with reason)
- [ ] It contains a mapping table (iOS intent → Android mechanism → deep link → min API)
- [ ] It ends with a recommendation + open questions
- [ ] No production code, manifest, or dependency files changed (`git status` shows only the new doc and `plans/README.md`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- You are tempted to start implementing shortcuts — this plan is design-only.
- The iOS registry has changed substantially from the Current state description.

## Maintenance notes

- The output doc is the input to a future implementation plan; it should make the
  build/no-build decision easy for the maintainer.
- Re-run the spike if iOS adds/removes consumer intents.
