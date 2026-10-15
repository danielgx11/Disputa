# fixture-generation Specification

## Purpose
TBD - created by archiving change local-championships. Update Purpose after archive.
## Requirements
### Requirement: League and groups use a complete single round-robin
For league or group play, the system SHALL generate every unique pairing exactly once (turno único). Home and away SHALL NOT be distinguished. Round order SHALL be shuffled. The set of pairings SHALL remain complete.

#### Scenario: Four-team league
- **WHEN** fixtures are generated for a four-team league
- **THEN** the calendar contains exactly six matches and every pair of teams appears once

#### Scenario: Shuffle does not drop games
- **WHEN** the same four teams are drawn twice
- **THEN** both calendars contain the same six pairings, possibly in a different round order

### Requirement: Knockout size is a power of two with no byes
The system SHALL generate a knockout bracket only when the number of qualified teams is at least two and is a power of two. The bracket SHALL contain no bye.

#### Scenario: Four qualifiers
- **WHEN** four teams qualify and the user generates the bracket
- **THEN** the bracket has two semifinals and a final, with no empty slot

#### Scenario: Block a five-team field
- **WHEN** five teams would enter the knockout
- **THEN** the system does not generate the bracket and reports that the field must be a power of two

### Requirement: User can fill up to the next power of two
When automatic qualifiers are not a power of two, the system SHALL generate the bracket only after the user adds best remaining teams until the next power of two, ordered by the same standings criteria as the prior phase.

#### Scenario: Six automatic qualifiers become eight
- **WHEN** six teams qualify automatically and the user confirms the next two best remaining teams
- **THEN** the system generates an eight-team bracket that includes those eight teams

### Requirement: League knockout is seeded by table position
For league-then-knockout, the system SHALL seed qualifiers 1..N by league table order and place them so seed 1 plays seed N in the first round and seed 1 can meet seed 2 only in the final.

#### Scenario: Four league qualifiers
- **WHEN** the league order is A, B, C, D and the bracket is generated
- **THEN** the semifinals are A vs D and B vs C

#### Scenario: Eight league qualifiers
- **WHEN** seeds 1 through 8 qualify
- **THEN** the round of 16 pairings are 1 vs 8, 4 vs 5, 2 vs 7, and 3 vs 6, with 1 and 2 in opposite halves

### Requirement: Group knockout uses placement pots
For groups-then-knockout, the system SHALL order qualifiers in pots: all group winners, then all runners-up, then any extra best remaining teams. Inside each pot, order is points, then goal difference, then goals for. Seeds 1..N follow that order, then the same placement as league knockout.

#### Scenario: Two groups, two advance
- **WHEN** Group A standings are A1 then A2, Group B standings are B1 then B2, and the bracket is generated
- **THEN** the semifinals are A1 vs B2 and B1 vs A2

### Requirement: Unresolved table tie blocks the bracket
The system SHALL NOT generate a knockout bracket when two or more teams still share the same rank after every standings tie-breaker needed to pick seeds or the last qualifier.

#### Scenario: Tie for the last berth
- **WHEN** two teams are tied on points, goal difference, goals for, and head-to-head for the last knockout place
- **THEN** the system refuses to generate the bracket and asks the user to correct a score

### Requirement: Third-place match sits beside the final
When third place is enabled and the bracket has a semifinal, generating the knockout SHALL also create one third-place match between the two semifinal losers. That match SHALL NOT exist in a two-team knockout.

#### Scenario: Four-team bracket with third place
- **WHEN** a four-team knockout with third place enabled is generated
- **THEN** the fixtures include two semifinals, one final, and one third-place match

