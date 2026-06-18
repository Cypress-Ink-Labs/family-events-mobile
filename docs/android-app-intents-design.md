# Design spike: Android Shortcuts / App Actions parity with iOS App Intents

> **Status**: design spike (no implementation). Produced for plan
> `plans/013-android-app-intents-spike.md` at commit `744e1c6`. This document is
> the input to a future build plan; it should make the build / no-build decision
> easy. Do not treat anything below as shipped — nothing here is wired.

## TL;DR / Recommendation

iOS exposes a rich App Intents surface (Siri / Shortcuts) for both consumer and
admin actions. Android already has a *partial* equivalent that the plan's
"Current state" understated: `MainActivity.kt` publishes five **dynamic launcher
shortcuts** (Plan, Explore, Map, Calendar, Saved) that target validated
`familyevents://tab/*` deep links. What Android is missing relative to iOS is:

1. **Content-targeted shortcuts** — "Open event", "Save event", "Add to
   calendar", "Rate", "Comment" — i.e. shortcuts/voice actions that act on a
   *specific event*, not just a tab.
2. **A voice surface** (Google Assistant App Actions / built-in intents) — the
   Android analogue of iOS `AppShortcutsProvider` phrases.

**Recommendation: build Tier 1 only now; do not build Tier 2/3 speculatively.**

- **Tier 1 (do first, cheap, high certainty)**: migrate the existing raw
  `ShortcutManager` code to `ShortcutManagerCompat` + a static `shortcuts.xml`,
  and add a small number of *static* launcher shortcuts. This is a refactor +
  manifest change with no new dependency beyond `androidx.core` (already present
  transitively) and no Play Console review. It closes the "shortcuts use the
  deprecated raw API and silently no-op below API 25" gap and gives us a clean
  base. Low effort, low risk.
- **Tier 2 (defer, needs product signal)**: dynamic *content* shortcuts for
  recently-viewed / saved events (push a shortcut per event via
  `ShortcutManagerCompat.pushDynamicShortcut`). Useful, but only worth it once we
  know users want launcher long-press to deep-link into specific events.
- **Tier 3 (defer hard, review-gated)**: Google Assistant App Actions
  (`shortcuts.xml` `<capability>` + built-in intents / `actions.xml`). This
  requires a Play Console submission + Actions review, the BII catalogue does not
  cleanly cover "save/rate/comment on a custom event" (those would need custom
  intents with limited Assistant discoverability), and demand is unproven. Do not
  build until there is explicit product demand and a maintainer decision.

The full rationale, the iOS inventory, and the per-intent mapping table are below.

---

## Step 1 — iOS consumer intent inventory

Source: `ios/Packages/FEAppIntents/Sources/FEAppIntents/AppIntentsRegistry.swift`
(420 lines; verified unchanged since the plan was written —
`git diff --stat 744e1c6..HEAD -- ios/Packages/FEAppIntents` is empty).

The registry declares ~13 `AppIntent`s plus an `AppShortcutsProvider`
(`FamilyEventsShortcuts`) that surfaces three of them as Siri phrases. Each
`perform()` is currently a thin stub returning a dialog string (it does not yet
drive a real route) — i.e. iOS itself is mid-build here; the *surface* exists but
the routing is a placeholder. That is relevant: Android parity does not have to
replicate behavior that iOS hasn't finished wiring either.

### Consumer intents (candidates for Android)

| iOS intent | Parameters | `perform()` intent (target route) |
|------------|-----------|-----------------------------------|
| `OpenDestinationIntent` | `destination: FamilyEventsDestination` (plan / explore / saved / calendar / profile) | Open a top-level destination/tab. |
| `SearchEventsIntent` | `query: String` | Open event search, optionally pre-filled. |
| `OpenEventIntent` | `event: FamilyEventsEventEntity` (id + title) | Open a specific event's detail. |
| `SaveEventIntent` | `event`, `saved: Bool` | Save / unsave a specific event. |
| `AddEventToCalendarIntent` | `event` | Add a specific event to the calendar. |
| `RateEventIntent` | `event`, `score: Int` (1–5) | Rate a specific event. |
| `AddCommentIntent` | `event`, `body: String` | Add a comment to a specific event. |

`FamilyEventsEventEntity` / `FamilyEventsEventQuery` provide the parameter
resolution glue (the query currently returns no suggestions — another sign the
iOS side is a scaffold).

### Admin intents — excluded, and why

The registry also declares: `OpenAdminSectionIntent`, `AdminUpdateEventIntent`,
`AdminModerateCommentIntent`, `AdminCreateInviteIntent`, `AdminRevokeInviteIntent`,
`AdminRunSourceIntent`, `AdminRunCronIntent`.

These are **out of scope for the Android consumer app**. Android is consumer-only
by contract: admin deep links are explicitly rejected by `DeepLinkPolicy`
(`familyevents://admin/*` and `https://family-events.org/admin/*` both parse to
`null` — see `android/core/.../DeepLinkPolicyTest.kt`). Exposing admin shortcuts
would have no valid deep-link target to resolve to and would violate the
consumer-only boundary. The iOS admin intents are themselves server-gated and
guarded by `EndpointPolicyTests`; they are intentionally not mirrored on Android.

