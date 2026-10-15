# career-leaderboard Specification

## Purpose
TBD - created by archiving change local-championships. Update Purpose after archive.
## Requirements
### Requirement: Career lists use only closed championships
The system SHALL include a championship in career totals only after it is closed. Open championships SHALL NOT change either list.

#### Scenario: Open title is ignored
- **WHEN** a team has won a final but the championship is still open
- **THEN** that team does not gain a career title from it

### Requirement: Titles count only first place
The titles list SHALL count one title per closed championship for the unique champion. Runner-up and third place SHALL NOT count as titles.

#### Scenario: Champion gets the title
- **WHEN** a championship is closed with team A as champion and team B as runner-up
- **THEN** A has one more title and B has no title from that championship

### Requirement: Points come only from league and groups
On the points list, the system SHALL sum only 3/1/0 points from league and group matches in closed championships. Knockout and third-place matches SHALL add 0 points.

#### Scenario: Knockout win adds no career points
- **WHEN** a team wins a closed-championship semifinal 2-0 after earning 9 group points
- **THEN** that championship adds 9 career points, not 12

#### Scenario: Knockout-only championship adds zero points
- **WHEN** a team wins a closed knockout-only championship
- **THEN** that team gains a title and 0 career points from it

### Requirement: Goals include knockout and third place
On the points list, the system SHALL show goals for and goals against from every played match in closed championships, including knockout and third place. Penalty-shootout goals SHALL be excluded.

#### Scenario: Knockout goals count, penalties do not
- **WHEN** a team plays a closed final 2-2 and wins on penalties
- **THEN** career goals for increase by 2 and goals against increase by 2

### Requirement: Two separate ordered lists
The system SHALL present titles and points as two lists. Titles SHALL sort by title count descending, then name. Points SHALL sort by career points descending, then goals for descending, then fewer goals against, then name.

#### Scenario: Switch between lists
- **WHEN** the user opens career rankings
- **THEN** the UI offers a titles list and a points list that also shows goals for and goals against

