# Proposal

## Why

Friends run recurring EA FC championships and have nowhere to keep teams, fixtures, scores, and career stats. Disputa v1 is a local iPhone app that stores that history on device, with no network.

## What Changes

- Add a persistent team registry (name, optional photo, optional description).
- Add championships with a name, a date, and a format: league only, knockout only, groups then knockout, or league then knockout.
- Generate a complete single-round-robin calendar with shuffled round order, and a position-seeded knockout bracket (optional third-place match).
- Record match scores, including penalty winners in knockout, with edit and lock rules until the championship is closed.
- Show per-championship standings and two device-wide career lists (titles vs points and goals) from closed championships only.
- Ship a dark, Portuguese UI on iOS 18+ over a Kotlin Multiplatform shared module. **No Android, Compose, or network in this change.**

## Capabilities

### New Capabilities

- `team-registry`: Persist teams on device and reuse them across championships.
- `championship-setup`: Create a championship with name, date, format, teams, groups, and optional third-place match.
- `fixture-generation`: Build the round-robin calendar and the position-seeded knockout bracket.
- `match-results`: Record, edit, and lock scores, including knockout penalties.
- `standings`: Rank teams inside a championship with 3/1/0 and the agreed tie-breakers.
- `career-leaderboard`: Show titles and career points/goals from closed championships.

### Modified Capabilities

- None. The repository has no existing specs.

## Impact

- New OpenSpec source of truth for Disputa v1.
- Future apply will add `shared` (KMP) and `iosApp` (SwiftUI). No Android module, no HTTP client, no cloud.
- Data stays on one iPhone. The model must not block a later file export/import.
- Phase 2 (documented only): home-and-away, random knockout draw, export/import.
