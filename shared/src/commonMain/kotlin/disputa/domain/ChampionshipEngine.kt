package disputa.domain

import disputa.commons.DomainError
import disputa.commons.MatchId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.MatchPhase
import disputa.domain.rules.Career
import disputa.domain.rules.Knockout
import disputa.domain.rules.RoundRobin
import disputa.domain.rules.Scoring
import disputa.domain.rules.Standings

object ChampionshipEngine {
    fun generatePhaseFixtures(snapshot: ChampionshipSnapshot): Outcome<ChampionshipSnapshot> {
        if (snapshot.championship.status == ChampionshipStatus.Closed) {
            return Outcome.Err(DomainError.ChampionshipClosed)
        }
        val existing = snapshot.matches.filter { it.phase == MatchPhase.League || it.phase == MatchPhase.Group }
        if (existing.any { it.isPlayed }) return Outcome.Err(DomainError.FixturesAlreadyPlayed)
        val generated = when (snapshot.championship.format) {
            ChampionshipFormat.LeagueOnly, ChampionshipFormat.LeagueThenKnockout -> {
                RoundRobin.generate(
                    championshipId = snapshot.championship.id,
                    teamIds = snapshot.teamIds,
                    seed = snapshot.championship.fixtureSeed,
                    phase = MatchPhase.League,
                )
            }
            ChampionshipFormat.GroupsThenKnockout -> {
                snapshot.groups.flatMapIndexed { index, group ->
                    RoundRobin.generate(
                        championshipId = snapshot.championship.id,
                        teamIds = group.teamIds,
                        seed = snapshot.championship.fixtureSeed + index,
                        phase = MatchPhase.Group,
                        groupId = group.id,
                    )
                }
            }
            ChampionshipFormat.KnockoutOnly -> emptyList()
        }
        val keepKo = snapshot.matches.filter { it.phase == MatchPhase.Knockout || it.phase == MatchPhase.ThirdPlace }
        return Outcome.Ok(snapshot.copy(matches = generated + keepKo))
    }

    fun generateKnockout(snapshot: ChampionshipSnapshot, extraIds: List<TeamId> = emptyList()): Outcome<ChampionshipSnapshot> {
        if (snapshot.championship.status == ChampionshipStatus.Closed) {
            return Outcome.Err(DomainError.ChampionshipClosed)
        }
        if (Scoring.knockoutPlayed(snapshot)) return Outcome.Err(DomainError.KnockoutAlreadyStarted)
        if (snapshot.championship.format == ChampionshipFormat.LeagueOnly) {
            return Outcome.Err(DomainError.BracketNotPowerOfTwo)
        }
        val seeds = when (val result = Knockout.orderedQualifiers(snapshot, extraIds)) {
            is Outcome.Err -> return result
            is Outcome.Ok -> result.value
        }
        val groupOf = if (snapshot.championship.format == ChampionshipFormat.GroupsThenKnockout) {
            Knockout.groupOf(snapshot)
        } else {
            emptyMap()
        }
        val bracket = when (
            val result = Knockout.generateFromSeeds(
                championshipId = snapshot.championship.id,
                seeds = seeds,
                thirdPlace = snapshot.championship.thirdPlaceEnabled,
                groupOf = groupOf,
            )
        ) {
            is Outcome.Err -> return result
            is Outcome.Ok -> result.value
        }
        val prior = snapshot.matches.filter { it.phase == MatchPhase.League || it.phase == MatchPhase.Group }
        return Outcome.Ok(snapshot.copy(matches = prior + bracket))
    }

    fun applyScore(
        snapshot: ChampionshipSnapshot,
        matchId: MatchId,
        goalsA: Int,
        goalsB: Int,
        penaltyWinner: TeamId?,
    ) = Scoring.applyScore(snapshot, matchId, goalsA, goalsB, penaltyWinner)

    fun clearScore(snapshot: ChampionshipSnapshot, matchId: MatchId) =
        Scoring.clearScore(snapshot, matchId)

    fun close(snapshot: ChampionshipSnapshot): Outcome<ChampionshipSnapshot> {
        if (snapshot.championship.status == ChampionshipStatus.Closed) {
            return Outcome.Ok(snapshot)
        }
        when (snapshot.championship.format) {
            ChampionshipFormat.LeagueOnly -> {
                val table = Standings.compute(
                    snapshot.teamIds,
                    snapshot.matches.filter { it.phase == MatchPhase.League },
                )
                if (Standings.uniqueFirst(table) == null) {
                    return Outcome.Err(
                        DomainError.ChampionUndefined,
                        tiedTeamIds = table.rows.take(2).map { it.teamId.value },
                    )
                }
            }
            else -> {
                val finalMatch = snapshot.matches
                    .filter { it.phase == MatchPhase.Knockout }
                    .maxByOrNull { it.roundIndex }
                    ?: return Outcome.Err(DomainError.ChampionUndefined)
                if (Scoring.winner(finalMatch) == null) return Outcome.Err(DomainError.ChampionUndefined)
            }
        }
        if (snapshot.championship.thirdPlaceEnabled) {
            val third = snapshot.matches.find { it.phase == MatchPhase.ThirdPlace }
            if (third == null || Scoring.winner(third) == null) {
                return Outcome.Err(DomainError.ThirdPlaceIncomplete)
            }
        }
        return Outcome.Ok(
            snapshot.copy(
                championship = snapshot.championship.copy(status = ChampionshipStatus.Closed),
            ),
        )
    }

    fun careerChampion(snapshot: ChampionshipSnapshot) = Career.championOf(snapshot)
}
