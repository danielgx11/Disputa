package disputa.storage

import disputa.commons.ChampionshipId
import disputa.commons.GroupId
import disputa.commons.MatchId
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.Group
import disputa.domain.model.KnockoutSeeding
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase
import disputa.domain.model.Team as DomainTeam
import disputa.domain.model.Championship as DomainChampionship
import kotlinx.datetime.LocalDate

internal fun Team.toDomain() = DomainTeam(
    id = TeamId(id),
    name = name,
    about = description,
    photoRelativePath = photo_relative_path,
)

internal fun Championship.toDomain() = DomainChampionship(
    id = ChampionshipId(id),
    name = name,
    date = LocalDate.parse(date),
    format = ChampionshipFormat.valueOf(format),
    status = ChampionshipStatus.valueOf(status),
    qualifierCount = qualifier_count?.toInt(),
    thirdPlaceEnabled = third_place_enabled != 0L,
    fixtureSeed = fixture_seed,
    legCount = leg_count.toInt(),
    knockoutSeeding = KnockoutSeeding.valueOf(knockout_seeding),
)

internal fun MatchEntity.toDomain() = Match(
    id = MatchId(id),
    championshipId = ChampionshipId(championship_id),
    phase = MatchPhase.valueOf(phase),
    groupId = group_id?.let(::GroupId),
    roundIndex = round_index.toInt(),
    slot = slot.toLong().toInt(),
    teamA = team_a?.let(::TeamId),
    teamB = team_b?.let(::TeamId),
    goalsA = goals_a?.toInt(),
    goalsB = goals_b?.toInt(),
    penaltyWinner = penalty_winner?.let(::TeamId),
)

internal fun List<GroupMembership>.toGroups(): List<Group> =
    groupBy { it.group_id }.map { (id, rows) ->
        Group(
            id = GroupId(id),
            name = rows.first().group_name,
            teamIds = rows.map { TeamId(it.team_id) },
        )
    }