---

## Step 2 — Android's existing intent substrate

### Deep links (the substrate everything should reuse)

`android/core/src/main/java/com/familyevents/core/DeepLinkPolicy.kt` validates and
parses raw URIs into a `DeepLinkTarget` sealed type. It is the single source of
truth for what is launchable; **any shortcut or App Action must resolve to a URI
that `DeepLinkPolicy.parse` accepts** — no new routing logic.

Recognized targets:

| `DeepLinkTarget` | URI shape | Notes |
|------------------|-----------|-------|
| `Event(EventId)` | `familyevents://event/<id>` | content-targeted |
| `City(CityId)` | `familyevents://city/<id>` | content-targeted |
| `Tab(tab)` | `familyevents://tab/<name>` | top-level destination |
| `ResetPassword(token)` | `familyevents://reset-password?token=…` | auth flow |
| `Share(EventId)` | `https://family-events.org/share/<id>` | App Link (autoVerify) |

Admin URIs (`familyevents://admin/*`, `…/admin/*`) parse to `null` by design.

Manifest registration (`android/app/src/main/AndroidManifest.xml`): a `VIEW`
intent-filter handles the `familyevents` scheme for hosts `event`, `city`, `tab`,
`reset-password`; a separate `autoVerify` filter handles
`https://family-events.org/share`. The activity entry point is `MainActivity`,
which passes `intent?.data` into `FamilyEventsApp(initialUrl = …)`; routing is
consumed in `android/app/.../AppShell.kt` (the only non-test caller of
`DeepLinkPolicy` / `DeepLinkTarget`).

### Existing shortcut wiring (correction to the plan's "Current state")

The plan said there is "no `ShortcutManagerCompat` usage" and "no `shortcuts.xml`,
no `<capability>` App Actions wiring." Two of those are accurate; one is not, and
the spike must record the actual state:

- **There ARE dynamic launcher shortcuts already.**
  `android/app/src/main/java/com/familyevents/app/MainActivity.kt` calls
  `publishShortcuts()` in `onCreate`, which (guarded by
  `Build.VERSION.SDK_INT >= N_MR1`, i.e. API 25) sets
  `ShortcutManager.dynamicShortcuts` to five shortcuts — Plan, Explore, Map,
  Calendar, Saved — each an `ACTION_VIEW` intent on `familyevents://tab/<name>`
  targeting `MainActivity`. So the launcher-shortcut surface partially exists.
- **It uses the raw framework `ShortcutManager`, NOT `ShortcutManagerCompat`.**
  Consequences: below API 25 it silently does nothing (the early return); it
  cannot benefit from the AndroidX back-compat for pinned shortcuts; and it is not
  declared statically, so the shortcuts only appear after the first launch.
- **No `res/xml/shortcuts.xml`** (no static shortcuts).
- **No `<capability>` / App Actions / `actions.xml`** — no Google Assistant /
  voice surface at all. Confirmed by grep:
  `grep -rn "shortcuts|ShortcutManager|<capability|app-actions" android` returns
  only `MainActivity.kt`, this design doc's neighbors, and a README mention.

### minSdk

`android/gradle/libs.versions.toml` → `minSdk = "24"`. This matters for the
mechanism choice (see Step 4): `ShortcutManagerCompat` *API* is callable on all
versions, but **dynamic** shortcuts require runtime API 25 and **pinned**
shortcuts require API 26; on API 24 the compat calls are graceful no-ops. Static
manifest shortcuts (`shortcuts.xml`) are honored from API 25+. So API 24 devices
simply get no launcher shortcuts — acceptable, and better than today's silent
raw-API behavior.

---

## Step 3 — iOS intent → Android mechanism mapping

Every row resolves to an existing, `DeepLinkPolicy`-validated URI. "min API" is
the runtime version at which the shortcut/action actually appears (the compat
APIs are call-safe below it; they just no-op).

| iOS intent | Android mechanism | Target deep link | Min API | Notes |
|------------|-------------------|------------------|---------|-------|
| `OpenDestinationIntent` (plan/explore/saved/calendar) | **Static** launcher shortcut in `shortcuts.xml` (+ existing dynamic until migrated) | `familyevents://tab/<name>` | 25 (24 = no-op) | Already exists dynamically; promote to static so it shows pre-launch. `map` exists on Android with no iOS counterpart. |
| `SearchEventsIntent` | Static launcher shortcut → Explore tab | `familyevents://tab/explore` | 25 | No query param in the current deep-link grammar; a `query` would need a new validated route (`familyevents://search?q=`) → open question. Until then it lands on Explore without pre-filling. |
| `OpenEventIntent` | **Dynamic content** shortcut (`pushDynamicShortcut`, e.g. recently viewed / saved events) | `familyevents://event/<id>` | 25 | Tier 2. URI already valid; needs a "which events to surface" policy. |
| `SaveEventIntent` | No clean shortcut analogue | (none today) | — | iOS has a *parameterized boolean* action; Android shortcuts can't carry a toggle without a custom action route. Would need `familyevents://event/<id>?action=save` (new validated route) or a custom App Action. Defer. |
| `AddEventToCalendarIntent` | No clean analogue | (none today) | — | Same shape as Save: an *action on an event*, not navigation. Needs a new action route or App Action. Defer. |
| `RateEventIntent` | No clean analogue | (none today) | — | Carries a `score`; only expressible via a custom App Action with parameters, not a launcher shortcut. Defer. |
| `AddCommentIntent` | No clean analogue | (none today) | — | Carries free-text `body`; custom App Action only. Defer. |
| Voice phrases (iOS `FamilyEventsShortcuts`) | **App Actions** `<capability>` in `shortcuts.xml` + built-in intents (Assistant) | maps to the URIs above | 21+ (Assistant) | Review-gated (Play Console + Actions review). BII catalogue does not cover save/rate/comment-on-custom-event cleanly. Tier 3. |

