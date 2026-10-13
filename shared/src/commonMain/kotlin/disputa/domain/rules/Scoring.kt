package disputa.domain.rules

import disputa.commons.DomainError
import disputa.commons.MatchId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase

object Scoring {
    fun knockoutPlayed(snapshot: ChampionshipSnapshot): Boolean =
        snapshot.matches.any {
            (it.phase == MatchPhase.Knockout || it.phase == MatchPhase.ThirdPlace) && it.isPlayed
        }

    fun applyScore(
        snapshot: ChampionshipSnapshot,
        matchId: MatchId,
        goalsA: Int,
        goalsB: Int,
        penaltyWinner: TeamId?,
    ): Outcome<ChampionshipSnapshot> {
        if (snapshot.championship.status == ChampionshipStatus.Closed) {
            return Outcome.Err(DomainError.ChampionshipClosed)
        }
        if (goalsA < 0 || goalsB < 0) return Outcome.Err(DomainError.InvalidScore)
        val match = snapshot.matches.find { it.id == matchId } ?: return Outcome.Err(DomainError.MatchNotFound)
        if (match.teamA == null || match.teamB == null) return Outcome.Err(DomainError.MatchNotFound)
        if (match.phase == MatchPhase.League || match.phase == MatchPhase.Group) {
            if (knockoutPlayed(snapshot)) return Outcome.Err(DomainError.PriorPhaseLocked)
        }
        if (match.phase == MatchPhase.Knockout || match.phase == MatchPhase.ThirdPlace) {
            if (goalsA == goalsB && penaltyWinner == null) {
                return Outcome.Err(DomainError.KnockoutDrawNeedsPenaltyWinner)
            }
            if (goalsA != goalsB && penaltyWinner != null) {
                return Outcome.Err(DomainError.InvalidScore)
            }
            if (penaltyWinner != null && penaltyWinner != match.teamA && penaltyWinner != match.teamB) {
                return Outcome.Err(DomainError.InvalidScore)
            }
        } else if (penaltyWinner != null) {
            return Outcome.Err(DomainError.InvalidScore)
        }
        var updated = snapshot.matches.map { item ->
            if (item.id == matchId) {
                item.copy(goalsA = goalsA, goalsB = goalsB, penaltyWinner = penaltyWinner)
            } else {
                item
            }
        }
        val scored = updated.first { it.id == matchId }
        updated = propagate(updated, scored)
        var next = snapshot.copy(matches = updated)
        if ((match.phase == MatchPhase.League || match.phase == MatchPhase.Group) &&
            snapshot.matches.any { it.phase == MatchPhase.Knockout || it.phase == MatchPhase.ThirdPlace } &&
            !knockoutPlayed(snapshot)
        ) {
            next = next.copy(
                matches = next.matches.filter { it.phase == MatchPhase.League || it.phase == MatchPhase.Group },
            )
        }
        return Outcome.Ok(next)
    }

    fun clearScore(snapshot: ChampionshipSnapshot, matchId: MatchId): Outcome<ChampionshipSnapshot> {
        if (snapshot.championship.status == ChampionshipStatus.Closed) {
            return Outcome.Err(DomainError.ChampionshipClosed)
        }
        val match = snapshot.matches.find { it.id == matchId } ?: return Outcome.Err(DomainError.MatchNotFound)
        if (match.phase == MatchPhase.League || match.phase == MatchPhase.Group) {
            if (knockoutPlayed(snapshot)) return Outcome.Err(DomainError.PriorPhaseLocked)
        }
        var updated = snapshot.matches.map { item ->
            if (item.id == matchId) item.copy(goalsA = null, goalsB = null, penaltyWinner = null) else item
        }
        if (match.phase == MatchPhase.Knockout) {
            updated = clearDependents(updated, match)
        }
        var next = snapshot.copy(matches = updated)
        if ((match.phase == MatchPhase.League || match.phase == MatchPhase.Group) &&
            snapshot.matches.any { it.phase == MatchPhase.Knockout || it.phase == MatchPhase.ThirdPlace } &&
            !knockoutPlayed(snapshot)
        ) {
            next = next.copy(
                matches = next.matches.filter { it.phase == MatchPhase.League || it.phase == MatchPhase.Group },
            )
        }
        return Outcome.Ok(next)
    }

    fun winner(match: Match): TeamId? {
        if (!match.isPlayed || match.teamA == null || match.teamB == null) return null
        val ga = match.goalsA!!
        val gb = match.goalsB!!
        return when {
            ga > gb -> match.teamA
            gb > ga -> match.teamB
            else -> match.penaltyWinner
        }
    }

    fun loser(match: Match): TeamId? {
        val win = winner(match) ?: return null
        return if (win == match.teamA) match.teamB else match.teamA
    }

    private fun propagate(matches: List<Match>, scored: Match): List<Match> {
        if (scored.phase != MatchPhase.Knockout) return matches
        val win = winner(scored) ?: return matches
        val lose = loser(scored)
        val nextRound = scored.roundIndex + 1
        val nextSlot = scored.slot / 2
        val asTeamA = scored.slot % 2 == 0
        val maxRound = matches.filter { it.phase == MatchPhase.Knockout }.maxOf { it.roundIndex }
        var result = matches.map { match ->
            if (match.phase == MatchPhase.Knockout && match.roundIndex == nextRound && match.slot == nextSlot) {
                if (asTeamA) match.copy(teamA = win, goalsA = null, goalsB = null, penaltyWinner = null)
                else match.copy(teamB = win, goalsA = null, goalsB = null, penaltyWinner = null)
            } else {
                match
            }
        }
        if (scored.roundIndex == maxRound - 1 && lose != null) {
            val third = result.find { it.phase == MatchPhase.ThirdPlace } ?: return result
            val asA = scored.slot == 0
            result = result.map { match ->
                if (match.id != third.id) match
                else if (asA) match.copy(teamA = lose, goalsA = null, goalsB = null, penaltyWinner = null)
                else match.copy(teamB = lose, goalsA = null, goalsB = null, penaltyWinner = null)
            }
        }
        return result
    }

    private fun clearDependents(matches: List<Match>, from: Match): List<Match> {
        val maxRound = matches.filter { it.phase == MatchPhase.Knockout }.maxOfOrNull { it.roundIndex } ?: return matches
        return matches.map { match ->
            val laterKo = match.phase == MatchPhase.Knockout && match.roundIndex > from.roundIndex &&
                match.slot == from.slot / (1 shl (match.roundIndex - from.roundIndex))
            val thirdFromSemi = match.phase == MatchPhase.ThirdPlace && from.roundIndex == maxRound - 1
            if (laterKo || thirdFromSemi) {
                match.copy(
                    teamA = if (laterKo && match.roundIndex > from.roundIndex + 1) null else match.teamA,
                    teamB = if (laterKo && match.roundIndex > from.roundIndex + 1) null else match.teamB,
                    goalsA = null,
                    goalsB = null,
                    penaltyWinner = null,
                )
            } else {
                match
            }
        }
    }
}
