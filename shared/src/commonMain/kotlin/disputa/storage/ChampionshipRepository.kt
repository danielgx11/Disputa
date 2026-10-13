package disputa.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import disputa.commons.ChampionshipId
import disputa.commons.MatchId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.ChampionshipEngine
import disputa.domain.model.Championship
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.Match
import disputa.domain.rules.ChampionshipDraft
import disputa.domain.rules.SetupRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChampionshipRepository(
    private val db: DisputaDatabase,
) {
    fun observe(): Flow<List<Championship>> =
        db.disputaQueries.selectChampionships().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    fun observeClosedSnapshots(): Flow<List<ChampionshipSnapshot>> =
        observe().map { list ->
            list.filter { it.status == disputa.domain.model.ChampionshipStatus.Closed }
                .mapNotNull { getSnapshot(it.id) }
        }

    fun getSnapshot(id: ChampionshipId): ChampionshipSnapshot? {
        val championship = db.disputaQueries.selectChampionship(id.value).executeAsOneOrNull()?.toDomain() ?: return null
        val teams = db.disputaQueries.selectChampionshipTeams(id.value).executeAsList().map { TeamId(it) }
        val groups = db.disputaQueries.selectGroups(id.value).executeAsList().toGroups()
        val matches = db.disputaQueries.selectMatches(id.value).executeAsList().map { it.toDomain() }
        return ChampionshipSnapshot(championship, teams, groups, matches)
    }

    fun create(draft: ChampionshipDraft): Outcome<ChampionshipSnapshot> {
        return when (val created = SetupRules.create(draft)) {
            is Outcome.Err -> created
            is Outcome.Ok -> {
                persist(created.value)
                created
            }
        }
    }

    fun generateFixtures(id: ChampionshipId): Outcome<ChampionshipSnapshot> = mutate(id) {
        ChampionshipEngine.generatePhaseFixtures(it)
    }

    fun generateKnockout(id: ChampionshipId, extraIds: List<TeamId>): Outcome<ChampionshipSnapshot> =
        mutate(id) { ChampionshipEngine.generateKnockout(it, extraIds) }

    fun applyScore(
        id: ChampionshipId,
        matchId: MatchId,
        goalsA: Int,
        goalsB: Int,
        penaltyWinner: TeamId?,
    ): Outcome<ChampionshipSnapshot> = mutate(id) {
        ChampionshipEngine.applyScore(it, matchId, goalsA, goalsB, penaltyWinner)
    }

    fun clearScore(id: ChampionshipId, matchId: MatchId): Outcome<ChampionshipSnapshot> =
        mutate(id) { ChampionshipEngine.clearScore(it, matchId) }

    fun close(id: ChampionshipId): Outcome<ChampionshipSnapshot> =
        mutate(id) { ChampionshipEngine.close(it) }

    fun delete(id: ChampionshipId) {
        db.transaction {
            db.disputaQueries.deleteMatchesForChampionship(id.value)
            db.disputaQueries.clearGroups(id.value)
            db.disputaQueries.clearChampionshipTeams(id.value)
            db.disputaQueries.deleteChampionship(id.value)
        }
    }

    private fun mutate(
        id: ChampionshipId,
        transform: (ChampionshipSnapshot) -> Outcome<ChampionshipSnapshot>,
    ): Outcome<ChampionshipSnapshot> {
        val current = getSnapshot(id) ?: return Outcome.Err(disputa.commons.DomainError.MatchNotFound)
        return when (val next = transform(current)) {
            is Outcome.Err -> next
            is Outcome.Ok -> {
                persist(next.value)
                next
            }
        }
    }

    private fun persist(snapshot: ChampionshipSnapshot) {
        val c = snapshot.championship
        db.transaction {
            db.disputaQueries.insertChampionship(
                id = c.id.value,
                name = c.name,
                date = c.date.toString(),
                format = c.format.name,
                status = c.status.name,
                qualifier_count = c.qualifierCount?.toLong(),
                third_place_enabled = if (c.thirdPlaceEnabled) 1 else 0,
                fixture_seed = c.fixtureSeed,
                leg_count = c.legCount.toLong(),
                knockout_seeding = c.knockoutSeeding.name,
            )
            db.disputaQueries.clearChampionshipTeams(c.id.value)
            snapshot.teamIds.forEach { db.disputaQueries.insertChampionshipTeam(c.id.value, it.value) }
            db.disputaQueries.clearGroups(c.id.value)
            snapshot.groups.forEach { group ->
                group.teamIds.forEach { teamId ->
                    db.disputaQueries.insertGroupMember(c.id.value, group.id.value, group.name, teamId.value)
                }
            }
            db.disputaQueries.clearMatches(c.id.value)
            snapshot.matches.forEach { persistMatch(it) }
        }
    }

    private fun persistMatch(match: Match) {
        db.disputaQueries.insertMatch(
            id = match.id.value,
            championship_id = match.championshipId.value,
            phase = match.phase.name,
            group_id = match.groupId?.value,
            round_index = match.roundIndex.toLong(),
            slot = match.slot.toLong(),
            team_a = match.teamA?.value,
            team_b = match.teamB?.value,
            goals_a = match.goalsA?.toLong(),
            goals_b = match.goalsB?.toLong(),
            penalty_winner = match.penaltyWinner?.value,
        )
    }
}