Key asymmetry: iOS App Intents are **action-shaped** (parameterized verbs that run
in-app, e.g. "save this event", "rate 4"), whereas Android launcher shortcuts are
**navigation-shaped** (open a URI). Navigation intents (open tab, open event,
search) map cleanly to deep links today. The mutating actions (save / add to
calendar / rate / comment) have **no launcher-shortcut analogue** and would
require either (a) new action-bearing validated deep-link routes, or (b) custom
Google Assistant App Actions — both bigger, both deferred.

---

## Step 4 — Open questions, effort, and recommendation

### Effort by tier

| Tier | Work | New deps | Review gate | Effort |
|------|------|----------|-------------|--------|
| 1 | Migrate `MainActivity` raw `ShortcutManager` → `ShortcutManagerCompat`; add `res/xml/shortcuts.xml` static shortcuts for the tab destinations; reuse existing tab deep links | `androidx.core` (already on classpath via Compose/AppCompat) — confirm, likely none net-new | none | **S** (refactor + 1 XML file + test) |
| 2 | Dynamic content shortcuts: push per-event shortcuts (recently viewed / saved) via `pushDynamicShortcut`, capped at the launcher limit (~4–5), targeting `familyevents://event/<id>` | none net-new | none | **M** (needs an event-selection policy + lifecycle for pushing/expiring shortcuts) |
| 3 | Google Assistant App Actions: `<capability>` + built-in intents / custom intents; possibly new action-bearing deep-link routes for save/rate/comment | App Actions test tool; possibly `app-actions`/`actions.xml` | **Play Console + Actions review** | **L** (review cycle, BII gaps, ongoing maintenance) |

### Open questions (a human must answer before a build plan)

1. **Does Tier 1 even need new shortcuts, or just a hardening migration?** The
   five tab shortcuts already work on API 25+. Is the goal parity-of-mechanism
   (use `ShortcutManagerCompat` + static XML) or new user-facing entry points? If
   the former, this is a tiny refactor; if the latter, decide *which* new
   shortcuts add value.
2. **Search with a query**: do we add a `familyevents://search?q=` validated route
   so `SearchEventsIntent` parity can pre-fill, or accept "open Explore" as good
   enough? Adding the route touches `DeepLinkPolicy` + `AppShell` + tests.
3. **Mutating actions (save / calendar / rate / comment)**: are these wanted on
   Android at all? If yes, do we model them as action-bearing deep links
   (`familyevents://event/<id>?action=…`, requiring `DeepLinkPolicy` changes and a
   confirmation UX) or only via Assistant App Actions? This is the biggest design
   fork.
4. **Is voice (Assistant) in scope at all?** Tier 3 is the only part that needs a
   Play Console review and has unproven demand. Recommend explicitly deferring
   until there is product signal.
5. **Dynamic content-shortcut policy (Tier 2)**: which events get a launcher
   shortcut (recently viewed? saved? top-rated?), how many, and when do they
   expire? Needs UX + product input.
6. **`map` tab has no iOS counterpart** — keep it as Android-only, or drop it for
   strict parity? (Recommend keep; it's a valid Android destination.)

### Final recommendation

Build **Tier 1** as a small, low-risk hardening change (compat migration + static
`shortcuts.xml`), reusing the existing tab deep links and adding a unit/instrumented
test that each shortcut URI is accepted by `DeepLinkPolicy.parse`. **Defer Tier 2**
until there is a clear product decision on content shortcuts, and **defer Tier 3**
(Assistant App Actions) until demand is proven, given its review gate and the BII
catalogue gaps for the mutating actions. The action-shaped iOS intents (save /
calendar / rate / comment) should **not** be mirrored as launcher shortcuts; if
wanted later they need either new validated action routes or custom App Actions,
which is a separate, larger plan.

---

## Appendix — commands run for this spike

```text
git diff --stat 744e1c6..HEAD -- ios/Packages/FEAppIntents        # empty (no drift)
grep -rn "shortcuts|ShortcutManager|<capability|app-actions" android  # only MainActivity.kt + README
grep -rn "familyevents://|DeepLink|app-links|android:scheme" android  # deep-link substrate
grep -rn "minSdk" android/gradle/libs.versions.toml               # minSdk = 24
```
