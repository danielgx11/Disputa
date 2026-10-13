package disputa.domain.model

import disputa.commons.ChampionshipId
import disputa.commons.GroupId
import disputa.commons.MatchId
import disputa.commons.TeamId
import kotlinx.datetime.LocalDate

data class Team(
    val id: TeamId,
    val name: String,
    val about: String?,
    val photoRelativePath: String?,
)

enum class ChampionshipFormat {
    LeagueOnly,
    KnockoutOnly,
    GroupsThenKnockout,
    LeagueThenKnockout,
}

enum class KnockoutSeeding { ByPosition, Random }

enum class ChampionshipStatus { Open, Closed }

data class Championship(
    val id: ChampionshipId,
    val name: String,
    val date: LocalDate,
    val format: ChampionshipFormat,
    val status: ChampionshipStatus,
    val qualifierCount: Int?,
    val thirdPlaceEnabled: Boolean,
    val fixtureSeed: Long,
    val legCount: Int = 1,
    val knockoutSeeding: KnockoutSeeding = KnockoutSeeding.ByPosition,
) {
    val dateIso: String get() = date.toString()
}

data class Group(
    val id: GroupId,
    val name: String,
    val teamIds: List<TeamId>,
)

enum class MatchPhase { League, Group, Knockout, ThirdPlace }

data class Match(
    val id: MatchId,
    val championshipId: ChampionshipId,
    val phase: MatchPhase,
    val groupId: GroupId?,
    val roundIndex: Int,
    val slot: Int,
    val teamA: TeamId?,
    val teamB: TeamId?,
    val goalsA: Int?,
    val goalsB: Int?,
    val penaltyWinner: TeamId?,
) {
    val isPlayed: Boolean get() = goalsA != null && goalsB != null
}

data class ChampionshipSnapshot(
    val championship: Championship,
    val teamIds: List<TeamId>,
    val groups: List<Group>,
    val matches: List<Match>,
)

data class StandingRow(
    val teamId: TeamId,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int,
    val rank: Int,
    val unresolved: Boolean,
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

data class StandingsTable(
    val rows: List<StandingRow>,
    val unresolvedTeamIds: List<TeamId>,
)

data class TitleRow(
    val teamId: TeamId,
    val titles: Int,
)

data class PointsRow(
    val teamId: TeamId,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
)

data class CareerBoard(
    val titles: List<TitleRow>,
    val points: List<PointsRow>,
)
