# Spec Delta

## Purpose

Lets users keep friend teams on the device so the same club can enter many championships without retyping.

## ADDED Requirements

### Requirement: Team has a required name
The system SHALL persist a team only when its name is non-empty after trimming whitespace.

#### Scenario: Save with a name
- **WHEN** the user saves a team named "Galáticos"
- **THEN** the team is stored on the device and appears in the team list

#### Scenario: Reject empty name
- **WHEN** the user saves a team with a blank name
- **THEN** the system does not persist the team and reports that a name is required

### Requirement: Photo and description are optional
The system SHALL allow a team to be saved with no photo and no description, and SHALL store those fields when provided.

#### Scenario: Save name only
- **WHEN** the user saves a team with a name and no photo or description
- **THEN** the team is stored and later shown with a placeholder instead of a photo

#### Scenario: Save with photo and description
- **WHEN** the user saves a team with a name, a photo, and a short description
- **THEN** later views of that team show the same name, photo, and description

### Requirement: Team can be edited
The system SHALL let the user change a stored team's name, photo, and description, and SHALL apply the new values in every championship that already includes that team.

#### Scenario: Rename a team in use
- **WHEN** the user renames a team that already belongs to an open championship
- **THEN** that championship shows the new name for the same team identity

### Requirement: Delete is blocked when the team was used
The system SHALL refuse to delete a team that has been added to any championship, and SHALL allow delete only when the team was never added.

#### Scenario: Unused team can be deleted
- **WHEN** the user deletes a team that has never been added to a championship
- **THEN** the team is removed from the device registry

#### Scenario: Used team cannot be deleted
- **WHEN** the user tries to delete a team that belongs to at least one championship
- **THEN** the team remains stored and the system reports that it cannot be deleted
