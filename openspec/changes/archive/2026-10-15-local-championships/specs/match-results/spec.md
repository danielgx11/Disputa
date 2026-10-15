# Spec Delta

## Purpose

Records match scores on the device, including knockout penalty winners, with edit and lock rules until the championship is closed.

## ADDED Requirements

### Requirement: User can record a regular-time score
The system SHALL store non-negative integer goals for each side of a generated match that does not yet have a score, while the championship is not closed.

#### Scenario: League result
- **WHEN** the user enters 3-1 for a league match
- **THEN** that match is stored as 3-1 and standings use those goals

### Requirement: Knockout draw requires a penalty winner
For a knockout or third-place match, if regular-time goals are equal the system SHALL require a penalty-shootout winner before the result is stored. Penalty goals SHALL NOT be stored as match goals.

#### Scenario: Final decided on penalties
- **WHEN** the user enters 2-2 and marks team A as the penalty winner
- **THEN** the match is stored as 2-2, team A advances, and neither team's goals-for increases by penalty goals

#### Scenario: Knockout draw without a winner is rejected
- **WHEN** the user enters 1-1 in a semifinal and does not pick a penalty winner
- **THEN** the system does not store the result

### Requirement: Scores can be edited or cleared before close
While the championship is not closed, the system SHALL let the user change or clear a stored score unless a later-phase lock applies.

#### Scenario: Correct a league score
- **WHEN** the user changes a league match from 2-0 to 2-1 before close and before any knockout score exists
- **THEN** the stored score is 2-1 and standings recompute

#### Scenario: Closed championship rejects edits
- **WHEN** the championship is closed and the user tries to change a score
- **THEN** the stored score is unchanged

### Requirement: Prior phase locks after a knockout score
The system SHALL refuse to change or clear a league or group score once any knockout or third-place match has a stored score.

#### Scenario: League edit blocked after a semifinal is played
- **WHEN** a semifinal already has a score and the user tries to edit a group match
- **THEN** the group score stays the same and the system reports that the prior phase is locked

### Requirement: Empty knockout is discarded when the prior phase changes
If a knockout bracket exists and no knockout or third-place match has a score, changing a prior-phase score SHALL delete that bracket. The user MUST generate the knockout again.

#### Scenario: League correction wipes an unused bracket
- **WHEN** the bracket has been generated with no knockout scores and the user edits a league score
- **THEN** the bracket is removed and standings reflect the new league score

### Requirement: Close requires a champion
The system SHALL close a championship only when a champion is defined. If third place is enabled, the third-place match MUST also have a stored result. Rankings SHALL ignore championships that are not closed.

#### Scenario: Close a finished knockout
- **WHEN** the final has a winner, the optional third-place match is complete, and the user closes the championship
- **THEN** the championship is marked closed and career lists may include it

#### Scenario: Block close without a champion
- **WHEN** the user tries to close a league-only championship whose first place is still tied after every tie-breaker
- **THEN** the championship stays open and the system asks the user to correct a score
