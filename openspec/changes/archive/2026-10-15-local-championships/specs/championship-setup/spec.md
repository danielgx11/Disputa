# Spec Delta

## Purpose

Lets users create a dated championship, pick a format, add stored teams, and configure groups and an optional third-place match.

## ADDED Requirements

### Requirement: Championship has name, date, and format
The system SHALL persist a championship only with a non-empty name, a calendar date, and exactly one format: league-only, knockout-only, groups-then-knockout, or league-then-knockout.

#### Scenario: Create a league-then-knockout
- **WHEN** the user saves a championship named "Copa da Galera" dated 2026-10-09 with format league-then-knockout
- **THEN** the championship appears in the list with that name, date, and format

#### Scenario: Reject missing name
- **WHEN** the user saves a championship with a blank name
- **THEN** the system does not persist it and reports that a name is required

### Requirement: Teams come from the registry
The system SHALL add only teams that already exist in the device registry, and SHALL allow the same team in several championships.

#### Scenario: Reuse a team
- **WHEN** the user adds team "Galáticos" to two championships
- **THEN** both championships list that team under the same identity

### Requirement: Knockout-only size is a power of two
The system SHALL persist a knockout-only championship only when the number of added teams is at least two and is a power of two.

#### Scenario: Eight teams allowed
- **WHEN** the user creates a knockout-only championship with eight teams
- **THEN** the championship is persisted

#### Scenario: Six teams rejected
- **WHEN** the user tries to create a knockout-only championship with six teams
- **THEN** the system does not persist it and reports that the team count must be a power of two

### Requirement: Groups are equal size
For groups-then-knockout, the system SHALL require at least two groups of the same size, each with at least two teams, and SHALL let the user either draw groups at random or assign teams by hand.

#### Scenario: Manual two groups of four
- **WHEN** the user assigns eight teams into Group A and Group B, four each
- **THEN** the championship stores those two groups

#### Scenario: Uneven groups rejected
- **WHEN** the user tries to save groups of three and five
- **THEN** the system refuses to save and reports that groups must be the same size

#### Scenario: Random draw
- **WHEN** the user asks for a random draw of eight teams into two groups of four
- **THEN** every team is in exactly one group and both groups have four teams

### Requirement: Qualifiers are configured at setup
The system SHALL store how many teams advance: a user-chosen N for league-then-knockout, and a user-chosen number per group for groups-then-knockout. League-only has no knockout. Knockout-only advances every team.

#### Scenario: League chooses four qualifiers
- **WHEN** the user creates league-then-knockout with eight teams and N = 4
- **THEN** the championship is stored with four knockout berths

### Requirement: Third-place match is optional with a semifinal
The system SHALL offer a third-place match only when the knockout size is at least four, and SHALL persist the user's on or off choice. League-only SHALL NOT have a third-place match.

#### Scenario: Enable third place for four qualifiers
- **WHEN** the user enables third place on a championship with a four-team knockout
- **THEN** the championship is stored with a third-place match planned

#### Scenario: Hidden for two-team final only
- **WHEN** the knockout size is two
- **THEN** the system does not offer a third-place match

### Requirement: Championship can be deleted before or after close
The system SHALL let the user delete a championship and SHALL remove its matches from later career totals.

#### Scenario: Delete a closed championship
- **WHEN** the user deletes a closed championship that awarded a title
- **THEN** that championship disappears and career lists no longer include its title, points, or goals
