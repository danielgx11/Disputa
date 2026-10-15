# Tasks

## 1. Shared module skeleton

- [x] 1.1 Create `shared` with `commonMain`, `iosMain`, and `jvm` test target, plus packages `domain`, `storage`, `features`, and `commons`, and verify the Gradle project lists those source sets
- [x] 1.2 Add Kotlin, SQLDelight, SKIE (or chosen Flow bridge), and kotlinx-datetime using versions confirmed at apply time, and verify `./gradlew :shared:jvmTest` configures

## 2. Domain rules

- [x] 2.1 Implement team name validation and identity, and verify tests reject a blank name and keep a stable UUID
- [x] 2.2 Implement championship setup rules (formats, power-of-two knockout-only, equal groups, third place only when knockout size ≥ 4), and verify tests cover eight-team knockout allowed and six-team knockout-only rejected
- [x] 2.3 Implement single round-robin generation with shuffled rounds, and verify a four-team league always yields the same six pairings
- [x] 2.4 Implement standings (3/1/0, goal difference, goals for, head-to-head, unresolved tie), and verify tests for a points tie broken by goal difference and a remaining tie when two teams never met
- [x] 2.5 Implement knockout seeding (league 1 vs N, 1 and 2 only in the final; group pots yielding A1 vs B2 and B1 vs A2), and verify the four- and eight-seed fixtures in `fixture-generation`
- [x] 2.6 Implement fill-to-next-power-of-two from best remaining teams, and verify six automatic qualifiers plus two best remaining produce an eight-team field
- [x] 2.7 Implement score application, penalty winner without adding penalty goals, prior-phase lock, and unused-bracket discard, and verify the `match-results` scenarios
- [x] 2.8 Implement close rules and career aggregation (titles vs points; knockout adds goals but not points), and verify a closed 2-2 penalty final adds 2 GF / 2 GA and 0 extra points

## 3. Storage

- [x] 3.1 Write SQLDelight schema and queries for teams, championships, memberships, groups, and matches, and verify JVM tests insert and read a championship with matches
- [x] 3.2 Store team photos as sandbox files with a relative path in the team row, and verify replacing a photo updates the path and the old file is removed
- [x] 3.3 Derive standings and career lists from stored matches (no totals table), and verify deleting a closed championship removes its title and goals from career reads

## 4. Feature ViewModels

- [x] 4.1 Add Kotlin ViewModels with one immutable `StateFlow` for team list/form, championship list/wizard/hub, score entry, and career rankings, and verify JVM tests emit the empty, error, and loaded states used by the specs
- [x] 4.2 Map domain error enums to UI state without Portuguese strings in Kotlin, and verify each user-facing failure has an enum the iOS layer can localize

## 5. iOS app

- [x] 5.1 Create the iOS 18+ SwiftUI app with tabs Campeonatos, Rankings, and Times, CoreUI tokens (blueberry, mango, lemon), and a pt-BR String Catalog, and verify the app launches on an iOS 18 simulator
- [x] 5.2 Add `@MainActor` wrappers that collect shared ViewModels and clear on `deinit`, and verify a screen update appears after a Kotlin state emission
- [x] 5.3 Build team list and form (name, photo, description, blocked delete), and verify creating a team and failing to delete a used team in the simulator
- [x] 5.4 Build championship wizard and hub (format, date, groups random or manual, table, fixtures, bracket, close), and verify creating league-then-knockout and generating fixtures
- [x] 5.5 Build score entry including knockout penalties, edit/clear, and lock messaging, and verify a 2-2 final requires a penalty winner
- [x] 5.6 Build career rankings with two segments, and verify titles ignore runner-up and the points list shows goals from knockout matches

## Workflow follow-up

- Archive `local-championships` after a full pass in the simulator on a real championship.
