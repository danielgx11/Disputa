package disputa.domain

import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.MatchPhase
import disputa.domain.rules.Career
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringAndCareerTest {
    @Test
    fun penaltyWinnerDoesNotAddGoalsAndKnockoutAddsNoPoints() {
        val snapshot = snapshot(
            ChampionshipFormat.KnockoutOnly,
            listOf("A", "B", "C", "D"),
            thirdPlace = true,
        )
        val withKo = (ChampionshipEngine.generateKnockout(snapshot) as Outcome.Ok).value
        val semis = withKo.matches.filter { it.phase == MatchPhase.Knockout && it.roundIndex == 0 }
        var current = withKo
        semis.forEachIndexed { index, match ->
            current = (ChampionshipEngine.applyScore(current, match.id, 1, 0, null) as Outcome.Ok).value
        }
        val finalMatch = current.matches.first { it.phase == MatchPhase.Knockout && it.roundIndex == 1 }
        val rejected = ChampionshipEngine.applyScore(current, finalMatch.id, 2, 2, null)
        assertIs<Outcome.Err>(rejected)
        assertEquals(DomainError.KnockoutDrawNeedsPenaltyWinner, rejected.error)
        current = (ChampionshipEngine.applyScore(current, finalMatch.id, 2, 2, finalMatch.teamA) as Outcome.Ok).value
        val third = current.matches.first { it.phase == MatchPhase.ThirdPlace }
        current = (ChampionshipEngine.applyScore(current, third.id, 1, 0, null) as Outcome.Ok).value
        current = (ChampionshipEngine.close(current) as Outcome.Ok).value
        val teams = listOf("A", "B", "C", "D").map { namedTeam(it) }
        val board = Career.board(teams, listOf(current))
        val winner = Career.championOf(current)!!
        assertEquals(1, board.titles.first { it.teamId == winner }.titles)
        assertTrue(board.titles.filter { it.teamId != winner }.all { it.titles == 0 })
        assertTrue(board.points.all { it.points == 0 })
        val winnerPoints = board.points.first { it.teamId == winner }
        assertEquals(3, winnerPoints.goalsFor)
        assertEquals(2, winnerPoints.goalsAgainst)
    }

    @Test
    fun priorPhaseLocksAndUnusedBracketIsDiscarded() {
        val snapshot = snapshot(
            ChampionshipFormat.LeagueThenKnockout,
            listOf("A", "B", "C", "D"),
            qualifierCount = 4,
        )
        var current = (ChampionshipEngine.generatePhaseFixtures(snapshot) as Outcome.Ok).value
        current.matches.forEach { match ->
            current = (ChampionshipEngine.applyScore(current, match.id, 1, 0, null) as Outcome.Ok).value
        }
        current = (ChampionshipEngine.generateKnockout(current) as Outcome.Ok).value
        assertTrue(current.matches.any { it.phase == MatchPhase.Knockout })
        val league = current.matches.first { it.phase == MatchPhase.League }
        current = (ChampionshipEngine.clearScore(current, league.id) as Outcome.Ok).value
        assertTrue(current.matches.none { it.phase == MatchPhase.Knockout })
        current.matches.filter { it.phase == MatchPhase.League && !it.isPlayed }.forEach { match ->
            current = (ChampionshipEngine.applyScore(current, match.id, 2, 1, null) as Outcome.Ok).value
        }
        current = (ChampionshipEngine.generateKnockout(current) as Outcome.Ok).value
        val semi = current.matches.first { it.phase == MatchPhase.Knockout }
        current = (ChampionshipEngine.applyScore(current, semi.id, 1, 0, null) as Outcome.Ok).value
        val leagueAgain = current.matches.first { it.phase == MatchPhase.League }
        val locked = ChampionshipEngine.applyScore(current, leagueAgain.id, 9, 0, null)
        assertIs<Outcome.Err>(locked)
        assertEquals(DomainError.PriorPhaseLocked, locked.error)
    }

    @Test
    fun openChampionshipIsIgnoredByCareer() {
        val snapshot = snapshot(ChampionshipFormat.KnockoutOnly, listOf("A", "B"))
        val withKo = (ChampionshipEngine.generateKnockout(snapshot) as Outcome.Ok).value
        val match = withKo.matches.first()
        val played = (ChampionshipEngine.applyScore(withKo, match.id, 1, 0, null) as Outcome.Ok).value
        val board = Career.board(listOf(namedTeam("A"), namedTeam("B")), listOf(played))
        assertTrue(board.titles.all { it.titles == 0 })
        assertNull(Career.championOf(played))
        assertEquals(TeamId("A"), disputa.domain.rules.Scoring.winner(played.matches.first()))
    }
}
