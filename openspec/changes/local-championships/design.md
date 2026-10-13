# Design

## Context

See proposal.md for why. The repo is empty besides git metadata and this OpenSpec change. v1 is one iPhone, offline, iOS 18+, SwiftUI UI, Kotlin Multiplatform shared logic. Android/Compose is out of this change. Career totals must be replayable from stored matches so a later file export can move the same history.

## Goals / Non-Goals

**Goals:**

- Share domain, persistence, and screen state in Kotlin.
- Keep SwiftUI thin: layout, navigation, photos, strings.
- Store matches as the source of truth; derive tables and career lists.
- Leave room for home-and-away, random knockout draw, and file export without rewriting identities.

**Non-Goals:**

- Android module, Compose, Ktor, accounts, iCloud, or multi-device sync.
- Implementing export/import, ida e volta, or random knockout in v1.
- AndroidX lifecycle ViewModel in shared code until Compose exists.

## Decisions

### Shared Kotlin, native SwiftUI

`shared` targets `iosArm64` / `iosSimulatorArm64` and `jvm` (tests only). No `android` target.

```
shared/src/commonMain/kotlin/disputa/
  domain/
  storage/
  features/
  commons/
iosApp/
  CoreUI/
  Features/
  Wrappers/
```

- `domain`: entities, format rules, standings, seeding, career aggregation, error enums.
- `storage`: SQLDelight, file paths for photos, repository implementations.
- `features`: one Kotlin ViewModel per screen, one immutable `StateFlow`.
- `commons`: identifiers, clocks, result types.
- iOS `CoreUI`: blueberry / mango / lemon tokens, type, empty and error states.
- iOS wrappers: `@MainActor ObservableObject` that collects the flow and calls `clear()` on `deinit`.

Alternative considered: AndroidX ViewModel in `commonMain`. Rejected until Compose; it would force an iOS `ViewModelStore` for no gain.

### Persistence

SQLDelight with a native iOS driver. UUIDs as text. Championship date as ISO-8601 date. Photo bytes live in the app sandbox; the row stores a relative path.

Suggested tables: `team`, `championship`, `championship_team`, `group_membership`, `match`. `match` holds phase, optional group, two team ids, optional regular-time goals, optional penalty winner, timestamps. No career-totals table.

Alternative considered: SwiftData only on iOS. Rejected because standings and seeding must stay in Kotlin.

### Fixtures and seeding

Round-robin: circle method, then shuffle round order with a seeded RNG stored on the championship so regeneration is reproducible.

Knockout placement for n = 2^k: assign seeds 1..n, put seed i at the standard mirrored slot so 1 plays n, 1 and 2 are in opposite halves, 1 and 4 in opposite quarters. Group pots fill seeds before that placement.

### UI

Three tabs in Portuguese: Campeonatos, Rankings, Times. Championship wizard, championship hub (tabela, jogos, chave, encerrar), score sheet, team form, rankings with two segments. Dark-only. Tokens: blueberry background and surface, mango primary actions, lemon titles and stat highlights.

Strings live in an iOS String Catalog. Domain returns enums; Swift maps them.

### Dependencies and wiring

Pin Kotlin, SQLDelight, SKIE (or equivalent Flow bridging), and kotlinx-datetime at apply time by checking current stable versions. Manual `AppGraph` in Swift, no Koin, no network stack.

### Phase 2 hooks (do not build)

- `legCount` on championship/phase (default 1).
- `knockoutSeeding` enum `byPosition` | `random` (v1 always `byPosition`).
- Stable UUIDs so a later export file can round-trip.

## Risks / Trade-offs

- [SKIE / Kotlin-Swift interop surprises] → Keep wrappers small; domain tests run on JVM.
- [Photo files orphaned after team edit] → Replace file then update the path in one repository call.
- [Unresolved ties feel strict] → Surface the exact tied teams; do not invent a sort.
- [One device is the source of truth] → Say so in empty career states; do not pretend sync exists.

## Migration Plan

Greenfield. No data to migrate. If apply is rolled back, delete the app container; OpenSpec artifacts stay.

## Open Questions

None that change specs, this approach, or the task list. Library versions are chosen at apply.
