# standings Specification

## Purpose
TBD - created by archiving change local-championships. Update Purpose after archive.
## Requirements
### Requirement: Points are three, one, or zero
For each played league or group match, the system SHALL award 3 points to the side with more goals, 1 point to each side when goals are equal, and 0 points to the side with fewer goals. Knockout and third-place matches SHALL NOT award table points.

#### Scenario: Win, draw, loss
- **WHEN** a team has results 2-0, 1-1, and 0-1 in the league
- **THEN** that team has 4 points, 3 goals for, and 2 goals against

### Requirement: Table order uses a fixed tie-break chain
The system SHALL order teams by points, then goal difference, then goals for, then head-to-head among the still-tied teams that have already played each other. Head-to-head SHALL apply the same points, goal difference, and goals-for order on only those matches.

#### Scenario: Goal difference breaks a points tie
- **WHEN** team A has 6 points and +4 goal difference and team B has 6 points and +1
- **THEN** A is ranked above B

#### Scenario: Head-to-head after equal totals
- **WHEN** A and B have the same points, goal difference, and goals for, A beat B, and they played each other
- **THEN** A is ranked above B

### Requirement: Head-to-head is skipped when they have not met
The system SHALL NOT use head-to-head between teams that have not played each other. Remaining ties SHALL be treated as unresolved for ranking decisions that need a single order.

#### Scenario: Two teams never met
- **WHEN** A and B are still tied after points, goal difference, and goals for, and they have no match against each other
- **THEN** the system reports an unresolved tie instead of picking a rank between them

### Requirement: League-only champion is first in the table
For a league-only championship, the system SHALL treat the unique first-place team as champion when the championship is closed.

#### Scenario: Unique leader
- **WHEN** one team is alone in first after every tie-breaker and the user closes the championship
- **THEN** that team is the champion

